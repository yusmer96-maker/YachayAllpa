package pe.yachayallpa.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Tendencia de la campana para una zona: junta el estado de El Nino / La Nina
 * (ENSO) con la lluvia observada reciente y devuelve un riesgo de sequia con sus
 * razones a la vista.
 *
 * QUE ES Y QUE NO ES. Es una TENDENCIA ESTACIONAL probabilistica, no un
 * pronostico del tiempo. La relacion entre El Nino y la lluvia de la sierra
 * central del Peru es moderada y cambia por microclima (valle, puna, ceja de
 * selva se comportan distinto). Por eso el resultado se muestra con sus factores
 * y con el aviso de confirmar con el agricultor y el agronomo. El motor de
 * siembra sigue decidiendo por la historia de 15 anos; esto solo anade el
 * contexto de "esta campana pinta mas seca o mas humeda de lo normal".
 */
public final class PronosticoCampana {
    public enum Riesgo { BAJO, MEDIO, ALTO }

    public final Riesgo riesgo;
    public final double indicador;      // 0..1, para el medidor
    /** Clave del rumbo de la campana: "rumbo_seco", "rumbo_humedo", "rumbo_neutral". */
    public final String rumbo;
    /** Claves de los factores, sin idioma; la app las escribe. */
    public final List<Motivo> factores;
    public final Enso enso;

    private PronosticoCampana(Riesgo riesgo, double indicador, String rumbo,
                              List<Motivo> factores, Enso enso) {
        this.riesgo = riesgo;
        this.indicador = indicador;
        this.rumbo = rumbo;
        this.factores = java.util.Collections.unmodifiableList(factores);
        this.enso = enso;
    }

    /** Peso del ENSO en la escala de riesgo de sequia para la sierra central. */
    private static int desplazeEnso(Enso e) {
        if (e == null || e.esNeutral()) return 0;
        int signo = (e.fase == Enso.Fase.EL_NINO) ? 1 : -1; // El Nino seca, La Nina moja
        double a = Math.abs(e.oni);
        int magnitud;
        if (a < 1.0) magnitud = 10;
        else if (a < 1.5) magnitud = 18;
        else if (a < 2.0) magnitud = 26;
        else magnitud = 34;
        return signo * magnitud;
    }

    public static PronosticoCampana evaluar(Enso enso, double lluvia30, double promedio30) {
        List<Motivo> factores = new ArrayList<>();
        int base = 40;

        int ensoShift = desplazeEnso(enso);
        boolean conSenal = enso != null && !enso.esNeutral();
        if (conSenal) factores.add(new Motivo(
                enso.fase == Enso.Fase.EL_NINO ? "enso_seco" : "enso_humedo"));
        else factores.add(new Motivo("enso_neutral"));

        double ref = Math.max(promedio30, 1.0);
        double r = lluvia30 / ref;
        int lluviaShift;
        if (r >= 1.5) { lluviaShift = -18; factores.add(new Motivo("mes_mucho_mas")); }
        else if (r >= 1.2) { lluviaShift = -9; factores.add(new Motivo("mes_algo_mas")); }
        else if (r >= 0.8) { lluviaShift = 0; factores.add(new Motivo("mes_normal")); }
        else if (r >= 0.5) { lluviaShift = 10; factores.add(new Motivo("mes_menos")); }
        else { lluviaShift = 18; factores.add(new Motivo("mes_mucho_menos")); }

        int puntaje = Math.max(5, Math.min(95, base + ensoShift + lluviaShift));

        Riesgo riesgo = puntaje >= 60 ? Riesgo.ALTO : (puntaje <= 34 ? Riesgo.BAJO : Riesgo.MEDIO);
        String rumbo = conSenal
                ? (enso.fase == Enso.Fase.EL_NINO ? "rumbo_seco" : "rumbo_humedo")
                : "rumbo_neutral";

        return new PronosticoCampana(riesgo, puntaje / 100.0, rumbo, factores, enso);
    }
}
