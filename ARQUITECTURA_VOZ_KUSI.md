# Voz e idioma de Kusi

## Qué hay ahora

Kusi escribe en **castellano o en quechua de Huánuco**, y **habla solo en
castellano** con el lector de voz del propio teléfono (Android TTS). No hay
servidor de voz, no hay clave de terceros en el APK y no se manda nada a nadie
para hablar: la app pide red únicamente para la lluvia de NASA POWER y, si está
configurada, para los índices del VPS.

Se quitó Fish Audio (15-nov-2026). Con él se fueron el cliente `VozKusi`, el
catálogo de audio del servidor, el token dentro del APK y la carpeta
`servidor-voz/`.

## Una sola fuente de texto

```text
Textos.kt  (claves -> castellano | quechua)
    |
    +--> pantalla: lo que se lee
    |
    +--> Voz.decir(frase en castellano)  ->  Android TTS
```

La voz lee **exactamente la frase que está escrita**. Antes había dos listas
(el texto de la pantalla y el catálogo de frases del servidor) que podían
separarse; ahora la pantalla pasa la versión castellana de la misma clave.

- `Voz.decir(texto)`: lee si la frase es nueva. Al recomponer la pantalla o
  girar el teléfono no vuelve a sonar.
- `Voz.repetir(texto)`: es el botón del altavoz. Manda el usuario: suena
  siempre y, si Kusi estaba hablando, lo calla.
- En quechua el altavoz no se muestra, porque Android no trae esa voz.

## El idioma

Se pregunta una vez, al abrir la app, con los dos botones escritos cada uno en
su idioma. Se guarda en `SharedPreferences("ajustes")` y se cambia cuando se
quiera en **Ajustes > Idioma**, sin perder lo ya respondido.

El motor (`core/`) **no tiene frases dentro**: devuelve claves con sus cifras
(`Motivo`, `Veredicto`, `PronosticoCampana.rumbo`) y la app las escribe en el
idioma elegido. Así la misma cuenta se lee igual en los dos idiomas.

## Quechua: estado

Variedad del **Huallaga (Huánuco)**, con los préstamos castellanos que se usan
de verdad en la zona. Las cifras, las unidades y los nombres de provincia no se
traducen. Está **pendiente de revisión con hablantes de Huánuco**; la propia
ventana de idioma lo dice.

`herramientas/revisar_textos.py` comprueba que las dos tablas tengan las mismas
claves, los mismos huecos de formato (`%s`, `%d`, `%%`), ninguna clave repetida,
ninguna frase quechua vacía o idéntica al castellano sin motivo, y que el UI no
pida claves que no existan ni deje claves sin usar.
