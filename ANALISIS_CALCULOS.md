# Auditoría de cálculos y datos (15-nov-2026)

Todo lo de aquí se midió con los 23 packs reales y el crudo del VPS. Las sondas
se corrieron sobre `core/` compilado; las pruebas permanentes están en
`pipeline/src/test/java/.../Pruebas.java`.

## 1. Lluvia "reciente" contaminada por GEOS-IT (corregido)

NASA POWER sirve `PRECTOTCORR` de MERRA-2 hasta el último mes publicado y
rellena el resto con GEOS-IT. El CSV no lo distingue y su cabecera dice MERRA-2
para todo. Comprobado contra la API (`header.sources`) en la celda de zona_03:

| Periodo | Fuente | Lluvia | Días con lluvia |
|---|---|---:|---:|
| sep-2026 | MERRA2 | 9 mm | 2 de 30 |
| oct-2026 | GEOSIT | 431 mm | 31 de 31 |

El salto cae el 1-oct en las 23 celdas a la vez. Efecto en lo que mostrábamos:
"llovió 3 a 11 veces lo normal" en todas las zonas, riesgo de sequía rebajado,
la razón "llovió más de lo habitual" en todos los cultivos y la reserva inicial
de agua inflada al sembrar hoy.

**Arreglo, una sola regla en los tres sitios:** lo reciente se corta al final
del mes de hace dos meses (`PowerActualizador.confiableHasta`, el pipeline y
`servidor-datos/actualizar.js`). El 15-nov corta el 30-sep. Los 23 packs se
regeneraron (versión 20261115) y la pantalla dice "datos fiables hasta el 30/9".

Antes y después en los packs: zona_03 pasó de 367 frente a 34 mm a 9 frente a 13
mm; septiembre sale algo por debajo de lo normal en la sierra y cerca de lo
normal en la selva.

## 2. VPS (`yachay-datos`, timer diario)

**Actualización 15-nov (noche):** el VPS ya no calcula índices con NASA POWER (SPI,
percentil de humedad, tendencias): la app no los usaba. Ahora publica por zona la
lluvia medida por satélite hasta ayer (NASA GPM IMERG Late, diario) en 6 semanas y en
3, 7, 15 y 30 días, con su normal de la misma fuente (IMERG Final mensual 2011-2025),
más el pronóstico Open-Meteo. Ver la sección 12. Lo de abajo es el estado anterior.

- Publicaba con el mismo GEOS-IT: zona_03 SPI-3 4,75 y humedad de raíz en el
  percentil 99. Con el corte, el mismo crudo da SPI-3 -0,27 y percentil 16;
  zona_15 da SPI-6 -1,0 (algo seco).
- Fallo añadido: la descarga es incremental, así que un día GEOS-IT guardado no
  se volvía a pedir cuando sale su MERRA-2. Ahora se purga todo lo posterior
  al límite en cada corrida.
- El VPS **no** hace los pronósticos ni la recomendación: eso corre en el
  teléfono (`MotorTemporada`, `PronosticoCampana`). La app aún no lee sus
  índices (`DatosZona` sin usar). No combina datos de SENASA: SENASA no publica
  clima. La fuente de estaciones en tierra sería SENAMHI.
- **Desplegado el 15-nov** (md5 igual al local): purgó 43 días GEOS-IT por zona
  y publica solo MERRA-2. Además publica el pronóstico de 7 días por celda
  (Open-Meteo, a la altura de la celda): lluvia, probabilidad y mínima.
- `yachay-voz` (Fish) parado y deshabilitado; quitada la ruta `/voz` del nginx
  (`nginx -t` correcto). Quedan en el VPS la carpeta `servidor-voz/` con su
  `.env` y `voz-cache/`.

## 3. Preferencias ("Conoce tu tierra")

| Pregunta | ¿Cambia las tres opciones? |
|---|---|
| Tipo de suelo | sí, en el 100 % de los casos (capacidad 40/80/120 mm) |
| Qué cuidar | sí, en el 99 % |
| ¿Descansó? | antes solo "No" movía algo (0, 1, 2 y 3 daban lo mismo) |

Arreglo: la escala de descanso sube de una en una (2 siembras +10, 3 +20, 4+
+40) y las respuestas valen Sí = 1, No lo sé = 2, No = 4. Además, cambiar el
cultivo anterior ya no borra esa respuesta.

## 4. "Por qué sale esto"

Las razones eran datos reales, pero en el 30 % de los casos dos opciones
mostraban el mismo texto. Ahora cada opción lleva sus cifras: cobertura en %
y los mm que pide su ciclo, campañas con estrés y la altura de la chacra frente
al rango del cultivo. La barra de helada dice "helada X de cada 100 noches en su
época más fría", que es lo que mide el cálculo.

## 5. Altura (GPS o mapa)

Influye, y es medible (zona_05, celda a 3404 m, tras papa):

| Altura | Primera opción | Helada papa | Opciones posibles |
|---:|---|---:|---:|
| 1800 m | frijol | fuera de rango | 15 |
| 2400 m | arveja | 0 % | 19 |
| 3600 m | quinua | 0 % | 15 |
| 4200 m | quinua | 26 % | 10 |

Error de altura sobre 1.725 casos: la primera opción o su veredicto cambia con
+50 m en el 5 %, con +150 m en el 13 % y con +300 m en el 23 %. La app acepta la
altura del GPS solo con precisión vertical de 150 m o mejor; si no, estima con el
mapa de relieve (redondeo de 50 m).

**Corregido: la altura ya toca el agua.** Antes la demanda (ETo) era la de la
celda: la quinua daba 84,0 % a 2400 m y a 3800 m. El `.season` pasa a formato 2
con la temperatura media por bloque y el motor escala la ETo con Hargreaves,
(T+17,8), corrigiendo la media con -6,5 °C/km. Ahora la quinua en zona_05 cubre
75,1 % a 2400 m y 88,0 % a 3800 m. Con eso la altura pesa más: 150 m de error
cambian la primera opción en el 25 % de los casos (antes 13 %). El teléfono sigue
leyendo packs del formato 1, sin corrección.

## 6. Gráficas

Colores validados con `dataviz/validate_palette.js`: verde #1F7A4D para esta
campaña y ocre #9C5B22 para lo normal. Pasan las seis pruebas; la identidad
también va por forma y leyenda.

- Semanas: barra por semana con la lluvia medida (IMERG hasta ayer; si no llega,
  la del pack), en litros por m², cifra solo de la semana tocada. Lo normal va en la
  frase de arriba, sin raya en la gráfica.
- Campaña (nueva): 36 bloques de 10 días con la franja de 9 de cada 10 años,
  la mitad de los años, la línea de lo normal y la marca de hoy. Tocar un
  punto muestra sus cifras.

## 7. La recomendación (`core/Decision.java`)

Una sola acción arriba de todo: sembrar esta semana, esperar la lluvia, esperar
por helada, sembrar en tal mes o descansar. Sale del mismo veredicto y del
pronóstico de 7 días. Inicio de siembra según Sivakumar (1988): 20 mm en 3 días
seguidos, medidos (IMERG, últimos 3 días) o pronosticados; si no, esperar. Una mínima
corregida a la chacra de 0 °C o menos (-2 °C en cultivos que aguantan frío) es esperar.
Reglas probadas en `pruebasDecision`.

## 8. SENAMHI

- Hay unas 40 estaciones meteorológicas de SENAMHI en la zona (Huánuco 109003,
  Tingo María, Canchán, Chaglla, Carpish, Dos de Mayo, Puerto Inca...). Sus
  datos mensuales (2017 a hoy) están detrás de un captcha de Cloudflare: no se
  automatiza.
- La vía pública es PISCO (lluvia diaria de SENAMHI combinando estaciones y
  satélite), en la biblioteca IRI de Columbia, que desde 2025 exige una cuenta
  gratuita. Con ella, el pipeline puede corregir con datos de tierra las normales
  de lluvia de MERRA-2 por celda.
- Otra vía es pedir los datos a SENAMHI formalmente.

## 9. Catálogo frente a lo que se siembra en Huánuco

Fuente oficial abierta: INEI, *Huánuco: Compendio Estadístico 2025*, cuadro 12.2
(superficie sembrada por campaña, datos de la DRA Huánuco a junio 2025).
Campaña 2024-2025, en hectáreas:

| Cultivo | ha | En la app |
|---|---:|---|
| Papa (nativa 19 217, mejorada 17 304, de color 9 872) | 46 393 | sí, una sola papa |
| Arroz cáscara | 13 644 | **añadido** |
| Maíz amiláceo / amarillo duro | 13 043 / 10 848 | sí |
| Trigo / cebada | 7 823 / 6 182 | sí |
| Frijol grano seco / yuca | 5 379 / 5 191 | sí |
| Haba (seco + verde) / arveja (seco + verde) | 4 044 / 3 256 | sí |
| Avena (grano + forraje) | 2 637 | **añadido** |
| Olluco / zapallo / tarwi | 1 883 / 1 473 / 1 201 | sí (zapallo como calabaza) |
| Quinua / oca / mashua / camote | 760 / 716 / 358 / 240 | sí |
| Maní | 4 | sí, casi no se siembra |
| Pallar, kiwicha, yacón, arracacha | no figuran | sí, decidir si se quedan |

Arroz y avena usan Kc de FAO-56 y rangos orientativos; aún sin lámina botánica
(se muestra un icono). Los controles anti-robot de SENAMHI y BCRP no se saltan:
sus datos entran solo por descarga manual del usuario o por solicitud formal.

## 10. Agua: de "cobertura" a cosecha esperada (FAO-33)

Antes, si el mejor cultivo no llegaba al 75 % de cobertura de agua, la app
recomendaba descansar. Ese 75 % era un umbral sin base, y en zona_05 a 1950 m salía
"descansar" para los 24 cultivos anteriores posibles (37 % de los casos en toda
Huánuco).

Ahora cada campaña NASA da una cosecha relativa 1 − Ky·(1 − ETa/ETc) (FAO-33,
Doorenbos y Kassam 1979), y se promedian las 15. Ky de FAO-33 donde existe (papa,
maíz, frijol, arveja, trigo, maní); el resto, por cultivo parecido y marcado como
provisional en `Cultivo.ky()`.

- Desde 85 % de cosecha: el agua va bien.
- 50 a 85 %: se siembra con cuidado y se avisa de cuánto agua completaría el riego.
- Bajo 50 %: no compensa en secano; se recomienda descansar.
- Descansar también sale cuando lo pide la rotación (p. ej. después de habas).

Resultado en zona_05 a 1950 m: tras papa, "siembra frijol" (cosecha esperada 70 %);
tras quinua, "siembra trigo" (64 %); tras habas, descanso por rotación. En toda
Huánuco, descansar baja del 37 % al 22 % de los casos.

## 11. Lluvia efectiva: del 28 % fijo al método USDA (CROPWAT)

El balance restaba un 28 % fijo a toda lluvia (factor 0,72; 0,62 y 0,82 según el
suelo) antes de llegar a la raíz, llueva poco o mucho. Ahora se usa la lluvia
efectiva del USDA (SCS), la de CROPWAT de la FAO: mensual Pe = P(125 − 0,2P)/125
si P ≤ 250 mm, si no 125 + 0,1P, aplicada a cada bloque de diez días por su
equivalente mensual. Las lluvias pequeñas se aprovechan ~90 %; las fuertes, menos.
El tipo de suelo sigue entrando por la capacidad de agua (40, 80 o 120 mm).

Efecto en zona_05 (cosecha esperada tras papa):

| Altura | Antes | Ahora |
|---:|---|---|
| 1950 m | 52-78 %, todo "con cuidado" | 58-81 %, todo "con cuidado" (valle seco: en secano siempre se pierde algo) |
| 3200 m | 63-84 %, ninguno "muy buena" | quinua, arveja y kiwicha "muy buena" (86-88 %) |
| 3700 m | 3 "muy buena" | 8 "muy buena" |

En toda Huánuco "descansar" queda en 19 % de los casos. Cada tarjeta de opción
muestra su cosecha esperada para distinguir opciones con el mismo veredicto.

## 12. Lluvia reciente medida y fuentes de lluvia (15-nov, noche)

- **Por qué:** el corte MERRA-2 deja el último dato de POWER a 32-75 días. Con eso
  `Temporada.reciente()` nunca era cierto y la reserva inicial del suelo era siempre el
  50 % de la capacidad, hubiera llovido o no.
- **Qué se hizo:** el VPS publica `reciente` (IMERG Late, 1 día de retraso). La app arma
  `zonaViva = pack.conReciente(...)`: mismo histórico de 15 campañas, pero lluvia de 7,
  15 y 30 días y semanas hasta ayer. El motor usa esa lluvia de 15 días para la reserva
  inicial; la gráfica y "llovió el X % de lo normal" comparan IMERG con IMERG.
- **Pendiente:** IMERG da 1,5 a 15 veces más lluvia que POWER en septiembre 2026 (tabla
  en `REVISION_GLOBAL_2026-11-15.md`). Hay que comparar las dos con PISCO (SENAMHI)
  antes de decidir la fuente del histórico del balance de agua.
- **Altura:** un solo margen, `Cultivo.MARGEN_ALTURA` = 300 m (unos 2 °C a -6,5 °C/km),
  para el filtro de opciones, el puntaje, los motivos y la rueda. Antes eran 500 y 300.
