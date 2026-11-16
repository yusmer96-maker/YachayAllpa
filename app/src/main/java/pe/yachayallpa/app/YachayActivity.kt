package pe.yachayallpa.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pe.yachayallpa.core.Cultivo
import pe.yachayallpa.core.MotorTemporada
import pe.yachayallpa.core.Temporada
import java.util.concurrent.atomic.AtomicBoolean

/** La vista es Compose; la ubicación, los packs firmados y el motor siguen siendo Java. */
class YachayActivity : ComponentActivity() {
    private val main = Handler(Looper.getMainLooper())
    private var temporadas: Map<String, Temporada> = emptyMap()
    private var region: RegionHuanuco? = null
    private lateinit var repositorio: TemporadaRepositorio
    private lateinit var ajustes: android.content.SharedPreferences
    private var gestor: LocationManager? = null
    private var escucha: LocationListener? = null
    private val cerrada = AtomicBoolean(false)

    var cargando by mutableStateOf(true)
        private set
    var errorDatos by mutableStateOf<String?>(null)
        private set
    var zona by mutableStateOf<Temporada?>(null)
        private set
    var provincia by mutableStateOf<String?>(null)
        private set
    var altura by mutableIntStateOf(0)
        private set
    var alturaAproximada by mutableStateOf(false)
        private set
    /** Clave de `Textos`, no la frase: el estado se escribe en el idioma elegido. */
    var estadoGps by mutableStateOf("gps.buscando")
        private set
    /** La mejor fijación GPS de esta búsqueda. Solo en memoria: no se guarda ni se envía. */
    var fijacion by mutableStateOf<Location?>(null)
        private set
    var precision by mutableStateOf<Float?>(null)
        private set
    var buscandoGps by mutableStateOf(false)
        private set
    /** De dónde sale la altura: "gps", "mapa" (relieve del APK) o "tu" (escrita a mano). */
    var fuenteAltura by mutableStateOf("")
        private set
    var mapa by mutableStateOf<MapaRelieve.Mapa?>(null)
        private set
    private var centroMapa: DoubleArray? = null
    private val alturasGps = ArrayList<Double>()
    private var limiteGps: Runnable? = null
    var cultivoAnterior by mutableStateOf<Cultivo?>(null)
        private set
    var suelo by mutableStateOf(MotorTemporada.Suelo.NO_SE)
        private set
    var prioridad by mutableStateOf(MotorTemporada.Prioridad.EQUILIBRIO)
        private set
    /** 2 = "no lo sé": entre "sí descansó" (1) y "no descansó" (4). */
    var siembrasSinDescanso by mutableIntStateOf(2)
        private set
    /** "es" o "qu". Se guarda en el teléfono y se puede cambiar en Ajustes. */
    var idioma by mutableStateOf("es")
        private set
    /** Lluvia medida reciente y pronóstico de 7 días que publica el VPS; null sin señal. */
    var datos by mutableStateOf<DatosZona?>(null)
        private set
    /**
     * La zona con la que se calcula y se dibuja: la del pack (15 campañas NASA POWER)
     * con la lluvia medida hasta ayer por NASA GPM IMERG cuando el VPS la publicó.
     * Así el agua inicial del suelo, la gráfica y "llovió el X %" usan lo de esta semana.
     */
    val zonaViva by derivedStateOf {
        zona?.let { z -> datos?.aplicar(z, java.time.LocalDate.now()) ?: z }
    }
    var ventana by mutableStateOf<Ventana?>(null)
    /**
     * Volver a un paso ya hecho: 1 = ubicar la chacra, 2 = la rueda de la siembra,
     * 0 = la pantalla que toca. Se vuelve a 0 al ubicar o al elegir el cultivo.
     */
    var repaso by mutableIntStateOf(0)
        private set
    fun verPaso(n: Int) { repaso = n; ventana = null }

    private val lluviaNueva = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Thread {
                try {
                    val nuevas = repositorio.cargar()
                    main.post {
                        if (cerrada.get()) return@post
                        temporadas = nuevas
                        zona?.let { actual ->
                            nuevas[actual.distrito]?.let { renovada ->
                                if (!renovada.fechaCorte.isBefore(actual.fechaCorte)) zona = renovada
                            }
                        }
                    }
                } catch (_: Exception) { /* El pack firmado permanece disponible. */ }
            }.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(248, 247, 243)
        window.navigationBarColor = android.graphics.Color.rgb(248, 247, 243)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        repositorio = TemporadaRepositorio(this)
        ajustes = getSharedPreferences("ajustes", MODE_PRIVATE)
        idioma = ajustes.getString("idioma", "es") ?: "es"
        // Lo que el agricultor ya respondió se recuerda al volver a abrir la app.
        cultivoAnterior = ajustes.getString("cultivo", null)
            ?.let { n -> Cultivo.values().firstOrNull { it.name == n } }
        suelo = ajustes.getString("suelo", null)
            ?.let { n -> MotorTemporada.Suelo.values().firstOrNull { it.name == n } } ?: suelo
        prioridad = ajustes.getString("prioridad", null)
            ?.let { n -> MotorTemporada.Prioridad.values().firstOrNull { it.name == n } } ?: prioridad
        siembrasSinDescanso = ajustes.getInt("descanso", siembrasSinDescanso)
        val filtro = IntentFilter(ActualizacionNasa.CAMBIO)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(lluviaNueva, filtro, RECEIVER_NOT_EXPORTED)
        else registerReceiver(lluviaNueva, filtro)
        setContent { YachayPantalla(this) }
        Thread {
            try {
                val disponibles = repositorio.cargar()
                val mapa = RegionHuanuco(this)
                main.post {
                    if (cerrada.get()) return@post
                    temporadas = disponibles
                    region = mapa
                    cargando = false
                    // El idioma se pregunta una vez; después se cambia en Ajustes.
                    if (ajustes.contains("idioma")) buscarGps() else ventana = Ventana.Idioma
                }
            } catch (error: Exception) {
                main.post {
                    cargando = false
                    errorDatos = error.message ?: ""
                }
            }
        }.start()
    }

    /** Primera vez: se elige el idioma y se sigue con la ubicación. */
    fun elegirIdioma(codigo: String) {
        guardarIdioma(codigo)
        ventana = null
        buscarGps()
    }

    /** Desde Ajustes, en cualquier momento, sin perder lo ya respondido. */
    fun cambiarIdioma(codigo: String) { guardarIdioma(codigo) }

    private fun guardarIdioma(codigo: String) {
        val valido = if (codigo == "qu") "qu" else "es"
        idioma = valido
        ajustes.edit().putString("idioma", valido).apply()
    }

    fun buscarGps() {
        if (cargando || region == null) return
        val fino = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val aproximado = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!fino && !aproximado) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION), 42)
            return
        }
        detenerGps()
        gestor = getSystemService(LOCATION_SERVICE) as? LocationManager
        val manager = gestor ?: run { estadoGps = "gps.sin.servicio"; return }
        estadoGps = "gps.buscando"
        fijacion = null
        alturasGps.clear()
        // Punto de partida: la última posición conocida reciente, como provisional.
        var reciente: Location? = null
        for (proveedor in arrayOf(LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            try {
                val u = manager.getLastKnownLocation(proveedor) ?: continue
                val edad = System.currentTimeMillis() - u.time
                if (edad !in 0..900_000 || u.hasAccuracy() && u.accuracy > 2_500f) continue
                if (reciente == null || u.time > reciente.time) reciente = u
            } catch (_: SecurityException) { }
        }
        reciente?.let { adoptar(it) }
        escucha = object : LocationListener {
            override fun onLocationChanged(location: Location) { adoptar(location) }
        }
        var escuchando = false
        // Cada segundo y sin distancia mínima: el GPS mejora la precisión mientras escucha.
        for ((proveedor, cada) in arrayOf(LocationManager.GPS_PROVIDER to 1_000L,
                LocationManager.NETWORK_PROVIDER to 3_000L)) {
            try {
                if (manager.isProviderEnabled(proveedor)) {
                    manager.requestLocationUpdates(proveedor, cada, 0f, escucha!!)
                    escuchando = true
                }
            } catch (_: SecurityException) { }
        }
        buscandoGps = escuchando
        if (!escuchando && zona == null) estadoGps = "gps.activar"
        // Tope: a los 90 s se queda con lo mejor que tenga.
        val tope = Runnable { detenerGps() }
        limiteGps = tope
        main.postDelayed(tope, 90_000L)
    }

    /**
     * Cada fijación nueva solo reemplaza a la anterior si es más precisa (o si la
     * anterior tiene más de dos minutos). Se deja de escuchar cuando la posición
     * baja de ±15 m y ya hay tres alturas GPS buenas.
     */
    private fun adoptar(u: Location) {
        if (!u.latitude.isFinite() || !u.longitude.isFinite() ||
                u.hasAccuracy() && u.accuracy > 2_500f) return
        val mejor = fijacion
        val masPrecisa = mejor == null || !mejor.hasAccuracy() ||
                (u.hasAccuracy() && u.accuracy <= mejor.accuracy) ||
                u.time - mejor.time > 120_000L
        if (masPrecisa) {
            val mapaRegion = region ?: return
            val lugar = mapaRegion.provincia(u.latitude, u.longitude)
            if (lugar == null) {
                if (zona == null) estadoGps = "gps.fuera"
                return
            }
            val nueva = mapaRegion.cercana(temporadas, u.latitude, u.longitude) ?: return
            if (nueva.distrito != zona?.distrito) {
                ActualizacionNasa.programar(this, nueva)
                cargarDatos(nueva)
            }
            zona = nueva
            provincia = lugar
            if (repaso == 1) repaso = 0
            fijacion = Location(u)
            precision = if (u.hasAccuracy()) u.accuracy else null
            redibujarMapa(u.latitude, u.longitude)
        }
        // Altura: solo del GPS, con buena precisión vertical (o buena horizontal si
        // el teléfono no informa la vertical), y siempre como mediana de varias.
        val alturaBuena = u.provider == LocationManager.GPS_PROVIDER && u.hasAltitude() &&
                u.altitude in 30.0..5_600.0 &&
                (if (u.hasVerticalAccuracy()) u.verticalAccuracyMeters <= 40f
                 else u.hasAccuracy() && u.accuracy <= 25f)
        if (alturaBuena && fuenteAltura != "tu") registrarAlturaGps(u)
        else if (masPrecisa && (altura == 0 || fuenteAltura == "mapa")) {
            val relieve = AltitudMapa.metros(this, u.latitude, u.longitude)
            if (relieve > 0) {
                altura = relieve
                alturaAproximada = true
                fuenteAltura = "mapa"
            }
        }
        estadoGps = if (altura > 0) "gps.encontrada" else "gps.falta.altura"
        val p = precision
        if (p != null && p <= 15f && alturasGps.size >= 3) detenerGps()
    }

    /**
     * El GPS da la altura sobre el elipsoide, no sobre el mar. En Android 14+ se
     * convierte con el modelo del geoide del propio sistema (sin red); la
     * conversión no puede ir en el hilo de la pantalla.
     */
    private fun registrarAlturaGps(u: Location) {
        val copia = Location(u)
        Thread {
            var metros = copia.altitude
            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    android.location.altitude.AltitudeConverter()
                        .addMslAltitudeToLocation(this, copia)
                    if (copia.hasMslAltitude()) metros = copia.mslAltitudeMeters
                } catch (_: Exception) { }
            }
            main.post {
                if (cerrada.get() || fuenteAltura == "tu") return@post
                alturasGps.add(metros)
                while (alturasGps.size > 7) alturasGps.removeAt(0)
                val orden = alturasGps.sorted()
                val mediana = orden[orden.size / 2]
                altura = (kotlin.math.round(mediana / 10.0) * 10).toInt()
                alturaAproximada = false
                fuenteAltura = "gps"
                estadoGps = "gps.encontrada"
            }
        }.start()
    }

    /** Redibuja el relieve solo si la chacra se movió más de 150 m del mapa actual. */
    private fun redibujarMapa(lat: Double, lon: Double) {
        val previo = centroMapa
        if (previo != null) {
            val d = FloatArray(1)
            Location.distanceBetween(previo[0], previo[1], lat, lon, d)
            if (d[0] < 150f) return
        }
        centroMapa = doubleArrayOf(lat, lon)
        Thread {
            val nuevo = MapaRelieve.dibujar(this, lat, lon, 56, 480)
            main.post { if (!cerrada.get()) mapa = nuevo }
        }.start()
    }

    /** Centro {lat, lon} de la provincia actual, para centrar el mapa sin GPS. */
    fun centroProvincia(): DoubleArray? = provincia?.let { runCatching { region?.centro(it) }.getOrNull() }

    /** Contorno de la provincia actual, para dibujarlo sobre el relieve. */
    fun contornoProvincia(): Array<DoubleArray>? = provincia?.let { region?.contorno(it) }

    fun elegirProvincia(nombre: String) {
        val centro = region?.centro(nombre) ?: return
        val nueva = region?.cercana(temporadas, centro[0], centro[1]) ?: return
        detenerGps()
        provincia = nombre
        zona = nueva
        if (repaso == 1) repaso = 0
        altura = 0
        alturaAproximada = false
        fuenteAltura = ""
        // Elegida a mano: sin posición GPS, el mapa se centra en la provincia.
        fijacion = null
        precision = null
        centroMapa = null
        redibujarMapa(centro[0], centro[1])
        estadoGps = "gps.provincia.elegida"
        ActualizacionNasa.programar(this, nueva)
        cargarDatos(nueva)
        ventana = Ventana.Altura
    }

    fun provincias(): List<String> = region?.nombres() ?: emptyList()

    fun guardarAltura(valor: String): Boolean {
        val metros = valor.trim().toIntOrNull() ?: return false
        if (metros !in 50..5_500) return false
        altura = (kotlin.math.round(metros / 10.0) * 10).toInt()
        alturaAproximada = false
        fuenteAltura = "tu"
        estadoGps = "gps.altura.tuya"
        ventana = null
        return true
    }

    fun elegirCultivo(cultivo: Cultivo) {
        // Cambiar el cultivo anterior no cambia si la tierra descansó: no se
        // borra la respuesta que el usuario dio en Ajustes.
        cultivoAnterior = cultivo
        repaso = 0
        ajustes.edit().putString("cultivo", cultivo.name).apply()
        ventana = null
    }

    fun guardarOpciones(nuevoSuelo: MotorTemporada.Suelo,
                        nuevaPrioridad: MotorTemporada.Prioridad, descanso: Int) {
        suelo = nuevoSuelo
        prioridad = nuevaPrioridad
        siembrasSinDescanso = descanso
        ajustes.edit().putString("suelo", nuevoSuelo.name)
            .putString("prioridad", nuevaPrioridad.name).putInt("descanso", descanso).apply()
        ventana = null
    }

    /**
     * Primero lo guardado (sirve sin señal), luego lo nuevo del VPS en un hilo
     * aparte. Si la zona cambió mientras tanto, la respuesta vieja se descarta.
     */
    private fun cargarDatos(nueva: Temporada) {
        val id = DatosZona.idDe(nueva.distrito)
        datos = DatosZona.guardado(this, id)
        Thread {
            val fresco = DatosZona.renovar(this, id) ?: return@Thread
            main.post {
                if (!cerrada.get() && zona?.distrito == nueva.distrito) datos = fresco
            }
        }.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) buscarGps()
            else estadoGps = "gps.sin.permiso"
        }
    }

    private fun detenerGps() {
        escucha?.let { gestor?.removeUpdates(it) }
        escucha = null
        limiteGps?.let { main.removeCallbacks(it) }
        limiteGps = null
        buscandoGps = false
    }

    override fun onDestroy() {
        cerrada.set(true)
        detenerGps()
        unregisterReceiver(lluviaNueva)
        super.onDestroy()
    }
}

sealed interface Ventana {
    data object Idioma : Ventana
    data object Ubicacion : Ventana
    data object Provincias : Ventana
    data object Altura : Ventana
    data object Cultivos : Ventana
    data object Preferencias : Ventana
    data object Rotacion : Ventana
    data object Clima : Ventana
    data class Razones(val consejo: pe.yachayallpa.core.ConsejoTemporada) : Ventana
}
