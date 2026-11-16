package pe.yachayallpa.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * La rotación recomendada para las próximas campañas de una chacra.
 *
 * La primera campaña es la recomendación de ahora (con su pronóstico, si lo hay).
 * Cada campaña siguiente, un año después en la misma época, se calcula con el
 * mismo motor y la misma regla de decisión, tomando como cultivo anterior el del
 * paso previo. Para esas campañas futuras no hay pronóstico: solo la lluvia normal
 * de las 15 campañas NASA, así que son una guía, no una predicción.
 *
 * Un plan es una rotación, no la lista de lo que más rinde: recorre los pasos de la
 * rueda andina sin volver a uno ya usado hasta completar la vuelta, y cambia de
 * familia botánica entre campañas seguidas. Sin esa regla el plan se queda yendo y
 * viniendo entre los dos pasos que más rinden (quinua, cebada, quinua, cebada), que
 * es justo lo que una rotación evita. Cuando no quedan pasos disponibles a esa
 * altura, empieza otra vuelta.
 */
public final class PlanRotacion {
    private PlanRotacion() { }

    /**
     * Las tres estrategias que se comparan, como pide el reto: la que el agricultor ya
     * sigue y dos alternativas. Las tres se calculan con el mismo motor y los mismos
     * datos, así que sus cifras son comparables entre sí.
     */
    public enum Estrategia {
        /** Seguir como hasta ahora: el mismo cultivo campaña tras campaña. */
        ACTUAL,
        /** La rueda andina: cabecera, segunda, cereal, leguminosa, descanso. */
        ROTACION,
        /** La rueda andina dando prioridad al agua: menos demanda, ciclo más corto. */
        AGUA
    }

    /** Un plan completo con las cifras que permiten compararlo con los otros. */
    public static final class Plan {
        public final Estrategia estrategia;
        public final List<Campana> campanas;
        /** Media de la cosecha esperada (0..1) de las campañas que sí se siembran. */
        public final double cosechaMedia;
        /** Agua que piden en total los ciclos del plan y la que falta, en mm. */
        public final double aguaPedida, aguaFalta;
        /** Señales agronómicas del plan, para decir qué gana y qué arriesga. */
        public final boolean repiteFamilia, conLeguminosa, conDescanso;
        /**
         * El cultivo que el plan querría sembrar pero no se da a esta altura, o null.
         * Pasa en "seguir igual" cuando la chacra quedó fuera del rango del cultivo:
         * el plan sale todo en descanso y hay que decir por qué.
         */
        public final Cultivo fueraDeRango;

        Plan(Estrategia estrategia, List<Campana> campanas) {
            this(estrategia, campanas, null);
        }

        Plan(Estrategia estrategia, List<Campana> campanas, Cultivo fueraDeRango) {
            this.fueraDeRango = fueraDeRango;
            this.estrategia = estrategia;
            this.campanas = Collections.unmodifiableList(campanas);
            double suma = 0, pedida = 0, falta = 0;
            int siembras = 0;
            boolean repite = false, legumbre = false, descanso = false;
            for (int i = 0; i < campanas.size(); i++) {
                Cultivo c = campanas.get(i).consejo.cultivo;
                if (c == Cultivo.DESCANSO) { descanso = true; continue; }
                siembras++;
                suma += campanas.get(i).consejo.rendimientoEsperado;
                pedida += c.aguaCicloMm;
                falta += campanas.get(i).consejo.deficitMedioMm;
                if (c.fijaNitrogeno) legumbre = true;
                if (i > 0 && campanas.get(i - 1).consejo.cultivo.familia == c.familia) repite = true;
            }
            this.cosechaMedia = siembras == 0 ? 0 : suma / siembras;
            this.aguaPedida = pedida;
            this.aguaFalta = falta;
            this.repiteFamilia = repite;
            this.conLeguminosa = legumbre;
            this.conDescanso = descanso;
        }
    }

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

    /**
     * Las opciones que continúan la vuelta. Se avanza por la rueda en su orden
     * (cabecera, segunda, cereal, leguminosa, descanso) desde el paso de la campaña
     * anterior, y se toma el PRIMER paso que valga: que no se haya usado en esta
     * vuelta, que tenga cultivos posibles a esta altura y de otra familia. Así el
     * plan salta lo que no se da aquí (la papa en un valle bajo) en vez de volver a
     * un paso ya dado. Si ya se usaron todos, se cierra la vuelta y empieza otra; si
     * aun así no hay nada, se devuelven todas las opciones.
     */
    private static List<ConsejoTemporada> rotan(List<ConsejoTemporada> opciones, Cultivo previo,
                                                Set<Etapa> usadas) {
        for (int vuelta = 0; vuelta < 2; vuelta++) {
            Etapa paso = previo.etapa;
            for (int i = 0; i < Etapa.values().length; i++) {
                paso = paso.siguiente();
                if (usadas.contains(paso)) continue;
                List<ConsejoTemporada> r = new ArrayList<>();
                for (ConsejoTemporada c : opciones) {
                    if (c.cultivo.etapa == paso && c.cultivo.familia != previo.familia) r.add(c);
                }
                if (!r.isEmpty()) return r;
            }
            usadas.clear();
            usadas.add(previo.etapa);
        }
        return opciones;
    }

    public static List<Campana> armar(Temporada t, int altitud, Cultivo anterior, int seguidas,
                                      MotorTemporada.Suelo suelo, MotorTemporada.Prioridad prioridad,
                                      LocalDate hoy, Decision ahora, int campanas) {
        return armar(t, altitud, anterior, seguidas, suelo, prioridad, hoy, ahora, campanas, false);
    }

    /**
     * Para "cuidar el agua": entre las opciones que valen (no las que no conviene
     * sembrar), primero la que tiene menos déficit de agua en las 15 campañas; a
     * igualdad, la de más cosecha. El descanso y las que no convienen quedan detrás.
     */
    static List<ConsejoTemporada> menosAgua(List<ConsejoTemporada> opciones) {
        List<ConsejoTemporada> valen = new ArrayList<>(), resto = new ArrayList<>();
        for (ConsejoTemporada c : opciones) {
            boolean vale = c.cultivo != Cultivo.DESCANSO
                    && Veredicto.de(c).nivel != Veredicto.Nivel.REVISAR;
            (vale ? valen : resto).add(c);
        }
        valen.sort((a, b) -> a.deficitMedioMm != b.deficitMedioMm
                ? Double.compare(a.deficitMedioMm, b.deficitMedioMm)
                : Double.compare(b.rendimientoEsperado, a.rendimientoEsperado));
        valen.addAll(resto);
        return valen;
    }

    private static List<Campana> armar(Temporada t, int altitud, Cultivo anterior, int seguidas,
                                       MotorTemporada.Suelo suelo, MotorTemporada.Prioridad prioridad,
                                       LocalDate hoy, Decision ahora, int campanas,
                                       boolean ahorrarAgua) {
        if (t == null || anterior == null || ahora == null || hoy == null || campanas < 1)
            throw new IllegalArgumentException("Plan incompleto");
        List<Campana> plan = new ArrayList<>();
        plan.add(new Campana(hoy.getYear(), ahora.consejo, ahora.accion));
        MotorTemporada motor = new MotorTemporada();
        Cultivo previo = ahora.consejo.cultivo;
        // Los pasos ya dados en esta vuelta: el del cultivo que sembró y el de ahora.
        Set<Etapa> usadas = EnumSet.of(anterior.etapa, previo.etapa);
        int sinDescanso = previo == Cultivo.DESCANSO ? 0 : seguidas + 1;
        for (int k = 1; k < campanas; k++) {
            LocalDate fecha = hoy.plusYears(k);
            List<ConsejoTemporada> opciones = motor.recomendar(t, altitud, previo, sinDescanso,
                    suelo, prioridad, fecha);
            List<ConsejoTemporada> candidatas = rotan(opciones, previo, usadas);
            if (ahorrarAgua) candidatas = menosAgua(candidatas);
            Decision d = Decision.tomar(candidatas, null, altitud, fecha);
            plan.add(new Campana(fecha.getYear(), d.consejo, d.accion));
            previo = d.consejo.cultivo;
            usadas.add(previo.etapa);
            sinDescanso = previo == Cultivo.DESCANSO ? 0 : sinDescanso + 1;
        }
        return Collections.unmodifiableList(plan);
    }

    /**
     * Las tres estrategias calculadas con los mismos datos, para compararlas.
     * La primera campaña de todas es la recomendación de ahora, salvo en ACTUAL,
     * donde el agricultor sigue con el cultivo que ya sembró.
     */
    public static List<Plan> comparar(Temporada t, int altitud, Cultivo anterior, int seguidas,
                                      MotorTemporada.Suelo suelo, MotorTemporada.Prioridad prioridad,
                                      LocalDate hoy, Decision ahora, int campanas) {
        List<Plan> r = new ArrayList<>();
        r.add(new Plan(Estrategia.ACTUAL,
                seguir(t, altitud, anterior, seguidas, suelo, prioridad, hoy, campanas),
                anterior.seDaA(altitud) ? null : anterior));
        r.add(new Plan(Estrategia.ROTACION, armar(t, altitud, anterior, seguidas, suelo,
                prioridad, hoy, ahora, campanas)));
        List<ConsejoTemporada> conAgua = new MotorTemporada().recomendar(t, altitud, anterior,
                seguidas, suelo, MotorTemporada.Prioridad.CUIDAR_AGUA, hoy);
        Plan agua = new Plan(Estrategia.AGUA, armar(t, altitud, anterior, seguidas, suelo,
                MotorTemporada.Prioridad.CUIDAR_AGUA, hoy,
                Decision.tomar(menosAgua(conAgua), null, altitud, hoy), campanas, true));
        // Elegir campaña a campaña la de menos déficit no siempre da el plan con menos
        // déficit total. Si la andina pide menos agua, cuidar el agua ES la andina.
        Plan andina = r.get(1);
        r.add(agua.aguaFalta > andina.aguaFalta
                ? new Plan(Estrategia.AGUA, new ArrayList<>(andina.campanas)) : agua);
        return Collections.unmodifiableList(r);
    }

    /**
     * Seguir como hasta ahora: el mismo cultivo cada campaña. Si a esta altura ya no
     * se da, la campaña queda en descanso, que es lo que el motor acaba diciendo.
     */
    private static List<Campana> seguir(Temporada t, int altitud, Cultivo anterior, int seguidas,
                                        MotorTemporada.Suelo suelo, MotorTemporada.Prioridad prioridad,
                                        LocalDate hoy, int campanas) {
        List<Campana> plan = new ArrayList<>();
        MotorTemporada motor = new MotorTemporada();
        for (int k = 0; k < campanas; k++) {
            LocalDate fecha = hoy.plusYears(k);
            List<ConsejoTemporada> opciones = motor.recomendar(t, altitud, anterior,
                    seguidas + k, suelo, prioridad, fecha);
            ConsejoTemporada mismo = null, descanso = null;
            for (ConsejoTemporada c : opciones) {
                if (c.cultivo == anterior) mismo = c;
                if (c.cultivo == Cultivo.DESCANSO) descanso = c;
            }
            ConsejoTemporada elegido = mismo != null ? mismo
                    : descanso != null ? descanso : opciones.get(0);
            plan.add(new Campana(fecha.getYear(), elegido,
                    elegido.cultivo == Cultivo.DESCANSO ? Decision.Accion.DESCANSAR
                            : Decision.Accion.SEMBRAR_CUANDO_LLUEVA));
        }
        return plan;
    }
}
