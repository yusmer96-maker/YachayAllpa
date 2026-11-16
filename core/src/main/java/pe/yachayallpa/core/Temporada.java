package pe.yachayallpa.core;

import java.time.LocalDate;

/** Datos diarios recientes y escenarios históricos de diez días, preparados fuera del teléfono. */
public final class Temporada {
    public static final int PRIMER_ANO = 2011;
    public static final int ULTIMO_ANO = 2025;
    public static final int ESCENARIOS = ULTIMO_ANO - PRIMER_ANO + 1;
    public static final int BLOQUES = 36;
    public final int version;
    public final String distrito, fuente, productoLluvia;
    public final LocalDate fechaCorte;
    public final double lat, lon, elevacionCelda;
    public final double lluvia7, lluvia15, lluvia30, promedio30;
    private final double[] lluviaHist, etoHist, tminHist, desvHist;
    /** Temperatura media por bloque (formato 2). Null en packs del formato 1. */
    private final double[] tmedHist;
    private final double[] semanasActual, semanasHabitual;

    public Temporada(int version, String distrito, LocalDate fechaCorte, double lat, double lon,
                     double elevacionCelda, String productoLluvia, String fuente,
                     double lluvia7, double lluvia15, double lluvia30, double promedio30,
                     double[] lluviaHist, double[] etoHist, double[] tminHist, double[] desvHist,
                     double[] semanasActual, double[] semanasHabitual) {
        this(version, distrito, fechaCorte, lat, lon, elevacionCelda, productoLluvia, fuente,
                lluvia7, lluvia15, lluvia30, promedio30, lluviaHist, etoHist, tminHist, desvHist,
                null, semanasActual, semanasHabitual);
    }

    public Temporada(int version, String distrito, LocalDate fechaCorte, double lat, double lon,
                     double elevacionCelda, String productoLluvia, String fuente,
                     double lluvia7, double lluvia15, double lluvia30, double promedio30,
                     double[] lluviaHist, double[] etoHist, double[] tminHist, double[] desvHist,
                     double[] tmedHist, double[] semanasActual, double[] semanasHabitual) {
        if (version < 20200101 || distrito == null || distrito.isBlank() || fechaCorte == null
                || productoLluvia == null || productoLluvia.isBlank() || fuente == null
                || !Double.isFinite(lat) || !Double.isFinite(lon)
                || !Double.isFinite(elevacionCelda) || elevacionCelda < 0
                || !Double.isFinite(lluvia7) || !Double.isFinite(lluvia15)
                || !Double.isFinite(lluvia30) || !Double.isFinite(promedio30)
                || lluvia7 < 0 || lluvia15 < 0 || lluvia30 < 0 || promedio30 < 0) {
            throw new IllegalArgumentException("Metadatos de temporada inválidos");
        }
        this.version = version;
        this.distrito = distrito;
        this.fechaCorte = fechaCorte;
        this.lat = lat;
        this.lon = lon;
        this.elevacionCelda = elevacionCelda;
        this.productoLluvia = productoLluvia;
        this.fuente = fuente;
        this.lluvia7 = lluvia7;
        this.lluvia15 = lluvia15;
        this.lluvia30 = lluvia30;
        this.promedio30 = promedio30;
        this.lluviaHist = serie(lluviaHist, ESCENARIOS * BLOQUES, true);
        this.etoHist = serie(etoHist, ESCENARIOS * BLOQUES, true);
        this.tminHist = serie(tminHist, ESCENARIOS * BLOQUES, false);
        this.desvHist = serie(desvHist, ESCENARIOS * BLOQUES, true);
        this.tmedHist = tmedHist == null ? null : serie(tmedHist, ESCENARIOS * BLOQUES, false);
        this.semanasActual = serie(semanasActual, 6, true);
        this.semanasHabitual = serie(semanasHabitual, 6, true);
    }

    private static double[] serie(double[] valores, int esperado, boolean positivo) {
        if (valores == null || valores.length != esperado) throw new IllegalArgumentException("Serie incompleta");
        for (double v : valores) {
            if (!Double.isFinite(v) || (positivo && v < 0)) throw new IllegalArgumentException("Dato climático inválido");
        }
        return valores.clone();
    }

    private int indice(int escenario, int bloque) {
        if (escenario < 0 || escenario >= ESCENARIOS || bloque < 0 || bloque >= BLOQUES)
            throw new IllegalArgumentException("Escenario o bloque fuera de rango");
        return escenario * BLOQUES + bloque;
    }

    public double lluvia(int escenario, int bloque) { return lluviaHist[indice(escenario, bloque)]; }
    public double eto(int escenario, int bloque) { return etoHist[indice(escenario, bloque)]; }
    public double tmin(int escenario, int bloque) { return tminHist[indice(escenario, bloque)]; }
    public double desviacion(int escenario, int bloque) { return desvHist[indice(escenario, bloque)]; }
    /** Media del bloque en la celda; NaN si el pack es del formato 1. */
    public double tmedia(int escenario, int bloque) {
        return tmedHist == null ? Double.NaN : tmedHist[indice(escenario, bloque)];
    }
    public boolean tieneTmedia() { return tmedHist != null; }
    public double[] tmediasHistoricas() { return tmedHist == null ? null : tmedHist.clone(); }
    public double[] lluviasHistoricas() { return lluviaHist.clone(); }
    public double[] etosHistoricos() { return etoHist.clone(); }
    public double[] tminsHistoricas() { return tminHist.clone(); }
    public double[] desviacionesHistoricas() { return desvHist.clone(); }
    public double[] semanasActuales() { return semanasActual.clone(); }
    public double[] semanasHabituales() { return semanasHabitual.clone(); }

    /** Septiembre es el primer mes de la temporada agrícola. */
    public static int bloque(LocalDate fecha) {
        int mes = Math.floorMod(fecha.getMonthValue() - 9, 12);
        return mes * 3 + Math.min(2, (fecha.getDayOfMonth() - 1) / 10);
    }

    /** Producto de la lluvia reciente cuando viene medida por satélite (VPS). */
    public static final String PRODUCTO_IMERG = "GPM IMERG Late";

    /**
     * La misma zona con la lluvia reciente medida por NASA GPM IMERG hasta `hasta`
     * (normal de la misma fuente). La historia de 15 campañas no cambia: solo lo
     * reciente, que así llega hasta ayer en lugar de al corte MERRA-2 de hace 1-2 meses.
     */
    public Temporada conReciente(LocalDate hasta, String fuente, double l7, double l15, double l30,
                                 double normal30, double[] semanas, double[] habituales) {
        return new Temporada(version, distrito, hasta, lat, lon, elevacionCelda, PRODUCTO_IMERG, fuente,
                l7, l15, l30, normal30, lluviaHist, etoHist, tminHist, desvHist, tmedHist,
                semanas, habituales);
    }

    public boolean imerg() { return PRODUCTO_IMERG.equals(productoLluvia); }

    public boolean reciente(LocalDate hoy) {
        long dias = java.time.temporal.ChronoUnit.DAYS.between(fechaCorte, hoy);
        return dias >= 0 && dias <= 14;
    }
}
