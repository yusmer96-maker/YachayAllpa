package pe.yachayallpa.app;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.provider.Settings;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.text.InputType;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import pe.yachayallpa.core.Cultivo;
import pe.yachayallpa.core.Pack;
import pe.yachayallpa.core.Temporada;
import pe.yachayallpa.core.MotorTemporada;
import pe.yachayallpa.core.ConsejoTemporada;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

public final class MainActivity extends Activity {
    private static final int CREMA = 0xFFF7F6F1;
    private static final int TINTA = 0xFF20372E;
    private static final int VERDE = 0xFF2E7D32;
    private static final int VERDE_PROFUNDO = 0xFF245440;
    private static final int TERRACOTA = 0xFF8F4E3B;
    private static final int SALVIA = 0xFFE5EFE7;
    private static final int DURAZNO = 0xFFF4E8DF;
    private static final int AMBAR = 0xFFF9A825;
    private static final int ROJO = 0xFFC62828;
    private static final int GPS_PERMISO = 42;
    private static final int ELEGIR_PACK = 43;
    private static final int ELEGIR_TEMPORADA = 44;
    private static final int BUSCANDO = 0, ENCONTRADA = 1, SIN_GPS = 2, FUERA = 3;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Map<String, Pack> packs = Collections.emptyMap();
    private PackRepositorio repositorio;
    private TemporadaRepositorio temporadasRepositorio;
    private Map<String, Temporada> temporadas = Collections.emptyMap();
    private MotorTemporada.Suelo suelo = MotorTemporada.Suelo.NO_SE;
    private MotorTemporada.Prioridad prioridad = MotorTemporada.Prioridad.EQUILIBRIO;
    private boolean quechua, idiomaElegido;
    private Voz voz;
    private Typeface letra, letraNegrita, titular;
    private ScrollView scroll;
    private LinearLayout contenido;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private int paso, altura, campanias, estadoGps = BUSCANDO;
    private Pack pack;
    private RegionHuanuco region;
    private Temporada seleccionada;
    private String provincia;
    private Cultivo anterior;
    private boolean alturaGps, alturaMapa, alturaManual;
    private boolean cerrada;
    private final BroadcastReceiver lluviaNueva = new BroadcastReceiver() {
        @Override public void onReceive(Context contexto, Intent intento) {
            new Thread(() -> {
                try {
                    Map<String, Temporada> nuevas = temporadasRepositorio.cargar();
                    handler.post(() -> {
                        if (cerrada) return;
                        temporadas = nuevas;
                        if (seleccionada != null) {
                            Temporada actual = nuevas.get(seleccionada.distrito);
                            if (actual != null && !actual.fechaCorte.isBefore(seleccionada.fechaCorte)) {
                                seleccionada = actual;
                                if (paso == 3) mostrarResultado();
                                else if (paso == 4) mostrarDatos();
                                else if (paso == 0) mostrarInicio();
                            }
                        }
                    });
                } catch (Exception ignorado) { /* El pack firmado sigue disponible. */ }
            }, "nasa-recargar").start();
        }
    };

    @Override public void onCreate(Bundle estado) {
        super.onCreate(estado);
        getWindow().setStatusBarColor(CREMA);
        getWindow().setNavigationBarColor(CREMA);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        try {
            // Pesos reales de las fuentes variables: se leen mejor bajo el sol que negritas simuladas.
            letra = new Typeface.Builder(getAssets(), "fonts/DMSans.ttf")
                    .setFontVariationSettings("'wght' 700").build();
            letraNegrita = new Typeface.Builder(getAssets(), "fonts/DMSans.ttf")
                    .setFontVariationSettings("'wght' 800").build();
            titular = new Typeface.Builder(getAssets(), "fonts/Manrope.ttf")
                    .setFontVariationSettings("'wght' 800").build();
        } catch (RuntimeException e) {
            letra = Typeface.DEFAULT_BOLD;
            letraNegrita = Typeface.DEFAULT_BOLD;
            titular = Typeface.DEFAULT_BOLD;
        }
        voz = new Voz(this);
        repositorio = new PackRepositorio(this);
        temporadasRepositorio = new TemporadaRepositorio(this);
        IntentFilter filtro = new IntentFilter(ActualizacionNasa.CAMBIO);
        if (Build.VERSION.SDK_INT >= 33)
            registerReceiver(lluviaNueva, filtro, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(lluviaNueva, filtro);
        mostrarCarga();
        long inicio = SystemClock.uptimeMillis();
        // Verificar las firmas fuera del hilo visual permite mostrar la chacra mientras se leen los packs.
        new Thread(() -> {
            Map<String, Pack> cargados = Collections.emptyMap();
            Map<String, Temporada> temporadaCargada = Collections.emptyMap();
            RegionHuanuco regionCargada = null;
            String error = null;
            try { cargados = repositorio.cargar(); temporadaCargada = temporadasRepositorio.cargar(); regionCargada = new RegionHuanuco(this); }
            catch (Exception e) { error = e.getMessage(); }
            Map<String, Pack> finales = cargados;
            Map<String, Temporada> finalTemporada = temporadaCargada;
            RegionHuanuco finalRegion = regionCargada;
            String fallo = error;
            handler.postDelayed(() -> {
                if (cerrada) return;
                packs = finales;
                temporadas = finalTemporada;
                region = finalRegion;
                if (fallo != null) aviso("Datos rechazados: " + fallo);
                mostrarInicio();
                // Una sola ventana al inicio: el permiso GPS se pide al cerrar la elección de idioma.
                elegirIdioma();
            }, Math.max(0, 1400 - (SystemClock.uptimeMillis() - inicio)));
        }).start();
    }

    private int dp(float valor) {
        return (int) (getResources().getDisplayMetrics().density * valor + .5f);
    }

    private int barraSuperior(WindowInsets insets) {
        return Build.VERSION.SDK_INT >= 30
                ? insets.getInsets(WindowInsets.Type.statusBars()).top
                : insets.getSystemWindowInsetTop();
    }

    private int barraInferior(WindowInsets insets) {
        return Build.VERSION.SDK_INT >= 30
                ? insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                : insets.getSystemWindowInsetBottom();
    }

    private GradientDrawable fondo(int color, int borde, int radio) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radio));
        if (borde != 0) d.setStroke(dp(1.5f), borde);
        return d;
    }

    private TextView texto(String valor, int tam, int color, boolean fuerte) {
        TextView t = new TextView(this);
        t.setText(valor);
        t.setTextSize(tam);
        t.setTextColor(color);
        t.setTypeface(fuerte ? letraNegrita : letra);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(false);
        return t;
    }

    private TextView titulo(String valor, int tam, int color) {
        TextView t = texto(valor, tam, color, true);
        t.setTypeface(titular);
        return t;
    }

    private void separar(int alto) {
        contenido.addView(new View(this), new LinearLayout.LayoutParams(1, dp(alto)));
    }

    private TextView boton(String etiqueta, boolean principal) {
        TextView b = texto(etiqueta, 16, principal ? Color.WHITE : VERDE_PROFUNDO, true);
        b.setGravity(Gravity.CENTER);
        GradientDrawable forma=fondo(principal ? VERDE_PROFUNDO : SALVIA, 0, 16);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x2280A48D), forma, null));
        b.setMinHeight(dp(52));
        b.setPadding(dp(12), dp(7), dp(12), dp(7));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private void agregarBoton(TextView boton) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.bottomMargin = dp(9);
        contenido.addView(boton, p);
    }

    private final class VentanaDesplazable extends ScrollView {
        VentanaDesplazable() { super(MainActivity.this); }
        @Override protected void onMeasure(int ancho, int alto) {
            int maximo=(int)(getResources().getDisplayMetrics().heightPixels*.72f);
            int modo=View.MeasureSpec.getMode(alto);
            int disponible=modo==View.MeasureSpec.UNSPECIFIED ? maximo
                    : Math.min(maximo,View.MeasureSpec.getSize(alto));
            super.onMeasure(ancho, View.MeasureSpec.makeMeasureSpec(disponible,
                    View.MeasureSpec.AT_MOST));
        }
    }

    private void mostrarVentana(Dialog ventana, View cuerpo) {
        // El contenido debe desplazarse aun con letras grandes o con el teclado abierto.
        VentanaDesplazable desplazable = new VentanaDesplazable();
        desplazable.setFillViewport(false);
        desplazable.setVerticalScrollBarEnabled(false);
        desplazable.addView(cuerpo);
        ventana.setContentView(desplazable);
        ventana.show();
        Window window=ventana.getWindow();
        if(window!=null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            window.setLayout(getResources().getDisplayMetrics().widthPixels-dp(28),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void ventanaOpciones(String pregunta, String[] opciones, int elegido,
                                IntConsumer alElegir) {
        Dialog ventana = new Dialog(this);
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(dp(18),dp(18),dp(18),dp(18));
        caja.setBackground(fondo(CREMA,0,20));
        caja.addView(titulo(pregunta,22,TINTA));
        separarEn(caja,14);
        for(int i=0;i<opciones.length;i++) {
            final int indice=i;
            TextView opcion=boton((i==elegido?"✓  ":"")+opciones[i],i==elegido);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));
            p.bottomMargin=dp(8);
            caja.addView(opcion,p);
            opcion.setOnClickListener(v->{ventana.dismiss();alElegir.accept(indice);});
        }
        TextView cerrar=texto("Cerrar",15,VERDE_PROFUNDO,true);
        cerrar.setGravity(Gravity.CENTER);
        caja.addView(cerrar,new LinearLayout.LayoutParams(-1,dp(42)));
        cerrar.setOnClickListener(v->ventana.dismiss());
        mostrarVentana(ventana,caja);
    }

    private void ventanaMensaje(String encabezado, String mensaje) {
        Dialog ventana=new Dialog(this);
        LinearLayout caja=new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(dp(20),dp(20),dp(20),dp(20));
        caja.setBackground(fondo(CREMA,0,20));
        caja.addView(titulo(encabezado,22,TINTA));
        separarEn(caja,12);
        caja.addView(texto(mensaje,16,TINTA,true));
        separarEn(caja,18);
        TextView listo=boton("Entendido",true);
        caja.addView(listo,new LinearLayout.LayoutParams(-1,dp(52)));
        listo.setOnClickListener(v->ventana.dismiss());
        mostrarVentana(ventana,caja);
    }

    private void mostrarCarga() {
        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundColor(CREMA);
        ImageView paisaje = new ImageView(this);
        paisaje.setImageResource(R.drawable.portada_splash);
        paisaje.setScaleType(ImageView.ScaleType.CENTER_CROP);
        raiz.addView(paisaje, new FrameLayout.LayoutParams(-1, -1));
        setContentView(raiz);
        LinearLayout arriba = new LinearLayout(this);
        arriba.setOrientation(LinearLayout.VERTICAL);
        arriba.setPadding(dp(27), dp(58), dp(27), 0);
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        raiz.addView(arriba, ap);
        arriba.addView(titulo("Yachay\nAllpa", 44, TINTA));
        TextView lema = texto("La tierra enseña.\nEl clima avisa.", 19, TINTA, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10);
        arriba.addView(lema, lp);

        LinearLayout carga = new LinearLayout(this);
        carga.setOrientation(LinearLayout.VERTICAL);
        carga.setPadding(dp(18), dp(15), dp(18), dp(17));
        carga.setBackground(fondo(0xF5F8F6F0, 0, 14));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        cp.setMargins(dp(20), 0, dp(20), dp(35));
        raiz.addView(carga, cp);
        carga.addView(texto("PREPARANDO TU CHACRA", 13, VERDE_PROFUNDO, true));
        TextView detalle = texto("Leyendo los datos guardados en el teléfono", 15, TINTA, false);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = dp(5);
        carga.addView(detalle, dp);
        ProgressBar progreso = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progreso.setIndeterminate(true);
        progreso.setIndeterminateTintList(ColorStateList.valueOf(TERRACOTA));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(5));
        pp.topMargin = dp(13);
        carga.addView(progreso, pp);
        raiz.setOnApplyWindowInsetsListener((vista, insets) -> {
            arriba.setPadding(dp(27), barraSuperior(insets) + dp(30), dp(27), 0);
            FrameLayout.LayoutParams parametros =
                    (FrameLayout.LayoutParams) carga.getLayoutParams();
            parametros.bottomMargin = barraInferior(insets) + dp(14);
            carga.setLayoutParams(parametros);
            return insets;
        });
        raiz.requestApplyInsets();
    }

    private void pantalla(String etiqueta, String titulo, String subtitulo) {
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(CREMA);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setBackgroundColor(CREMA);
        raiz.setOnApplyWindowInsetsListener((vista, insets) -> {
            raiz.setPadding(0, barraSuperior(insets), 0, barraInferior(insets));
            return insets;
        });
        contenido = new LinearLayout(this);
        contenido.setOrientation(LinearLayout.VERTICAL);
        contenido.setPadding(dp(20), dp(14), dp(20), dp(28));
        contenido.setFocusableInTouchMode(true);
        scroll.addView(contenido);
        raiz.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        raiz.addView(barraInferiorNavegacion(), new LinearLayout.LayoutParams(-1, dp(70)));
        setContentView(raiz);
        raiz.requestApplyInsets();

        LinearLayout marca = new LinearLayout(this);
        marca.setGravity(Gravity.CENTER_VERTICAL);
        contenido.addView(marca, new LinearLayout.LayoutParams(-1, dp(50)));
        TextView simbolo = titulo("Y", 24, Color.WHITE);
        simbolo.setGravity(Gravity.CENTER);
        simbolo.setBackground(fondo(TERRACOTA, 0, 13));
        marca.addView(simbolo, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout tituloMarca = new LinearLayout(this);
        tituloMarca.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, -2, 1);
        mp.leftMargin = dp(10);
        marca.addView(tituloMarca, mp);
        tituloMarca.addView(titulo("Yachay Allpa", 21, VERDE_PROFUNDO));
        TextView lugarMarca=texto("HUÁNUCO · PERÚ", 11, 0xFF697B70, true);
        lugarMarca.setLetterSpacing(.08f);
        marca.addView(lugarMarca);
        separar(15);
        FrameLayout cabecera = new FrameLayout(this);
        int fondoCabecera=paso==3?VERDE_PROFUNDO:SALVIA;
        cabecera.setBackground(fondo(fondoCabecera, 0, 20));
        cabecera.setClipToOutline(true);
        contenido.addView(cabecera, new LinearLayout.LayoutParams(-1,
                dp(paso == 0 ? 172 : paso==1 ? 122 : paso==3 ? 120 : 106)));
        ImageView imagen = new ImageView(this);
        boolean paisaje=paso==0 || paso==4 || paso==5;
        if(paisaje) {
            imagen.setImageResource(paso==4?R.drawable.lluvia_cabecera:R.drawable.paisaje_cabecera);
            imagen.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cabecera.addView(imagen,new FrameLayout.LayoutParams(-1,-1));
            GradientDrawable sombra=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xC9234739,0x11234739});
            View velo=new View(this);velo.setBackground(sombra);
            cabecera.addView(velo,new FrameLayout.LayoutParams(-1,-1));
        } else {
            imagen.setImageResource(R.drawable.kusi_editorial);
            imagen.setScaleType(ImageView.ScaleType.FIT_CENTER);
            FrameLayout.LayoutParams k=new FrameLayout.LayoutParams(dp(142),-1,Gravity.RIGHT);
            k.rightMargin=dp(2);cabecera.addView(imagen,k);
        }
        String breve=paso==0?"Tu tierra, tu decisión":paso==1?"Vamos paso a paso":
                paso==3?"Hecho para tu chacra":
                paso==4?"Mira la lluvia": "Conoce tu terreno";
        TextView frase=titulo(breve,paso==0?25:22,paisaje||paso==3?Color.WHITE:TINTA);
        frase.setMaxWidth(dp(paso==0?260:205));
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.CENTER_VERTICAL);
        fp.leftMargin=dp(19);fp.rightMargin=dp(105);
        cabecera.addView(frase,fp);
        separar(17);
        TextView pasoTexto = texto(etiqueta.toUpperCase(new Locale("es","PE")), 12, TERRACOTA, true);
        pasoTexto.setLetterSpacing(.1f);
        contenido.addView(pasoTexto);
        separar(5);
        contenido.addView(titulo(titulo, 30, TINTA));
        separar(5);
        if(!subtitulo.isEmpty()) {
            contenido.addView(texto(subtitulo, 16, 0xFF63776A, false));
            separar(13);
        }
        if(paso==0 || paso==1) {
            LinearLayout progreso=new LinearLayout(this);
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(5));
            pp.bottomMargin=dp(18);contenido.addView(progreso,pp);
            for(int i=0;i<2;i++) {
                View segmento=new View(this);
                segmento.setBackground(fondo(i<=paso?VERDE_PROFUNDO:0xFFDDE5DA,0,3));
                LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-1,1);
                if(i>0)sp.leftMargin=dp(6);
                progreso.addView(segmento,sp);
            }
        }
        // En algunos teléfonos el foco inicial mueve el ScrollView: se devuelve arriba tras medirlo.
        contenido.requestFocus();
        scroll.postDelayed(() -> {
            if (scroll != null) {
                contenido.requestFocus();
                scroll.scrollTo(0, 0);
            }
        }, 280);
    }

    private void mostrarInicio() {
        paso = 0;
        pantalla("PASO 1 DE 2 · TU LUGAR", "Primero, tu lugar",
                "Así usamos el clima de la zona donde siembras.");
        mensajeAsistente(seleccionada==null?"Te ayudo a encontrar tu chacra.":
                altura==0?"Ya tengo tu zona. Falta saber la altura.":
                "Ya tengo tu zona y altura. Sigamos.");

        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding(dp(17), dp(15), dp(17), dp(15));
        tarjeta.setBackground(fondo(Color.WHITE, 0xFFE2E9E1, 18));
        contenido.addView(tarjeta, new LinearLayout.LayoutParams(-1, -2));
        tarjeta.addView(texto("📍  UBICACIÓN DE TU CHACRA", 12, TERRACOTA, true));
        TextView nombre;
        String detalle;
        if (seleccionada != null) {
            nombre = texto("Provincia de " + provincia, 23, TINTA, true);
            detalle = alturaGps ? "Altura GPS: " + altura + " m"
                    : alturaMapa ? "Altura aproximada del mapa: " + altura + " m"
                    : alturaManual ? "Altura que indicaste: " + altura + " m"
                    : "Buscando altura. Puedes continuar cuando aparezca.";
        } else if (estadoGps == BUSCANDO) {
            nombre = texto("Buscando señal GPS…", 22, TINTA, true);
            detalle = "Puede tardar un poco si estás bajo techo.";
        } else if (estadoGps == FUERA) {
            nombre = texto("Fuera de la región Huánuco", 20, TINTA, true);
            detalle = "Esta versión tiene datos para la región Huánuco.";
        } else {
            nombre = texto("Todavía no sé dónde estás", 21, TINTA, true);
            detalle = estadoGps == SIN_GPS ? "Activa la ubicación del teléfono o elige tu provincia."
                    : "Puedes elegir tu provincia y luego indicar la altura.";
        }
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(10);
        tarjeta.addView(nombre, np);
        LinearLayout.LayoutParams dt = new LinearLayout.LayoutParams(-1, -2);
        dt.topMargin = dp(5);
        tarjeta.addView(texto(detalle, 16, 0xFF5B6D60, false), dt);
        separar(16);

        TextView continuar = boton(seleccionada == null ? "Elegir mi provincia  →" :
                altura == 0 ? "Indicar altura  →" : "Continuar  →", true);
        agregarBoton(continuar);
        continuar.setOnClickListener(v -> {
            if (seleccionada == null) elegirDistrito();
            else if (altura == 0) pedirAltura();
            else {
                // La zona queda fija durante las respuestas; un ajuste tardío del GPS
                // no puede cambiar silenciosamente la lluvia ni la recomendación.
                detenerGps();
                ActualizacionNasa.programar(this, seleccionada);
                paso = 1; mostrarCultivos();
            }
        });
        TextView cambiar = texto(seleccionada == null && !ubicacionActivada()
                        ? "⌖  Activar ubicación" : seleccionada == null ? "⌖  Buscar mi ubicación" :
                        alturaGps ? "Cambiar ubicación" : "⌖  Mejorar altura con GPS",
                16, VERDE_PROFUNDO, true);
        cambiar.setGravity(Gravity.CENTER);
        contenido.addView(cambiar, new LinearLayout.LayoutParams(-1, dp(44)));
        cambiar.setOnClickListener(v -> {
            if (seleccionada == null && !ubicacionActivada())
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            else if (seleccionada == null || !alturaGps) iniciarGpsAutomatico();
            else elegirDistrito();
        });
        if (seleccionada != null && !alturaGps) {
            TextView corregir = texto("Corregir altura o ubicación", 15, VERDE_PROFUNDO, true);
            corregir.setGravity(Gravity.CENTER);
            corregir.setOnClickListener(v -> elegirCorreccion());
            contenido.addView(corregir, new LinearLayout.LayoutParams(-1, dp(42)));
        }
        TextView privado = texto("Funciona sin señal  ·  La app no envía tu ubicación exacta",
                14, 0xFF637A66, false);
        privado.setGravity(Gravity.CENTER);
        contenido.addView(privado);
        separar(8);
        if(idiomaElegido)voz.ayuda(seleccionada==null?"inicio":
                altura==0?"altura_pendiente":"inicio_listo",quechua);
    }

    private void elegirDistrito() {
        if (region == null || temporadas.isEmpty()) { aviso("No hay datos del clima para esta zona"); return; }
        List<String> nombres = region.nombres();
        Dialog dialogo = new Dialog(this);
        LinearLayout caja=new LinearLayout(this);caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(dp(18),dp(18),dp(18),dp(18));caja.setBackground(fondo(CREMA,0,20));
        caja.addView(titulo("¿En qué provincia está tu chacra?",23,TINTA));
        caja.addView(texto("El GPS puede ubicarla solo. Si eliges aquí, después indica la altura.",
                14,0xFF5B6D60,false));
        separarEn(caja,12);
        for(String nombre:nombres) {
            TextView opcion=boton("⛰  "+nombre,false);
            LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,dp(51));op.bottomMargin=dp(6);
            caja.addView(opcion,op);
            opcion.setOnClickListener(v->{
                detenerGps();double[] centro=region.centro(nombre);
                seleccionada=region.cercana(temporadas,centro[0],centro[1]);
                ActualizacionNasa.programar(this, seleccionada);
                provincia=nombre;altura=0;alturaGps=false;alturaMapa=false;
                alturaManual=false;estadoGps=ENCONTRADA;
                dialogo.dismiss();mostrarInicio();pedirAltura();
            });
        }
        mostrarVentana(dialogo,caja);
    }

    private void pedirAltura() {
        Dialog d=new Dialog(this);
        LinearLayout caja=new LinearLayout(this);caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(dp(20),dp(20),dp(20),dp(20));caja.setBackground(fondo(CREMA,0,20));
        caja.addView(titulo("Altura de tu chacra",23,TINTA));
        caja.addView(texto("Si la conoces, escríbela en metros sobre el mar. No uses la altura del celular dentro de una casa.",
                15,TINTA,false));
        separarEn(caja,10);
        EditText entrada=new EditText(this);entrada.setTextSize(22);entrada.setHint("Ejemplo: 3200");
        entrada.setTypeface(letraNegrita);
        entrada.setTextColor(TINTA);
        entrada.setBackground(fondo(Color.WHITE,0xFFDCE7DA,14));
        entrada.setPadding(dp(14),dp(8),dp(14),dp(8));
        entrada.setInputType(InputType.TYPE_CLASS_NUMBER);
        caja.addView(entrada,new LinearLayout.LayoutParams(-1,dp(54)));
        separarEn(caja,12);
        TextView guardar=boton("Usar esta altura",true);caja.addView(guardar);
        guardar.setOnClickListener(v->{
            try { int h=Integer.parseInt(entrada.getText().toString());
                if(h<50||h>5500)throw new NumberFormatException();
                altura=(int)Math.round(h/10.0)*10;alturaGps=false;alturaMapa=false;
                alturaManual=true;d.dismiss();mostrarInicio();
            }catch(NumberFormatException e){aviso("Indica una altura entre 50 y 5500 metros");}
        });
        mostrarVentana(d,caja);
    }

    private void elegirCorreccion() {
        ventanaOpciones("Corregir mi ubicación",
                new String[]{"Medir de nuevo con GPS", "Indicar otra altura",
                        "Elegir otra provincia"},-1, opcion -> {
                    if (opcion == 0) iniciarGpsAutomatico();
                    else if (opcion == 1) pedirAltura();
                    else elegirDistrito();
                });
    }

    private void iniciarGpsAutomatico() {
        if (temporadas.isEmpty() || region==null) { estadoGps = SIN_GPS; mostrarInicio(); return; }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_PERMISO);
            return;
        }
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) { estadoGps=SIN_GPS; if(paso==0)mostrarInicio(); return; }
        boolean gpsActivo=locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean redActiva=locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsActivo && !redActiva) {
            estadoGps = SIN_GPS;
            if (paso == 0) mostrarInicio();
            return;
        }
        detenerGps();
        Location guardada = buscarUbicacionReciente();
        if (guardada != null) adoptarUbicacion(guardada, true);
        else {
            estadoGps = BUSCANDO;
            if (paso == 0) mostrarInicio();
        }
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location ubicacion) {
                adoptarUbicacion(ubicacion, false);
            }
            @Override public void onStatusChanged(String proveedor, int estado, Bundle extra) { }
            @Override public void onProviderEnabled(String proveedor) { }
            @Override public void onProviderDisabled(String proveedor) { }
        };
        try {
            if(gpsActivo)locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000L, 0f, locationListener, Looper.getMainLooper());
            if(redActiva)locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    1000L, 0f, locationListener, Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (locationListener != null) {
                    detenerGps();
                    if (seleccionada == null) estadoGps = SIN_GPS;
                    if (paso == 0) mostrarInicio();
                }
            }, 45000);
        } catch (SecurityException | IllegalArgumentException e) {
            detenerGps();
            estadoGps = SIN_GPS;
            if (paso == 0) mostrarInicio();
        }
    }

    private boolean ubicacionActivada() {
        LocationManager gestor=(LocationManager)getSystemService(LOCATION_SERVICE);
        return gestor!=null && (gestor.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || gestor.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    @Override protected void onResume() {
        super.onResume();
        if (idiomaElegido && paso==0 && seleccionada==null && ubicacionActivada()
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) iniciarGpsAutomatico();
    }

    private Location buscarUbicacionReciente() {
        Location mejor = null;
        long ahora = System.currentTimeMillis();
        for (String proveedor : new String[]{LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                Location candidata = locationManager.getLastKnownLocation(proveedor);
                if (candidata == null) continue;
                long edad = ahora - candidata.getTime();
                if (edad < 0 || edad > 15 * 60 * 1000L) continue;
                if (candidata.hasAccuracy() && candidata.getAccuracy() > 2500f) continue;
                if (mejor == null || candidata.getTime() > mejor.getTime()) mejor = candidata;
            } catch (IllegalArgumentException | SecurityException ignorada) { }
        }
        return mejor;
    }

    private void adoptarUbicacion(Location ubicacion, boolean reciente) {
        if (ubicacion == null || !Double.isFinite(ubicacion.getLatitude())
                || !Double.isFinite(ubicacion.getLongitude())
                || (ubicacion.hasAccuracy() && ubicacion.getAccuracy() > 2500f)) return;
        Temporada zonaAnterior=seleccionada;
        int alturaAnterior=altura, estadoAnterior=estadoGps;
        String provinciaAnterior=provincia;
        // La posición solo decide provincia y celda NASA en memoria; jamás se escribe en disco.
        String lugar=region==null?null:region.provincia(ubicacion.getLatitude(),ubicacion.getLongitude());
        if(lugar==null) {
            estadoGps=FUERA;seleccionada=null;provincia=null;altura=0;
            alturaGps=false;alturaMapa=false;alturaManual=false;
        } else {
            Temporada zona=region.cercana(temporadas,ubicacion.getLatitude(),ubicacion.getLongitude());
            if(zona==null){estadoGps=SIN_GPS;return;}
            seleccionada=zona;provincia=lugar;
            if (zonaAnterior == null || !zonaAnterior.distrito.equals(zona.distrito))
                ActualizacionNasa.programar(this, zona);
            boolean nuevaAlturaGps=ubicacion.hasAltitude()&&Double.isFinite(ubicacion.getAltitude())
                    &&ubicacion.getAltitude()>=50&&ubicacion.getAltitude()<=5500
                    &&(!ubicacion.hasVerticalAccuracy()||ubicacion.getVerticalAccuracyMeters()<=150)
                    &&LocationManager.GPS_PROVIDER.equals(ubicacion.getProvider());
            if(nuevaAlturaGps && !alturaManual) {
                altura=(int)Math.round(ubicacion.getAltitude()/10.0)*10;
                alturaGps=true;alturaMapa=false;
            } else if(!alturaGps && !alturaManual) {
                int mapa=AltitudMapa.metros(this,ubicacion.getLatitude(),ubicacion.getLongitude());
                if(mapa>0){altura=mapa;alturaMapa=true;}
            }
            estadoGps=ENCONTRADA;
        }
        // El primer arreglo puede traer coordenadas sin altura; esperamos otro GPS más preciso.
        if (!reciente && alturaGps) detenerGps();
        if (paso == 0 && (zonaAnterior!=seleccionada || alturaAnterior!=altura
                || estadoAnterior!=estadoGps
                || !java.util.Objects.equals(provinciaAnterior,provincia))) mostrarInicio();
    }

    private void detenerGps() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationListener = null;
        }
    }

    @Override public void onRequestPermissionsResult(
            int pedido, String[] permisos, int[] respuestas) {
        super.onRequestPermissionsResult(pedido, permisos, respuestas);
        if (pedido != GPS_PERMISO) return;
        if (respuestas.length > 0 && respuestas[0] == PackageManager.PERMISSION_GRANTED)
            iniciarGpsAutomatico();
        else {
            estadoGps = SIN_GPS;
            if (paso == 0) mostrarInicio();
        }
    }

    private void mensajeAsistente(String mensaje) {
        LinearLayout tarjetaMensaje = new LinearLayout(this);
        tarjetaMensaje.setGravity(Gravity.CENTER_VERTICAL);
        tarjetaMensaje.setPadding(dp(9), dp(8), dp(13), dp(8));
        tarjetaMensaje.setBackground(fondo(DURAZNO, 0, 18));
        tarjetaMensaje.setMinimumHeight(dp(90));
        contenido.addView(tarjetaMensaje, new LinearLayout.LayoutParams(-1,-2));
        ImageView retrato = new ImageView(this);
        retrato.setImageResource(R.drawable.kusi_editorial);
        retrato.setScaleType(ImageView.ScaleType.FIT_CENTER);
        retrato.setContentDescription("Kusi, el cuy ayudante");
        tarjetaMensaje.addView(retrato, new LinearLayout.LayoutParams(
                dp(78),dp(83)));
        LinearLayout palabras = new LinearLayout(this);
        palabras.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, -2, 1);
        wp.leftMargin = dp(10);
        tarjetaMensaje.addView(palabras, wp);
        TextView hablar = texto(quechua ? "KUSI · AYUDA EN TEXTO" :
                "KUSI · TOCA PARA ESCUCHAR  🔊", 11, TERRACOTA, true);
        palabras.addView(hablar);
        tarjetaMensaje.setOnClickListener(v -> {
            if(quechua)aviso("La ayuda en quechua se muestra en texto por ahora");
            else {
                String evento=paso==1?"cultivo":
                        paso==3?eventoResultado():"inicio";
                voz.repetir(evento,false);
            }
        });
        LinearLayout.LayoutParams frase = new LinearLayout.LayoutParams(-1, -2);
        frase.topMargin = dp(5);
        palabras.addView(texto(mensaje, 16, TINTA, true), frase);
        separar(14);
    }

    private void mostrarCultivos() {
        paso=1;
        pantalla("PASO 2 DE 2 · TU SIEMBRA", "¿Qué sembraste antes?", "Toca el dibujo del cultivo anterior.");
        mensajeAsistente(quechua ? "Kay chakrachaw imata mururqanki?\n¿Qué sembraste aquí?" :
                "¿Qué sembraste aquí la vez pasada?");
        voz.ayuda("cultivo", quechua);

        agregarGrupoCultivos("TUBÉRCULOS Y RAÍCES DE HUÁNUCO", new Cultivo[]{
                Cultivo.PAPA,Cultivo.OCA,Cultivo.OLLUCO,Cultivo.MASHUA,
                Cultivo.CAMOTE,Cultivo.YACON,Cultivo.ARRACACHA,Cultivo.YUCA});
        agregarGrupoCultivos("OTROS CULTIVOS DE LA CHACRA", new Cultivo[]{
                Cultivo.QUINUA,Cultivo.MAIZ,Cultivo.CEBADA,Cultivo.HABAS,
                Cultivo.TARWI,Cultivo.ARVEJA,Cultivo.MAIZ_AMARILLO,Cultivo.FRIJOL,
                Cultivo.DESCANSO});
        TextView permanente=boton("Tengo café, cacao o plátano",false);
        agregarBoton(permanente);
        permanente.setOnClickListener(v->ventanaMensaje("Cultivos de varios años",
                "Café, cacao y plátano necesitan otro manejo. Esta guía calcula siembras "
                        + "de temporada. Consulta al técnico agrario de tu zona."));
    }

    private void agregarGrupoCultivos(String etiqueta, Cultivo[] cultivos) {
        TextView grupo=texto(etiqueta,12,TERRACOTA,true);
        grupo.setLetterSpacing(.07f);
        contenido.addView(grupo);
        separar(9);
        GridLayout cuadricula=new GridLayout(this);
        cuadricula.setColumnCount(2);
        contenido.addView(cuadricula);
        int ancho=(getResources().getDisplayMetrics().widthPixels-dp(52))/2;
        for(Cultivo cultivo:cultivos) {
            LinearLayout baldosa=new LinearLayout(this);
            baldosa.setOrientation(LinearLayout.VERTICAL);
            baldosa.setGravity(Gravity.CENTER);
            baldosa.setBackground(new RippleDrawable(ColorStateList.valueOf(0x2280A48D),
                    fondo(Color.WHITE,0xFFE1E9E0,18),null));
            String nombreCultivo=cultivo==Cultivo.DESCANSO?"La dejé descansar":cultivo.nombre;
            baldosa.setContentDescription(nombreCultivo);
            baldosa.setClickable(true);baldosa.setFocusable(true);
            baldosa.addView(imagenCultivo(cultivo),new LinearLayout.LayoutParams(dp(70),dp(70)));
            TextView nombre=texto(nombreCultivo,nombreCultivo.length()>13?15:17,TINTA,true);
            nombre.setGravity(Gravity.CENTER);baldosa.addView(nombre);
            GridLayout.LayoutParams p=new GridLayout.LayoutParams();
            p.width=ancho;p.height=dp(119);p.setMargins(dp(2),dp(2),dp(2),dp(9));
            cuadricula.addView(baldosa,p);
            baldosa.setOnClickListener(v->{
                anterior=cultivo;
                campanias=0;
                paso=3;
                mostrarResultado();
            });
        }
        separar(8);
    }

    private View imagenCultivo(Cultivo cultivo) {
        return new CultivoLamina(this,cultivo);
    }

    private String eventoResultado() {
        if(seleccionada==null || !seleccionada.reciente(LocalDate.now()))return "resultado_viejo";
        return seleccionada.promedio30>0
                && seleccionada.lluvia30>seleccionada.promedio30*1.8
                ? "resultado_lluvia" : "resultado";
    }

    private void mostrarResultado() {
        if (seleccionada == null || anterior == null || altura == 0) { mostrarInicio(); return; }
        Temporada t = seleccionada;
        paso = 3;
        pantalla("TU SIEMBRA · " + provincia.toUpperCase(new Locale("es","PE")),
                "Opciones para tu chacra", altura + " m · " + zonaClimatica(t)
                        + " · después de " + anterior.articulo);
        boolean reciente = t.reciente(LocalDate.now());
        boolean muchaLluvia = reciente && t.promedio30>0 && t.lluvia30>t.promedio30*1.8;
        String pista = !reciente ? "Estos datos de lluvia necesitan actualización."
                : muchaLluvia ? "Llovió bastante. Mira la tierra antes de sembrar."
                : "Mira estas opciones y elige con tu experiencia.";
        mensajeAsistente(pista);
        List<ConsejoTemporada> opciones = new MotorTemporada().recomendar(t, altura, anterior,
                campanias, suelo, prioridad, LocalDate.now());
        TextView principal=texto("MI PRIMERA OPCIÓN",12,TERRACOTA,true);
        principal.setLetterSpacing(.09f);
        contenido.addView(principal);
        separar(8);
        if(!opciones.isEmpty()) tarjetaTemporada(opciones.get(0),1);
        if(opciones.size()>1) {
            separar(6);
            contenido.addView(titulo("Otras posibilidades",23,TINTA));
            separar(10);
            for(int i=1;i<Math.min(3,opciones.size());i++) tarjetaTemporada(opciones.get(i),i+1);
        }
        separar(8);
        tarjetaLluvia(t,reciente);
        TextView como = texto("ⓘ  ¿Por qué salen estas opciones?", 14, VERDE_PROFUNDO, true);
        como.setGravity(Gravity.CENTER);como.setPadding(0,dp(12),0,dp(8));
        contenido.addView(como);como.setOnClickListener(v -> mostrarDatos());
        voz.ayuda(!reciente ? "resultado_viejo" :
                muchaLluvia ? "resultado_lluvia" : "resultado",quechua);
    }

    private void tarjetaLluvia(Temporada t, boolean reciente) {
        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding(dp(17), dp(15), dp(17), dp(16));
        tarjeta.setBackground(fondo(Color.WHITE,0xFFE0E9E2,18));
        contenido.addView(tarjeta,new LinearLayout.LayoutParams(-1,-2));
        TextView tituloLluvia=titulo("☔  Así va la lluvia",21,TINTA);
        tarjeta.addView(tituloLluvia);
        tarjeta.addView(texto("Total de 30 días hasta el " + fechaCorta(t.fechaCorte)
                + " · " + zonaClimatica(t),14,0xFF63776A,false));
        LinearLayout cifras=new LinearLayout(this);
        cifras.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(10);
        tarjeta.addView(cifras,cp);
        cifras.addView(titulo(Math.round(t.lluvia30)+" mm",30,VERDE_PROFUNDO));
        TextView habitual=texto("habitual: "+Math.round(t.promedio30)+" mm",14,0xFF63776A,false);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-2,-2);hp.leftMargin=dp(12);
        cifras.addView(habitual,hp);
        separarEn(tarjeta,10);
        tarjeta.addView(texto("Semana a semana · últimas 6 semanas",13,0xFF63776A,true));
        tarjeta.addView(new LluviaGrafico(this,t),new LinearLayout.LayoutParams(-1,dp(110)));
        tarjeta.addView(texto("● Verde: reciente     ● Beige: habitual",13,0xFF65756A,false));
        if(!reciente) {
            separarEn(tarjeta,7);
            tarjeta.addView(texto("El clima necesita datos más recientes",13,ROJO,true));
        }
        tarjeta.setOnClickListener(v->mostrarAyudaLluvia());
        tarjeta.setContentDescription("Gráfica de lluvia. Toca para escuchar la explicación.");
    }

    private void separarEn(LinearLayout caja, int alto) {
        caja.addView(new View(this),new LinearLayout.LayoutParams(1,dp(alto)));
    }

    private void tarjetaTemporada(ConsejoTemporada r,int puesto) {
        int color = r.puntaje>=70 ? VERDE_PROFUNDO : r.puntaje>=45 ? 0xFF9B6D17 : ROJO;
        String veredicto = r.puntaje>=70 ? "CONVIENE" : r.puntaje>=45 ? "REVISAR" : "EVITAR";
        boolean destacada=puesto==1;
        int base=destacada?(r.puntaje>=70?VERDE_PROFUNDO:r.puntaje>=45?0xFF6A5939:0xFF724743):Color.WHITE;
        int tinta=destacada?Color.WHITE:TINTA;
        LinearLayout card = new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(16),dp(16),dp(15));
        card.setBackground(fondo(base,destacada?0:0xFFE2E9E1,20));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,-2);
        cp.bottomMargin=dp(10);contenido.addView(card,cp);
        LinearLayout fila=new LinearLayout(this);fila.setGravity(Gravity.CENTER_VERTICAL);card.addView(fila);
        View imagen=imagenCultivo(r.cultivo);
        imagen.setBackground(fondo(destacada?0xFFF4F4EA:SALVIA,0,15));
        fila.addView(imagen,new LinearLayout.LayoutParams(dp(destacada?64:55),dp(destacada?64:55)));
        LinearLayout nombres=new LinearLayout(this);nombres.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1);np.leftMargin=dp(12);
        fila.addView(nombres,np);
        nombres.addView(titulo(r.cultivo.nombre,destacada?24:20,tinta));
        TextView puntos=texto(marcas(Math.max(1,Math.min(5,(int)Math.ceil(r.puntaje/20.0)))),
                16,destacada?0xFFD5E9DA:color,true);
        nombres.addView(puntos);
        TextView estado=texto(veredicto,11,destacada?color:color,true);
        estado.setGravity(Gravity.CENTER);estado.setPadding(dp(8),dp(6),dp(8),dp(6));
        estado.setBackground(fondo(destacada?Color.WHITE:
                r.puntaje>=70?0xFFE5F1E7:r.puntaje>=45?0xFFFFF0D4:0xFFFBE4DF,0,9));
        fila.addView(estado);
        if(r.cultivo!=Cultivo.DESCANSO) {
            separarEn(card,12);
            String agua=r.anosAguaCompleta>=10?"La lluvia suele alcanzar"
                    :r.anosAguaCompleta>=5?"La lluvia varía entre años"
                    :"La lluvia suele faltar";
            card.addView(texto("💧  "+agua,15,tinta,true));
            card.addView(texto("Alcanzó en "+r.anosAguaCompleta
                    +" de 15 temporadas analizadas",13,
                    destacada?0xFFD9E8DC:0xFF64776B,false));
            separarEn(card,4);
            String siembra=r.fechaSiembra.equals(LocalDate.now())
                    ? "Para este mes · revisa la tierra antes de sembrar"
                    : "Siembra estimada: "+mes(r.fechaSiembra.getMonthValue())
                    +" "+r.fechaSiembra.getYear();
            card.addView(texto(siembra,14,destacada?0xFFD9E8DC:0xFF64776B,false));
        } else {
            separarEn(card,11);
            card.addView(texto("Deja recuperar la tierra antes de otra siembra.",14,tinta,false));
        }
        TextView abrir=texto("Ver las razones  ⌄",14,destacada?0xFFE0EFE3:VERDE_PROFUNDO,true);
        abrir.setPadding(0,dp(11),0,dp(2));card.addView(abrir);
        LinearLayout detalle=new LinearLayout(this);detalle.setOrientation(LinearLayout.VERTICAL);
        detalle.setVisibility(View.GONE);card.addView(detalle);
        for(String razon:r.razones) {
            TextView linea=texto("• "+razon,14,tinta,false);
            linea.setPadding(0,dp(7),0,0);detalle.addView(linea);
        }
        if(r.cultivo!=Cultivo.DESCANSO)detalle.addView(texto("❄ Riesgo de helada: "
                +Math.round(r.riesgoHelada*100)+"%",14,tinta,false));
        TextView escuchar=boton(quechua?"🔊 Escuchar en español":"🔊 Escuchar",false);
        LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(48));ep.topMargin=dp(12);
        detalle.addView(escuchar,ep);
        escuchar.setOnClickListener(v -> voz.decir(r.cultivo.nombre+". "+veredicto+". "
                +String.join(". ",r.razones)));
        abrir.setOnClickListener(v->{boolean visible=detalle.getVisibility()==View.VISIBLE;
            detalle.setVisibility(visible?View.GONE:View.VISIBLE);
            abrir.setText(visible?"Ver las razones  ⌄":"Ocultar razones  ⌃");});
    }

    private void mostrarAyudaLluvia() {
        Dialog d=new Dialog(this);
        LinearLayout caja=new LinearLayout(this);caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(dp(18),dp(16),dp(18),dp(18));caja.setBackground(fondo(CREMA,0,20));
        LinearLayout fila=new LinearLayout(this);fila.setGravity(Gravity.CENTER_VERTICAL);caja.addView(fila);
        ImageView cuy=new ImageView(this);cuy.setImageResource(R.drawable.kusi_editorial);
        cuy.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fila.addView(cuy,new LinearLayout.LayoutParams(dp(82),dp(94)));
        TextView ayuda=texto("Verde: lluvia reciente. Beige: lo habitual. Si el verde es alto, mira si la tierra está lista.",
                16,TINTA,false);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,-2,1);ap.leftMargin=dp(9);
        fila.addView(ayuda,ap);
        TextView listo=boton("Entendido",true);caja.addView(listo);
        listo.setOnClickListener(v->d.dismiss());
        mostrarVentana(d,caja);
        if(!quechua)voz.repetir("grafico",false);
    }

    private void mostrarDatos() {
        paso=4;
        pantalla("EL CLIMA · TU ZONA", "Así va la lluvia", "Lluvia reciente y lo habitual en tu zona.");
        Temporada t=seleccionada;
        if(t!=null) {
            tarjetaLluvia(t,t.reciente(LocalDate.now()));
            separar(12);
            dato("📍  Tu zona", "Provincia de " + provincia + " · " + zonaClimatica(t));
            dato("🗓  Último día disponible", fechaCorta(t.fechaCorte)
                    + (t.reciente(LocalDate.now()) ? " · reciente" : " · necesita actualización"));
            dato("🌿  Cálculo", "15 temporadas de lluvia y frío. El agua del suelo es estimada.");
        } else {
            dato("📍  Elige tu chacra", "Así verás la lluvia de la zona más cercana.");
        }
        dato("🛰  Fuente", "NASA POWER · con señal se renueva la lluvia; sin señal se usa la última guardada.");
        if(alturaMapa) dato("⛰  Altura aproximada", "Calculada con relieve offline de "
                + "Mapzen y datos SRTM/GMTED del USGS. Puedes corregirla en Mi chacra.");
        TextView volver=boton("Volver",false);agregarBoton(volver);
        volver.setOnClickListener(v->{if(anterior!=null&&seleccionada!=null)mostrarResultado();else mostrarInicio();});
    }
    private void dato(String titulo,String descripcion) {
        LinearLayout caja=new LinearLayout(this);caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(dp(17),dp(13),dp(17),dp(14));
        caja.setBackground(fondo(Color.WHITE,0xFFE3EAE2,17));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);
        contenido.addView(caja,p);
        TextView etiqueta=texto(titulo.toUpperCase(new Locale("es","PE")),12,TERRACOTA,true);
        etiqueta.setLetterSpacing(.06f);
        caja.addView(etiqueta);
        separarEn(caja,6);
        caja.addView(texto(descripcion,17,TINTA,true));
    }

    private String marcas(int llenos) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 5; i++) s.append(i < llenos ? "●" : "○");
        return s.toString();
    }

    private String mes(int número) {
        return new String[]{"enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"}[número - 1];
    }

    private String fechaCorta(LocalDate fecha) {
        return fecha.getDayOfMonth() + " " + mes(fecha.getMonthValue()).substring(0,3)
                + " " + fecha.getYear();
    }

    private String zonaClimatica(Temporada temporada) {
        return temporada.distrito.replace("Zona climática", "zona");
    }

    private void importarPack() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, ELEGIR_TEMPORADA);
    }

    @Override protected void onActivityResult(int pedido, int resultado, Intent datos) {
        super.onActivityResult(pedido, resultado, datos);
        if (resultado != RESULT_OK || datos == null) return;
        try {
            byte[] prefijo=new byte[20];
            try(java.io.InputStream in=getContentResolver().openInputStream(datos.getData())) {
                if(in==null)throw new java.io.IOException("No se pudo leer el archivo");
                int leidos=in.read(prefijo);
                if(leidos<10)throw new java.io.IOException("Archivo vacío");
            }
            String cabecera=new String(prefijo,java.nio.charset.StandardCharsets.US_ASCII);
            if(cabecera.startsWith("YACHAY-TEMPORADA/1")) {
                Temporada recibida=temporadasRepositorio.importar(datos.getData());
                temporadas=temporadasRepositorio.cargar();
                if(seleccionada!=null&&seleccionada.distrito.equals(recibida.distrito))
                    seleccionada=recibida;
                aviso("Datos nuevos de "+recibida.distrito+" recibidos");
            } else if(cabecera.startsWith("YACHAY-PACK/1")) {
                Pack recibido=repositorio.importar(datos.getData());packs=repositorio.cargar();
                aviso("Datos de rotación de "+recibido.distrito+" recibidos");
            } else throw new java.io.IOException("No es un archivo Yachay Allpa");
            mostrarInicio();
        } catch(Exception e) { aviso("Datos rechazados: "+e.getMessage()); }
    }

    private void aviso(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    @Override public void onBackPressed() {
        if(paso==4){if(anterior!=null&&seleccionada!=null)mostrarResultado();else mostrarInicio();}
        else if(paso==5){if(anterior!=null&&seleccionada!=null)mostrarResultado();else mostrarInicio();}
        else if (paso == 3) { paso = 1; mostrarCultivos(); }
        else if (paso == 1) { paso = 0; mostrarInicio(); }
        else super.onBackPressed();
    }

    private LinearLayout barraInferiorNavegacion() {
        LinearLayout barra = new LinearLayout(this);
        barra.setGravity(Gravity.CENTER);
        barra.setPadding(dp(15),dp(4),dp(15),dp(5));
        barra.setBackground(fondo(Color.WHITE,0xFFE8ECE6,0));
        String[] nombres={"Inicio","Mi chacra","Clima"};
        String[] iconos={"⌂","♧","☔"};
        for(int i=0;i<3;i++) {
            int n=i;
            boolean activo=i==0&&(paso==0||paso==1||paso==3)
                    ||i==1&&paso==5||i==2&&paso==4;
            LinearLayout item=new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            if(activo)item.setBackground(fondo(SALVIA,0,15));
            TextView icono=texto(iconos[i],21,activo?VERDE_PROFUNDO:0xFF809186,true);
            icono.setGravity(Gravity.CENTER);
            item.addView(icono);
            TextView nombre=texto(nombres[i],12,activo?VERDE_PROFUNDO:0xFF708178,activo);
            nombre.setGravity(Gravity.CENTER);
            item.addView(nombre);
            LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-1,1);
            ip.leftMargin=dp(2);ip.rightMargin=dp(2);
            barra.addView(item,ip);
            item.setContentDescription(nombres[i]);
            item.setClickable(true);
            item.setOnClickListener(v -> {
                if(n==0) mostrarInicio();
                else if(n==1) mostrarChacra();
                else mostrarDatos();
            });
        }
        return barra;
    }

    private void elegirIdioma() {
        if(idiomaElegido)return;
        idiomaElegido=true;
        Dialog d=new Dialog(this);
        LinearLayout caja=new LinearLayout(this);caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(dp(20),dp(18),dp(20),dp(20));caja.setBackground(fondo(CREMA,0,22));
        LinearLayout bienvenida=new LinearLayout(this);
        bienvenida.setGravity(Gravity.CENTER_VERTICAL);
        bienvenida.setBackground(fondo(DURAZNO,0,18));
        bienvenida.setPadding(dp(14),dp(8),dp(7),dp(8));
        caja.addView(bienvenida,new LinearLayout.LayoutParams(-1,dp(125)));
        LinearLayout saludo=new LinearLayout(this);saludo.setOrientation(LinearLayout.VERTICAL);
        bienvenida.addView(saludo,new LinearLayout.LayoutParams(0,-2,1));
        saludo.addView(texto("HOLA, SOY KUSI",12,TERRACOTA,true));
        saludo.addView(titulo("Te acompaño en la chacra",23,TINTA));
        ImageView cuy=new ImageView(this);cuy.setImageResource(R.drawable.kusi_editorial);
        cuy.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bienvenida.addView(cuy,new LinearLayout.LayoutParams(dp(104),dp(118)));
        separarEn(caja,15);
        caja.addView(texto("¿En qué idioma quieres recibir la ayuda?",17,TINTA,true));
        separarEn(caja,14);
        TextView es=boton("Español",true);caja.addView(es,new LinearLayout.LayoutParams(-1,dp(52)));
        separarEn(caja,8);
        TextView qu=boton("Quechua de Huánuco · texto",false);
        caja.addView(qu,new LinearLayout.LayoutParams(-1,dp(52)));
        separarEn(caja,8);
        caja.addView(texto("La voz en quechua aún necesita revisión local.",13,0xFF68786D,false));
        es.setOnClickListener(v->{quechua=false;d.dismiss();voz.ayuda(
                seleccionada==null?"inicio":altura==0?"altura_pendiente":"inicio_listo",false);
                iniciarGpsAutomatico();});
        qu.setOnClickListener(v->{quechua=true;d.dismiss();voz.ayuda("inicio",true);
                iniciarGpsAutomatico();});
        d.setCancelable(false);
        mostrarVentana(d,caja);
    }

    private void mostrarChacra() {
        paso=5;
        pantalla("TU TERRENO · TUS DATOS", "Mi chacra", "Esto es lo que usamos para aconsejarte.");
        dato("📍  Lugar", seleccionada==null ? "Falta ubicar tu chacra"
                : "Provincia de " + provincia + " · "
                + (altura==0 ? "falta la altura" : altura + " m"
                + (alturaMapa ? " (altura aproximada)" : "")));
        dato("🌱  Última siembra", anterior==null ? "Aún no elegida"
                : anterior==Cultivo.DESCANSO ? "La tierra descansó"
                : anterior.nombre);
        dato("♻  Descanso de la tierra", anterior==Cultivo.DESCANSO
                ? "La dejaste sin cultivar la vez pasada"
                : campanias==0 ? "No indicado · es opcional"
                : campanias>=4 ? "No descansó en los últimos 3 años"
                : "Sí descansó en los últimos 3 años");
        String tipoSuelo=suelo==MotorTemporada.Suelo.SE_SECA_RAPIDO?"Se seca rápido"
                : suelo==MotorTemporada.Suelo.RETIENE_AGUA?"Retiene el agua":"No lo sé";
        dato("💧  Mi suelo", tipoSuelo + " · puedes cambiarlo si lo conoces");
        String objetivo=prioridad==MotorTemporada.Prioridad.CUIDAR_AGUA?"Cuidar el agua"
                : prioridad==MotorTemporada.Prioridad.CUIDAR_SUELO?"Cuidar la tierra"
                : prioridad==MotorTemporada.Prioridad.COSECHAR_PRONTO?"Cosechar pronto":"Equilibrar todo";
        dato("🎯  Lo que prefiero", objetivo);
        TextView preferencias=boton("Cambiar suelo o preferencia",false);
        agregarBoton(preferencias);
        preferencias.setOnClickListener(v->editarPreferencias());
        if(anterior!=Cultivo.DESCANSO) {
            TextView descanso=boton("¿Descansó la tierra? · opcional",false);
            agregarBoton(descanso);
            descanso.setOnClickListener(v->elegirDescanso());
        }
        TextView cambiar=boton("Corregir ubicación o altura",false);
        agregarBoton(cambiar);
        cambiar.setOnClickListener(v->{if(seleccionada==null)elegirDistrito();
                else elegirCorreccion();});
        TextView nuevo=boton("Cambiar la última siembra",false);
        agregarBoton(nuevo);
        nuevo.setOnClickListener(v->mostrarCultivos());
        if(seleccionada!=null && anterior!=null && altura>0) {
            TextView consejo=boton("Ver mis opciones  →",true);
            agregarBoton(consejo);
            consejo.setOnClickListener(v->mostrarResultado());
        }
    }

    private void editarPreferencias() {
        ventanaOpciones("Cuando deja de llover, mi tierra…",
                new String[]{"No lo sé", "Se seca rápido", "Retiene el agua"},
                suelo.ordinal(), opcion -> {
                    suelo=MotorTemporada.Suelo.values()[opcion];
                    elegirPrioridad();
                });
    }

    private void elegirDescanso() {
        ventanaOpciones("¿La tierra descansó en los últimos 3 años?",
                new String[]{"No lo sé", "Sí, descansó", "No, no descansó"},
                campanias==0?0:campanias>=4?2:1, opcion -> {
                    campanias=opcion==0?0:opcion==1?1:4;
                    mostrarChacra();
                });
    }

    private void elegirPrioridad() {
        ventanaOpciones("¿Qué quieres cuidar más?",
                new String[]{"Equilibrar todo", "Cuidar el agua",
                        "Cuidar la tierra", "Cosechar pronto"}, prioridad.ordinal(), opcion -> {
                    prioridad=MotorTemporada.Prioridad.values()[opcion];
                    mostrarChacra();
                });
    }

    @Override protected void onDestroy() {
        cerrada = true;
        unregisterReceiver(lluviaNueva);
        detenerGps();
        voz.cerrar();
        super.onDestroy();
    }
}
