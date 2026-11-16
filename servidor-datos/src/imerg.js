'use strict';

/**
 * Lluvia reciente medida por satelite: NASA GPM IMERG (Late Run, diario, 0,1 grados).
 *
 * NASA POWER solo es fiable hasta el fin del mes de hace dos meses (MERRA-2);
 * IMERG Late llega con un dia de retraso. De cada dia se baja SOLO el recorte de
 * Huanuco por OPeNDAP (unas 1500 celdas, no el archivo mundial) y se promedian
 * las celdas IMERG que caen dentro de cada celda POWER de la app.
 *
 * Acceso con token de NASA Earthdata (fichero .earthdata junto a actualizar.js,
 * solo legible por el servicio). Sin token o sin red, la app se queda con POWER.
 */

const fs = require('fs');
const path = require('path');

const BASE = 'https://gpm1.gesdisc.eosdis.nasa.gov';
const PRODUCTO = 'GPM_3IMERGDL.07';     // diario, Late Run: 1 dia de retraso
const MENSUAL = 'GPM_3IMERGM.07';       // mensual, Final (ajustado con pluviometros)
const NORMAL_DESDE = 2011;              // los mismos 15 anos que el pack de la app
const NORMAL_HASTA = 2025;
const MEDIO_LAT = 0.25;      // celda POWER: 0,5 grados de latitud
const MEDIO_LON = 0.3125;    // y 0,625 de longitud
const PASO = 0.1;            // malla IMERG

function token() {
  const ruta = process.env.EARTHDATA_TOKEN_FILE || path.join(__dirname, '..', '.earthdata');
  try { return fs.readFileSync(ruta, 'utf8').trim(); } catch (e) { return null; }
}

/** GES DISC responde a veces 401/429/5xx sueltos: hasta 4 intentos con espera creciente. */
async function pedir(url, tk) {
  let ultimo;
  for (let intento = 0; intento < 4; intento++) {
    if (intento) await new Promise((ok) => setTimeout(ok, 5000 * intento));
    try {
      const r = await fetch(url, { headers: { Authorization: `Bearer ${tk}` }, redirect: 'follow',
        signal: AbortSignal.timeout(60000) });
      if (r.ok) return r.text();
      ultimo = new Error(`IMERG HTTP ${r.status} en ${url.split('?')[0].split('/').pop()}`);
      if (r.status === 404) break;
    } catch (e) {
      ultimo = e;
    }
  }
  throw ultimo;
}

const indiceLon = (lon) => Math.floor((lon + 180) / PASO);
const indiceLat = (lat) => Math.floor((lat + 90) / PASO);

/** Recorte que cubre todas las zonas, en indices de la malla IMERG. */
function recorte(zonas) {
  return {
    lon0: indiceLon(Math.min(...zonas.map((z) => z.lon)) - MEDIO_LON),
    lon1: indiceLon(Math.max(...zonas.map((z) => z.lon)) + MEDIO_LON),
    lat0: indiceLat(Math.min(...zonas.map((z) => z.lat)) - MEDIO_LAT),
    lat1: indiceLat(Math.max(...zonas.map((z) => z.lat)) + MEDIO_LAT),
  };
}

/**
 * Respuesta .ascii de OPeNDAP -> [{lon, lat, v}]; descarta los valores de relleno.
 * Entiende las dos formas: la del diario (Grid, "precipitation.precipitation[..][lon=X]")
 * y la del mensual (arreglo suelto, "precipitation[0][i]" con "lat," y "lon," aparte).
 */
function leerAscii(texto) {
  const lineas = texto.split('\n');
  const valores = (l) => l.split(',').slice(1).map(Number);
  const latLinea = lineas.find((l) => /^(precipitation\.)?lat,/.test(l));
  const lonLinea = lineas.find((l) => /^lon,/.test(l));
  if (!latLinea) throw new Error('IMERG: respuesta sin latitudes');
  const lats = valores(latLinea);
  const lons = lonLinea ? valores(lonLinea) : [];
  const puntos = [];
  for (const l of lineas) {
    let lon;
    const grid = l.match(/^precipitation\.precipitation\[.*lon=([-\d.]+)\],/);
    const suelto = l.match(/^precipitation\[0\]\[(\d+)\],/);
    if (grid) lon = Number(grid[1]);
    else if (suelto) lon = lons[Number(suelto[1])];
    else continue;
    valores(l).forEach((v, i) => {
      if (Number.isFinite(v) && v >= 0 && Number.isFinite(lon)) puntos.push({ lon, lat: lats[i], v });
    });
  }
  return puntos;
}

/** Media de los puntos IMERG dentro de la celda POWER de cada zona. */
function porZona(puntos, zonas) {
  const r = {};
  for (const z of zonas) {
    const dentro = puntos.filter((p) => Math.abs(p.lat - z.lat) < MEDIO_LAT
      && Math.abs(p.lon - z.lon) < MEDIO_LON);
    if (dentro.length) r[z.id] = dentro.reduce((s, p) => s + p.v, 0) / dentro.length;
  }
  return r;
}

const consulta = (r) => `precipitation[0][${r.lon0}:${r.lon1}][${r.lat0}:${r.lat1}],`
  + `lat[${r.lat0}:${r.lat1}],lon[${r.lon0}:${r.lon1}]`;

/** Nombre del archivo de un dia (la letra de version cambia: V07B, V07C...). */
const listados = new Map();
async function archivoDelDia(fecha, tk) {
  const carpeta = `${BASE}/data/GPM_L3/${PRODUCTO}/${fecha.slice(0, 4)}/${fecha.slice(4, 6)}/`;
  if (!listados.has(carpeta)) listados.set(carpeta, await pedir(carpeta, tk));
  const m = listados.get(carpeta).match(
    new RegExp(`3B-DAY-L\\.MS\\.MRG\\.3IMERG\\.${fecha}-S000000-E235959\\.V07[A-Z]\\.nc4`));
  return m ? `${BASE}/opendap/GPM_L3/${PRODUCTO}/${fecha.slice(0, 4)}/${fecha.slice(4, 6)}/${m[0]}` : null;
}

/** Lluvia de un dia en cada zona (mm), o null si NASA aun no lo publica. */
async function unDia(fecha, zonas, tk) {
  const url = await archivoDelDia(fecha, tk);
  if (!url) return null;
  const medias = porZona(leerAscii(await pedir(`${url}.ascii?${consulta(recorte(zonas))}`, tk)), zonas);
  const dia = {};
  for (const id of Object.keys(medias)) dia[id] = Math.round(medias[id] * 10) / 10;
  return dia;
}

/**
 * Pone al dia la cache (fecha AAAAMMDD -> {zona: mm}) para los `dias` ultimos
 * hasta ayer. Devuelve la cache. Los dias que NASA aun no publica se reintentan
 * en la siguiente vuelta.
 */
async function actualizar(rutaCache, zonas, hoy, dias) {
  const tk = token();
  let cache = {};
  try { cache = JSON.parse(fs.readFileSync(rutaCache, 'utf8')); } catch (e) { /* primera vez */ }
  if (!tk) { console.log('IMERG: sin token de Earthdata, se omite'); return cache; }
  const quiero = [];
  for (let k = dias; k >= 1; k--) {
    const d = new Date(Date.UTC(hoy.getUTCFullYear(), hoy.getUTCMonth(), hoy.getUTCDate() - k));
    quiero.push(d.toISOString().slice(0, 10).replace(/-/g, ''));
  }
  let nuevos = 0;
  for (const f of quiero) {
    if (cache[f]) continue;
    try {
      const dia = await unDia(f, zonas, tk);
      if (dia) { cache[f] = dia; nuevos++; }
    } catch (e) {
      console.error(`IMERG ${f}: ${e.message}`);
    }
  }
  for (const f of Object.keys(cache)) if (!quiero.includes(f)) delete cache[f];
  fs.writeFileSync(rutaCache, JSON.stringify(cache));
  console.log(`IMERG: +${nuevos} dias, ${Object.keys(cache).length} en cache`);
  return cache;
}

/**
 * Lo normal segun el propio IMERG: lluvia media por dia de cada mes en 15 anos, con el
 * producto mensual Final. Cada mes toma los 15 anos mas recientes publicados (2011-2025;
 * si el Final de 2025 aun no salio, ese mes usa 2010-2024). Asi "lo normal" y "lo que
 * llovio" vienen de la misma fuente. Cada mes bajado se guarda: no se pide dos veces.
 * Devuelve {zona: [12 valores mm/dia]}.
 */
async function normales(rutaCache, zonas) {
  try {
    const c = JSON.parse(fs.readFileSync(rutaCache, 'utf8'));
    if (zonas.every((z) => Array.isArray(c[z.id]) && c[z.id].length === 12)) return c;
  } catch (e) { /* se calcula */ }
  const tk = token();
  if (!tk) return null;
  const rutaMeses = rutaCache.replace(/\.json$/, '_meses.json');
  let meses = {};
  try { meses = JSON.parse(fs.readFileSync(rutaMeses, 'utf8')); } catch (e) { /* primera vez */ }
  const listados = {};
  const anos = {};
  for (let m = 1; m <= 12; m++) {
    const mm = String(m).padStart(2, '0');
    anos[m] = [];
    for (let a = NORMAL_HASTA; a >= NORMAL_DESDE - 3 && anos[m].length < 15; a--) {
      const clave = `${a}${mm}`;
      if (!meses[clave]) {
        if (listados[a] === undefined) {
          try { listados[a] = await pedir(`${BASE}/data/GPM_L3/${MENSUAL}/${a}/`, tk); } catch (e) { listados[a] = ''; }
        }
        const f = listados[a].match(new RegExp(`3B-MO\\.MS\\.MRG\\.3IMERG\\.${a}${mm}01-S000000-E235959\\.${mm}\\.V07[A-Z]\\.HDF5(?!\\.)`));
        if (!f) continue;
        const url = `${BASE}/opendap/GPM_L3/${MENSUAL}/${a}/${f[0]}`;
        // El mensual viene en mm/h: por 24 da mm por dia.
        const medias = porZona(leerAscii(await pedir(`${url}.ascii?${consulta(recorte(zonas))}`, tk)), zonas);
        for (const id of Object.keys(medias)) medias[id] *= 24;
        meses[clave] = medias;
        fs.writeFileSync(rutaMeses, JSON.stringify(meses));
      }
      anos[m].push(clave);
    }
    if (anos[m].length < 15) throw new Error(`IMERG mensual: solo ${anos[m].length} anos para el mes ${mm}`);
    console.log(`IMERG normales: mes ${mm} con ${anos[m][anos[m].length - 1].slice(0, 4)}-${anos[m][0].slice(0, 4)}`);
  }
  const r = {};
  for (const z of zonas) {
    r[z.id] = [];
    for (let m = 1; m <= 12; m++) {
      const v = anos[m].map((k) => meses[k][z.id]).filter(Number.isFinite);
      r[z.id].push(Math.round(v.reduce((a, b) => a + b, 0) / v.length * 100) / 100);
    }
  }
  fs.writeFileSync(rutaCache, JSON.stringify(r));
  console.log(`IMERG: normales de 15 anos calculadas para ${Object.keys(r).length} zonas`);
  return r;
}

module.exports = { actualizar, normales, leerAscii, recorte };
