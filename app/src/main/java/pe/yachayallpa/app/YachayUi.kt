package pe.yachayallpa.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pe.yachayallpa.core.Clima
import pe.yachayallpa.core.ConsejoTemporada
import pe.yachayallpa.core.Cultivo
import pe.yachayallpa.core.Decision
import pe.yachayallpa.core.MotorTemporada
import pe.yachayallpa.core.Motivo
import pe.yachayallpa.core.PlanRotacion
import pe.yachayallpa.core.Temporada
import pe.yachayallpa.core.Veredicto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private val Fondo = Color(0xFFF8F7F3)
private val Tinta = Color(0xFF2C4A40)
private val TintaSuave = Color(0xFF6B8178)
private val Verde = Color(0xFF3B6958)
private val Salvia = Color(0xFFE5EEE7)
private val Lavanda = Color(0xFFF0EDF7)
private val Durazno = Color(0xFFF8ECE3)
private val Borde = Color(0xFFE6ECE6)
private val Onest = FontFamily(
    Font(R.font.onest_regular, FontWeight.Normal),
    Font(R.font.onest_medium, FontWeight.Medium),
    Font(R.font.onest_semibold, FontWeight.SemiBold),
    Font(R.font.onest_bold, FontWeight.Bold),
    Font(R.font.onest_extrabold, FontWeight.ExtraBold)
)

@Composable
private fun Letra(
    valor: String, tam: Int = 16, peso: FontWeight = FontWeight.Normal,
    color: Color = Tinta, modifier: Modifier = Modifier, lineas: Int = Int.MAX_VALUE,
    centrado: Boolean = false, espaciado: Float = 0f, minLineas: Int = 1
) {
    Text(valor, modifier = modifier, color = color, fontFamily = Onest,
        fontWeight = peso, fontSize = tam.sp, lineHeight = (tam * 1.32f).sp,
        letterSpacing = espaciado.sp,
        maxLines = lineas, minLines = minLineas, overflow = TextOverflow.Ellipsis,
        textAlign = if (centrado) TextAlign.Center else TextAlign.Start)
}

/** Icono vectorial, centrado por su propio lienzo (no por la línea de texto). */
@Composable
private fun Icono(res: Int, tam: Int = 20, color: Color = Verde, modifier: Modifier = Modifier) {
    Icon(painterResource(res), null, modifier.size(tam.dp), tint = color)
}

/** Etiqueta pequeña en mayúsculas: el "eyebrow" calmado del estilo voxio. */
@Composable
private fun Eyebrow(texto: String, color: Color = TintaSuave, modifier: Modifier = Modifier) {
    Letra(texto.uppercase(Locale("es", "PE")), 10, FontWeight.Medium, color,
        modifier = modifier, espaciado = 0.8f)
}

/**
 * El color de un botón dice lo que hace, no solo si es importante:
 * PRINCIPAL la acción de la pantalla; LUGAR lo de la chacra y su ubicación;
 * TIERRA lo de los cultivos; AGUA lo de lluvia y agua; NEUTRO cerrar o cancelar.
 */
enum class Tono(val claro: Color, val oscuro: Color, val tinta: Color) {
    PRINCIPAL(Color(0xFF3B6958), Color(0xFF3B6958), Color.White),
    LUGAR(Color(0xFFEDF4EE), Color(0xFFE3EEE5), Color(0xFF3B6958)),
    TIERRA(Color(0xFFFBEFE4), Color(0xFFF4E1CE), Color(0xFF7A5530)),
    AGUA(Color(0xFFF3F1FA), Color(0xFFE7E3F4), Color(0xFF4E4A86)),
    NEUTRO(Color(0xFFF4F4F1), Color(0xFFEBEBE6), Color(0xFF2C4A40))
}

/**
 * El botón de toda la app: plano, del color de su función, con un borde casi
 * imperceptible del mismo tono, icono opcional, la misma forma y la misma altura.
 */
@Composable
private fun Boton(
    texto: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    principal: Boolean = false, icono: Int? = null, tono: Tono? = null
) {
    val forma = RoundedCornerShape(16.dp)
    val t = if (principal) Tono.PRINCIPAL else tono ?: Tono.LUGAR
    val tinta = t.tinta
    Row(modifier.heightIn(min = 50.dp)
        .clip(forma)
        .background(Brush.verticalGradient(listOf(t.claro, t.oscuro)))
        .border(1.dp, if (t == Tono.PRINCIPAL) Color(0xFF2E5546).copy(alpha = .30f)
            else t.tinta.copy(alpha = .16f), forma)
        .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (icono != null) {
            Icono(icono, 18, tinta)
            Spacer(Modifier.width(8.dp))
        }
        Letra(texto, 15, if (t == Tono.PRINCIPAL) FontWeight.SemiBold else FontWeight.Medium, tinta,
            centrado = true, lineas = 2)
    }
}

/** El cierre de toda la app: verde sólido, X blanca y aro blanco. */
@Composable
private fun BotonCerrar(modifier: Modifier = Modifier, tamano: Int = 36, cerrar: () -> Unit) {
    Box(modifier.size(tamano.dp).shadow(4.dp, CircleShape, spotColor = Tinta)
        .clip(CircleShape).background(Color.White).padding(2.dp)
        .clip(CircleShape).background(Verde)
        .clickable(onClick = cerrar), contentAlignment = Alignment.Center) {
        Icono(R.drawable.ic_cerrar, 15, Color.White)
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Eyebrow(texto, Verde, modifier = Modifier.padding(bottom = 7.dp))
}

/** Tarjeta blanca elevada al estilo voxio: 18 px, borde fino y sombra suave. */
private fun Modifier.tarjeta(radio: Int = 18) = this
    .shadow(7.dp, RoundedCornerShape(radio.dp), spotColor = Tinta.copy(alpha = .55f),
        ambientColor = Tinta.copy(alpha = .35f))

@Composable
fun YachayPantalla(activity: YachayActivity) {
    // Una sola germinación de bienvenida; no se repite al girar el teléfono.
    var portadaVisible by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2400)
        portadaVisible = false
    }
    val zona = activity.zonaViva
    val altura = activity.altura
    val anterior = activity.cultivoAnterior
    val suelo = activity.suelo
    val prioridad = activity.prioridad
    val descanso = activity.siembrasSinDescanso
    val hoy = LocalDate.now()
    val todas = remember(zona, altura, anterior, suelo, prioridad, descanso, hoy) {
        if (zona != null && altura > 0 && anterior != null)
            MotorTemporada().recomendar(zona, altura, anterior, descanso,
                suelo, prioridad, hoy)
        else emptyList()
    }
    val consejos = todas.take(3)
    // La recomendación mira la lista entera (el descanso puede no estar entre
    // las tres de arriba) y el pronóstico de 7 días del VPS si lo hay.
    val datos = activity.datos
    val decision = remember(todas, datos, altura, hoy) {
        if (todas.isEmpty()) null
        else Decision.tomar(todas, datos?.paraDecision(hoy), altura, hoy)
    }
    // El paso vivo: 1 ubicar y altura, 2 ultima siembra, 3 opciones listas.
    val pasoReal = when {
        zona == null || altura == 0 -> 1
        anterior == null -> 2
        else -> 3
    }
    // Al volver a un paso ya hecho (tocando el camino o "Cambiar"), se ve ese paso.
    val paso = if (activity.repaso in 1 until pasoReal) activity.repaso else pasoReal
    MaterialTheme {
        // Un solo sitio decide el idioma; todas las pantallas leen de aquí.
        CompositionLocalProvider(LocalIdioma provides Idioma(activity.idioma)) {
            if (activity.cargando || (portadaVisible && activity.errorDatos == null)) {
                Carga()
            } else {
                Column(Modifier.fillMaxSize().background(Fondo).safeDrawingPadding()) {
                    Cabecera(activity, consejos.isNotEmpty() && paso == 3, decision)
                    CaminoPasos(paso, pasoReal, activity)
                    // El alto real que queda bajo la cabecera: la rueda se ajusta a él
                    // para que el paso de la siembra quepa entero, sin desplazarse.
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val altoLibre = maxHeight
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)) {
                            // La chacra, a la vista en cuanto se ubica: así se ve que
                            // hablamos de su lugar y no de uno cualquiera.
                            if (zona != null && paso < 3) {
                                Spacer(Modifier.height(2.dp))
                                TarjetaUbicacion(activity) { activity.ventana = Ventana.Ubicacion }
                                Spacer(Modifier.height(10.dp))
                            }
                            when {
                                zona == null || paso == 1 && altura > 0 -> PasoUbicar(activity)
                                altura == 0 -> PasoAltura(activity)
                                anterior == null || paso == 2 -> PasoSiembra(activity, altoLibre)
                                else -> Resultado(activity, consejos, decision, zona)
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
                Ventanas(activity)
            }
        }
    }
}

/** Cabecera: paisaje, marca, ajustes y la burbuja de Kusi, que es quien guia. */
@Composable
private fun Cabecera(activity: YachayActivity, hayConsejo: Boolean, decision: Decision?) {
    val T = LocalIdioma.current
    val clave = when {
        activity.errorDatos != null -> "datos.error"
        hayConsejo -> "kusi.resultado"
        activity.zona == null -> "kusi.inicio"
        activity.altura == 0 -> "kusi.altura"
        else -> "kusi.cultivo"
    }
    // Kusi solo escribe: no hay voz ni botón de audio.
    val frase = when {
        clave == "datos.error" -> T.f(clave, activity.errorDatos!!)
        // Con resultado: lo que recomienda, en una frase, y dónde ver la rotación.
        clave == "kusi.resultado" && decision != null -> when (decision.accion) {
            Decision.Accion.DESCANSAR -> T["kusi.descansar"]
            Decision.Accion.ESPERAR_LLUVIA -> T["kusi.esperar.lluvia"]
            Decision.Accion.ESPERAR_HELADA -> T["kusi.esperar.helada"]
            else -> T.f("kusi.sembrar", T.cultivo(decision.consejo.cultivo).lowercase(Locale("es", "PE")))
        }
        else -> T[clave]
    }
    Box(Modifier.fillMaxWidth().height(184.dp)) {
        Image(painterResource(R.drawable.paisaje_cabecera), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color(0x8C2C4A40), Color(0x1F2C4A40), Color(0xF7F8F7F3)))))
        Row(Modifier.align(Alignment.TopStart).fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_launcher), T["portada.logo"],
                Modifier.size(32.dp).clip(CircleShape), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(9.dp))
            Letra("Yachay Allpa", 17, FontWeight.SemiBold, Color.White, Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(100.dp))
                .background(Color.White.copy(alpha = .26f))
                .clickable { activity.ventana = Ventana.Preferencias }
                .padding(start = 9.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icono(R.drawable.ic_ajustes, 15, Color.White)
                Spacer(Modifier.width(5.dp))
                Letra(T["app.ajustes"], 12, FontWeight.Normal, Color.White)
            }
        }
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth().tarjeta()) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically) {
                KusiMascota(Modifier.size(58.dp))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow("KUSI", Verde)
                    Spacer(Modifier.height(2.dp))
                    Letra(frase, 14, FontWeight.Normal, lineas = 2)
                }
            }
        }
    }
}

/**
 * El camino de los tres pasos: hitos unidos por una línea que se llena de verde
 * hasta el paso actual. Hecho lleva visto; el actual late suave; el que falta
 * espera en gris. Tocar un hito lleva a ese paso.
 */
@Composable
private fun CaminoPasos(paso: Int, pasoReal: Int, activity: YachayActivity) {
    val T = LocalIdioma.current
    val latido = rememberInfiniteTransition(label = "camino")
    val onda by latido.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "onda")
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        // La línea va de centro a centro de los hitos de los extremos.
        Canvas(Modifier.fillMaxWidth().height(30.dp)) {
            val tercio = size.width / 3f
            val x1 = tercio / 2f
            val x3 = size.width - tercio / 2f
            val y = size.height / 2f
            drawLine(Borde, Offset(x1, y), Offset(x3, y), 3.dp.toPx(), StrokeCap.Round)
            val hasta = x1 + (x3 - x1) * ((pasoReal - 1) / 2f)
            drawLine(Verde, Offset(x1, y), Offset(hasta, y), 3.dp.toPx(), StrokeCap.Round)
        }
        Row(Modifier.fillMaxWidth()) {
            for (n in 1..3) {
                // Hecho = ya completado, aunque se esté mirando un paso anterior.
                val hecho = pasoReal > n && n != paso
                val vivo = paso == n
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable {
                    // Cada hito vuelve a su pantalla: ubicar, la rueda de la siembra o los resultados.
                    if (n <= pasoReal) activity.verPaso(if (n == pasoReal) 0 else n)
                }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                        if (vivo) Box(Modifier.size(30.dp).scale(.8f + .45f * onda)
                            .clip(CircleShape).background(Verde.copy(alpha = .25f * (1f - onda))))
                        Box(Modifier.size(26.dp).clip(CircleShape)
                            .background(if (hecho || vivo) Verde else Color.White)
                            .then(if (hecho || vivo) Modifier
                                else Modifier.border(1.5.dp, Borde, CircleShape)),
                            contentAlignment = Alignment.Center) {
                            if (hecho) Icono(R.drawable.ic_check, 14, Color.White)
                            else Letra("$n", 12, FontWeight.SemiBold,
                                if (vivo) Color.White else TintaSuave)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Letra(T["paso.$n"], 12, if (vivo) FontWeight.Medium else FontWeight.Normal,
                        if (vivo) Tinta else TintaSuave, centrado = true, lineas = 1)
                }
            }
        }
    }
}

/** Kusi vivo: respira suave. Solo escribe; no tiene voz. */
@Composable
private fun KusiMascota(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "kusi")
    val respira by t.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "respira")
    val escala = 1f + 0.03f * sin(respira * 2f * PI.toFloat())
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.kusi_editorial), LocalIdioma.current["kusi.alt"],
            Modifier.size(62.dp).scale(escala), contentScale = ContentScale.Fit)
    }
}

/** Una sola frase que compara lo llovido con lo habitual. */
@Composable
private fun Comparacion(reciente: Double, habitual: Double) {
    val T = LocalIdioma.current
    val ref = habitual.coerceAtLeast(1.0)
    val veces = reciente / ref
    val (texto, color, fondo) = when {
        veces >= 1.15 -> Triple(T.f("clima.mas", "%.1f".format(veces)),
            Color(0xFF315E46), Color(0xFFEAF2E9))
        veces <= 0.85 -> Triple(T.f("clima.menos", (veces * 100).toInt()),
            Color(0xFF9A5148), Color(0xFFF9E8E3))
        else -> Triple(T["clima.normal"], Color(0xFF7B6229), Color(0xFFFFF3DB))
    }
    Surface(shape = RoundedCornerShape(100.dp), color = fondo, modifier = Modifier.fillMaxWidth()) {
        Letra(texto, 13, FontWeight.Medium, color,
            Modifier.fillMaxWidth().padding(vertical = 9.dp), centrado = true)
    }
}

/**
 * Los dos colores y la clave de la palabra de una opción, sacados del veredicto
 * de core: el mismo que ordena la lista y el mismo que pinta las dos barras.
 */
private fun paleta(v: Veredicto): Triple<Color, Color, Veredicto.Nivel> = when (v.nivel) {
    Veredicto.Nivel.BUENA -> Triple(Color(0xFFEAF2E9), Color(0xFF315E46), v.nivel)
    Veredicto.Nivel.CUIDADO -> Triple(Color(0xFFFFF3DB), Color(0xFF7B6229), v.nivel)
    Veredicto.Nivel.REVISAR -> Triple(Color(0xFFF9E8E3), Color(0xFF9A5148), v.nivel)
}

private fun palabraNivel(T: Idioma, n: Veredicto.Nivel): String = when (n) {
    Veredicto.Nivel.BUENA -> T["nivel.buena"]
    Veredicto.Nivel.CUIDADO -> T["nivel.cuidado"]
    Veredicto.Nivel.REVISAR -> T["nivel.revisar"]
}

/**
 * Una razón del motor, escrita en el idioma elegido y con sus cifras: ninguna
 * frase es fija para todos los cultivos, todas salen del cálculo de esa opción
 * en esta chacra.
 */
private fun textoMotivo(T: Idioma, m: Motivo, c: Cultivo): String = when (m.clave) {
    // Cobertura y milímetros que pide el ciclo de ESTE cultivo.
    "cobertura" -> T.f("motivo.cobertura", m.entero(), m.entero2())
    "estres" -> T.f("motivo.estres", m.entero())
    "calendario" -> T.f("motivo.calendario", T.mes(m.entero()))
    "altura_bien", "altura_limite", "altura_fuera" ->
        T.f("motivo." + m.clave, m.entero(), c.altitudMin, c.altitudMax)
    else -> T["motivo." + m.clave]
}

/** Qué hacer: sale del veredicto, no de una cuenta distinta. */
private fun queHacer(T: Idioma, c: ConsejoTemporada, v: Veredicto): String = when {
    c.cultivo == Cultivo.DESCANSO -> T["hacer.descanso"]
    v.nivel == Veredicto.Nivel.BUENA -> T["hacer.buena"]
    v.nivel == Veredicto.Nivel.CUIDADO -> when {
        v.agua == Veredicto.Nivel.CUIDADO -> T["hacer.cuidado.agua"]
        v.helada == Veredicto.Nivel.CUIDADO -> T["hacer.cuidado.helada"]
        else -> T["hacer.cuidado"]
    }
    v.agua == Veredicto.Nivel.REVISAR -> T["hacer.revisar.agua"]
    v.helada == Veredicto.Nivel.REVISAR -> T["hacer.revisar.helada"]
    else -> T["hacer.revisar"]
}

/** Envoltura comun de los pasos: una sola decision grande y centrada. */
@Composable
private fun TarjetaPaso(
    icono: Int, titulo: String, texto: String,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Spacer(Modifier.height(4.dp))
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Borde),
        modifier = Modifier.fillMaxWidth().tarjeta()) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(62.dp).clip(RoundedCornerShape(21.dp)).background(Salvia),
                contentAlignment = Alignment.Center) {
                Icono(icono, 28, Verde)
            }
            Spacer(Modifier.height(12.dp))
            Letra(titulo, 19, FontWeight.SemiBold, centrado = true)
            Spacer(Modifier.height(5.dp))
            Letra(texto, 13, FontWeight.Normal, TintaSuave, centrado = true, lineas = 3)
            Spacer(Modifier.height(16.dp))
            contenido()
        }
    }
}

@Composable
private fun PasoUbicar(activity: YachayActivity) {
    val T = LocalIdioma.current
    TarjetaPaso(R.drawable.ic_lugar, T["ubicar.titulo"], T["ubicar.texto"]) {
        Boton(T["ubicar.gps"], { activity.buscarGps() }, Modifier.fillMaxWidth(), principal = true,
            icono = R.drawable.ic_gps)
        Spacer(Modifier.height(8.dp))
        Boton(T["ubicar.provincia"], { activity.ventana = Ventana.Provincias }, icono = R.drawable.ic_mapa,
            modifier =
            Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Letra(T[activity.estadoGps], 12, FontWeight.Normal, TintaSuave, centrado = true, lineas = 2)
    }
}

@Composable
private fun PasoAltura(activity: YachayActivity) {
    val T = LocalIdioma.current
    TarjetaPaso(R.drawable.ic_altura, T["altura.titulo"], T["altura.texto"]) {
        Boton(T["altura.escribir"], { activity.ventana = Ventana.Altura },
            Modifier.fillMaxWidth(), principal = true, icono = R.drawable.ic_altura)
        Spacer(Modifier.height(8.dp))
        Boton(T["ubicar.gps.otra"], { activity.buscarGps() }, Modifier.fillMaxWidth(),
            icono = R.drawable.ic_gps)
    }
}

@Composable
private fun PasoSiembra(activity: YachayActivity, altoLibre: Dp) {
    val T = LocalIdioma.current
    // Lo que no es rueda: tarjeta de ubicación (~96), etiqueta, botón, márgenes (~110).
    val maxRueda = (altoLibre - 214.dp).coerceIn(220.dp, 360.dp)
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Borde),
        modifier = Modifier.fillMaxWidth().tarjeta(20)) {
        Column(Modifier.padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Eyebrow(T["rueda.eyebrow"], Verde)
            Spacer(Modifier.height(6.dp))
            RuedaChacra(activity.altura, maxRueda, { activity.ventana = Ventana.Cultivos }) {
                activity.elegirCultivo(it)
            }
            Spacer(Modifier.height(8.dp))
            Boton(T["rueda.todos"], { activity.ventana = Ventana.Cultivos },
                icono = R.drawable.ic_brote, tono = Tono.TIERRA)
        }
    }
}

/**
 * Los cinco pasos de la rotación andina, los mismos `Etapa` con que el motor
 * premia seguir la costumbre (papa de cabecera tras el descanso, luego oca,
 * olluco o mashua, después cebada o trigo, una legumbre y otra vez descanso).
 * Dentro de cada paso, los cultivos van por la superficie que ocupan en Huánuco
 * (INEI, Compendio 2025, cuadro 12.2): primero lo que más se siembra.
 */
private class GrupoRueda(val numero: Int, val fondo: Color, val cultivos: List<Cultivo>)

private val GruposRueda = listOf(
    GrupoRueda(1, Color(0xFFF6E3D3), listOf(Cultivo.PAPA)),
    GrupoRueda(2, Color(0xFFF9EEDF), listOf(Cultivo.OLLUCO, Cultivo.OCA, Cultivo.MASHUA,
        Cultivo.YUCA, Cultivo.CALABAZA, Cultivo.QUINUA, Cultivo.CAMOTE, Cultivo.YACON,
        Cultivo.ARRACACHA, Cultivo.KIWICHA)),
    GrupoRueda(3, Color(0xFFFFF1D6), listOf(Cultivo.MAIZ, Cultivo.ARROZ, Cultivo.MAIZ_AMARILLO,
        Cultivo.TRIGO, Cultivo.CEBADA, Cultivo.AVENA)),
    GrupoRueda(4, Color(0xFFE3EEE5), listOf(Cultivo.FRIJOL, Cultivo.HABAS, Cultivo.ARVEJA,
        Cultivo.TARWI, Cultivo.PALLAR, Cultivo.MANI)),
    GrupoRueda(5, Color(0xFFEFE8DD), listOf(Cultivo.DESCANSO))
)

/** Lo que se da a esta altura: el mismo margen que usa el motor (Cultivo.MARGEN_ALTURA). */
private fun aEstaAltura(c: Cultivo, altura: Int) = c.seDaA(altura)

/** El cultivo que representa un paso: el más sembrado que se da a esta altura. */
private fun representante(g: GrupoRueda, altura: Int): Cultivo =
    g.cultivos.firstOrNull { aEstaAltura(it, altura) } ?: g.cultivos.first()

/**
 * La rueda de la chacra: la rotación que la app usa para aconsejar, dibujada en
 * cinco sectores numerados en el orden en que se siembran, y una luz que da la
 * vuelta para que se lea el ciclo. En cada sector, el cultivo que más se siembra
 * a la altura de la chacra; se toca directamente. Kusi va al centro.
 */
@Composable
private fun RuedaChacra(altura: Int, maxLado: Dp, verTodos: () -> Unit,
                        elegir: (Cultivo) -> Unit) {
    val T = LocalIdioma.current
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    val giro = rememberInfiniteTransition(label = "rueda")
    val luz by giro.animateFloat(0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), label = "luz")
    val n = GruposRueda.size
    val paso = 360f / n
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val lado = minOf(maxWidth, maxLado)
        val p = entrada.value
        Box(Modifier.size(lado).graphicsLayer {
            alpha = p
            rotationZ = -30f * (1f - p)
            scaleX = .9f + .1f * p
            scaleY = .9f + .1f * p
        }) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.width * .48f
                val arco = Size(r * 2, r * 2)
                val esquina = Offset(c.x - r, c.y - r)
                // Sector k centrado en -90° + k·72°: el paso 1 arriba, en sentido horario.
                GruposRueda.forEachIndexed { k, g ->
                    drawArc(g.fondo, -90f + k * paso - paso / 2f, paso, true, esquina, arco)
                }
                for (k in 0 until n) {
                    val ang = Math.toRadians((-90f + k * paso - paso / 2f).toDouble())
                    drawLine(Color.White, c, Offset(c.x + r * kotlin.math.cos(ang).toFloat(),
                        c.y + r * kotlin.math.sin(ang).toFloat()), 3.dp.toPx())
                }
                // La luz que recorre el ciclo, con su estela, sobre el borde.
                drawArc(Verde.copy(alpha = .20f), luz - 50f, 50f, false, esquina, arco,
                    style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                val rl = Math.toRadians(luz.toDouble())
                val pl = Offset(c.x + r * kotlin.math.cos(rl).toFloat(),
                    c.y + r * kotlin.math.sin(rl).toFloat())
                drawCircle(Verde.copy(alpha = .25f), 7.dp.toPx(), pl)
                drawCircle(Verde, 3.5.dp.toPx(), pl)
                drawCircle(Color.White, size.width * .12f, c)
            }
            Image(painterResource(R.drawable.kusi_editorial), T["kusi.alt"],
                Modifier.align(Alignment.Center).size(lado * .19f), contentScale = ContentScale.Fit)
            // Un cultivo por paso, a media distancia entre Kusi y el borde.
            val radio = lado * .31f
            GruposRueda.forEachIndexed { k, g ->
                val cultivo = representante(g, altura)
                val ang = Math.toRadians((-90f + k * paso).toDouble())
                val x = lado / 2 + radio * kotlin.math.cos(ang).toFloat()
                val y = lado / 2 + radio * kotlin.math.sin(ang).toFloat()
                val aparece = (p - .25f - k * .08f).coerceIn(0f, .35f) / .35f
                ItemRueda(cultivo, g.numero, aparece, (lado / 300.dp).coerceIn(.8f, 1.1f),
                    Modifier.offset(x - 44.dp, y - 42.dp).width(88.dp)) { elegir(cultivo) }
            }
        }
    }
}

/** Un paso de la rueda: el dibujo en un círculo blanco, su número y el nombre debajo. */
@Composable
private fun ItemRueda(cultivo: Cultivo, numero: Int, aparece: Float, tamano: Float,
                      modifier: Modifier, tocar: () -> Unit) {
    val T = LocalIdioma.current
    Column(modifier.graphicsLayer {
        alpha = aparece
        scaleX = (.6f + .4f * aparece) * tamano
        scaleY = (.6f + .4f * aparece) * tamano
    }.clip(RoundedCornerShape(14.dp)).clickable(onClick = tocar).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(62.dp)) {
            Box(Modifier.size(56.dp).align(Alignment.Center)
                .shadow(5.dp, CircleShape, spotColor = Tinta.copy(alpha = .4f))
                .clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                ArteCultivo(cultivo, Modifier.fillMaxSize().padding(6.dp))
            }
            // El número del paso en la rotación, en una pastilla verde.
            Box(Modifier.size(20.dp).align(Alignment.TopStart).clip(CircleShape)
                .background(Verde), contentAlignment = Alignment.Center) {
                Letra("$numero", 11, FontWeight.SemiBold, Color.White)
            }
        }
        Spacer(Modifier.height(2.dp))
        Letra(T.cultivo(cultivo), 12, FontWeight.Medium, Tinta, centrado = true, lineas = 2)
    }
}

/** El resultado: la primera opcion manda, dos alternativas al lado y la lluvia. */
@Composable
private fun Resultado(activity: YachayActivity, consejos: List<ConsejoTemporada>,
                      decision: Decision?, zona: Temporada) {
    if (consejos.isEmpty()) return
    val T = LocalIdioma.current
    Spacer(Modifier.height(2.dp))
    // Se lee como una frase: en tu chacra, tras lo que sembraste, te recomiendo...
    BarraContexto(activity)
    Spacer(Modifier.height(10.dp))
    decision?.let {
        TarjetaDecision(it) { activity.ventana = Ventana.Razones(it.consejo) }
        Spacer(Modifier.height(12.dp))
    }
    Row(Modifier.fillMaxWidth().padding(start = 2.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Eyebrow(T["opciones.titulo"], TintaSuave, Modifier.weight(1f))
        // La rotación recomendada de las próximas campañas, en tono tierra (cultivos).
        Row(Modifier.clip(RoundedCornerShape(100.dp)).background(Tono.TIERRA.oscuro)
            .border(1.dp, Tono.TIERRA.tinta.copy(alpha = .16f), RoundedCornerShape(100.dp))
            .clickable { activity.ventana = Ventana.Rotacion }
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icono(R.drawable.ic_rotacion, 14, Tono.TIERRA.tinta)
            Spacer(Modifier.width(5.dp))
            Letra(T["plan.boton"], 12, FontWeight.Medium, Tono.TIERRA.tinta, lineas = 1)
        }
    }
    // Las tres tarjetas miden lo mismo: la fila toma la altura de la más alta y
    // cada nombre reserva dos líneas, sea "Papa" o "Maíz amiláceo".
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        consejos.forEach { c ->
            OpcionChica(c, Modifier.weight(1f)) { activity.ventana = Ventana.Razones(c) }
        }
    }
    Spacer(Modifier.height(10.dp))
    FilaClima(zona, activity.datos) { activity.ventana = Ventana.Clima }
}

/**
 * Dónde y tras qué: una franja con dos mitades que se tocan. A la izquierda la
 * chacra (relieve, provincia y altura) abre el mapa; a la derecha la última
 * siembra abre la rueda para cambiarla, con "Cambiar" bien visible.
 */
@Composable
private fun BarraContexto(activity: YachayActivity) {
    val T = LocalIdioma.current
    val mapa = activity.mapa
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Verde.copy(alpha = .16f)),
        modifier = Modifier.fillMaxWidth().tarjeta(18)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Row(Modifier.weight(1f).clickable { activity.ventana = Ventana.Ubicacion }
                .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Salvia),
                    contentAlignment = Alignment.Center) {
                    if (mapa != null) Image(mapa.imagen.asImageBitmap(), null, Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                    else Icono(R.drawable.ic_lugar, 20, Verde)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow(T["contexto.chacra"], Verde)
                    Letra(activity.provincia ?: "Huánuco", 14, FontWeight.Medium, Tinta, lineas = 1)
                    Letra("${activity.altura} m", 12, FontWeight.Normal, TintaSuave, lineas = 1)
                }
            }
            Box(Modifier.fillMaxHeight().padding(vertical = 10.dp).width(1.dp).background(Borde))
            Row(Modifier.weight(1f).clickable { activity.verPaso(2) }
                .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Salvia),
                    contentAlignment = Alignment.Center) {
                    activity.cultivoAnterior?.let { ArteCultivo(it, Modifier.fillMaxSize().padding(4.dp)) }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow(T["contexto.sembraste"], Verde)
                    Letra(activity.cultivoAnterior?.let { T.cultivo(it) } ?: "", 14,
                        FontWeight.Medium, Tinta, lineas = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Letra(T["siembra.cambiar"], 12, FontWeight.SemiBold, Verde)
                        Icono(R.drawable.ic_flecha, 12, Verde)
                    }
                }
            }
        }
    }
}

/** El título de la recomendación: un verbo, el cultivo y cuándo. */
private fun tituloDecision(T: Idioma, d: Decision): String {
    val cultivo = T.cultivo(d.consejo.cultivo).lowercase(Locale("es", "PE"))
    return when (d.accion) {
        Decision.Accion.DESCANSAR -> T["decision.DESCANSAR"]
        Decision.Accion.SEMBRAR_EN_MES -> T.f("decision.SEMBRAR_EN_MES", cultivo,
            T.mes(d.consejo.fechaSiembra?.monthValue ?: 1))
        else -> T.f("decision." + d.accion.name, cultivo)
    }
}

/** Cada cifra que sostiene la recomendación, escrita y con su icono. */
private fun textoPorque(T: Idioma, m: Motivo, d: Decision): Pair<Int, String> = when (m.clave) {
    "lluvia_prevista" -> R.drawable.ic_gota to T.f(
        if (d.accion == Decision.Accion.ESPERAR_LLUVIA) "porque.lluvia_poca"
        else "porque.lluvia_prevista", m.entero())
    "lluvia_medida" -> R.drawable.ic_gota to T.f("porque.lluvia_medida", m.entero())
    "helada_prevista" -> R.drawable.ic_helada to T.f("porque.helada_prevista", m.entero2(), m.entero())
    "cosecha_15" -> R.drawable.ic_check to T.f("porque.cosecha_15", m.entero())
    "riego" -> R.drawable.ic_gota to T.f("porque.riego", m.entero())
    "mes_bueno" -> R.drawable.ic_tiempo to T["porque.mes_bueno"]
    "ninguno_encaja" -> R.drawable.ic_gota to T.f("porque.ninguno_encaja", m.entero())
    else -> R.drawable.ic_brote to textoMotivo(T, m, d.consejo.cultivo)
}

/**
 * La recomendación: qué hacer ahora, con las cifras que lo sostienen (las 15
 * campañas NASA y el pronóstico de 7 días). Verde si es sembrar, ámbar si es
 * esperar, tierra si es descansar; siempre con su icono y su texto.
 */
@Composable
private fun TarjetaDecision(d: Decision, abrir: () -> Unit) {
    val T = LocalIdioma.current
    val siembra = d.accion == Decision.Accion.SEMBRAR_YA ||
        d.accion == Decision.Accion.SEMBRAR_CUANDO_LLUEVA
    val espera = d.accion == Decision.Accion.ESPERAR_LLUVIA ||
        d.accion == Decision.Accion.ESPERAR_HELADA
    // El color dice lo que significa: verde va bien, ámbar con cuidado, lavanda
    // esperar el clima (lluvia o helada) y tierra descansar.
    val (fondo, acento) = when {
        d.accion == Decision.Accion.DESCANSAR -> Color(0xFFF6ECE0) to Color(0xFF7A5530)
        espera -> Color(0xFFF1EFF9) to Color(0xFF4E4A86)
        siembra && d.veredicto.nivel == Veredicto.Nivel.BUENA -> Color(0xFFEAF2E9) to Color(0xFF315E46)
        else -> Color(0xFFFFF4DF) to Color(0xFF7B6229)
    }
    // Lo que se ve son, como mucho, dos motivos; la cosecha va en su chapa.
    // Si el título ocupa dos líneas (más de ~25 letras a 18 sp), solo el motivo que decide:
    // la pantalla cabe sin desplazarse y el resto sigue en "Ver por qué".
    val dosLineas = tituloDecision(T, d).length > 25 && d.accion != Decision.Accion.DESCANSAR
    val motivos = d.porque.filter { it.clave != "cosecha_15" }.take(if (dosLineas) 1 else 2)
    Surface(shape = RoundedCornerShape(20.dp), color = fondo,
        border = androidx.compose.foundation.BorderStroke(1.dp, acento.copy(alpha = .14f)),
        modifier = Modifier.fillMaxWidth().tarjeta(20).clickable(onClick = abrir)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(T["decision.eyebrow"], acento, Modifier.weight(1f))
                Letra(T["decision.ver"], 12, FontWeight.Medium, acento)
                Icono(R.drawable.ic_flecha, 13, acento)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = .85f)), contentAlignment = Alignment.Center) {
                    // Descansar lleva el dibujo de la tierra en descanso, como en las opciones.
                    ArteCultivo(if (d.accion == Decision.Accion.DESCANSAR) Cultivo.DESCANSO
                        else d.consejo.cultivo, Modifier.fillMaxSize().padding(4.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Letra(tituloDecision(T, d), 18, FontWeight.SemiBold, Tinta, lineas = 2)
                    if (d.accion != Decision.Accion.DESCANSAR) {
                        Spacer(Modifier.height(5.dp))
                        // El mismo veredicto que verá al entrar, y la cosecha esperada.
                        Chapa(if (d.veredicto.nivel == Veredicto.Nivel.BUENA) R.drawable.ic_check
                            else R.drawable.ic_alerta,
                            textoVeredicto(T, d.veredicto.nivel) + "  ·  " +
                                T.f("decision.cosecha", (d.consejo.rendimientoEsperado * 100).roundToInt()),
                            colorNivel(d.veredicto.nivel), Color.White.copy(alpha = .8f))
                    }
                }
            }
            if (motivos.isNotEmpty()) Spacer(Modifier.height(8.dp))
            motivos.forEach { m ->
                val (icono, texto) = textoPorque(T, m, d)
                Row(Modifier.padding(bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icono(icono, 14, acento)
                    Spacer(Modifier.width(7.dp))
                    // Sin chapa de veredicto, el descanso tiene sitio para su motivo entero.
                    Letra(texto, 13, FontWeight.Normal, Tinta, Modifier.weight(1f),
                        lineas = if (d.accion == Decision.Accion.DESCANSAR) 2 else 1)
                }
            }
            if (d.accion == Decision.Accion.SEMBRAR_CUANDO_LLUEVA)
                Letra(T["decision.sin_pronostico"], 12, FontWeight.Normal, TintaSuave, lineas = 2)
        }
    }
}

/** El nombre del veredicto, el mismo en la recomendación, las opciones y la ventana. */
private fun textoVeredicto(T: Idioma, n: Veredicto.Nivel): String = when (n) {
    Veredicto.Nivel.BUENA -> T["veredicto.buena"]
    Veredicto.Nivel.CUIDADO -> T["veredicto.cuidado"]
    Veredicto.Nivel.REVISAR -> T["veredicto.revisar"]
}

@Composable
private fun OpcionChica(consejo: ConsejoTemporada, modifier: Modifier, abrir: () -> Unit) {
    val T = LocalIdioma.current
    val (fondo, acento, nivel) = paleta(Veredicto.de(consejo))
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Borde),
        modifier = modifier.height(132.dp).tarjeta(16).clickable(onClick = abrir)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(fondo),
                contentAlignment = Alignment.Center) {
                ArteCultivo(consejo.cultivo, Modifier.fillMaxSize().padding(4.dp))
            }
            Letra(T.cultivo(consejo.cultivo), 13, FontWeight.Medium, centrado = true,
                lineas = 2)
            // La cosecha esperada (FAO-33) distingue opciones con el mismo veredicto.
            if (consejo.cultivo != Cultivo.DESCANSO)
                Letra(T.f("opcion.cosecha", (consejo.rendimientoEsperado * 100).roundToInt()), 11,
                    FontWeight.Normal, TintaSuave, centrado = true, lineas = 1)
            Box(Modifier.clip(RoundedCornerShape(100.dp)).background(acento.copy(alpha = .12f))
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Letra(palabraNivel(T, nivel), 9, FontWeight.Medium, acento, espaciado = 0.4f,
                    centrado = true, lineas = 1)
            }
        }
    }
}

@Composable
private fun FilaClima(zona: Temporada, datos: DatosZona?, abrir: () -> Unit) {
    val T = LocalIdioma.current
    val siete = remember(datos) { datos?.paraDecision(LocalDate.now()) }
    Surface(shape = RoundedCornerShape(16.dp), color = Lavanda,
        modifier = Modifier.fillMaxWidth().clickable(onClick = abrir)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = .7f)), contentAlignment = Alignment.Center) {
                Icono(R.drawable.ic_gota, 18, Verde)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                // Arriba lo que viene (pronóstico), abajo lo medido (NASA), con su fecha.
                Letra(siete?.let { T.f("pron.total", Math.round(it.lluvia7).toInt()) } ?: T["clima.fila"], 14,
                    FontWeight.Medium, Tinta, lineas = 1)
                Letra(T.f("clima.resumen", zona.lluvia30.roundToInt(), fechaCorta(zona.fechaCorte),
                    zona.promedio30.toInt()), 12, FontWeight.Normal, TintaSuave, lineas = 1)
            }
            Icono(R.drawable.ic_flecha, 18, Verde)
        }
    }
}

/** Una bienvenida breve: conocer el lugar, recordar la siembra y explorar opciones. */
@Composable
private fun Carga() {
    val T = LocalIdioma.current
    val compacto = LocalConfiguration.current.screenHeightDp < 700
    Column(Modifier.fillMaxSize().background(Fondo).safeDrawingPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = if (compacto) 16.dp else 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_launcher), T["portada.logo"],
                Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(10.dp))
            Letra("Yachay Allpa", 20, FontWeight.Bold)
        }
        Spacer(Modifier.height(if (compacto) 18.dp else 26.dp))
        Eyebrow(T["portada.eyebrow"], Verde)
        Spacer(Modifier.height(8.dp))
        Letra(T["portada.titulo"], if (compacto) 27 else 31,
            FontWeight.Bold, centrado = true)
        Spacer(Modifier.height(6.dp))
        Letra(T["portada.sub"], 14, color = TintaSuave, centrado = true)
        Image(painterResource(R.drawable.splash_aprender), T["portada.ilustracion"],
            Modifier.widthIn(max = 360.dp).fillMaxWidth()
                .height(if (compacto) 190.dp else 240.dp),
            contentScale = ContentScale.Fit)
        Row(Modifier.widthIn(max = 380.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PasoPortada("1", R.drawable.ic_lugar, T["portada.p1"], Salvia, Modifier.weight(1f))
            PasoPortada("2", R.drawable.ic_brote, T["portada.p2"], Durazno, Modifier.weight(1f))
            PasoPortada("3", R.drawable.ic_check, T["portada.p3"], Lavanda, Modifier.weight(1f))
        }
        Spacer(Modifier.height(if (compacto) 8.dp else 16.dp))
        BroteCarga()
        Letra(T["portada.cargando"], 12, FontWeight.Normal, TintaSuave,
            centrado = true)
        Spacer(Modifier.height(6.dp))
        Letra(T["portada.sin.senal"], 11, color = TintaSuave, centrado = true)
    }
}

@Composable
private fun PasoPortada(numero: String, icono: Int, texto: String, fondo: Color,
    modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(fondo),
            contentAlignment = Alignment.Center) {
            Icono(icono, 21)
        }
        Spacer(Modifier.height(7.dp))
        Letra("$numero. ${texto.substringBefore('\n')}", 11, FontWeight.Medium,
            centrado = true)
        Letra(texto.substringAfter('\n'), 11, color = TintaSuave, centrado = true)
    }
}

/** La carga es indeterminada: lluvia, raíces y hojas, sin porcentajes inventados. */
@Composable
private fun BroteCarga() {
    val alt = LocalIdioma.current["portada.brote.alt"]
    val ciclo = rememberInfiniteTransition(label = "siembra")
    val tiempo by ciclo.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "germinacion")
    Canvas(Modifier.size(width = 160.dp, height = 66.dp).semantics {
        contentDescription = alt
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    }) {
        fun crecer(desde: Float, hasta: Float): Float {
            val t = ((tiempo - desde) / (hasta - desde)).coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }
        val x = size.width / 2f
        val suelo = size.height * .73f
        val salida = 1f - crecer(.86f, 1f)
        val tallo = crecer(.24f, .66f) * salida
        val hojas = crecer(.44f, .78f) * salida
        val raiz = crecer(.10f, .43f) * salida
        val grosor = 2.dp.toPx()
        drawOval(Durazno, Offset(x - 32.dp.toPx(), suelo - 3.dp.toPx()),
            Size(64.dp.toPx(), 12.dp.toPx()))
        drawLine(Color(0xFFC4A58A), Offset(x - 48.dp.toPx(), suelo + 4.dp.toPx()),
            Offset(x + 48.dp.toPx(), suelo + 4.dp.toPx()), grosor, StrokeCap.Round)
        // Gotas en fases distintas: mojan la tierra y se desvanecen al aparecer las hojas.
        for (i in 0..2) {
            val fase = (tiempo * 2f + i * .28f) % 1f
            val opacidad = sin(fase * PI).toFloat().coerceIn(0f, 1f) * (1f - hojas) * .7f
            val gotaX = x + (i - 1) * 20.dp.toPx()
            val gotaY = 4.dp.toPx() + fase * 30.dp.toPx()
            drawLine(Color(0xFF7BA7A1).copy(alpha = opacidad),
                Offset(gotaX + 2.dp.toPx(), gotaY), Offset(gotaX, gotaY + 5.dp.toPx()),
                grosor, StrokeCap.Round)
        }
        drawLine(Color(0xFFB48A66), Offset(x, suelo + 3.dp.toPx()),
            Offset(x - 5.dp.toPx() * raiz, suelo + 14.dp.toPx() * raiz),
            1.5.dp.toPx(), StrokeCap.Round)
        drawLine(Color(0xFFB48A66), Offset(x, suelo + 7.dp.toPx() * raiz),
            Offset(x + 7.dp.toPx() * raiz, suelo + 12.dp.toPx() * raiz),
            1.5.dp.toPx(), StrokeCap.Round)
        drawOval(Color(0xFF9A714E), Offset(x - 4.dp.toPx(), suelo - 2.dp.toPx()),
            Size(8.dp.toPx(), 5.dp.toPx()))
        val punta = suelo - 29.dp.toPx() * tallo
        drawLine(Verde, Offset(x, suelo), Offset(x, punta), grosor, StrokeCap.Round)
        val ancho = 19.dp.toPx() * hojas
        val alto = 12.dp.toPx() * hojas
        val izquierda = Path().apply {
            moveTo(x, punta + alto)
            cubicTo(x - ancho, punta + alto, x - ancho, punta - alto * .5f, x, punta + alto)
            close()
        }
        val derecha = Path().apply {
            moveTo(x, punta + alto * .45f)
            cubicTo(x, punta - alto, x + ancho * 1.1f, punta - alto * .7f,
                x + ancho, punta - alto * .3f)
            cubicTo(x + ancho, punta + alto * .6f, x + ancho * .25f,
                punta + alto, x, punta + alto * .45f)
            close()
        }
        drawPath(izquierda, Color(0xFF7E9D69))
        drawPath(derecha, Verde)
    }
}


private fun fechaCorta(v: LocalDate): String = "${v.dayOfMonth}/${v.monthValue}"

private fun fecha(valor: LocalDate): String = valor.format(
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "PE")))

/**
 * El dibujo de cada cultivo, ya separado de su lámina por
 * herramientas/separar_cultivos.py (sin trozos de los dibujos vecinos); arroz y avena,
 * limpiados de su fondo con herramientas/limpiar_dibujo.py. Null dibuja un icono.
 */
private fun dibujo(c: Cultivo): Int? = when (c) {
    Cultivo.PAPA -> R.drawable.cultivo_papa
    Cultivo.QUINUA -> R.drawable.cultivo_quinua
    Cultivo.OCA -> R.drawable.cultivo_oca
    Cultivo.OLLUCO -> R.drawable.cultivo_olluco
    Cultivo.MAIZ -> R.drawable.cultivo_maiz
    Cultivo.CEBADA -> R.drawable.cultivo_cebada
    Cultivo.HABAS -> R.drawable.cultivo_habas
    Cultivo.TARWI -> R.drawable.cultivo_tarwi
    Cultivo.ARVEJA -> R.drawable.cultivo_arveja
    Cultivo.DESCANSO -> R.drawable.cultivo_descanso
    Cultivo.MAIZ_AMARILLO -> R.drawable.cultivo_maiz_amarillo
    Cultivo.FRIJOL -> R.drawable.cultivo_frijol
    Cultivo.YUCA -> R.drawable.cultivo_yuca
    Cultivo.MASHUA -> R.drawable.cultivo_mashua
    Cultivo.CAMOTE -> R.drawable.cultivo_camote
    Cultivo.YACON -> R.drawable.cultivo_yacon
    Cultivo.ARRACACHA -> R.drawable.cultivo_arracacha
    Cultivo.PALLAR -> R.drawable.cultivo_pallar
    Cultivo.TRIGO -> R.drawable.cultivo_trigo
    Cultivo.MANI -> R.drawable.cultivo_mani
    Cultivo.KIWICHA -> R.drawable.cultivo_kiwicha
    Cultivo.CALABAZA -> R.drawable.cultivo_calabaza
    Cultivo.ARROZ -> R.drawable.cultivo_arroz
    Cultivo.AVENA -> R.drawable.cultivo_avena
}

private object Imagenes {
    private val cache = mutableMapOf<Cultivo, Bitmap>()
    @Synchronized fun de(contexto: android.content.Context, cultivo: Cultivo): Bitmap? {
        val id = dibujo(cultivo) ?: return null
        return cache.getOrPut(cultivo) {
            BitmapFactory.decodeResource(contexto.resources, id)
                ?: error("Falta el dibujo de $cultivo")
        }
    }
}

@Composable
private fun ArteCultivo(cultivo: Cultivo, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val bitmap = remember(cultivo) { Imagenes.de(contexto, cultivo) }
    if (bitmap == null) {
        Box(modifier, contentAlignment = Alignment.Center) { Icono(R.drawable.ic_brote, 30, Verde) }
    } else {
        Image(bitmap.asImageBitmap(), LocalIdioma.current.cultivo(cultivo), modifier,
            contentScale = ContentScale.Fit)
    }
}

@Composable
private fun MarcoVentana(
    titulo: String, cerrar: () -> Unit, obligatorio: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val alto = (LocalConfiguration.current.screenHeightDp * .78f).dp
    Dialog(onDismissRequest = { if (!obligatorio) cerrar() },
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnBackPress = !obligatorio, dismissOnClickOutside = !obligatorio)) {
        Surface(shape = RoundedCornerShape(28.dp), color = Fondo,
            modifier = Modifier.fillMaxWidth(.92f).heightIn(max = alto)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Letra(titulo, 22, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (!obligatorio) BotonCerrar(cerrar = cerrar)
                }
                Spacer(Modifier.height(14.dp))
                contenido()
            }
        }
    }
}

@Composable
private fun Ventanas(activity: YachayActivity) {
    val T = LocalIdioma.current
    when (val actual = activity.ventana) {
        Ventana.Idioma -> MarcoVentana(T["idioma.titulo"], {}, obligatorio = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.kusi_editorial), T["kusi.nombre"],
                    Modifier.size(82.dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.width(10.dp))
                Letra(T["idioma.saludo"],
                    15, FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Letra(T["idioma.pregunta"], 16, FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            // Los dos botones se leen en los dos idiomas a la vez: quien elige
            // quechua no tiene que entender el castellano para encontrarlo.
            Boton(Textos.ES["idioma.es"]!!, { activity.elegirIdioma("es") },
                Modifier.fillMaxWidth(), principal = true)
            Spacer(Modifier.height(8.dp))
            Boton(Textos.QU["idioma.qu"]!!, { activity.elegirIdioma("qu") },
                Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Letra(T["idioma.nota"], 12, FontWeight.Normal, TintaSuave)
        }
        Ventana.Ubicacion -> VentanaUbicacion(activity)
        Ventana.Rotacion -> VentanaRotacion(activity)
        Ventana.Provincias -> Provincias(activity)
        Ventana.Altura -> MarcoVentana(T["altura.ventana"], { activity.ventana = null }) {
            var valor by remember { mutableStateOf("") }
            var incorrecto by remember { mutableStateOf(false) }
            Letra(T["altura.unidad"], 13, FontWeight.Normal, TintaSuave)
            Spacer(Modifier.height(13.dp))
            OutlinedTextField(value = valor, onValueChange = { valor = it; incorrecto = false },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium,
                    color = Tinta, fontSize = 19.sp),
                placeholder = { Letra(T["altura.ejemplo"], 16, FontWeight.Normal, TintaSuave) })
            if (incorrecto) {
                Spacer(Modifier.height(6.dp))
                Letra(T["altura.rango"], 13, FontWeight.Medium, Color(0xFF9A5148))
            }
            Spacer(Modifier.height(15.dp))
            Boton(T["altura.usar"], {
                if (!activity.guardarAltura(valor)) incorrecto = true
            }, Modifier.fillMaxWidth(), principal = true, icono = R.drawable.ic_check)
        }
        Ventana.Cultivos -> Cultivos(activity)
        Ventana.Preferencias -> Preferencias(activity)
        Ventana.Clima -> MarcoVentana(T["clima.titulo"], { activity.ventana = null }) {
            activity.zonaViva?.let { zona ->
                // Primero lo que viene: pronóstico real de 7 días (VPS, Open-Meteo).
                val dias = activity.datos?.desde(LocalDate.now()).orEmpty().take(7)
                if (dias.size >= 5) {
                    GraficoPronostico(dias, activity.datos!!.elevacionPronostico, activity.altura)
                    Spacer(Modifier.height(12.dp))
                }
                // Después lo medido por NASA, con la fecha de su último dato fiable.
                Comparacion(zona.lluvia30, zona.promedio30)
                Spacer(Modifier.height(10.dp))
                GraficoLluvia(zona)
                Spacer(Modifier.height(8.dp))
                Letra(T["clima.celda"], 11, FontWeight.Normal, TintaSuave, centrado = true,
                    modifier = Modifier.fillMaxWidth())
            }
        }
        is Ventana.Razones -> Razones(activity, actual.consejo)
        null -> Unit
    }
}

@Composable
private fun Provincias(activity: YachayActivity) {
    val T = LocalIdioma.current
    val alto = (LocalConfiguration.current.screenHeightDp * .78f).dp
    Dialog(onDismissRequest = { activity.ventana = null },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = Fondo,
            modifier = Modifier.fillMaxWidth(.92f).height(alto)) {
            Column(Modifier.padding(20.dp)) {
                Letra(T["provincias.titulo"], 22, FontWeight.SemiBold)
                Spacer(Modifier.height(13.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activity.provincias()) { provincia ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable { activity.elegirProvincia(provincia) }
                            .padding(horizontal = 13.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icono(R.drawable.ic_lugar, 17, Verde)
                            Spacer(Modifier.width(10.dp))
                            Letra(provincia, 15, FontWeight.Normal, Tinta, Modifier.weight(1f))
                            Icono(R.drawable.ic_flecha, 15, Verde)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Boton(T["app.cerrar"], { activity.ventana = null }, Modifier.fillMaxWidth(),
                    tono = Tono.NEUTRO)
            }
        }
    }
}

private val Raices = listOf(Cultivo.PAPA, Cultivo.OCA, Cultivo.OLLUCO, Cultivo.MASHUA,
    Cultivo.CAMOTE, Cultivo.YACON, Cultivo.ARRACACHA, Cultivo.YUCA)
private val Granos = listOf(Cultivo.QUINUA, Cultivo.MAIZ, Cultivo.CEBADA, Cultivo.HABAS,
    Cultivo.TARWI, Cultivo.ARVEJA, Cultivo.MAIZ_AMARILLO, Cultivo.FRIJOL, Cultivo.ARROZ,
    Cultivo.AVENA)
private val Mas = listOf(Cultivo.PALLAR, Cultivo.TRIGO, Cultivo.MANI, Cultivo.KIWICHA,
    Cultivo.CALABAZA, Cultivo.DESCANSO)

@Composable
private fun Cultivos(activity: YachayActivity) {
    val T = LocalIdioma.current
    var grupo by remember { mutableIntStateOf(0) }
    val nombres = listOf(T["cultivos.raices"], T["cultivos.granos"], T["cultivos.mas"])
    val lista = when (grupo) { 0 -> Raices; 1 -> Granos; else -> Mas }
    val alto = (LocalConfiguration.current.screenHeightDp * .82f).dp
    Dialog(onDismissRequest = { activity.ventana = null },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = Fondo,
            modifier = Modifier.fillMaxWidth(.94f).height(alto)) {
            Column(Modifier.padding(15.dp)) {
                Letra(T["cultivos.titulo"], 21, FontWeight.SemiBold)
                Letra(T["cultivos.toca"], 12, FontWeight.Normal, TintaSuave)
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    nombres.forEachIndexed { indice, nombre ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(100.dp))
                            .background(if (grupo == indice) Verde else Salvia)
                            .clickable { grupo = indice }.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center) {
                            Letra(nombre, 12, FontWeight.Medium,
                                if (grupo == indice) Color.White else Verde,
                                centrado = true)
                        }
                    }
                }
                Spacer(Modifier.height(11.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(lista) { cultivo ->
                        Surface(shape = RoundedCornerShape(19.dp), color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Borde),
                            modifier = Modifier.fillMaxWidth().clickable {
                                activity.elegirCultivo(cultivo)
                            }) {
                            Column(Modifier.padding(9.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.fillMaxWidth().height(90.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(if (grupo == 0) Durazno else Salvia),
                                    contentAlignment = Alignment.Center) {
                                    ArteCultivo(cultivo, Modifier.fillMaxSize().padding(4.dp))
                                }
                                Spacer(Modifier.height(7.dp))
                                Letra(T.cultivo(cultivo), 13, FontWeight.Medium, lineas = 2,
                                    minLineas = 2, centrado = true)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Boton(T["app.cerrar"], { activity.ventana = null }, Modifier.fillMaxWidth(),
                    tono = Tono.NEUTRO)
            }
        }
    }
}

/**
 * Conoce tu tierra: tres preguntas, cada una en su tarjeta con icono y sus
 * opciones como chips en una linea. Sin scroll: todo se ve de un golpe.
 */
@Composable
private fun Preferencias(activity: YachayActivity) {
    val T = LocalIdioma.current
    var suelo by remember { mutableStateOf(activity.suelo) }
    var prioridad by remember { mutableStateOf(activity.prioridad) }
    var descanso by remember { mutableIntStateOf(activity.siembrasSinDescanso) }
    // "No lo sé" queda en medio de "sí descansó" y "no descansó": cada
    // respuesta mueve el consejo, y la de en medio no inventa una historia.
    val valoresDescanso = listOf(2, 1, 4)
    Dialog(onDismissRequest = { activity.ventana = null },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = Fondo,
            modifier = Modifier.fillMaxWidth(.92f)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow(T["app.ajustes"], Verde)
                        Letra(T["pref.titulo"], 21, FontWeight.SemiBold)
                        Letra(T["pref.opcional"], 12, FontWeight.Normal, TintaSuave)
                    }
                    BotonCerrar { activity.ventana = null }
                }
                Spacer(Modifier.height(16.dp))
                // El idioma se cambia aquí en cualquier momento, no solo al abrir
                // la app por primera vez.
                Pregunta(R.drawable.ic_voz, T["idioma.etiqueta"], Tono.NEUTRO) {
                    Chips(listOf(Textos.ES["idioma.es.corto"]!!, Textos.QU["idioma.qu.corto"]!!),
                        if (T.quechua) 1 else 0, Tono.NEUTRO) { i ->
                        activity.cambiarIdioma(if (i == 1) "qu" else "es")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Pregunta(R.drawable.ic_gota, T["pref.suelo"], Tono.AGUA) {
                    Chips(listOf(T["pref.suelo.0"], T["pref.suelo.1"], T["pref.suelo.2"]),
                        suelo.ordinal, Tono.AGUA) { i ->
                        suelo = MotorTemporada.Suelo.values()[i]
                    }
                }
                Spacer(Modifier.height(10.dp))
                Pregunta(R.drawable.ic_brote, T["pref.cuidar"], Tono.LUGAR) {
                    Chips(listOf(T["pref.cuidar.0"], T["pref.cuidar.1"], T["pref.cuidar.2"],
                        T["pref.cuidar.3"]), prioridad.ordinal, Tono.LUGAR) { i ->
                        prioridad = MotorTemporada.Prioridad.values()[i]
                    }
                }
                Spacer(Modifier.height(10.dp))
                Pregunta(R.drawable.ic_tiempo, T["pref.descanso"], Tono.TIERRA) {
                    Chips(listOf(T["pref.descanso.0"], T["pref.descanso.1"], T["pref.descanso.2"]),
                        valoresDescanso.indexOf(descanso).coerceAtLeast(0), Tono.TIERRA) { i ->
                        descanso = valoresDescanso[i]
                    }
                }
                Spacer(Modifier.height(18.dp))
                Boton(T["app.guardar"], { activity.guardarOpciones(suelo, prioridad, descanso) },
                    Modifier.fillMaxWidth(), principal = true, icono = R.drawable.ic_check)
            }
        }
    }
}

@Composable
private fun Pregunta(icono: Int, titulo: String, tono: Tono,
                     contenido: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Borde),
        modifier = Modifier.fillMaxWidth().tarjeta()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // El icono lleva el tono de lo que trata la pregunta.
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(tono.oscuro),
                    contentAlignment = Alignment.Center) { Icono(icono, 17, tono.tinta) }
                Spacer(Modifier.width(10.dp))
                Letra(titulo, 14, FontWeight.Medium, Tinta, lineas = 2)
            }
            Spacer(Modifier.height(10.dp))
            contenido()
        }
    }
}

/** Opciones como chips: una fila si son tres, dos filas de dos si son cuatro. */
@Composable
private fun Chips(opciones: List<String>, activa: Int, tono: Tono, elegir: (Int) -> Unit) {
    val conIndice = opciones.withIndex().toList()
    val filas = if (conIndice.size > 3) conIndice.chunked(2) else listOf(conIndice)
    Column {
        filas.forEachIndexed { f, fila ->
            if (f > 0) Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                fila.forEach { (i, nombre) ->
                    val sel = i == activa
                    val forma = RoundedCornerShape(14.dp)
                    // Elegido: fondo suave del tono, borde fino y visto. Sin bloque macizo.
                    Row(Modifier.weight(1f).fillMaxHeight().clip(forma)
                        .background(if (sel) tono.oscuro else Color.White)
                        .border(1.dp, if (sel) tono.tinta.copy(alpha = .35f) else Borde, forma)
                        .clickable { elegir(i) }.padding(horizontal = 6.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        if (sel) {
                            Icono(R.drawable.ic_check, 13, tono.tinta)
                            Spacer(Modifier.width(4.dp))
                        }
                        Letra(nombre, 12, if (sel) FontWeight.Medium else FontWeight.Normal,
                            if (sel) tono.tinta else Tinta, centrado = true, lineas = 2)
                    }
                }
                if (fila.size == 1 && filas.size > 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Color de cada nivel: verde bien, ámbar cuidado, terracota peligro. */
private fun colorNivel(n: Veredicto.Nivel): Color = when (n) {
    Veredicto.Nivel.BUENA -> Color(0xFF315E46)
    Veredicto.Nivel.CUIDADO -> Color(0xFF7B6229)
    Veredicto.Nivel.REVISAR -> Color(0xFF9A5148)
}

/** El icono que acompaña a cada razón del motor. */
private fun iconoMotivo(clave: String): Int = when {
    clave == "repetir" || clave == "parientes" -> R.drawable.ic_plaga
    clave == "leguminosa" -> R.drawable.ic_raiz
    clave == "rotacion" -> R.drawable.ic_rotacion
    clave == "estres" -> R.drawable.ic_alerta
    clave == "helada" -> R.drawable.ic_helada
    clave == "calendario" -> R.drawable.ic_calendario
    clave == "lluvia_mucha" -> R.drawable.ic_gota
    clave.startsWith("altura") -> R.drawable.ic_altura
    else -> R.drawable.ic_brote
}

/** Una etiqueta pequeña con icono: el veredicto o la fecha de siembra. */
@Composable
private fun Chapa(icono: Int, texto: String, tinta: Color, fondo: Color) {
    Row(Modifier.clip(RoundedCornerShape(100.dp)).background(fondo)
        .padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icono(icono, 14, tinta)
        Spacer(Modifier.width(5.dp))
        Letra(texto, 12, FontWeight.Medium, tinta, lineas = 1)
    }
}


/**
 * Una fila de medidor a todo el ancho: icono y nombre a la izquierda con su estado
 * en color debajo; la cifra grande a la derecha; una barra con la meta marcada y
 * nombrada; y una línea que lo explica. Nada se corta.
 */
@Composable
private fun FilaMedidor(icono: Int, titulo: String, estado: String, nivel: Veredicto.Nivel,
                        valor: String, unidad: String, relleno: Float, limite: Float,
                        marca: String, explica: String) {
    val color = colorNivel(nivel)
    val medidor = rememberTextMeasurer()
    val estiloMarca = TextStyle(fontFamily = Onest, fontSize = 10.sp, fontWeight = FontWeight.Medium,
        color = TintaSuave)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(color.copy(alpha = .12f)),
            contentAlignment = Alignment.Center) { Icono(icono, 18, color) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Eyebrow(titulo, TintaSuave)
            // Estado, cifra y unidad sobre la misma línea base.
            Row(Modifier.fillMaxWidth()) {
                Letra(estado, 13, FontWeight.Medium, color, Modifier.weight(1f).alignByBaseline(),
                    lineas = 1)
                Letra(valor, 24, FontWeight.SemiBold, Tinta, Modifier.alignByBaseline(), lineas = 1)
                Spacer(Modifier.width(5.dp))
                Letra(unidad, 11, FontWeight.Normal, TintaSuave, Modifier.alignByBaseline(), lineas = 1)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    // La barra y la explicación empiezan en el mismo borde que el icono.
    Canvas(Modifier.fillMaxWidth().height(26.dp)) {
        val alto = 8.dp.toPx()
        val y = 2.dp.toPx()
        drawRoundRect(Borde, Offset(0f, y), Size(size.width, alto), CornerRadius(alto / 2))
        val w = size.width * relleno.coerceIn(0f, 1f)
        if (w > 0f) drawRoundRect(color, Offset(0f, y), Size(w.coerceAtLeast(alto), alto),
            CornerRadius(alto / 2))
        val x = size.width * limite.coerceIn(0f, 1f)
        drawLine(Tinta, Offset(x, 0f), Offset(x, y + alto + 2.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        val t = medidor.measure(marca, estiloMarca)
        drawText(t, topLeft = Offset((x - t.size.width / 2f).coerceIn(0f, size.width - t.size.width),
            y + alto + 4.dp.toPx()))
    }
    Letra(explica, 12, FontWeight.Normal, TintaSuave, lineas = 2)
}

/**
 * Por qué sale esta opción, en una ventana que cabe sin desplazarse: cabecera con
 * el veredicto y la fecha, dos medidores (agua y helada) con cifras reales, qué
 * hacer, y las razones del motor con su icono. El agua y la helada ya están en
 * los medidores, así que no se repiten en la lista.
 */
@Composable
private fun Razones(activity: YachayActivity, consejo: ConsejoTemporada) {
    val T = LocalIdioma.current
    val v = Veredicto.de(consejo)
    val (fondoNivel, acento, _) = paleta(v)
    val alto = (LocalConfiguration.current.screenHeightDp * .92f).dp
    Dialog(onDismissRequest = { activity.ventana = null },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = Fondo,
            modifier = Modifier.fillMaxWidth(.94f).heightIn(max = alto)) {
            Column(Modifier.verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
                // --- Cabecera ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(62.dp).clip(CircleShape).background(fondoNivel),
                        contentAlignment = Alignment.Center) {
                        ArteCultivo(consejo.cultivo, Modifier.fillMaxSize().padding(6.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Letra(T.cultivo(consejo.cultivo), 22, FontWeight.SemiBold, lineas = 2)
                        Spacer(Modifier.height(6.dp))
                        Chapa(if (v.nivel == Veredicto.Nivel.BUENA) R.drawable.ic_check
                            else R.drawable.ic_alerta, textoVeredicto(T, v.nivel), acento, fondoNivel)
                        // La fecha bajo la etiqueta, alineada con el nombre.
                        consejo.fechaSiembra?.let {
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icono(R.drawable.ic_calendario, 14, TintaSuave)
                                Spacer(Modifier.width(5.dp))
                                Letra(T.f("razon.siembra", T.mes(it.monthValue), it.year), 13,
                                    FontWeight.Normal, TintaSuave, lineas = 1)
                            }
                        }
                    }
                    BotonCerrar { activity.ventana = null }
                }
                Spacer(Modifier.height(14.dp))
                // --- Agua y helada, lado a lado ---
                if (consejo.cultivo != Cultivo.DESCANSO) {
                    val cobertura = (consejo.coberturaAgua * 100).roundToInt()
                    val cosecha = (consejo.rendimientoEsperado * 100).roundToInt()
                    val demanda = consejo.razones.firstOrNull { it.clave == "cobertura" }?.entero2() ?: 0
                    val faltan = (demanda * (1 - consejo.coberturaAgua)).roundToInt()
                    val noches = (consejo.riesgoHelada * 100).roundToInt()
                    val limite = (consejo.cultivo.toleranciaHelada * 100).roundToInt()
                    Eyebrow(T["razon.datos"], Verde)
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Borde),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            // Agua: la cifra es la cosecha esperada (FAO-33), que es lo que decide.
                            FilaMedidor(R.drawable.ic_gota, T["razon.agua.titulo"],
                                T["estado.agua." + v.agua.ordinal], v.agua, "$cosecha %",
                                T["razon.agua.de"], consejo.rendimientoEsperado.toFloat(),
                                Veredicto.AGUA_BUENA.toFloat(),
                                T.f("razon.agua.marca", (Veredicto.AGUA_BUENA * 100).roundToInt()),
                                if (faltan > 0 && demanda > 0)
                                    T.f("razon.agua.falta", cobertura, faltan, demanda)
                                else T.f("razon.agua.alcanza", demanda))
                            Box(Modifier.padding(vertical = 12.dp).fillMaxWidth().height(1.dp)
                                .background(Borde))
                            // Helada: noches con helada de 100 en su época más fría, frente a su límite.
                            FilaMedidor(R.drawable.ic_helada, T["razon.helada.titulo"],
                                T["estado.helada." + v.helada.ordinal], v.helada, "$noches",
                                T["razon.helada.de"], (consejo.riesgoHelada / .5).toFloat(),
                                (consejo.cultivo.toleranciaHelada / .5).toFloat(),
                                T.f("razon.helada.marca", limite), T["razon.helada.epoca"])
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                // --- Qué hacer ---
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(fondoNivel)
                    .border(1.dp, acento.copy(alpha = .14f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    // Icono con su propio relleno, centrado en vertical con el texto.
                    Icono(if (v.nivel == Veredicto.Nivel.BUENA) R.drawable.ic_check
                        else R.drawable.ic_alerta, 20, acento)
                    Spacer(Modifier.width(12.dp))
                    Letra(queHacer(T, consejo, v), 14, FontWeight.Medium, Tinta,
                        Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                // --- Por qué ---
                Eyebrow(T["razones.titulo"], Verde)
                Spacer(Modifier.height(6.dp))
                consejo.razones.filter { it.clave != "cobertura" && it.clave != "cosecha" }.forEach { razon ->
                    Row(Modifier.padding(bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(Salvia),
                            contentAlignment = Alignment.Center) {
                            Icono(iconoMotivo(razon.clave), 16, Verde)
                        }
                        Spacer(Modifier.width(10.dp))
                        Letra(textoMotivo(T, razon, consejo.cultivo), 13, FontWeight.Normal, Tinta,
                            Modifier.weight(1f), lineas = 2)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Letra(T["razones.fuente"], 11, FontWeight.Normal, TintaSuave,
                    Modifier.fillMaxWidth(), centrado = true)
            }
        }
    }
}

// Colores de las gráficas, validados con dataviz/validate_palette.js sobre fondo
// claro: verde de esta campaña y ocre de lo normal pasan las seis pruebas. La
// separación para daltonismo está en la franja mínima (ΔE 6,1), así que la
// identidad también va por la forma (barra frente a raya) y por la leyenda.
private val SerieCampana = Color(0xFF1F7A4D)
private val SerieNormal = Color(0xFF9C5B22)

/** Tope redondo del eje: 0, la mitad y el tope, en milímetros limpios. */
private fun topeEje(maximo: Double): Int {
    val paso = intArrayOf(5, 10, 20, 25, 50, 100, 200).firstOrNull { maximo / it <= 4 } ?: 250
    return (kotlin.math.ceil(maximo.coerceAtLeast(1.0) / paso) * paso).toInt()
}

/** Leyenda: la marca de color va al lado y el texto en tinta, nunca en el color de la serie. */
@Composable
private fun ItemLeyenda(texto: String, marca: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        marca()
        Spacer(Modifier.width(6.dp))
        Letra(texto, 11, FontWeight.Normal, TintaSuave, lineas = 1)
    }
}

/**
 * Las seis semanas hasta el último dato fiable: una barra por semana para esta
 * campaña y una raya ocre con lo normal de esas mismas fechas en 15 años. Al
 * tocar una semana se leen sus dos cifras arriba.
 */
@Composable
private fun GraficoLluvia(zona: Temporada) {
    val T = LocalIdioma.current
    val actual = remember(zona) { zona.semanasActuales() }
    val normal = remember(zona) { zona.semanasHabituales() }
    val tope = remember(zona) {
        topeEje((actual.maxOrNull() ?: 0.0).coerceAtLeast(5.0))
    }
    var elegida by remember(zona) { mutableIntStateOf(5) }
    val medidor = rememberTextMeasurer()
    val estiloEje = TextStyle(fontFamily = Onest, fontSize = 9.sp, color = TintaSuave)
    val estiloValor = TextStyle(fontFamily = Onest, fontSize = 10.sp,
        fontWeight = FontWeight.Medium, color = Tinta)
    val finSemana = { i: Int -> zona.fechaCorte.minusWeeks((5 - i).toLong()) }
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Borde),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 11.dp)) {
            Letra(T["clima.semanas"], 13, FontWeight.SemiBold, Tinta)
            Spacer(Modifier.height(2.dp))
            Letra(T.f("grafico.semana.lectura", fechaCorta(finSemana(elegida).minusDays(6)),
                fechaCorta(finSemana(elegida)), actual[elegida].roundToInt(), normal[elegida].roundToInt()),
                12, FontWeight.Normal, TintaSuave, lineas = 1)
            Spacer(Modifier.height(8.dp))
            Canvas(Modifier.fillMaxWidth().height(120.dp).pointerInput(zona) {
                detectTapGestures { toque ->
                    val x0 = 26.dp.toPx()
                    val paso = (size.width - x0) / 6f
                    elegida = ((toque.x - x0) / paso).toInt().coerceIn(0, 5)
                }
            }) {
                val x0 = 26.dp.toPx()
                val y0 = 14.dp.toPx()
                val base = size.height - 16.dp.toPx()
                val alto = base - y0
                val y = { mm: Double -> base - (mm / tope * alto).toFloat() }
                // Rejilla: tres líneas finas, sólidas y recesivas, con su cifra.
                for (k in 0..2) {
                    val v = tope * k / 2.0
                    drawLine(Borde, Offset(x0, y(v)), Offset(size.width, y(v)), 1f)
                    val t = medidor.measure("${v.toInt()}", estiloEje)
                    drawText(t, topLeft = Offset(x0 - t.size.width - 5.dp.toPx(),
                        y(v) - t.size.height / 2f))
                }
                val paso = (size.width - x0) / 6f
                val ancho = minOf(24.dp.toPx(), paso * .42f)
                val radio = 4.dp.toPx()
                for (i in 0..5) {
                    val cx = x0 + paso * (i + .5f)
                    if (i == elegida) drawRect(Salvia.copy(alpha = .7f),
                        Offset(cx - paso / 2f + 2.dp.toPx(), y0), Size(paso - 4.dp.toPx(), alto))
                    // Barra: punta redondeada de 4 px, base recta sobre el eje.
                    val h = (base - y(actual[i])).coerceAtLeast(1.5f)
                    drawRoundRect(SerieCampana, Offset(cx - ancho / 2f, base - h),
                        Size(ancho, h), CornerRadius(radio))
                    drawRect(SerieCampana, Offset(cx - ancho / 2f, base - minOf(radio, h)),
                        Size(ancho, minOf(radio, h)))
                    val f = medidor.measure(fechaCorta(finSemana(i)), estiloEje)
                    drawText(f, topLeft = Offset(cx - f.size.width / 2f, base + 4.dp.toPx()))
                }
                // Solo una cifra en la gráfica: la de la semana elegida, sobre su barra.
                val cx = x0 + paso * (elegida + .5f)
                val v = medidor.measure(T.f("grafico.litros", actual[elegida].roundToInt()), estiloValor)
                val arriba = y(actual[elegida]) - v.size.height - 4.dp.toPx()
                drawText(v, topLeft = Offset((cx - v.size.width / 2f)
                    .coerceIn(x0, size.width - v.size.width), arriba.coerceAtLeast(0f)))
                drawLine(Borde, Offset(x0, base), Offset(size.width, base), 1.5f)
            }
            Spacer(Modifier.height(4.dp))
            // Por qué el último dato es de hace semanas: NASA publica la lluvia revisada con retraso.
            Letra(if (zona.imerg()) T.f("clima.hasta.imerg", fechaCorta(zona.fechaCorte))
                else T.f("clima.hasta", fechaCorta(zona.fechaCorte)), 10, FontWeight.Normal, TintaSuave)
        }
    }
}

/**
 * Los próximos 7 días: lluvia prevista por día (mm) y, debajo, la marca de
 * helada cuando la mínima, corregida a la altura de la chacra, baja de 0 °C.
 * Es una sola serie: el título la nombra y no lleva leyenda de colores.
 */
@Composable
private fun GraficoPronostico(dias: List<DatosZona.Dia>, elevacionCelda: Double, altura: Int) {
    val T = LocalIdioma.current
    val minimas = remember(dias, altura) {
        dias.map { Clima.tminCorregida(it.tmin, elevacionCelda, altura.toDouble()) }
    }
    val tope = remember(dias) { topeEje(dias.maxOf { it.lluvia }.coerceAtLeast(5.0)) }
    var elegido by remember(dias) { mutableIntStateOf(0) }
    val medidor = rememberTextMeasurer()
    val estiloEje = TextStyle(fontFamily = Onest, fontSize = 9.sp, color = TintaSuave)
    val estiloDia = TextStyle(fontFamily = Onest, fontSize = 10.sp,
        fontWeight = FontWeight.Normal, color = Tinta)
    val helada = painterResource(R.drawable.ic_helada)
    val hayHelada = minimas.any { it <= 0 }
    val n = dias.size
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Borde),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 11.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Letra(T["pron.titulo"], 13, FontWeight.SemiBold, Tinta, Modifier.weight(1f), lineas = 1)
                Box(Modifier.clip(RoundedCornerShape(100.dp)).background(Tono.AGUA.oscuro)
                    .padding(horizontal = 9.dp, vertical = 3.dp)) {
                    Letra(T.f("pron.suma", dias.sumOf { it.lluvia }.roundToInt()), 11,
                        FontWeight.Medium, Tono.AGUA.tinta, lineas = 1)
                }
            }
            Spacer(Modifier.height(2.dp))
            val d = dias[elegido]
            Letra(T.f("pron.lectura", T["dia." + d.fecha.dayOfWeek.value], d.fecha.dayOfMonth,
                d.lluvia.roundToInt(), d.prob.coerceAtLeast(0), minimas[elegido].roundToInt()),
                12, FontWeight.Normal, TintaSuave, lineas = 2)
            Spacer(Modifier.height(8.dp))
            Canvas(Modifier.fillMaxWidth().height(124.dp).pointerInput(dias) {
                detectTapGestures { toque ->
                    val x0 = 26.dp.toPx()
                    elegido = ((toque.x - x0) / ((size.width - x0) / n)).toInt().coerceIn(0, n - 1)
                }
            }) {
                // Mismas medidas que la gráfica semanal: las dos quedan alineadas.
                val x0 = 26.dp.toPx()
                val y0 = 8.dp.toPx()
                val base = size.height - 34.dp.toPx()
                val alto = base - y0
                val y = { mm: Double -> base - (mm.coerceAtMost(tope.toDouble()) / tope * alto).toFloat() }
                for (k in 0..2) {
                    val v = tope * k / 2.0
                    drawLine(Borde, Offset(x0, y(v)), Offset(size.width, y(v)), 1f)
                    val t = medidor.measure("${v.toInt()}", estiloEje)
                    drawText(t, topLeft = Offset(x0 - t.size.width - 5.dp.toPx(),
                        y(v) - t.size.height / 2f))
                }
                val paso = (size.width - x0) / n
                val ancho = minOf(24.dp.toPx(), paso * .46f)
                val radio = 4.dp.toPx()
                for (i in 0 until n) {
                    val cx = x0 + paso * (i + .5f)
                    if (i == elegido) drawRect(Salvia.copy(alpha = .7f),
                        Offset(cx - paso / 2f + 1.dp.toPx(), y0), Size(paso - 2.dp.toPx(), alto))
                    val h = (base - y(dias[i].lluvia)).coerceAtLeast(1.5f)
                    drawRoundRect(SerieCampana, Offset(cx - ancho / 2f, base - h),
                        Size(ancho, h), CornerRadius(radio))
                    drawRect(SerieCampana, Offset(cx - ancho / 2f, base - minOf(radio, h)),
                        Size(ancho, minOf(radio, h)))
                    val et = medidor.measure(T["dia." + dias[i].fecha.dayOfWeek.value], estiloDia)
                    drawText(et, topLeft = Offset(cx - et.size.width / 2f, base + 4.dp.toPx()))
                    if (minimas[i] <= 0) {
                        val lado = 12.dp.toPx()
                        translate(cx - lado / 2f, base + 19.dp.toPx()) {
                            with(helada) {
                                draw(Size(lado, lado),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Tinta))
                            }
                        }
                    }
                }
                // Una sola cifra: la del día elegido, sobre su barra.
                val cx = x0 + paso * (elegido + .5f)
                val v = medidor.measure(T.f("grafico.litros", dias[elegido].lluvia.roundToInt()),
                    estiloDia.copy(fontWeight = FontWeight.Medium))
                drawText(v, topLeft = Offset((cx - v.size.width / 2f)
                    .coerceIn(x0, size.width - v.size.width),
                    (y(dias[elegido].lluvia) - v.size.height - 3.dp.toPx()).coerceAtLeast(0f)))
                drawLine(Borde, Offset(x0, base), Offset(size.width, base), 1.5f)
            }
            if (hayHelada) {
                Spacer(Modifier.height(6.dp))
                ItemLeyenda(T["pron.helada"]) { Icono(R.drawable.ic_helada, 12, Tinta) }
            }
            Spacer(Modifier.height(4.dp))
            Letra(T["pron.fuente"], 10, FontWeight.Normal, TintaSuave)
        }
    }
}

/** 9.93, -76.24 → "9°55′ S · 76°14′ O": la coordenada que el agricultor puede comprobar. */
private fun coordenadas(lat: Double, lon: Double): String {
    fun gms(v: Double): String {
        val a = abs(v)
        val g = a.toInt()
        val m = ((a - g) * 60).toInt()
        return "$g°" + "%02d".format(m) + "′"
    }
    return "${gms(lat)} ${if (lat < 0) "S" else "N"} · ${gms(lon)} ${if (lon < 0) "O" else "E"}"
}

/**
 * Dónde está la chacra, a la vista: el relieve de su valle dibujado en el
 * teléfono, el borde de su provincia, el pin con su círculo de precisión, y al
 * lado la provincia, la altura con su fuente y la precisión. Nada sale del
 * teléfono para dibujarlo.
 */
@Composable
private fun TarjetaUbicacion(activity: YachayActivity, grande: Boolean = false, abrir: () -> Unit) {
    val T = LocalIdioma.current
    val fijacion = activity.fijacion
    val mapa = activity.mapa
    val contorno = remember(activity.provincia, mapa) { activity.contornoProvincia() }
    val ladoMapa = if (grande) 220.dp else 64.dp
    val pulso = rememberInfiniteTransition(label = "pin")
    val onda by pulso.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "onda")
    val visor: @Composable (Modifier) -> Unit = { mod ->
        Box(mod.clip(RoundedCornerShape(if (grande) 18.dp else 14.dp)).background(Salvia),
            contentAlignment = Alignment.Center) {
            if (mapa != null) {
                Image(mapa.imagen.asImageBitmap(), null, Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop)
                Canvas(Modifier.fillMaxSize()) {
                    val k = size.width / mapa.imagen.width
                    // Borde de la provincia: línea fina en tinta.
                    contorno?.let { (lats, lons) ->
                        val camino = Path()
                        lats.indices.forEach { i ->
                            val q = mapa.aPixel(lats[i], lons[i])
                            if (i == 0) camino.moveTo(q[0] * k, q[1] * k) else camino.lineTo(q[0] * k, q[1] * k)
                        }
                        camino.close()
                        drawPath(camino, Tinta.copy(alpha = .55f),
                            style = Stroke(1.5.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
                    }
                    if (fijacion != null) {
                        val q = mapa.aPixel(fijacion.latitude, fijacion.longitude)
                        val c = Offset(q[0] * k, q[1] * k)
                        // Círculo de precisión real (con un mínimo visible) y una onda que late.
                        val mpp = mapa.metrosPorPixel(fijacion.latitude) / k
                        val rp = ((activity.precision ?: 50f) / mpp).toFloat().coerceAtLeast(7.dp.toPx())
                        drawCircle(Verde.copy(alpha = .16f), rp, c)
                        drawCircle(Verde.copy(alpha = .35f * (1f - onda)), rp + 14.dp.toPx() * onda, c,
                            style = Stroke(2.dp.toPx()))
                        drawCircle(Color.White, 7.dp.toPx(), c)
                        drawCircle(Verde, 5.dp.toPx(), c)
                    }
                }
            } else {
                Icono(R.drawable.ic_lugar, if (grande) 34 else 26, Verde)
            }
        }
    }
    // Compacta y en verde salvia con borde verde: resalta sin salirse del estilo.
    Surface(shape = RoundedCornerShape(18.dp), color = if (grande) Color.White else Color(0xFFE8F1EA),
        border = androidx.compose.foundation.BorderStroke(1.dp,
            if (grande) Borde else Verde.copy(alpha = .16f)),
        modifier = Modifier.fillMaxWidth().then(if (grande) Modifier else Modifier.tarjeta())
            .clickable(onClick = abrir)) {
        val compactos: @Composable ColumnScope.() -> Unit = {
            Eyebrow(T["ubic.eyebrow"], Verde)
            Letra(T.f("ubic.provincia", activity.provincia ?: "Huánuco"), 15,
                FontWeight.SemiBold, Tinta, lineas = 1)
            val fuente = when (activity.fuenteAltura) {
                "gps" -> T["ubic.fuente.gps"]
                "tu" -> T["ubic.fuente.tu"]
                else -> T["ubic.fuente.mapa"]
            }
            val p = activity.precision
            val dato = (if (activity.altura > 0) T.f("ubic.compacto", activity.altura, fuente) else "") +
                when {
                    p != null && activity.buscandoGps -> "  ·  " + T["ubic.afinando"]
                    p != null -> "  ·  ±${p.toInt()} m"
                    else -> ""
                }
            Letra(dato, 12, FontWeight.Medium, Verde, lineas = 1)
        }
        val textos: @Composable ColumnScope.() -> Unit = {
            Eyebrow(T["ubic.eyebrow"], Verde)
            Spacer(Modifier.height(3.dp))
            Letra(T.f("ubic.provincia", activity.provincia ?: "Huánuco"), 16,
                FontWeight.SemiBold, Tinta, lineas = 1)
            if (activity.altura > 0) {
                val clave = when (activity.fuenteAltura) {
                    "gps" -> "ubic.altura.gps"
                    "tu" -> "ubic.altura.tu"
                    else -> "ubic.altura.mapa"
                }
                Letra(T.f(clave, activity.altura), 13, FontWeight.Normal, Tinta, lineas = 2)
            }
            val p = activity.precision
            when {
                p != null && activity.buscandoGps ->
                    Letra(T.f("ubic.mejorando", p.toInt()), 12, FontWeight.Medium, Verde, lineas = 1)
                p != null -> Letra(T.f("ubic.precision", p.toInt()), 12, FontWeight.Medium, Verde, lineas = 1)
                else -> Letra(T["ubic.sin.gps"], 12, FontWeight.Normal, TintaSuave, lineas = 1)
            }
            fijacion?.let {
                Letra(coordenadas(it.latitude, it.longitude), 11, FontWeight.Normal, TintaSuave,
                    lineas = 1)
            }
        }
        if (grande) {
            Column(Modifier.padding(10.dp)) {
                visor(Modifier.fillMaxWidth().height(ladoMapa))
                Spacer(Modifier.height(10.dp))
                Column(Modifier.padding(horizontal = 4.dp), content = textos)
            }
        } else {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                visor(Modifier.size(ladoMapa))
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), content = compactos)
                Icono(R.drawable.ic_flecha, 16, Verde)
            }
        }
    }
}

/** Una ficha de dato: etiqueta arriba, cifra grande y de dónde sale. */
@Composable
private fun FichaDato(etiqueta: String, valor: String, detalle: String, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFE8F1EA),
        border = androidx.compose.foundation.BorderStroke(1.dp, Verde.copy(alpha = .12f)),
        modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Eyebrow(etiqueta, Verde)
            Spacer(Modifier.height(2.dp))
            Letra(valor, 15, FontWeight.SemiBold, Tinta, lineas = 2)
            Letra(detalle, 11, FontWeight.Normal, TintaSuave, lineas = 1)
        }
    }
}

/** Escala redonda para el mapa: 1, 2, 5, 10 o 20 km según lo que quepa. */
private fun escalaKm(metrosPorPx: Double, anchoPx: Float): Pair<Int, Float> {
    val objetivo = anchoPx * .26f * metrosPorPx / 1000.0
    val km = intArrayOf(1, 2, 5, 10, 20).lastOrNull { it <= objetivo } ?: 1
    return km to (km * 1000.0 / metrosPorPx).toFloat()
}

/**
 * La ventana de la ubicación: el mapa ES la ventana. Relieve del valle de borde
 * a borde, con norte, escala en km, el contorno de la provincia y el pin latiendo
 * con su círculo de precisión; abajo, en un degradado, dónde está la chacra. Luego
 * tres fichas (altura, precisión, coordenadas) y los botones. Todo se dibuja en el
 * teléfono: la posición no sale de él.
 */
@Composable
private fun VentanaUbicacion(activity: YachayActivity) {
    val T = LocalIdioma.current
    val fijacion = activity.fijacion
    val alto = (LocalConfiguration.current.screenHeightDp * .9f).dp
    Dialog(onDismissRequest = { activity.ventana = null },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = Fondo,
            modifier = Modifier.fillMaxWidth(.92f).heightIn(max = alto)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // --- El mapa, de borde a borde ---
                // clipToBounds: nada del mapa puede salirse de su marco.
                Box(Modifier.fillMaxWidth().height(340.dp).clipToBounds().background(Salvia)) {
                    // El mapa de la NASA, que se mueve y se acerca con los dedos.
                    MapaInteractivo(activity, Modifier.fillMaxSize())
                    // Degradado inferior con el lugar, en blanco.
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(120.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent,
                            Tinta.copy(alpha = .82f)))))
                    Column(Modifier.align(Alignment.BottomStart)
                        .padding(start = 18.dp, bottom = 14.dp, end = 90.dp)) {
                        Eyebrow(T["ubic.eyebrow"], Color.White.copy(alpha = .85f))
                        Letra(T.f("ubic.provincia", activity.provincia ?: "Huánuco"), 22,
                            FontWeight.SemiBold, Color.White, lineas = 2)
                    }
                    // Norte, arriba a la izquierda.
                    Box(Modifier.align(Alignment.TopStart).padding(14.dp).size(34.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = .9f)),
                        contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(14.dp, 10.dp).offset(y = (-4).dp)) {
                            val p = Path().apply {
                                moveTo(size.width / 2f, 0f); lineTo(size.width, size.height)
                                lineTo(0f, size.height); close()
                            }
                            drawPath(p, Verde)
                        }
                        Letra("N", 10, FontWeight.SemiBold, Tinta, Modifier.offset(y = 7.dp))
                    }
                    // Cerrar, arriba a la derecha: verde sólido con aro blanco, se ve
                    // sobre cualquier parte del relieve.
                    BotonCerrar(Modifier.align(Alignment.TopEnd).padding(12.dp), 40) {
                        activity.ventana = null
                    }
                }
                // --- Tres fichas ---
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val fuente = when (activity.fuenteAltura) {
                            "gps" -> T["ubic.fuente.gps"]
                            "tu" -> T["ubic.fuente.tu"]
                            else -> T["ubic.fuente.mapa"]
                        }
                        FichaDato(T["ubic.dato.altura"],
                            if (activity.altura > 0) "${activity.altura} m" else "—", fuente,
                            Modifier.weight(1f).fillMaxHeight())
                        val p = activity.precision
                        FichaDato(T["ubic.dato.precision"], p?.let { "±${it.toInt()} m" } ?: "—",
                            when {
                                p != null && activity.buscandoGps -> T["ubic.afinando"]
                                p != null -> T["ubic.fuente.gps"]
                                else -> T["ubic.sin.gps"]
                            }, Modifier.weight(1f).fillMaxHeight())
                        FichaDato(T["ubic.dato.coord"],
                            fijacion?.let { coordenadas(it.latitude, it.longitude).replace(" · ", "\n") }
                                ?: "—", "WGS84", Modifier.weight(1f).fillMaxHeight())
                    }
                    Spacer(Modifier.height(10.dp))
                    Letra(T["ubicar.privacidad"], 12, FontWeight.Normal, TintaSuave, centrado = true,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Boton(T["ubicar.gps"], {
                        activity.ventana = null
                        activity.buscarGps()
                    }, Modifier.fillMaxWidth(), principal = true, icono = R.drawable.ic_gps)
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Boton(T["ubic.btn.provincia"], { activity.ventana = Ventana.Provincias },
                            Modifier.weight(1f), icono = R.drawable.ic_mapa)
                        if (activity.zona != null) Boton(T["ubic.btn.altura"],
                            { activity.ventana = Ventana.Altura }, Modifier.weight(1f),
                            icono = R.drawable.ic_altura)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Mapa interactivo con el relieve de la NASA (ASTER GDEM vía GIBS), teñido con
// la paleta de la app por herramientas/teselas_nasa.py y guardado en el APK:
// funciona sin señal y la posición no sale del teléfono.
// ---------------------------------------------------------------------------

private const val ZOOM_MIN = 8f
private const val ZOOM_MAX = 12.5f

/** Lat/lon a coordenadas del mundo Mercator, de 0 a 1. */
private fun aMundo(lat: Double, lon: Double): Offset {
    val r = Math.toRadians(lat)
    return Offset(((lon + 180.0) / 360.0).toFloat(),
        ((1.0 - kotlin.math.ln(kotlin.math.tan(Math.PI / 4.0 + r / 2.0)) / Math.PI) / 2.0).toFloat())
}

/** Límites del mapa incluido: la región de Huánuco, para no perderse fuera. */
private val MundoNO = aMundo(-8.2, -77.5)
private val MundoSE = aMundo(-10.7, -74.2)

/** Teselas ya leídas del APK; se guardan unas cuantas para mover el mapa sin parpadeo. */
private object TeselasNasa {
    private val cache = object : android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(90) {}
    private val faltan = java.util.Collections.synchronizedSet(HashSet<String>())
    fun clave(z: Int, x: Int, y: Int) = "$z/${x}_$y"
    fun en(z: Int, x: Int, y: Int) = cache.get(clave(z, x, y))
    fun falta(z: Int, x: Int, y: Int) = faltan.contains(clave(z, x, y))
    fun leer(ctx: android.content.Context, z: Int, x: Int, y: Int): Boolean {
        val k = clave(z, x, y)
        if (cache.get(k) != null || faltan.contains(k)) return false
        return try {
            ctx.assets.open("mapa/$k.jpg").use { BitmapFactory.decodeStream(it) }?.let {
                cache.put(k, it.asImageBitmap()); true
            } ?: run { faltan.add(k); false }
        } catch (_: Exception) { faltan.add(k); false }
    }
}

@Composable
private fun MapaInteractivo(activity: YachayActivity, modifier: Modifier) {
    val T = LocalIdioma.current
    val ctx = LocalContext.current
    val densidad = LocalDensity.current
    val base = with(densidad) { 160.dp.toPx() }          // una tesela a zoom entero
    val fijacion = activity.fijacion
    val chacra = remember(fijacion, activity.provincia) {
        fijacion?.let { aMundo(it.latitude, it.longitude) }
            ?: activity.provincia?.let { activity.centroProvincia()?.let { c -> aMundo(c[0], c[1]) } }
            ?: aMundo(-9.93, -76.24)
    }
    var zoom by remember { mutableStateOf(10.6f) }
    var centro by remember(chacra) { mutableStateOf(chacra) }
    var tamano by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var cargadas by remember { mutableIntStateOf(0) }    // sube cuando llega una tesela
    val contorno = remember(activity.provincia) { activity.contornoProvincia() }
    val contornoMundo = remember(contorno) {
        contorno?.let { (lats, lons) -> lats.indices.map { aMundo(lats[it], lons[it]) } }
    }
    val medidor = rememberTextMeasurer()
    val latido = rememberInfiniteTransition(label = "pin")
    val onda by latido.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "onda")
    val alcance = rememberCoroutineScope()

    fun limitar(c: Offset) = Offset(c.x.coerceIn(MundoNO.x, MundoSE.x), c.y.coerceIn(MundoNO.y, MundoSE.y))
    fun mundoPx(z: Float) = base * Math.pow(2.0, z.toDouble()).toFloat()

    // Las teselas que se ven ahora: se leen del APK en segundo plano.
    val zi = kotlin.math.round(zoom).toInt().coerceIn(8, 11)
    LaunchedEffect(zi, centro, tamano) {
        if (tamano.width == 0) return@LaunchedEffect
        val n = 1 shl zi
        val w = mundoPx(zoom)
        val x0 = ((centro.x - tamano.width / 2f / w) * n).toInt() - 1
        val x1 = ((centro.x + tamano.width / 2f / w) * n).toInt() + 1
        val y0 = ((centro.y - tamano.height / 2f / w) * n).toInt() - 1
        val y1 = ((centro.y + tamano.height / 2f / w) * n).toInt() + 1
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            for (x in x0..x1) for (y in y0..y1) {
                if (TeselasNasa.leer(ctx, zi, x, y)) cargadas++
                // El nivel de arriba sirve de respaldo mientras llega el detalle.
                if (zi > 8 && TeselasNasa.leer(ctx, zi - 1, x / 2, y / 2)) cargadas++
            }
        }
    }

    Box(modifier.clipToBounds().background(Color(0xFFE8E4C2))
        .onSizeChanged { tamano = it }
        .pointerInput(chacra) {
            detectTransformGestures { foco, mover, escala, _ ->
                val w0 = mundoPx(zoom)
                val nuevo = (zoom + kotlin.math.log2(escala)).coerceIn(ZOOM_MIN, ZOOM_MAX)
                val w1 = mundoPx(nuevo)
                val medio = Offset(size.width / 2f, size.height / 2f)
                // El punto bajo los dedos no se mueve al acercar o alejar.
                val bajoDedo = centro + (foco - medio) / w0
                centro = limitar(bajoDedo - (foco - medio) / w1 - mover / w1)
                zoom = nuevo
            }
        }
        .pointerInput(chacra) {
            detectTapGestures(onDoubleTap = { zoom = (zoom + 1f).coerceAtMost(ZOOM_MAX) })
        }) {
        Canvas(Modifier.fillMaxSize()) {
            cargadas.let { }                              // redibuja al llegar teselas
            val w = mundoPx(zoom)
            val medio = Offset(size.width / 2f, size.height / 2f)
            val aPantalla = { m: Offset -> medio + (m - centro) * w }
            val n = 1 shl zi
            val lado = w / n
            val x0 = ((centro.x - size.width / 2f / w) * n).toInt() - 1
            val x1 = ((centro.x + size.width / 2f / w) * n).toInt() + 1
            val y0 = ((centro.y - size.height / 2f / w) * n).toInt() - 1
            val y1 = ((centro.y + size.height / 2f / w) * n).toInt() + 1
            for (x in x0..x1) for (y in y0..y1) {
                val o = aPantalla(Offset(x.toFloat() / n, y.toFloat() / n))
                val destino = androidx.compose.ui.unit.IntOffset(o.x.toInt(), o.y.toInt())
                val medida = androidx.compose.ui.unit.IntSize(kotlin.math.ceil(lado).toInt() + 1,
                    kotlin.math.ceil(lado).toInt() + 1)
                val img = TeselasNasa.en(zi, x, y)
                if (img != null) {
                    drawImage(img, dstOffset = destino, dstSize = medida,
                        filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium)
                } else {
                    // Respaldo: el cuarto que toca de la tesela del nivel de arriba.
                    TeselasNasa.en(zi - 1, x / 2, y / 2)?.let { p ->
                        drawImage(p, srcOffset = androidx.compose.ui.unit.IntOffset((x % 2) * 128, (y % 2) * 128),
                            srcSize = androidx.compose.ui.unit.IntSize(128, 128),
                            dstOffset = destino, dstSize = medida,
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium)
                    }
                }
            }
            // Borde de la provincia.
            contornoMundo?.let { puntos ->
                val camino = Path()
                puntos.forEachIndexed { i, m ->
                    val q = aPantalla(m)
                    if (i == 0) camino.moveTo(q.x, q.y) else camino.lineTo(q.x, q.y)
                }
                camino.close()
                drawPath(camino, Color.White.copy(alpha = .7f), style = Stroke(4.dp.toPx(),
                    join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawPath(camino, Tinta.copy(alpha = .7f), style = Stroke(1.6.dp.toPx(),
                    join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            // Metros por píxel de pantalla en la latitud de la chacra.
            val lat = fijacion?.latitude ?: -9.9
            val mpp = 40075016.686 * kotlin.math.cos(Math.toRadians(lat)) / w
            if (fijacion != null) {
                val c = aPantalla(chacra)
                val rp = ((activity.precision ?: 50f) / mpp).toFloat().coerceAtLeast(9.dp.toPx())
                drawCircle(Verde.copy(alpha = .18f), rp, c)
                drawCircle(Verde.copy(alpha = .45f * (1f - onda)), rp + 26.dp.toPx() * onda, c,
                    style = Stroke(2.5.dp.toPx()))
                val cabeza = Offset(c.x, c.y - 22.dp.toPx())
                val gota = Path().apply {
                    moveTo(c.x, c.y)
                    cubicTo(c.x - 4.dp.toPx(), c.y - 8.dp.toPx(), cabeza.x - 12.dp.toPx(),
                        cabeza.y + 6.dp.toPx(), cabeza.x - 12.dp.toPx(), cabeza.y)
                    cubicTo(cabeza.x - 12.dp.toPx(), cabeza.y - 16.dp.toPx(),
                        cabeza.x + 12.dp.toPx(), cabeza.y - 16.dp.toPx(), cabeza.x + 12.dp.toPx(), cabeza.y)
                    cubicTo(cabeza.x + 12.dp.toPx(), cabeza.y + 6.dp.toPx(),
                        c.x + 4.dp.toPx(), c.y - 8.dp.toPx(), c.x, c.y)
                    close()
                }
                drawPath(gota, Color.White, style = Stroke(5.dp.toPx()))
                drawPath(gota, Verde)
                drawCircle(Color.White, 4.5.dp.toPx(), cabeza)
            }
            // Escala redonda, abajo a la derecha.
            val (km, largo) = escalaKm(mpp, size.width)
            val x2 = size.width - 16.dp.toPx()
            val yEsc = size.height - 18.dp.toPx()
            drawLine(Color.White, Offset(x2 - largo, yEsc), Offset(x2, yEsc), 3.dp.toPx(), StrokeCap.Round)
            val t = medidor.measure("$km km", TextStyle(fontFamily = Onest, fontSize = 11.sp,
                fontWeight = FontWeight.Medium, color = Color.White))
            drawText(t, topLeft = Offset(x2 - t.size.width, yEsc - t.size.height - 4.dp.toPx()))
        }
        // Botones: acercar, alejar y volver a la chacra, a la derecha y al medio.
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonMapa("+") { zoom = (zoom + 1f).coerceAtMost(ZOOM_MAX) }
            BotonMapa("−") { zoom = (zoom - 1f).coerceAtLeast(ZOOM_MIN) }
            Box(Modifier.size(38.dp).shadow(3.dp, CircleShape).clip(CircleShape)
                .background(Color.White).clickable {
                    centro = chacra
                    zoom = 11.2f
                }, contentAlignment = Alignment.Center) {
                Icono(R.drawable.ic_lugar, 18, Verde)
            }
        }
        Letra(T["mapa.fuente"], 9, FontWeight.Normal, Color.White.copy(alpha = .85f),
            Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
    }
}

@Composable
private fun BotonMapa(signo: String, tocar: () -> Unit) {
    Box(Modifier.size(38.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White)
        .clickable(onClick = tocar), contentAlignment = Alignment.Center) {
        Letra(signo, 20, FontWeight.Medium, Tinta)
    }
}

/**
 * La rotación recomendada: la rueda de los 5 pasos con cada campaña del plan en su
 * sector, unidas en orden por una línea con flechas, y debajo la lista por año con
 * la cosecha esperada. La campaña de ahora usa el pronóstico; las siguientes, la
 * lluvia normal de las 15 campañas NASA (lo dice la nota).
 */
@Composable
private fun VentanaRotacion(activity: YachayActivity) {
    val T = LocalIdioma.current
    val zona = activity.zonaViva ?: return
    val anterior = activity.cultivoAnterior ?: return
    val hoy = remember { LocalDate.now() }
    val plan = remember(zona, activity.altura, anterior, activity.suelo, activity.prioridad,
        activity.siembrasSinDescanso, activity.datos) {
        val motor = MotorTemporada()
        val opciones = motor.recomendar(zona, activity.altura, anterior, activity.siembrasSinDescanso,
            activity.suelo, activity.prioridad, hoy)
        val ahora = Decision.tomar(opciones, activity.datos?.paraDecision(hoy), activity.altura, hoy)
        PlanRotacion.armar(zona, activity.altura, anterior, activity.siembrasSinDescanso,
            activity.suelo, activity.prioridad, hoy, ahora, 4)
    }
    val alto = (LocalConfiguration.current.screenHeightDp * .92f).dp
    Dialog(onDismissRequest = { activity.ventana = null },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = Fondo,
            modifier = Modifier.fillMaxWidth(.94f).heightIn(max = alto)) {
            Column(Modifier.verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow(T["plan.eyebrow"], Verde)
                        Letra(T["plan.titulo"], 20, FontWeight.SemiBold, Tinta, lineas = 2)
                    }
                    BotonCerrar { activity.ventana = null }
                }
                Spacer(Modifier.height(8.dp))
                RuedaPlan(plan, activity.altura, hoy)
                Spacer(Modifier.height(10.dp))
                // La lista: de dónde parte y cada campaña con su año y su cosecha.
                FilaPlan(T["plan.sembraste"], anterior, null, true, null)
                plan.forEachIndexed { i, c ->
                    FilaPlan(if (i == 0) T["plan.ahora"] else campana(hoy, i), c.consejo.cultivo,
                        c.consejo, false,
                        c.consejo.fechaSiembra?.let { f -> T.f("plan.siembra", T.mes(f.monthValue).take(3), f.year) }
                            ?: T["plan.toda"])
                }
                Spacer(Modifier.height(8.dp))
                Letra(T["plan.nota"], 11, FontWeight.Normal, TintaSuave, Modifier.fillMaxWidth(),
                    centrado = true)
            }
        }
    }
}

/** En la rueda: "nov 2027" si hay siembra; si es descanso, la campaña "2027-28". */
private fun etiquetaCampana(T: Idioma, c: PlanRotacion.Campana, hoy: LocalDate, i: Int): String =
    c.consejo.fechaSiembra?.let { "${T.mes(it.monthValue).take(3)} ${it.year}" } ?: campana(hoy, i)

/** "2027-28": la campaña agrícola (septiembre a agosto) de una fecha de siembra. */
private fun campana(fecha: LocalDate, saltos: Int): String {
    val ini = (if (fecha.monthValue >= 9) fecha.year else fecha.year - 1) + saltos
    return "$ini-" + "%02d".format((ini + 1) % 100)
}

/** Una fila del plan: etiqueta del año, dibujo, nombre y su cosecha con el color del veredicto. */
@Composable
private fun FilaPlan(etiqueta: String, cultivo: Cultivo, consejo: ConsejoTemporada?, apagado: Boolean,
                     cuando: String?) {
    val T = LocalIdioma.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(76.dp)) {
            Letra(etiqueta, 12, FontWeight.Medium, if (apagado) TintaSuave else Verde, lineas = 1)
        }
        Box(Modifier.size(36.dp).clip(CircleShape).background(if (apagado) Borde else Salvia),
            contentAlignment = Alignment.Center) {
            ArteCultivo(cultivo, Modifier.fillMaxSize().padding(3.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Letra(T.cultivo(cultivo), 14, FontWeight.Medium, if (apagado) TintaSuave else Tinta, lineas = 1)
            cuando?.let { Letra(it, 11, FontWeight.Normal, TintaSuave, lineas = 1) }
        }
        if (consejo != null && cultivo != Cultivo.DESCANSO) {
            val n = Veredicto.de(consejo).nivel
            Box(Modifier.clip(RoundedCornerShape(100.dp)).background(colorNivel(n).copy(alpha = .12f))
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Letra(T.f("opcion.cosecha", (consejo.rendimientoEsperado * 100).roundToInt()), 11,
                    FontWeight.Medium, colorNivel(n), lineas = 1)
            }
        }
    }
}

/**
 * La rueda con el plan: cada campaña se dibuja en el sector de su paso de rotación
 * (papa, otros tubérculos, granos, legumbres, descanso) y una línea con flechas
 * las une en el orden en que se siembran. Si dos campañas caen en el mismo paso,
 * se dibuja la primera; la lista de abajo muestra todas.
 */
@Composable
private fun RuedaPlan(plan: List<PlanRotacion.Campana>, altura: Int, hoy: LocalDate) {
    val T = LocalIdioma.current
    val n = GruposRueda.size
    val paso = 360f / n
    // Primera campaña de cada paso de la rotación.
    val enSector = remember(plan) {
        val m = linkedMapOf<Int, Pair<Int, PlanRotacion.Campana>>()
        plan.forEachIndexed { i, c -> m.putIfAbsent(c.consejo.cultivo.etapa.ordinal, i to c) }
        m
    }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val lado = minOf(maxWidth, 300.dp)
        val radio = lado * .29f
        Box(Modifier.size(lado)) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.width * .44f
                val esquina = Offset(c.x - r, c.y - r)
                GruposRueda.forEachIndexed { k, g ->
                    drawArc(g.fondo, -90f + k * paso - paso / 2f, paso, true, esquina, Size(r * 2, r * 2))
                }
                for (k in 0 until n) {
                    val ang = Math.toRadians((-90f + k * paso - paso / 2f).toDouble())
                    drawLine(Color.White, c, Offset(c.x + r * kotlin.math.cos(ang).toFloat(),
                        c.y + r * kotlin.math.sin(ang).toFloat()), 3.dp.toPx())
                }
                drawCircle(Color.White, size.width * .12f, c)
                // El camino del plan por el borde exterior, en el sentido de la rotación:
                // lejos de los dibujos y sin cortar por el centro.
                val rr = size.width * .485f
                val origen = Offset(c.x - rr, c.y - rr)
                val anillo = Size(rr * 2, rr * 2)
                val margen = 7f
                for (k in 1 until plan.size) {
                    val ka = plan[k - 1].consejo.cultivo.etapa.ordinal
                    val kb = plan[k].consejo.cultivo.etapa.ordinal
                    val giro = ((kb - ka) * paso + 360f) % 360f
                    if (giro == 0f) continue
                    val inicio = -90f + ka * paso + margen
                    val barrido = giro - 2 * margen
                    drawArc(Verde, inicio, barrido, false, origen, anillo,
                        style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                    val fin = Math.toRadians((inicio + barrido).toDouble())
                    val p = Offset(c.x + rr * kotlin.math.cos(fin).toFloat(),
                        c.y + rr * kotlin.math.sin(fin).toFloat())
                    val u = Offset(-kotlin.math.sin(fin).toFloat(), kotlin.math.cos(fin).toFloat())
                    val nor = Offset(-u.y, u.x)
                    val l = 6.dp.toPx()
                    drawLine(Verde, p, p - u * l + nor * l * .7f, 2.5.dp.toPx(), StrokeCap.Round)
                    drawLine(Verde, p, p - u * l - nor * l * .7f, 2.5.dp.toPx(), StrokeCap.Round)
                }
            }
            Image(painterResource(R.drawable.kusi_editorial), T["kusi.alt"],
                Modifier.align(Alignment.Center).size(lado * .19f), contentScale = ContentScale.Fit)
            // Los cinco pasos siempre con dibujo: el del plan con su campaña; los
            // que el plan no toca, atenuados y sin etiqueta.
            GruposRueda.forEachIndexed { k, g ->
                val enPlan = enSector[k]
                val cultivo = enPlan?.second?.consejo?.cultivo ?: representante(g, altura)
                val ang = Math.toRadians((-90f + k * paso).toDouble())
                val x = lado / 2 + radio * kotlin.math.cos(ang).toFloat()
                val y = lado / 2 + radio * kotlin.math.sin(ang).toFloat()
                Column(Modifier.offset(x - 40.dp, y - 34.dp).width(80.dp)
                    .graphicsLayer { alpha = if (enPlan != null) 1f else .35f },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).shadow(4.dp, CircleShape, spotColor = Tinta.copy(alpha = .4f))
                        .clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        ArteCultivo(cultivo, Modifier.fillMaxSize().padding(5.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    if (enPlan != null) {
                        val i = enPlan.first
                        Box(Modifier.clip(RoundedCornerShape(100.dp))
                            .background(if (i == 0) Verde else Color.White)
                            .padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Letra(if (i == 0) T["plan.ahora"] else etiquetaCampana(T, enPlan.second, hoy, i),
                                10, FontWeight.Medium, if (i == 0) Color.White else Verde, lineas = 1)
                        }
                    } else {
                        // Atenuado porque no entra en el plan; si es por la altura, se dice.
                        Letra(if (aEstaAltura(cultivo, altura)) T.cultivo(cultivo) else T["plan.no.altura"],
                            10, FontWeight.Normal, Tinta, lineas = 1)
                    }
                }
            }
        }
    }
}
