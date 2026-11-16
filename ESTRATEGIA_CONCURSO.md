# Cómo se gana el NASA Space Apps (investigado el 15-nov-2026)

## Los cinco criterios oficiales

Space Apps puntúa con cinco criterios, iguales desde hace años
([guía de envío de proyecto](https://www.spaceappschallenge.org/resources/project-submission-guide/),
[Judging and Awards 2019](https://sa-2019.s3.amazonaws.com/media/documents/Space_Apps_2019_Judging_and_Awards.pdf)):

| Criterio | Qué pregunta el jurado |
|---|---|
| **Impact** | Cuánto beneficio real produce. ¿Resuelve un problema grande o pequeño? |
| **Creativity** | Si el enfoque es novedoso o solo una mejora incremental. |
| **Validity** | Si es científica y técnicamente sólido, y si funcionaría de verdad. |
| **Relevance** | Si responde al reto concreto bajo el que se presenta. |
| **Presentation** | Si el equipo lo comunicó bien. |

En Roma 2021 se indicó que gana la puntuación más alta **en cada criterio**, y que a
igualdad decide la puntuación global. El juicio local lo organiza cada sede.

## El dato que manda sobre el diseño

La guía de 2026 limita la demostración a **30 segundos y 7 diapositivas**. Pasarse
**deja el proyecto fuera del juicio global**. Esto no es un detalle de formato: obliga
a que la app se entienda entera en medio minuto. Una pantalla, una respuesta.

## Qué ganó de verdad en 2025

De **11.500 proyectos**, 10 ganadores globales, juzgados por expertos de NASA
([anuncio de NASA, 18-dic-2025](https://www.nasa.gov/learning-resources/stem-engagement-at-nasa/nasa-announces-2025-international-space-apps-challenge-global-winners/)).
Premios: Best Use of Science · Best Use of Data · Best Use of Technology · Galactic
Impact · Best Mission Concept · Most Inspirational · Best Use of Storytelling ·
Global Connection · Art & Technology · Local Impact.

Dos referencias directas para nosotros:

- **QUEÑARIS (Perú) — Local Impact.** Escasez de agua en Arequipa por degradación de
  los bosques de queñua. Combina microorganismos nativos, datos satelitales NASA,
  drones e IA. Mismo país, mismo tipo de problema: es el premio que nos corresponde
  por naturaleza.
- **Zumorroda-X (Egipto) — Art & Technology**, en el reto de agricultura
  ("NASA Farm Navigators"). Eran **minijuegos** donde el jugador es un agricultor que
  se adapta a olas de calor e inundaciones.

**Hallazgo decisivo: ninguna de las diez descripciones nombra un dataset concreto ni
una fórmula.** Solo dicen "datos de observación de la Tierra de la NASA", "IA". No gana
el modelo más complejo: gana el problema real bien contado y bien resuelto.

## Qué se deduce para Yachay Allpa

1. **El rigor suma en Validity, pero no basta.** Decir "escenarios históricos, no
   predicción" protege Validity y hunde Impact. La salida no es mentir con un
   pronóstico: es **condicionar la historia al presente** (años análogos de El Niño) y
   dar una **acción**: qué sembrar y en qué ventana para no perder por helada o sequía.
2. **Nuestro premio es Local Impact**, igual que QUEÑARIS. Huánuco, 11 provincias,
   agricultura de secano, quechua, funciona sin señal. Eso es impacto local medible.
3. **Best Use of Data** es el segundo objetivo: NASA POWER (15 campañas), ENSO de NOAA
   y, si entra, SMAP para humedad de suelo real.
4. **Presentation manda el diseño.** Si en 30 segundos no se ve "siembra esto, en esta
   fecha, porque el agua y la helada dicen esto", el diseño está mal. De ahí la
   pantalla única con pasos y la barra de campaña siempre visible.
5. **Creativity**: el cuy Kusi que habla con voz de servidor y guía paso a paso es
   diferenciador real, y encaja con que un ganador fuese literalmente minijuegos.

## El replanteo: de recomendador a plan de campaña

Hoy la app responde **«qué sembrar»** y lo justifica con historia. Para ganar tiene que
responder **«qué siembro, cuándo, y qué pérdida me evita»**. Ese es el giro entero.

### La frase de los 30 segundos

Todo el producto debe caber en esto, dicho por Kusi y visible en una pantalla:

> «Siembra **tarwi** entre el **15 de noviembre y el 5 de diciembre**. Esquivas la
> helada y aprovechas la lluvia. En años de El Niño como este, funcionó en **4 de 5**.»

Cultivo + ventana + pérdida evitada + evidencia condicionada. Si una pieza de la app no
sirve para construir esa frase, sobra.

### Los cuatro cambios

1. **Predicción condicionada, no promedio de 15 años.** Usar solo las campañas con el
   mismo estado del Pacífico que la actual (El Niño: 2014, 2015, 2018, 2019, 2023).
   Datos reales de NASA POWER filtrados por el ONI de NOAA. Deja de ser «historia» y
   pasa a ser «qué pasó las veces que el año se parecía a este». Es nuestro
   **Best Use of Data** y responde la objeción de fondo.
2. **Ventana de siembra, no solo cultivo.** El motor ya tiene `tmin` por bloque de diez
   días: barrer las fechas posibles y devolver el rango donde el riesgo de helada cae y
   la cobertura de agua sube. Eso es lo que **evita la pérdida**, y es acción, no dato.
3. **Las dos pérdidas con nombre: helada y sequía.** Cada opción muestra dos medidores,
   agua y helada, con su cifra condicionada. Si el riesgo es alto, se dice y se propone
   la alternativa.
4. **Comparar rotaciones, que es lo que pide el reto.** El reto pide explícitamente
   comparar **al menos tres estrategias, incluida la actual**, con su evidencia, sus
   beneficios, sus vacíos y sus riesgos. Hoy mostramos tres cultivos sueltos. Hay que
   mostrar: **«repetir lo del año pasado»** frente a dos rotaciones alternativas, y para
   cada una por qué se sugiere y qué no sabemos. Esto es **Relevance** literal.

### Humedad de suelo de NASA: ya la teníamos al alcance

El reto pide aprovechar las bases de la NASA, y la humedad de suelo es la primera que
lista (SMAP). No hace falta montar el pipeline de SMAP para empezar: **NASA POWER sirve
humedad de suelo en la misma llamada que ya hacemos**, sin autenticación y con serie
larga. Comprobado el 15-nov-2026 contra la celda de Huánuco (lat -9,93, lon -76,24):

| Parámetro | Qué es | Unidad |
|---|---|---|
| `GWETTOP` | Humedad de la superficie | fracción 0-1 |
| `GWETROOT` | **Humedad de la zona de raíces** | fracción 0-1 |
| `GWETPROF` | Humedad del perfil completo | fracción 0-1 |

Serie real devuelta: la zona de raíces sube de **0,50 el 1-oct** a **0,60 el 10-nov**,
acompañando a la lluvia. Es dato utilizable hoy.

**Honestidad sobre la fuente:** `GWET*` procede de MERRA-2, el reanálisis de la NASA, es
decir **humedad modelada y asimilada por la NASA**, no la medición directa del satélite
SMAP. Hay que decirlo así en el informe. SMAP puro exigiría Earthdata con credenciales
y otro pipeline; queda como ampliación, no como requisito.

**Qué cambia en el motor.** Hoy el balance arranca suponiendo `capacidad * 0,5` cuando
no hay lluvia reciente, es decir, adivinando con cuánta agua empieza la campaña. Con
`GWETROOT` el balance **arranca con la humedad observada**. Eso convierte el modelo en
observación más modelo, que es literalmente lo que pide el reto, y permite responder la
pregunta que importa el día de la siembra: **¿la tierra está lista?**

Además sirve para los análogos: comparar la humedad de raíz de hoy con la que había en
la misma fecha de las campañas de El Niño anteriores.

### Que acierte todo el año: cuatro capas, no una

El Niño dura unos meses. Si la predicción cuelga solo de él, la app se queda muda
medio año. La salida es **una capa por horizonte temporal, con peso dinámico**:

| Capa | Qué responde | Horizonte | Fuente | ¿Todo el año? |
|---|---|---|---|---|
| 1. **Tierra hoy** | ¿Está lista para sembrar? | hoy | `GWETROOT` en percentil | sí |
| 2. **Sequía acumulada** | ¿Vengo de déficit? | 1-6 meses | SPI de lluvia POWER | sí |
| 3. **Empuje estacional** | ¿La campaña viene seca o húmeda? | 3-6 meses | ONI de NOAA | solo con señal |
| 4. **Normal y tendencia** | ¿Qué es normal aquí y hacia dónde va? | años | POWER 1981-2026 | sí |

**La regla que lo sostiene:** cuando el ONI está entre -0,5 y +0,5 (Neutral), la capa 3
pesa **cero** y mandan la 1, la 2 y la 4. Nunca hay un mes sin respuesta.

**Base histórica comprobada (15-nov-2026):** `GWETROOT` devuelve serie desde **1981**
para la celda de Huánuco, 365 días válidos en 1981. Son **45 años**, muy por encima de
los 30 que la OMM pide para un índice de sequía. Es decir:

- **Capa 1 — percentil de humedad de raíz.** Comparar la humedad de hoy con la de esa
  misma fecha en 45 años. «La tierra está en el percentil 20: más seca de lo normal para
  un 15 de noviembre.» Eso es una anomalía de humedad de suelo en el sentido del reto, y
  es la señal más accionable que existe el día de sembrar.
- **Capa 2 — SPI (Standardized Precipitation Index)** a 1, 3 y 6 meses con la lluvia de
  POWER sobre esos 45 años. Es el índice de sequía estándar de la OMM, reconocible por
  cualquier jurado, y se calcula solo con lo que ya descargamos.
- **Capa 4 — tendencia.** Pendiente de lluvia y de temperatura entre 1981 y 2026 para
  decir si la zona se está secando o calentando. El reto pide explícitamente
  «long-term water trends».

### Cómo se mantiene al día sin gastar el teléfono

El VPS (que ya existe y ya sirve la voz) baja POWER **una vez al día por celda**,
recalcula percentiles, SPI y tendencias, y publica un `.season` firmado pequeño. El
teléfono solo descarga ese fichero cuando hay señal. Es lo que el propio README ya
señalaba como la evolución correcta, y evita que 23 celdas × N teléfonos golpeen la API
de NASA. La app sigue funcionando sin señal con el último pack válido.

### Lo que ya tenemos y hay que subir a primer plano

Funciona **sin señal**, cubre **las 11 provincias de Huánuco** con datos reales, el
quechua está contemplado y **Kusi habla**. Eso es exactamente el perfil del premio
**Local Impact** que ganó QUEÑARIS. No es decoración: es el argumento.

### Lo que sobra

Todo lo que no entre en la frase de 30 segundos: procedencias técnicas en pantalla,
párrafos de método, cifras sin acción. El rigor va al documento y a la sección de
razones, no a la primera pantalla.

## Lo que falta confirmar

Los pesos exactos de cada criterio están en la Judging and Awards Guide 2026 oficial;
`spaceappschallenge.org` no se deja leer desde aquí (error de certificado), así que esa
parte no está verificada. Las categorías de premio y el límite de 30 s / 7 slides sí.
