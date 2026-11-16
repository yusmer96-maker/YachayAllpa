# Plan de análisis de temporada — Yachay Allpa

Fecha del análisis: 14 de noviembre de 2026. Este documento especifica la siguiente versión del motor; **la APK actual aún usa el modelo anterior**.

## Diagnóstico verificable

El motor actual suma cuatro notas: agua 30, altitud 20, helada 20 y rotación 30. El agua se calcula con `min(1, 0,8 × lluvia media mensual del ciclo / agua referencial del cultivo)`. Cuando el cociente llega a 1, dos cultivos con lluvias suficientes reciben los mismos 30 puntos aunque sus riesgos sean distintos. Los meses de siembra son fijos y los seis distritos cargados caen en la misma celda NASA POWER (elevación de celda 3404,48 m); solo la altura de la chacra introduce una diferencia climática entre esos packs. Por eso se repiten opciones y los 100 puntos son frecuentes.

La comparación 1991–2005 frente a 2011–2025 describe un cambio de promedios; **no responde si la temporada 2026–2027 trae suficiente lluvia**. La pantalla principal dejará de mostrar 1991 como si fuera una fecha de actualización. La comparación de largo plazo quedará en una explicación opcional.

Consulta reproducible a NASA POWER para la celda de Huánuco:

`https://power.larc.nasa.gov/api/temporal/daily/point?parameters=PRECTOTCORR,T2M_MIN&community=AG&longitude=-76.24&latitude=-9.93&start=20260901&end=20261112&format=CSV`

El 14/11/2026 la API entregó 72 días válidos hasta el **11/11/2026**; el 12/11 figura como `-999` y se excluye. La respuesta cruda quedó guardada en `datos-nasa/huanuco-2026-09-01_2026-11-12.csv` (SHA-256 `2396937392c21bb78b4cc45927a36b2210d41070c9e3ad455e7a4e58439a731d`). La lluvia acumulada del 01/09 al 11/11 fue **327,8 mm**. Para idénticos días en 2011–2025, el promedio de los 15 años fue **97,5 mm** y el máximo anual **216,7 mm**. Octubre de 2026 registró **250,3 mm**, frente a **53,8 mm** de promedio para octubre en 2011–2025. Son datos de una celda amplia y deben contrastarse con una estación o agricultores locales antes de afirmar lo ocurrido en una chacra concreta.

## La pregunta que resolverá el motor

**Para esta chacra y este cultivo: ¿conviene sembrar ahora, esperar o dejar descansar, dados la lluvia observada, el agua que necesitará el cultivo y el riesgo de helada?** Es la misma estructura útil de 2plant: una decisión comprensible primero; los mapas, cálculos y fuentes después.

## Datos y actualización

| Dato | Fuente y frecuencia | Uso |
|---|---|---|
| Lluvia diaria reciente | NASA POWER `PRECTOTCORR`, actualización del pack al recibir nuevos datos | Acumulados de 7, 15 y 30 días; fecha de corte y anomalía de la temporada |
| Temperatura mínima diaria | NASA POWER `T2M_MIN` y altitud GPS válida | Riesgo de helada por etapa; indicar que la corrección de altura es aproximada |
| Serie diaria histórica | NASA POWER, años completos 2011–2025 | Distribuciones de lluvia y heladas por ventana del calendario; nunca etiquetar como pronóstico |
| Lluvia espacial más fina | NASA GPM IMERG Late Run, 0,1° y acceso PPS por verificar | Reducir la repetición causada por la celda POWER; registrar versión y calidad |
| Pronóstico estacional | NASA GEOS-S2S-3, solo cuando haya acceso y validación local | Probabilidad prospectiva; no simular un pronóstico con promedios históricos |

El teléfono seguirá **sin permiso de internet**. El pipeline descargará los datos, calculará y firmará un `.pack` con `fechaUltimoDato`, URLs, resolución espacial, porcentaje de días válidos y versión del algoritmo. La app mostrará “NASA actualizado al 11/11/2026” o la fecha real de cada pack. Si tiene más de 14 días, mostrará “Datos antiguos” y no usará la palabra “ahora” para el clima.

## Cálculos de la siguiente versión

1. **Inicio de lluvias observado.** Calcular la lluvia acumulada de 7/15/30 días y la racha seca desde septiembre. Comparar cada ventana con la misma ventana del calendario en 2011–2025. Mostrar tanto los milímetros como el rango histórico; un percentil por sí solo oculta la cantidad real.
2. **Agua necesaria por etapa.** Calcular `ETc(día) = Kc(etapa) × ET0(día)` según FAO-56, con variables meteorológicas NASA POWER. Para cada cultivo se necesitarán coeficientes y duración de germinación, crecimiento y floración validados por un agrónomo de Huánuco. Hasta entonces, el modelo actual de milímetros por ciclo es solo referencia y no debe dar un veredicto absoluto.
3. **Balance de secano.** Para cada ventana de 10 días, comparar lluvia útil estimada con `ETc`. Calcular déficit acumulado y el peor tramo, pues una suma suficiente en cinco meses puede ocultar dos semanas secas durante la germinación. La fracción de lluvia útil dependerá de supuestos explícitos y se mostrará un rango si no hay suelo medido.
4. **Lo que falta de la temporada.** Combinar la lluvia **observada** de 2026 con las continuaciones reales de cada año histórico para obtener varios escenarios. Reportar la fracción de escenarios que cubren la necesidad de agua; rotularla “riesgo histórico condicionado”, no “pronóstico NASA”. Si se integra un pronóstico GEOS-S2S-3 válido, reemplazar esta parte por sus miembros de ensamble y registrar fecha y habilidad del producto.
5. **Helada.** Estimar el riesgo en los días vulnerables usando Tmin diaria, dispersión histórica y corrección por altitud. Separar el frío ya observado del riesgo de los días futuros. Mantener visible la incertidumbre de microclima.
6. **Rotación y descanso.** Aplicar la secuencia andina y las alertas de mismo cultivo/familia como restricciones agronómicas; después ordenar por riesgo climático de la **fecha de siembra concreta**. Evitar que un `min(1, ...)` convierta varios candidatos en empates perfectos. Si los datos sustentan un empate, mostrarlo como empate y explicar cómo decidir en la chacra.

Para cada cultivo `c`, fecha de siembra `s` y continuación histórica `y`, el motor tendrá una trayectoria diaria `B_y(d) = B_y(d-1) + P_útil_y(d) - ETc_c(d)`. Se marcará un escenario con déficit si el balance cae bajo el umbral validado para germinación o floración; la helada será otro evento de fallo. La medida a ordenar será `escenarios sin fallo / escenarios completos`, junto con la gravedad del déficit y la regla de rotación. Los umbrales de semáforo se fijarán **después** del backtest y la validación agronómica; no se elegirán solo para obtener tres colores. Los escenarios históricos solo describen un rango plausible y no son una predicción meteorológica.

## Resultado en pantalla

Primera tarjeta: **“Conviene sembrar / Mejor esperar / Dejar descansar”**, cultivo y mes, semáforo, una razón principal y un dibujo simple de la lluvia de la temporada. Debajo: **“Llovió X mm en los últimos 30 días; lo habitual para estas fechas fue Y–Z mm”** y **“Datos NASA hasta DD/MM/AAAA”**. Un toque abre “¿Cómo lo calculamos?” con lluvia, demanda de agua, helada, rotación, fuente y supuestos. Los porcentajes solo aparecerán allí, con número de años/escenarios que los sustentan.

## Entrega y pruebas

1. Añadir descarga 2026 y validación de fechas, faltantes y procedencia; generar packs firmados nuevos.
2. Sustituir la nota de agua saturada por balance por ventanas, con casos de déficit temprano y lluvia abundante mal distribuida.
3. Probar con años históricos fuera de muestra: para cada año, simular una decisión con solo datos disponibles hasta la fecha de corte y comprobar si el riesgo anticipó déficit/helada. Comparar con el motor actual.
4. Validar umbrales y frases con agrónomos y agricultores de Huánuco. Comparar lluvia satelital con SENAMHI/pluviómetros si se consiguen registros.
5. Instalar en el Redmi A3 y probar sin red, con pack reciente, pack vencido, GPS con y sin altura, y tres escenarios de chacra que deban producir respuestas distintas.

## Fuentes

- NASA POWER Daily API: https://power.larc.nasa.gov/docs/services/api/temporal/daily/
- NASA POWER, latencia y resolución: https://power.larc.nasa.gov/docs/faqs/data/
- NASA GPM IMERG Late GIS: https://gpm.nasa.gov/data/directory/imerg-late-run-pps-near-real-time-gis
- NASA GEOS-S2S-3: https://gmao.gsfc.nasa.gov/seasonal-decadal-analysis_prediction/
- FAO-56, `ETc = Kc × ET0`: https://www.fao.org/4/x0490e/x0490e0a.htm
- Proyecto 2plant, ganador global 2024: https://www.spaceappschallenge.org/nasa-space-apps-2024/find-a-team/nvs-knot/?tab=project
