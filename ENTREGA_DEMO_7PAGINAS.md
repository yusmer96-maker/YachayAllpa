# Demo de 7 páginas (Space Apps)

Guion de trabajo. **El PDF final va en INGLÉS**: la página del proyecto y todo lo
enlazado deben estar en inglés para entrar al juzgamiento global. Solo un video puede
hablarse en otro idioma, y con subtítulos en inglés.

## Reglas que manda la organización
- Demo: video de 30 s **o** 7 páginas como máximo, portada incluida. El jurado deja de
  mirar al pasar de ahí.
- Debe verse el nombre del equipo, los integrantes y el reto elegido.
- Debe mostrar cómo los datos abiertos de NASA hicieron posible el proyecto.
- Se cuelga fuera del sitio (GitHub o Drive) con enlace público, sin pedir permiso.
- Pocas palabras por página y buenas imágenes.

## Cómo puntúan (los cinco criterios)
| Criterio | Qué preguntan | Dónde lo respondemos |
|---|---|---|
| Impact | ¿Resuelve un problema grande? ¿A cuántos ayuda? | pág. 2 y 7 |
| Creativity | ¿Es novedoso o una mejora menor? | pág. 3 y 4 (offline + voz del lugar) |
| Validity | ¿Es científicamente válido? ¿Funciona en el mundo real? | pág. 5 |
| Relevance | ¿Responde al reto? ¿Está completo y es usable? | pág. 3 y 6 |
| Presentation | ¿Cuentan bien la historia? | todas |

La propia guía avisa: muchos equipos cuidan los cuatro primeros y descuidan el quinto.

## Las 7 páginas

**1. Portada.** Nombre, una frase que diga qué hace ("Qué sembrar, cuándo y por qué, con
datos de la NASA, sin señal"), el teléfono real con la pantalla de recomendación,
equipo, integrantes y reto. Nada más.

**2. El problema, con una cifra.** En la sierra de Huánuco se siembra una campaña al año
en secano: si la lluvia falla, se pierde el año. Los datos de la NASA existen pero no
llegan a la chacra: están en inglés, en mapas globales y piden internet. Cerrar con un
dato propio: a 1950 m, en 15 campañas, el trigo solo alcanza el 68 % de su cosecha por
falta de agua.

**3. Qué hace la app, en tres pantallas reales.** Una sola acción arriba ("Espera la
lluvia para sembrar trigo"), el porqué con medidores, y la rotación de las próximas
campañas. Subrayar: una recomendación, no un tablero de datos.

**4. Arquitectura y flujo (el diagrama).** Dos mitades:
- *Servidor (una vez al día):* NASA POWER (15 campañas) → packs firmados ECDSA P-256;
  NASA GPM IMERG (lluvia hasta ayer) + Open-Meteo (7 días) → JSON pequeño por zona.
- *Teléfono:* descarga cuando hay señal, guarda, y **calcula sin conexión**: balance de
  agua, helada, rotación y decisión.
Flecha de vuelta: sin señal sigue aconsejando con lo guardado. Ese es el hallazgo de
diseño: lo pesado arriba, la decisión abajo.

**5. La ciencia (validez).** Cadena de una ojeada:
lluvia efectiva USDA → ETo FAO-56 corregida a la altura de la chacra (−6,5 °C/km) →
ETa/ETc → cosecha esperada FAO-33 (1 − Ky·(1 − ETa/ETc)) → veredicto.
Inicio de siembra: 20 mm en 3 días (Sivakumar, 1988). Todo sobre 15 campañas, no un año.
Decir también lo que falta: validar con INIA. Eso suma, no resta.

**6. Datos de la NASA, uno por uno.** Qué aporta cada fuente, en una línea:
- POWER (MERRA-2): las 15 campañas del balance de agua.
- GPM IMERG Late: la lluvia medida hasta ayer, que fija la humedad de arranque.
- GPM IMERG Final: lo normal de 15 años, para comparar con lo mismo.
- GIBS / ASTER GDEM: el relieve del mapa, que funciona sin internet.
Añadir el detalle que demuestra criterio: POWER rellena los meses recientes con GEOS-IT,
que sesga a húmedo, y por eso cortamos en el último mes fiable.

**7. Impacto y lo que sigue.** A quién sirve (23 zonas de Huánuco, español y quechua,
sin señal, teléfonos de gama baja), qué sigue (validación con INIA, meses de siembra
reales del MIDAGRI, comparación con PISCO de SENAMHI) y los enlaces: repositorio y APK.

## Qué falta para hacerlo
1. El nombre del reto elegido y el nombre del equipo con sus integrantes.
2. Capturas finales en inglés (hace falta una pasada de la app con textos en inglés, o
   capturas en español con rótulos en inglés al lado).
3. El diagrama de la página 4, que se dibuja a partir de este guion.
