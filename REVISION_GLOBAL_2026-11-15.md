# Revisión global: app y VPS (15-nov-2026)

Pregunta: ¿lo que la app dice en palabras y colores sale de cálculos medibles?
Revisado en el código (core, app, servidor-datos) y en el VPS. Pruebas del motor: todas pasan.

## 1. Cadena de cada salida cualitativa

| Lo que ve el agricultor | De dónde sale | Tipo |
|---|---|---|
| Cosecha esperada (69 %, 79 %...) | FAO-33: 1 − Ky·(1 − ETa/ETc), media de 15 campañas NASA POWER 2011-2025; ETo FAO-56 con corrección por altura; lluvia efectiva USDA | Medido + modelo publicado |
| Agua: "le alcanza / justa / falta" | Cosecha esperada frente a 85 % y 50 % | Umbral sobre cifra medida |
| Helada | Noches bajo la tolerancia del cultivo, tmin NASA corregida a la altura de la chacra (−6,5 °C/km) | Medido + gradiente estándar |
| Veredicto (Muy buena / con cuidado / no conviene) | El peor de tres niveles: agua, helada y puntaje | Mixto (ver 2.3) |
| Te recomiendo: sembrar / esperar / descansar | Veredicto + pronóstico Open-Meteo de 7 días (lluvia y helada) | Mixto (ver 2.3) |
| Frase de Kusi | La acción de la decisión, sin lógica propia | Derivado |
| Plan de rotación | Etapas andinas (convención) + el mismo motor en cada campaña, con lluvia normal | Convención + medido |
| Lluvia de 7 días | Open-Meteo vía VPS, suma directa | Pronóstico |
| Lluvia por semana y "Llovió el X % de lo normal" | Pack NASA POWER hasta el fin del mes M−2 | Medido |

Conclusión: no hay plantillas fijas. Cada texto se arma con cifras de esa chacra y ese cultivo.
Lo cualitativo sale de lo cuantitativo, salvo los pesos y umbrales del punto 2.3.

## 2. Hallazgos, por importancia

### 2.1 La fuente de lluvia del modelo no está validada (prioridad alta)
Septiembre 2026, lluvia total por zona (litros por m²):

| Zona | Altura celda | IMERG | NASA POWER |
|---|---|---|---|
| zona_05 (Huánuco) | 3404 m | 45 | 18 |
| zona_03 | 2575 m | 134 | 9 |
| zona_09 | 2441 m | 124 | 35 |
| zona_10 | 1166 m | 235 | 35 |
| zona_15 | 1733 m | 135 | 50 |
| zona_16 | 684 m | 211 | 53 |
| zona_22 | 212 m | 101 | 60 |

IMERG da entre 1,5 y 15 veces más que POWER (MERRA-2). Todo el balance de agua usa POWER.
Si POWER se queda corto, la app subestima el agua y empuja hacia "con cuidado" y "descansar".
Paso siguiente: comparar las dos con PISCO (SENAMHI, con estaciones) y quedarse con la que más se acerque.

### 2.2 El agua reciente nunca entra al cálculo (prioridad alta)
`MotorTemporada.simular` arranca la reserva del suelo con la lluvia de los últimos 15 días solo si
`Temporada.reciente(hoy)`: fecha de corte a 14 días o menos. Desde el corte MERRA-2 (fin del mes M−2)
el corte siempre tiene 32-75 días, así que la condición nunca se cumple: la reserva inicial es siempre
el 50 % de la capacidad del suelo, haya llovido o no. El motivo "lluvia_mucha" tampoco sale nunca.
Arreglo: dar al motor la lluvia de los últimos 15-30 días de IMERG (llega con 1 día de retraso).

### 2.3 Pesos y umbrales de criterio, no derivados de datos (prioridad media)
- Puntaje: 35 agua + 20 helada + 20 altura + 25 rotación; niveles en 70 y 45. En la práctica solo
  baja el veredicto por altura fuera de rango o por repetir cultivo; agua y helada mandan.
- Decisión: sembrar con 10 mm o más en 7 días; helada en 0 °C (−2 °C si tolera frío); fecha buena a
  más de 30 días = esperar al mes. Referencia publicada para el inicio de siembra: Sivakumar (1988),
  20 mm en 3 días sin racha seca de 7 días en los 30 siguientes. Conviene alinearlo y citarlo.
- Altura: los textos usan ±300 m del rango y el puntaje baja en 500 m. Unificar en un solo margen.

### 2.4 Parámetros de cultivo provisionales (prioridad media)
Ky de FAO-33 donde existe; oca, olluco, mashua, yacón, arracacha, tarwi, kiwicha y otros toman el de un
cultivo parecido. Kc, ciclo y rangos de altura son orientativos (el propio código lo dice). Validar con INIA.

### 2.5 El VPS calcula índices que la app no usa (prioridad baja)
`zona_XX.json` publica percentil de humedad del suelo, SPI 1/3/6, tendencias por década, lluvia 30 días
y su normal. La app solo lee `pronostico`. O se usan (el percentil de humedad del suelo encaja con 2.2)
o se quitan, para no tener dos mecanismos para lo mismo.

## 3. Lo que está bien
- Corte GEOS-IT aplicado igual en teléfono, pipeline y VPS.
- Packs firmados (ECDSA P-256); el teléfono rechaza datos alterados.
- Veredicto, tarjetas, razones y Kusi usan la misma función: no se contradicen.
- Pronóstico en la decisión con su fecha de emisión y sin inventar si falta señal.
- Token Earthdata en el VPS con permiso 600 del usuario `yachay`.

## 4. Orden propuesto
1. IMERG en el VPS: semanas recientes con su propio normal y lluvia de los últimos 30 días.
2. El motor usa esa lluvia reciente para la reserva inicial (arregla 2.2).
3. Comparación POWER / IMERG / PISCO por zona; elegir la fuente del histórico (2.1).
4. Alinear umbrales de la decisión con la referencia publicada y unificar el margen de altura (2.3).
5. Usar o quitar los índices sin uso del VPS (2.5).

## 5. Estado (16-nov, 00:20 UTC)
- 2.2 corregido: el VPS publica IMERG hasta ayer (`reciente`); la app arma `zonaViva` y el motor usa la lluvia de 15 días para la reserva inicial. En producción: zona_05 con 73 mm en 30 días frente a 72 de normal (IMERG 2010-2025).
- 2.3 corregido: inicio de siembra con 20 mm en 3 días seguidos (Sivakumar, 1988), medidos o pronosticados; margen de altura único de 300 m (`Cultivo.MARGEN_ALTURA`).
- 2.5 corregido: el VPS ya no calcula SPI, percentil de humedad ni tendencias; se quitó `src/indices.js`. Queda `datos/crudo` (25 MB de POWER diario) sin uso, útil para la comparación con PISCO.
- Normal IMERG frente a POWER por mes (mm): zona_05 parecidos (enero 101 frente a 99); zona_03 muy distintos (enero 217 frente a 61). La diferencia es de algunas zonas, no de todas.
- 2.1 y 2.4 siguen pendientes: elegir la fuente del histórico con PISCO; validar Ky, Kc y rangos con INIA.
