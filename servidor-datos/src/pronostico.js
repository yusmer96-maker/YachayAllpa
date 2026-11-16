'use strict';

/**
 * Pronostico real de los proximos 7 dias por celda: lluvia (mm y probabilidad)
 * y temperatura minima, de Open-Meteo (combina modelos como ECMWF y GFS; gratis
 * y sin clave para uso no comercial).
 *
 * Se pide a la altura de la celda NASA (`elevation`), la misma referencia que
 * usa la app para la helada: el telefono corrige luego la minima a la altura de
 * la chacra con el mismo gradiente (-6,5 C por km). Es pronostico, no
 * observacion: la app lo muestra separado de la lluvia medida de NASA.
 */

const { pedir } = require('./power');

const BASE = 'https://api.open-meteo.com/v1/forecast';
const DIAS = 7;

async function pronosticar(lat, lon, elev) {
  const url = `${BASE}?latitude=${lat}&longitude=${lon}&elevation=${Math.round(elev)}`
    + '&daily=precipitation_sum,precipitation_probability_max,temperature_2m_min,temperature_2m_max'
    + `&timezone=America%2FLima&forecast_days=${DIAS}`;
  const json = await pedir(url);
  const d = json && json.daily;
  if (!d || !Array.isArray(d.time) || d.time.length === 0) throw new Error('Open-Meteo sin datos');
  const redondo = (v, n = 1) => (Number.isFinite(v) ? Math.round(v * 10 ** n) / 10 ** n : null);
  const dias = d.time.map((fecha, i) => ({
    fecha,
    lluvia: redondo(d.precipitation_sum?.[i]),
    prob: Number.isFinite(d.precipitation_probability_max?.[i]) ? d.precipitation_probability_max[i] : null,
    tmin: redondo(d.temperature_2m_min?.[i]),
    tmax: redondo(d.temperature_2m_max?.[i]),
  }));
  // Un dia sin lluvia ni minima no sirve: mejor no publicar que publicar huecos.
  if (dias.some((x) => x.lluvia === null || x.tmin === null)) throw new Error('Open-Meteo con huecos');
  return {
    fuente: 'Open-Meteo (modelos ECMWF/GFS)',
    emitido: new Date().toISOString(),
    elevacion: Math.round(elev),
    dias,
  };
}

module.exports = { pronosticar };
