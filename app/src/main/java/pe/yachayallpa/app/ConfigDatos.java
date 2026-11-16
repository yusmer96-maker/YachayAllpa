package pe.yachayallpa.app;

/**
 * Lo unico que la app pide por red, aparte del CSV de lluvia de NASA: los
 * indices que el VPS calcula a diario (humedad de raiz en percentil, SPI y
 * tendencias) y publica como un JSON pequeno por celda.
 *
 * No hay servidor de voz ni llaves de terceros en el APK: Kusi habla con el
 * lector del propio telefono (ver `Voz`). Si `BASE_DATOS` queda vacio, la app
 * no intenta ninguna descarga de indices y sigue funcionando con el `.season`
 * firmado que viaja dentro del APK.
 */
public final class ConfigDatos {
    private ConfigDatos() {}

    /** Carpeta HTTPS con zona_01.json ... zona_23.json. Vacio = sin indices. */
    public static final String BASE_DATOS = "https://kusi.mipatitaapp.com/datos/";

    /** Tiempo maximo de espera por la red, en milisegundos. */
    public static final int ESPERA_MS = 9000;
}
