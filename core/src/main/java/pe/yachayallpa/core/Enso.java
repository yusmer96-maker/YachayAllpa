package pe.yachayallpa.core;

import java.time.LocalDate;

/**
 * Estado de El Nino / La Nina (ENSO) en un momento dado, segun NOAA.
 *
 * NO es un pronostico del tiempo de una chacra. Es la fase del oceano Pacifico
 * (El Nino, La Nina o Neutral) medida por el indice ONI, que inclina la campana
 * hacia mas seca o mas humeda de lo normal. La relacion con la sierra de Huanuco
 * es una TENDENCIA ESTACIONAL moderada, no una certeza: en El Nino la sierra
 * central del Peru suele llover menos; en La Nina, mas. La decision final es del
 * agricultor y el agronomo.
 *
 * Es un dato global (no lleva coordenadas): el mismo numero vale para todo el
 * pais, asi que pedirlo no revela donde esta la chacra.
 */
public final class Enso {
    public enum Fase { EL_NINO, LA_NINA, NEUTRAL }

    public final Fase fase;
    public final double oni;          // anomalia ONI, grados C
    public final String temporada;    // trimestre del ONI, p.ej. "ASO 2026"
    public final String alerta;       // "Aviso de El Nino", "Vigilancia", "" ...
    public final int tendencia;       // -1 bajando, 0 estable, +1 subiendo
    public final LocalDate fecha;     // "al" de los datos
    public final String fuente;

    public Enso(Fase fase, double oni, String temporada, String alerta,
                int tendencia, LocalDate fecha, String fuente) {
        this.fase = fase;
        this.oni = oni;
        this.temporada = temporada == null ? "" : temporada;
        this.alerta = alerta == null ? "" : alerta;
        this.tendencia = Integer.signum(tendencia);
        this.fecha = fecha;
        this.fuente = fuente == null ? "" : fuente;
    }

    /**
     * Intensidad por el valor absoluto del ONI (umbrales estandar de NOAA), como
     * numero y no como palabra: 0 neutral, 1 debil, 2 moderado, 3 fuerte,
     * 4 muy fuerte. La palabra la pone la app en el idioma elegido.
     */
    public int nivelIntensidad() {
        double a = Math.abs(oni);
        if (a < 0.5) return 0;
        if (a < 1.0) return 1;
        if (a < 1.5) return 2;
        if (a < 2.0) return 3;
        return 4;
    }

    public boolean esNeutral() { return fase == Fase.NEUTRAL || Math.abs(oni) < 0.5; }
}
