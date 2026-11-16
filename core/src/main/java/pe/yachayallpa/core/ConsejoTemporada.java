package pe.yachayallpa.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Resultado de escenarios históricos; nunca equivale a una predicción de cosecha. */
public final class ConsejoTemporada {
    public final Cultivo cultivo;
    public final LocalDate fechaSiembra;
    public final int puntaje, anosConEstres, anosAguaCompleta;
    public final double coberturaAgua, riesgoHelada, deficitMedioMm;
    /**
     * Cosecha esperada respecto a la posible (0..1), promedio de las 15 campañas:
     * cada año 1 − Ky·(1 − agua recibida / agua pedida), FAO-33. Es lo que decide
     * si el agua alcanza; la cobertura sola no dice cuánto se pierde.
     */
    public final double rendimientoEsperado;
    /** Razones sin idioma: clave y cifra. La app las escribe en castellano o quechua. */
    public final List<Motivo> razones;

    public ConsejoTemporada(Cultivo cultivo, LocalDate fechaSiembra, int puntaje,
                            int anosConEstres, int anosAguaCompleta,
                            double coberturaAgua, double riesgoHelada,
                            double deficitMedioMm, List<Motivo> razones) {
        this(cultivo, fechaSiembra, puntaje, anosConEstres, anosAguaCompleta, coberturaAgua,
                riesgoHelada, deficitMedioMm,
                Math.max(0, 1 - cultivo.ky() * (1 - coberturaAgua)), razones);
    }

    public ConsejoTemporada(Cultivo cultivo, LocalDate fechaSiembra, int puntaje,
                            int anosConEstres, int anosAguaCompleta,
                            double coberturaAgua, double riesgoHelada,
                            double deficitMedioMm, double rendimientoEsperado,
                            List<Motivo> razones) {
        this.rendimientoEsperado = rendimientoEsperado;
        this.cultivo = cultivo;
        this.fechaSiembra = fechaSiembra;
        this.puntaje = puntaje;
        this.anosConEstres = anosConEstres;
        this.anosAguaCompleta = anosAguaCompleta;
        this.coberturaAgua = coberturaAgua;
        this.riesgoHelada = riesgoHelada;
        this.deficitMedioMm = deficitMedioMm;
        this.razones = Collections.unmodifiableList(new ArrayList<>(razones));
    }
}
