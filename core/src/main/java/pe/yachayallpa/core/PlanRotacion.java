package pe.yachayallpa.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * La rotación recomendada para las próximas campañas de una chacra.
 *
 * La primera campaña es la recomendación de ahora (con su pronóstico, si lo hay).
 * Cada campaña siguiente, un año después en la misma época, se calcula con el
 * mismo motor y la misma regla de decisión, tomando como cultivo anterior el del
 * paso previo. Para esas campañas futuras no hay pronóstico: solo la lluvia normal
 * de las 15 campañas NASA, así que son una guía, no una predicción.
 */
public final class PlanRotacion {
    private PlanRotacion() { }

    /** Una campaña del plan: el año, el cultivo elegido con su consejo y la acción. */
    public static final class Campana {
        public final int anio;
        public final ConsejoTemporada consejo;
        public final Decision.Accion accion;

        Campana(int anio, ConsejoTemporada consejo, Decision.Accion accion) {
            this.anio = anio;
            this.consejo = consejo;
            this.accion = accion;
        }
    }

    public static List<Campana> armar(Temporada t, int altitud, Cultivo anterior, int seguidas,
                                      MotorTemporada.Suelo suelo, MotorTemporada.Prioridad prioridad,
                                      LocalDate hoy, Decision ahora, int campanas) {
        if (t == null || anterior == null || ahora == null || hoy == null || campanas < 1)
            throw new IllegalArgumentException("Plan incompleto");
        List<Campana> plan = new ArrayList<>();
        plan.add(new Campana(hoy.getYear(), ahora.consejo, ahora.accion));
        MotorTemporada motor = new MotorTemporada();
        Cultivo previo = ahora.consejo.cultivo;
        int sinDescanso = previo == Cultivo.DESCANSO ? 0 : seguidas + 1;
        for (int k = 1; k < campanas; k++) {
            LocalDate fecha = hoy.plusYears(k);
            List<ConsejoTemporada> opciones = motor.recomendar(t, altitud, previo, sinDescanso,
                    suelo, prioridad, fecha);
            Decision d = Decision.tomar(opciones, null, altitud, fecha);
            plan.add(new Campana(fecha.getYear(), d.consejo, d.accion));
            previo = d.consejo.cultivo;
            sinDescanso = previo == Cultivo.DESCANSO ? 0 : sinDescanso + 1;
        }
        return Collections.unmodifiableList(plan);
    }
}
