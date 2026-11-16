'use strict';

/**
 * yachay-datos: el trabajo diario del VPS.
 *
 * Publica, por cada una de las 23 celdas, un JSON pequeno con:
 *  - reciente: la lluvia MEDIDA por satelite hasta ayer (NASA GPM IMERG Late) en
 *    las ultimas 6 semanas y en 7, 15 y 30 dias, cada cifra con su normal de la
 *    misma fuente (IMERG Final mensual 2011-2025);
 *  - pronostico: los proximos 7 dias (Open-Meteo).
 *
 * La historia de 15 campanas con la que se calcula el agua de cada cultivo va en
 * los packs firmados de la app (NASA POWER); aqui solo va lo que cambia cada dia.
 * Todo es cuantitativo; la frase en palabras la pone la app.
 */

const fs = require('fs');
const path = require('path');
const { aAAAAMMDD, esperar } = require('./src/power');
const { pronosticar } = require('./src/pronostico');
const imerg = require('./src/imerg');

const RAIZ = process.env.DATOS_DIR || '/opt/yachayallpa/datos';
const PUBLICO = path.join(RAIZ, 'publico');
const PAUSA_MS = Number(process.env.PAUSA_MS || 1500);
const SEMANAS = 6;

const zonas = JSON.parse(fs.readFileSync(path.join(__dirname, 'zonas.json'), 'utf8'));

function guardar(ruta, obj) {
  const tmp = `${ruta}.tmp`;
  fs.writeFileSync(tmp, JSON.stringify(obj));
  fs.renameSync(tmp, ruta);
}

/** Fechas AAAAMMDD de los `n` dias que terminan en `fin` (AAAAMMDD). */
function diasHasta(fin, n) {
  const d = new Date(Date.UTC(+fin.slice(0, 4), +fin.slice(4, 6) - 1, +fin.slice(6, 8)));
  const r = [];
  for (let k = n - 1; k >= 0; k--) {
    const x = new Date(d); x.setUTCDate(d.getUTCDate() - k);
    r.push(aAAAAMMDD(x));
  }
  return r;
}

/** Lluvia normal de esas fechas segun IMERG 2011-2025: suma de la media diaria de cada mes. */
function normalDe(fechas, normalZona) {
  if (!normalZona) return null;
  const s = fechas.reduce((t, f) => t + normalZona[+f.slice(4, 6) - 1], 0);
  return Math.round(s * 10) / 10;
}

/**
 * Lluvia medida hasta ayer (IMERG) y su normal de la misma fuente: las 6 semanas,
 * los ultimos 3 (inicio de siembra), 7, 15 y 30 dias. Una suma con mas de un dia sin dato no se publica (null).
 */
function reciente(id, cache, normalZona) {
  const fechas = Object.keys(cache).filter((f) => cache[f][id] !== undefined).sort();
  if (fechas.length < 30 || !normalZona) return null;
  const fin = fechas[fechas.length - 1];
  const suma = (lista) => {
    const v = lista.map((f) => cache[f] && cache[f][id]).filter((x) => x !== undefined);
    return v.length >= lista.length - 1 ? Math.round(v.reduce((a, b) => a + b, 0) * 10) / 10 : null;
  };
  const iso = (f) => `${f.slice(0, 4)}-${f.slice(4, 6)}-${f.slice(6)}`;
  const semanas = [];
  for (let i = SEMANAS - 1; i >= 0; i--) {
    const finSemana = diasHasta(fin, 7 * i + 1)[0];
    const dias = diasHasta(finSemana, 7);
    semanas.push({ fin: iso(finSemana), mm: suma(dias), normal: normalDe(dias, normalZona) });
  }
  return {
    fuente: 'NASA GPM IMERG V07: Late diario (lo medido) y Final mensual, 15 anos por mes 2010-2025 (lo normal)',
    hasta: iso(fin),
    lluvia3: suma(diasHasta(fin, 3)),
    lluvia7: suma(diasHasta(fin, 7)),
    lluvia15: suma(diasHasta(fin, 15)),
    lluvia30: suma(diasHasta(fin, 30)),
    normal30: normalDe(diasHasta(fin, 30), normalZona),
    semanas,
  };
}

async function principal() {
  fs.mkdirSync(PUBLICO, { recursive: true });
  const soloUna = process.argv[2];
  const lista = soloUna ? zonas.filter((z) => z.id === soloUna) : zonas;
  // Lluvia de satelite: si falla, cada zona se publica solo con el pronostico.
  let cache = {}, normales = null;
  try {
    normales = await imerg.normales(path.join(RAIZ, 'imerg_normal.json'), zonas);
    cache = await imerg.actualizar(path.join(RAIZ, 'imerg.json'), zonas, new Date(), 7 * (SEMANAS + 2));
  } catch (e) {
    console.error(`IMERG: ${e.message}`);
  }
  const resumen = [];
  for (const z of lista) {
    const salida = { zona: z.id, lat: z.lat, lon: z.lon, actualizado: new Date().toISOString() };
    const r = reciente(z.id, cache, normales && normales[z.id]);
    if (r) salida.reciente = r;
    try {
      salida.pronostico = await pronosticar(z.lat, z.lon, z.elev);
    } catch (e) {
      console.error(`${z.id}: pronostico no disponible (${e.message})`);
    }
    if (salida.reciente || salida.pronostico) {
      guardar(path.join(PUBLICO, `${z.id}.json`), salida);
      resumen.push({ zona: z.id, hasta: r ? r.hasta : null, lluvia30: r ? r.lluvia30 : null,
        normal30: r ? r.normal30 : null });
    }
    if (lista.length > 1) await esperar(PAUSA_MS);
  }
  guardar(path.join(PUBLICO, 'indice.json'),
    { actualizado: new Date().toISOString(), zonas: resumen.length, resumen });
  console.log(`listo: ${resumen.length}/${lista.length} zonas`);
}

principal().catch((e) => { console.error(e); process.exit(1); });
