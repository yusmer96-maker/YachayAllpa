# Fuentes y trazabilidad

- [NASA POWER Daily API](https://power.larc.nasa.gov/docs/services/api/temporal/daily/): CSV de `PRECTOTCORR,T2M_MIN,T2M_MAX,T2M,T2MDEW,WS2M,PS,ALLSKY_SFC_SW_DWN`, comunidad AG; cada `.pack` y `.season` conserva URL exacta y elevación de celda. Los CSV originales de esta entrega están en `datos/nasa/`. La app consulta directamente `PRECTOTCORR` por HTTPS solo para la celda seleccionada; conserva URL y fecha en el resultado. POWER representa una celda de grilla; los seis centros iniciales de la provincia de Huánuco caen en la misma celda; la cobertura regional nueva usa 22 centros de celdas distintas o vecinas. NASA advierte contra solicitudes repetidas a la misma celda; para un despliegue amplio conviene un servidor que agregue y firme una vez por celda.
- [NASA POWER, fuentes meteorológicas](https://power.larc.nasa.gov/docs/methodology/data/sources_meteorology/): `PRECTOTCORR`, temperatura, viento y presión proceden de MERRA-2 (0,5° × 0,625°). POWER completa los meses recientes con GEOS-IT, que sesga a húmedo; la app, el pipeline y el VPS cortan los datos al fin del mes M−2 (`PowerActualizador.confiableHasta`) y usan solo MERRA-2.
- **NASA GPM IMERG V07**, lluvia por satélite a 0,1°. Lo medido hasta ayer: *GPM IMERG Late Precipitation L3 1 day 0.1° × 0.1° V07* (`GPM_3IMERGDL`), Huffman, G. J. et al., NASA GES DISC, [doi:10.5067/GPM/IMERGDL/DAY/07](https://doi.org/10.5067/GPM/IMERGDL/DAY/07). Lo normal 2011-2025: *GPM IMERG Final Precipitation L3 1 month 0.1° × 0.1° V07* (`GPM_3IMERGM`), [doi:10.5067/GPM/IMERG/3B-MONTH/07](https://doi.org/10.5067/GPM/IMERG/3B-MONTH/07). El VPS baja solo el recorte de Huánuco por OPeNDAP con una cuenta NASA Earthdata y promedia las celdas IMERG dentro de cada celda POWER (`servidor-datos/src/imerg.js`).
- [Open-Meteo Forecast API](https://open-meteo.com/en/docs): pronóstico de 7 días por celda (lluvia, probabilidad, mínima y máxima), a la altura de la celda NASA. Datos bajo [CC BY 4.0](https://open-meteo.com/en/license); API gratuita para uso no comercial. Es pronóstico, no medición: la app lo muestra aparte.
- [NASA GIBS](https://nasa-gibs.github.io/gibs-api-docs/), capa `ASTER_GDEM_Greyscale_Shaded_Relief`: relieve del mapa interactivo (426 teselas, niveles 8-11, en `app/src/main/assets/mapa/`), teñido con la paleta de la app. Modelo de elevación ASTER GDEM v3, NASA/METI, [doi:10.5067/ASTER/ASTGTM.003](https://doi.org/10.5067/ASTER/ASTGTM.003). Descarga con `herramientas/teselas_nasa.py`.
- [INIA, Agenda Regional de Innovación Agraria Huánuco](https://repositorio.inia.gob.pe/bitstreams/3b20bfe9-ad39-46da-828e-9b11b7688a54/download): cultivo regional de papa, cacao, café, plátano, maíz amarillo duro, yuca y frijol; la producción regional no prueba la aptitud de una parcela concreta.
- [FAO, necesidades hídricas orientativas](https://www.fao.org/4/s2022e/s2022e07.htm): maíz 500–800 mm, frijol 300–500 mm por ciclo. No sustituye la calibración local.
- [FAO-56, coeficientes de cultivo](https://www.fao.org/4/x0490e/x0490e0b.htm): incluye yuca y sus etapas; la duración y los coeficientes usados aquí siguen pendientes de validación local.
- [AWS Open Data Terrain Tiles / Mapzen](https://registry.opendata.aws/terrain-tiles/): 25 teselas Terrarium guardadas en `app/src/main/assets/relieve/` para estimar altura sin internet. El encabezado de las teselas de Huánuco identifica SRTM y GMTED; [atribución de Tilezen](https://github.com/tilezen/joerd/blob/master/docs/attribution.md): datos SRTM y GMTED2010 cortesía de U.S. Geological Survey. La cifra se redondea a 50 m para mostrar su incertidumbre.
- [FAO-56, evapotranspiración del cultivo](https://www.fao.org/4/x0490e/x0490e0a.htm): `ETc=Kc×ETo`, etapas del cultivo y Penman-Monteith diario. Los Kc actuales son provisionales, no valores locales validados.
- [MINAM, cartografía base de Huánuco](https://geoservidorperu.minam.gob.pe/arcgis/rest/services/ZEE_VISOR/13_CARTOGRAFIA_BASE_HUANUCO/MapServer/layers): límite departamental usado para escoger 23 celdas NASA que intersectan la región.
- [geoBoundaries gbOpen Perú ADM2](https://www.geoboundaries.org/api.html): límites provinciales simplificados para identificar una de las 11 provincias offline; CC BY 4.0, fuente cartográfica de apoyo, no medición climática.
- [FAO, Producción orgánica de cultivos andinos](https://www.fao.org/fileadmin/user_upload/mountain_partnership/docs/produccion_organica_de_cultivos_andinos.pdf): rotación, fertilidad y control de plagas.
- [FAO, La mujer campesina y las semillas andinas](https://www.fao.org/4/x0227s/x0227s07.htm): oca y olluco después de papa.
- [MINAM/SINIA, sistemas de laymes](https://sinia.minam.gob.pe/sites/default/files/siar-apurimac/archivos/public/docs/1473.pdf): rotación y descanso comunal; se debe validar la aplicabilidad a Huánuco.
- [CienciAgro, estudio sobre parcelas de quinua y papa](https://19147850-1216-4065-8b34-fd5e3f1771f0.filesusr.com/ugd/c6a5bd_900922c8c0b547e7830113cb605aaeb9.pdf): evidencia de rotaciones observadas; no se usa como calibración numérica.
- Doorenbos, J. y Kassam, A. H. (1979). *Yield response to water*. FAO Irrigation and Drainage Paper 33: cosecha relativa = 1 − Ky·(1 − ETa/ETc) y valores de Ky. Los Ky de oca, olluco, mashua, yacón, arracacha, tarwi y kiwicha se toman de un cultivo parecido y son provisionales.
- USDA Soil Conservation Service, lluvia efectiva mensual, tal como la aplica [FAO CROPWAT](https://www.fao.org/land-water/databases-and-software/cropwat/en/): Pe = P(125 − 0,2P)/125 si P ≤ 250 mm; si no, 125 + 0,1P.
- Hargreaves, G. H. y Samani, Z. A. (1985). Reference crop evapotranspiration from temperature. *Applied Engineering in Agriculture* 1(2): 96-99. Su factor (T + 17,8) ajusta la ETo de la celda a la temperatura de la chacra, con el gradiente de −6,5 °C/km de la atmósfera estándar (ICAO).
- Sivakumar, M. V. K. (1988). Predicting rainy season potential from the onset of rains in Southern Sahelian and Sudanian climatic zones of West Africa. *Agricultural and Forest Meteorology* 42: 295-305. Criterio de inicio de siembra: 20 mm acumulados en 3 días seguidos (`Decision.LLUVIA_INICIO_3D`).
- Sivakumar no se calibró en los Andes: el umbral es el de referencia publicada y requiere prueba local.
- [Abramowitz y Stegun, Handbook of Mathematical Functions](https://www.nist.gov/mathematics-statistics/handbook-mathematical-functions-abramowitz-and-stegun), §7.1.26: aproximación de la distribución normal.
- [NIST FIPS 186-4](https://csrc.nist.gov/pubs/fips/186-4/final): ECDSA. La implementación usa los proveedores criptográficos del JDK y Android.

- [Diccionario del quechua del Huallaga](https://www.illaa.org/pirwa/diccionarios/DicQuechuaHuallaga.pdf), p. 363: `muruy` = la siembra, `muru-n` = sembrar. Una frase completa y su locución requieren validación de hablantes locales.
- [FAO, Guía de campo de los cultivos andinos](https://www.fao.org/4/ai185s/ai185s.pdf): documenta «chacra», rotación y descanso. Las preguntas de la interfaz usan esos términos en español claro; su comprensión exacta en comunidades de Huánuco requiere prueba con agricultores.
- [SENASA/MINAGRI, feria de agrobiodiversidad de Quisqui (Huánuco)](https://www.senasa.gob.pe/senasacontigo/minagri-mas-de-600-variedades-de-papas-nativas-se-expondran-en-feria-campesina-de-huanuco/): registra papa, oca, olluco, mashua, camote, yacón, arracacha y yuca, además de pallar, trigo, maní, kiwicha y calabaza. La lista justifica el catálogo regional, **no** los umbrales numéricos de agua o altura.
- [INIA, manejo agronómico de mashua](https://repositorio.inia.gob.pe/server/api/core/bitstreams/09437a44-e9a6-4f8e-a3e8-f431d0035033/content): referencia para especie y práctica de siembra; parámetros provisionales sujetos a validación local.
- [INIA, manejo agronómico de yacón](https://repositorio.inia.gob.pe/server/api/core/bitstreams/9cdf6af6-5958-4183-8dee-0296eebeec2f/content): referencia para especie y época de siembra de secano; parámetros provisionales sujetos a validación local.
- [Onest](https://github.com/google/fonts/blob/main/ofl/onest/METADATA.pb), Google Fonts, licencia SIL OFL en `app/src/main/assets/fonts/Onest-OFL.txt`: única familia de la interfaz Compose, instanciada en pesos 400, 500, 600, 700 y 800.
- Ilustraciones (portada, cabecera, Kusi, icono y los dibujos de cada cultivo, incluidos arroz y avena) hechas para este proyecto e incluidas en el APK. Arroz y avena se limpiaron de su fondo con `herramientas/limpiar_dibujo.py`.
- La app ya no usa El Niño (NOAA CPC) ni voz: se retiraron el 15-nov-2026 y quedan fuera de la compilación.

## Fuentes pendientes de cada parámetro de cultivo

Completar con documento, página, variedad, lugar, altura, sistema de secano y validación de un agrónomo. Los valores actuales son **referenciales**.

| Cultivo | Altitud mínima/máxima | Agua por ciclo | Kc por etapa | Meses de ciclo | Tolerancia de helada | Meses de siembra | Validó / fecha |
|---|---|---|---|---|---|---|---|
| Papa |  |  |  |  |  |  |  |
| Quinua |  |  |  |  |  |  |  |
| Oca |  |  |  |  |  |  |  |
| Olluco |  |  |  |  |  |  |  |
| Maíz amiláceo |  |  |  |  |  |  |  |
| Cebada |  |  |  |  |  |  |  |
| Habas |  |  |  |  |  |  |  |
| Tarwi |  |  |  |  |  |  |  |
| Arveja |  |  |  |  |  |  |  |
| Maíz amarillo duro |  | FAO: 500–800 mm, pendiente variedad local |  |  |  |  |  |
| Frijol |  | FAO: 300–500 mm, pendiente variedad local |  |  |  |  |  |
| Yuca |  |  | FAO-56 general, pendiente variedad local |  |  |  |  |
| Mashua | FAO/INIA, verificar rango para Huánuco |  | FAO-56 aproximado, validar | INIA, validar variedad |  | INIA, validar localidad |  |
| Camote | FAO/SENASA, verificar rango para Huánuco |  | FAO-56 aproximado, validar |  |  |  |  |
| Yacón | FAO/INIA, verificar rango para Huánuco |  | FAO-56 aproximado, validar | INIA, validar variedad |  | INIA: secano sep-oct, validar localidad |  |
| Arracacha | FAO, verificar rango para Huánuco |  | FAO-56 aproximado, validar |  |  |  |  |
| Pallar | SENASA confirma presencia; altitud provisional |  | Kc provisional |  |  | Calendario provisional |  |
| Trigo | SENASA confirma presencia; altitud provisional |  | Kc provisional |  |  | Calendario provisional |  |
| Maní | SENASA confirma presencia; altitud provisional |  | Kc provisional |  |  | Calendario provisional |  |
| Kiwicha | SENASA confirma presencia; altitud provisional |  | Kc provisional |  |  | Calendario provisional |  |
| Calabaza | SENASA confirma presencia; altitud provisional |  | Kc provisional |  |  | Calendario provisional |  |
| Descanso |  |  |  |  |  |  |  |
