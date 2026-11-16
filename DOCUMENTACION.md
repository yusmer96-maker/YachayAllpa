# Yachay Allpa

Asesor de siembra y rotación para **las 11 provincias de Huánuco**. Funciona sin señal con 23 zonas climáticas NASA POWER precargadas. Cuando vuelve la conexión, Android programa una descarga HTTPS del CSV diario de lluvia para la zona elegida y guarda el resultado en almacenamiento privado. El GPS exacto nunca se manda a NASA: solo se consulta el centro fijo de la celda. Si el GPS no entrega altura, un mapa de relieve incluido en el APK da una estimación; el usuario puede corregirla.

```text
NASA POWER (CSV diario, 2011–noviembre de 2026)
       ↓
pipeline/ (ETo FAO-56 + 15 temporadas + 23 celdas + firma)
       ↓
.season firmado → teléfono → core/ balance de agua y rotación
                           ↑
             NASA POWER HTTPS → lluvia reciente en caché privado
                                      ↓
                 ubicación → última siembra → 3 opciones
                                      ↓
                   lluvia en barras + Kusi, cuy asistente
```

La app Android usa **Kotlin + Jetpack Compose** en una sola vista. Ubicación, último cultivo, tres opciones y lluvia quedan visibles en la misma página; idioma, provincia, altura, catálogo, preferencias, razones y gráfico se abren como ventanas desplazables. Onest es la única familia tipográfica, con cinco pesos en `app/src/main/res/font/onest.xml`. Las láminas botánicas de los tubérculos tienen fondo transparente. `core/` y `pipeline/` siguen en Java 11; la app reutiliza el mismo motor y los packs firmados.

## Dos cálculos en el mismo `core/`

El motor mensual inicial sigue disponible para probar la rotación tradicional y la deriva entre 1991–2005 y 2011–2025. **La app usa el motor de temporada nuevo**. El primero conserva estas reglas:

| Regla mensual | Peso |
|---|---:|
| Agua del ciclo | 30 |
| Altitud | 20 |
| Helada | 20 |
| Rotación andina | 30 |

El motor activo usa 15 temporadas agrícolas 2011–2025. Divide cada una en bloques de diez días, calcula ETo por FAO-56 Penman-Monteith a partir de NASA POWER, estima demanda del cultivo `ETc=Kc×ETo`, y sigue una reserva de agua del suelo durante germinación, floración y maduración. Si se siembra hoy y el archivo es reciente, la lluvia observada de 15 días inicia **una estimación** de la reserva; no equivale a humedad de suelo medida. También corrige Tmin por altitud y cuenta años con estrés de agua o helada.

| Componente activo | Peso base |
|---|---:|
| Cobertura de agua por etapas | 35 |
| Riesgo de helada | 20 |
| Aptitud de altura | 20 |
| Rotación y nitrógeno | 25 |

La prioridad elegida en Preferencias ajusta el resultado; descansar se modela aparte. Lo que se pinta, lo que se dice y lo que se lee en voz alta salen de un **único veredicto** (`core/Veredicto.java`), que es el menor de tres niveles: puntaje, agua y helada. Por eso una opción no puede aparecer como «muy buena» encima de una barra roja, y la primera opción no puede salir peor que las dos alternativas que lleva al lado: el mismo veredicto ordena la lista. El agua se juzga por la **cobertura media de la demanda del ciclo**, y la helada contra lo que aguanta ese cultivo. La prueba `coherenciaDelVeredicto` de `pipeline` barre alturas y cultivos y falla si aparece una sola contradicción. La lluvia reciente se compara con las mismas fechas de 15 años. «El agua suele alcanzar» significa que el balance modelado cubrió la demanda hídrica completa en al menos 10 de 15 temporadas históricas; la tarjeta muestra el conteo. Son **escenarios históricos, no un pronóstico** de esta campaña. La cifra grande de lluvia suma 30 días; la ventana Clima muestra seis semanas separadas. La fecha y procedencia se ven en esa ventana.

## Cobertura de Huánuco

Las 23 celdas cubren las 11 provincias usando límites provinciales de geoBoundaries y el [geoservidor MINAM](https://geoservidorperu.minam.gob.pe/arcgis/rest/services/ZEE_VISOR/13_CARTOGRAFIA_BASE_HUANUCO/MapServer/layers) como contraste. Un archivo local de límites provinciales permite al GPS identificar las 11 provincias sin enviar ubicación. Si el teléfono entrega coordenadas sin altura, 25 teselas de relieve Terrarium incluidas en el APK estiman una altura aproximada, redondeada a 50 m. NASA POWER trabaja con celdas meteorológicas de 0,5° × 0,625°; **la lluvia de la celda no es la lluvia medida en una chacra**. Las series descargadas para esta entrega llegan al 11 de noviembre de 2026. El CSV original y la URL exacta quedan en `datos/nasa/`; cada `.season` está firmado.

El catálogo modela 24 opciones de campaña de sierra y zonas bajas; arroz y avena se añadieron tras contrastarlo con la superficie sembrada oficial de Huánuco (INEI, Compendio Estadístico 2025, cuadro 12.2; ver `ANALISIS_CALCULOS.md`). Incluye papa, oca, olluco, mashua, camote, yacón, arracacha y yuca, además de quinua, maíz, cebada, habas, tarwi, arveja, frijol, pallar, trigo, maní, kiwicha y calabaza, registradas en una [feria campesina de Huánuco documentada por SENASA](https://www.senasa.gob.pe/senasacontigo/minagri-mas-de-600-variedades-de-papas-nativas-se-expondran-en-feria-campesina-de-huanuco/). [INIA](https://repositorio.inia.gob.pe/bitstreams/3b20bfe9-ad39-46da-828e-9b11b7688a54/download) también prioriza cacao, café y plátano; son perennes y el modelo de rotación de campaña no los evalúa. **La presencia regional de un cultivo no demuestra su aptitud en una chacra concreta**.

## Compilar y actualizar

JDK 21, Android SDK 35 y Gradle wrapper. El código de `core/` y `pipeline/` sigue compilándose con Java 11. La primera compilación descarga Kotlin y AndroidX; las siguientes pueden usar `--offline`.

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :pipeline:pruebas :app:assembleDebug
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :pipeline:run --args='actualizar /ruta/llaves/privada.key /ruta/datos/nasa'
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Para un CSV ya descargado, agregue `[dirCsvLocal]` al comando `actualizar`; los nombres esperados son `zona_01-2011-reciente.csv` hasta `zona_23-2011-reciente.csv`, además de los seis centros iniciales. El comando `construir` conserva los `.pack` mensuales iniciales. `llaves <dir>` crea una pareja ECDSA P-256 sin sobrescribir la privada. Al usar `:pipeline:run`, las rutas relativas se resuelven desde `pipeline/`; use rutas absolutas como en el ejemplo.

Sin Gradle, el motor, pipeline y pruebas se ejecutan así:

```sh
mkdir -p out
find core/src/main/java pipeline/src/main/java pipeline/src/test/java -name '*.java' > fuentes.txt
javac --release 11 -d out @fuentes.txt
java -cp out pe.yachayallpa.pipeline.Pruebas
java -cp out pe.yachayallpa.pipeline.Main actualizar /ruta/llaves/privada.key /ruta/datos/nasa
```

El APK requiere Android SDK/Gradle y dependencias oficiales Kotlin/AndroidX Compose. `core/` y `pipeline/` no incorporan librerías de terceros. La licencia es Apache 2.0.

## Packs, ubicación y voz

`.pack` y `.season` son texto UTF-8 canónico, máximo 64 KB, firmados con ECDSA P-256/SHA-256. El verificador comprueba la firma **antes de interpretar cifras**, también para archivos dentro del APK. Un archivo firmado con versión anterior a la instalada se rechaza. La llave privada no se incluye en el APK. La actualización directa de lluvia llega por HTTPS y se valida contra la ubicación de la celda; **no está firmada con nuestra llave**, por lo que se conserva separada de las series históricas firmadas. El caché se limita a 2 MB. El APK declara `INTERNET` para esta función; por ello ya no corresponde afirmar que sea incapaz de conectarse. El GPS se usa en memoria para provincia, celda y altura redondeada a 10 m. Si solo entrega coordenadas, el relieve offline aporta una altura aproximada redondeada a 50 m. La posición exacta no se guarda ni se envía; Android conserva temporalmente el identificador de la zona para el trabajo programado.

La app se lee entera en **castellano o en quechua de Huánuco**: el idioma se pregunta al abrirla y se cambia en Ajustes. Todo el texto vive en una sola tabla bilingüe (`app/src/main/java/pe/yachayallpa/app/Textos.kt`) y el motor no lleva frases dentro: devuelve claves con sus cifras y la app las escribe. `herramientas/revisar_textos.py` comprueba que las dos tablas cuadren en claves y en huecos de formato. El quechua está pendiente de revisión con hablantes de Huánuco, y la propia ventana de idioma lo dice.

Kusi **habla solo en castellano**, con el lector de voz del teléfono (Android TTS), y lee exactamente la frase que está escrita; en quechua no se muestra el altavoz. No hay servidor de voz ni claves de terceros en el APK: se quitó Fish Audio el 15-nov-2026. Ver `ARQUITECTURA_VOZ_KUSI.md`.

## Lo que no hace

Es una guía para **secano**. No considera riego, humedad de suelo medida, plagas, variedades, mercado o pronóstico meteorológico. El suelo elegido en Mi chacra ajusta una capacidad estimada, no medida. Los `Kc`, rangos de altura, duración, calendario y tolerancia al frío son orientativos y **deben validarse con agricultores y un agrónomo local**. El gradiente térmico fijo y la normal de Tmin pueden fallar en valles y microclimas. Los límites provinciales offline son simplificados. La renovación directa refresca lluvia observada y gráfico, no vuelve a calcular Tmin ni ETo históricos. Si la lluvia queda desactualizada o NASA no responde, la app usa la última serie válida e indica su fecha. A escala de muchos teléfonos, las peticiones directas repetidas a una misma celda pueden saturar POWER: un VPS que descargue una vez por celda y distribuya packs firmados sería la evolución adecuada.

## Mapa de la chacra

La ventana "Tu chacra está aquí" muestra un mapa que se mueve, se acerca y se aleja (dedos, doble toque o botones + y −). Es el relieve sombreado **ASTER GDEM (NASA/METI)** servido por **NASA GIBS**, teñido por altura con la paleta de la app y guardado dentro del APK: 426 teselas, niveles 8 a 11 (unos 76 m por píxel), 7 MB. Funciona sin señal y la posición no sale del teléfono. Se regenera con `python3 -I herramientas/teselas_nasa.py` (las teselas crudas quedan en `datos/gibs/`).

## Pendiente (15-nov-2026)

- El quechua de la tabla bilingüe necesita revisión de hablantes de Huánuco; la estructura ya está completa y comprobada por script.
- `MainActivity.java`, `CultivoLamina.java` y `LluviaGrafico.java` son la pantalla vieja de Views: no se lanzan y quedan excluidos de la compilación en `app/build.gradle`.
- `Voz.java` y `EnsoRepositorio.java` quedaron sin uso (Kusi solo escribe; sin barra de El Niño) y están excluidos de la compilación, pendientes de borrar.
- `servidor-datos/` está desplegado: publica índices (solo MERRA-2) y el pronóstico de 7 días que la app usa en la recomendación.
- SENAMHI: pendiente de una cuenta IRI para PISCO (ver `ANALISIS_CALCULOS.md`).

Véase [SOURCES.md](SOURCES.md) para las fuentes y la tabla de parámetros pendiente de validar.
