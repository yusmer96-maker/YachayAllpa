package pe.yachayallpa.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * La recomendación: UNA acción concreta para esta chacra, no una lista de
 * opciones "con cuidado". Junta dos cosas que la app ya tiene:
 *
 *  - el veredicto de las 15 campañas NASA (qué cultivo encaja y en qué fecha),
 *  - el pronóstico de los próximos 7 días (lluvia y mínima), si hay señal.
 *
 * Reglas, en este orden:
 *  1. Si lo primero es descansar, o ningún cultivo pasa de "revisar", se
 *     recomienda descansar.
 *  2. Si la fecha buena de ese cultivo está a más de 30 días, se dice el mes.
 *  3. Si toca sembrar ahora y hay pronóstico: helada prevista en la chacra
 *     (mínima corregida por altura) → esperar; si no hay 20 mm en 3 días seguidos
 *     (medidos en los últimos 3 días o pronosticados) → esperar la lluvia; si no,
 *     sembrar esta semana.
 *  4. Sin pronóstico, se recomienda sembrar cuando la tierra esté húmeda.
 *
 * Inicio de siembra: criterio de Sivakumar (1988, Agric. For. Meteorol. 42:295-305),
 * 20 mm acumulados en 3 días seguidos, el más usado para el inicio de la campaña en
 * secano. Helada: 0 °C (-2 °C para cultivos que aguantan frío).
 */
public final class Decision {
    public enum Accion { SEMBRAR_YA, SEMBRAR_CUANDO_LLUEVA, ESPERAR_LLUVIA, ESPERAR_HELADA,
        SEMBRAR_EN_MES, DESCANSAR }

    /** Lluvia en 3 días seguidos que marca el inicio de la siembra (Sivakumar, 1988). */
    public static final double LLUVIA_INICIO_3D = 20;

    /** Pronóstico de 7 días a la altura de la celda NASA. */
    public static final class Pronostico {
        /** lluvia3: los 3 días seguidos más lluviosos del pronóstico. medida3: lo medido
         *  por satélite en los últimos 3 días (NaN si no hay dato fresco). */
        public final double lluvia7, lluvia3, medida3, tminMinima, elevacionCelda;
        public final LocalDate diaMasFrio;

        public Pronostico(double lluvia7, double lluvia3, double medida3, double tminMinima,
                          LocalDate diaMasFrio, double elevacionCelda) {
            if (!Double.isFinite(lluvia7) || lluvia7 < 0 || !Double.isFinite(lluvia3) || lluvia3 < 0
                    || lluvia3 > lluvia7 + 1e-9 || (Double.isFinite(medida3) && medida3 < 0)
                    || !Double.isFinite(tminMinima)
                    || !Double.isFinite(elevacionCelda) || diaMasFrio == null)
                throw new IllegalArgumentException("Pronóstico inválido");
            this.lluvia7 = lluvia7;
            this.lluvia3 = lluvia3;
            this.medida3 = medida3;
            this.tminMinima = tminMinima;
            this.diaMasFrio = diaMasFrio;
            this.elevacionCelda = elevacionCelda;
        }
    }

    public final Accion accion;
    public final ConsejoTemporada consejo;
    public final Veredicto veredicto;
    /** Cifras que sostienen la acción, sin idioma (la app las escribe). */
    public final List<Motivo> porque;

    private Decision(Accion accion, ConsejoTemporada consejo, List<Motivo> porque) {
        this.accion = accion;
        this.consejo = consejo;
        this.veredicto = Veredicto.de(consejo);
        this.porque = Collections.unmodifiableList(porque);
    }

    public static Decision tomar(List<ConsejoTemporada> opciones, Pronostico p,
                                 int altitud, LocalDate hoy) {
        if (opciones == null || opciones.isEmpty() || hoy == null)
            throw new IllegalArgumentException("Sin opciones para decidir");
        ConsejoTemporada mejor = opciones.get(0);
        Veredicto v = Veredicto.de(mejor);
        List<Motivo> porque = new ArrayList<>();
        if (mejor.cultivo == Cultivo.DESCANSO) {
            porque.addAll(mejor.razones);
            return new Decision(Accion.DESCANSAR, mejor, porque);
        }
        if (v.nivel == Veredicto.Nivel.REVISAR) {
            // Ningún cultivo pasa la prueba: se dice con la cosecha esperada del mejor.
            porque.add(new Motivo("ninguno_encaja", mejor.rendimientoEsperado * 100));
            ConsejoTemporada descanso = null;
            for (ConsejoTemporada c : opciones) if (c.cultivo == Cultivo.DESCANSO) descanso = c;
            return new Decision(Accion.DESCANSAR, descanso != null ? descanso : mejor, porque);
        }
        porque.add(new Motivo("cosecha_15", mejor.rendimientoEsperado * 100));
        // Si el agua de lluvia deja perder más del 15 %, el riego es lo que la completa.
        if (mejor.rendimientoEsperado < Veredicto.AGUA_BUENA)
            porque.add(new Motivo("riego", (1 - mejor.coberturaAgua) * 100));
        LocalDate fecha = mejor.fechaSiembra;
        if (fecha != null && fecha.isAfter(hoy.plusDays(30))) {
            porque.add(new Motivo("mes_bueno", fecha.getMonthValue()));
            return new Decision(Accion.SEMBRAR_EN_MES, mejor, porque);
        }
        if (p == null) return new Decision(Accion.SEMBRAR_CUANDO_LLUEVA, mejor, porque);
        double tminChacra = Clima.tminCorregida(p.tminMinima, p.elevacionCelda, altitud);
        double umbral = mejor.cultivo.toleranciaHelada >= .40 ? -2 : 0;
        if (tminChacra <= umbral) {
            porque.add(0, new Motivo("helada_prevista", tminChacra, p.diaMasFrio.getDayOfMonth()));
            return new Decision(Accion.ESPERAR_HELADA, mejor, porque);
        }
        // Lo que ya cayó cuenta igual que lo que viene: la tierra ya está húmeda.
        if (Double.isFinite(p.medida3) && p.medida3 >= LLUVIA_INICIO_3D) {
            porque.add(0, new Motivo("lluvia_medida", p.medida3));
            return new Decision(Accion.SEMBRAR_YA, mejor, porque);
        }
        porque.add(0, new Motivo("lluvia_prevista", p.lluvia3));
        return new Decision(p.lluvia3 < LLUVIA_INICIO_3D ? Accion.ESPERAR_LLUVIA : Accion.SEMBRAR_YA,
                mejor, porque);
    }
}
