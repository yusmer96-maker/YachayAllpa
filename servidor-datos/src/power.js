'use strict';

/**
 * Descarga de NASA POWER, una peticion por celda y de forma incremental.
 *
 * NASA avisa contra pedir lo mismo muchas veces: por eso esto vive en el VPS y
 * no en cada telefono. La primera vez baja desde 1981; despues solo los dias
 * nuevos. Entre celdas se espera, para no castigar la API.
 */

const https = require('https');

const BASE = 'https://power.larc.nasa.gov/api/temporal/daily/point';
const PARAMETROS = 'PRECTOTCORR,T2M_MIN,T2M_MAX,GWETROOT,GWETTOP';
const FALTA = -999;

function aAAAAMMDD(d) {
  return `${d.getUTCFullYear()}${String(d.getUTCMonth() + 1).padStart(2, '0')}${String(d.getUTCDate()).padStart(2, '0')}`;
}

function pedir(url) {
  return new Promise((cumplir, fallar) => {
    const req = https.get(url, {
      headers: { 'User-Agent': 'yachay-allpa-datos/1.0', 'Accept': 'application/json' },
      timeout: 120000,
    }, (res) => {
      if (res.statusCode !== 200) {
        res.resume();
        fallar(new Error(`POWER devolvio ${res.statusCode}`));
        return;
      }
      let cuerpo = '';
      res.setEncoding('utf8');
      res.on('data', (t) => { cuerpo += t; });
      res.on('end', () => {
        try { cumplir(JSON.parse(cuerpo)); }
        catch (e) { fallar(new Error('respuesta ilegible de POWER')); }
      });
    });
    req.on('timeout', () => { req.destroy(); fallar(new Error('POWER no respondio a tiempo')); });
    req.on('error', fallar);
  });
}

/**
 * Trae los dias de una celda entre dos fechas (AAAAMMDD). Devuelve
 * { AAAAMMDD: { p, tmin, tmax, gr, gt } } saltando los -999.
 */
async function descargar(lat, lon, inicio, fin) {
  const url = `${BASE}?parameters=${PARAMETROS}&community=AG`
    + `&longitude=${lon}&latitude=${lat}&start=${inicio}&end=${fin}&format=JSON`;
  const json = await pedir(url);
  const p = json?.properties?.parameter;
  if (!p || !p.PRECTOTCORR) throw new Error('POWER sin parametros');
  const dias = {};
  for (const fecha of Object.keys(p.PRECTOTCORR)) {
    const fila = {
      p: p.PRECTOTCORR?.[fecha],
      tmin: p.T2M_MIN?.[fecha],
      tmax: p.T2M_MAX?.[fecha],
      gr: p.GWETROOT?.[fecha],
      gt: p.GWETTOP?.[fecha],
    };
    // Un dia sin lluvia utilizable no sirve para nada aguas abajo.
    if (fila.p === undefined || fila.p === FALTA) continue;
    for (const k of Object.keys(fila)) if (fila[k] === FALTA) fila[k] = null;
    dias[fecha] = fila;
  }
  return dias;
}

const esperar = (ms) => new Promise((r) => setTimeout(r, ms));

module.exports = { descargar, aAAAAMMDD, esperar, pedir, PARAMETROS };
