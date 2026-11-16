package pe.yachayallpa.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Balance hídrico de diez días repetido en 15 temporadas NASA, más rotación y helada. */
public final class MotorTemporada {
    public enum Suelo { NO_SE, SE_SECA_RAPIDO, RETIENE_AGUA }
    public enum Prioridad { EQUILIBRIO, CUIDAR_AGUA, CUIDAR_SUELO, COSECHAR_PRONTO }

    private static final class Balance {
        double cobertura, deficit, helada, demanda, rendimiento;
        int anosEstres, anosAguaCompleta;
    }

    public List<ConsejoTemporada> recomendar(Temporada t, int altitud, Cultivo anterior,
            int siembrasSinDescanso, Suelo suelo, Prioridad prioridad, LocalDate hoy) {
        if (t == null || anterior == null || suelo == null || prioridad == null || hoy == null
                || altitud < 0 || siembrasSinDescanso < 0) throw new IllegalArgumentException("Chacra incompleta");
        List<ConsejoTemporada> opciones = new ArrayList<>();
        for (Cultivo c : Cultivo.values()) {
            if (c == Cultivo.DESCANSO) continue;
            // Una zona baja no debe recibir como opción un cultivo que está cientos de metros fuera de rango.
            if (!c.seDaA(altitud)) continue;
            ConsejoTemporada mejor = null;
            for (int mes : c.mesesSiembra()) {
                LocalDate fecha = proximaSiembra(hoy, mes);
                // Una fecha lejana debe verse como planificación, no como orden de sembrar hoy.
                Balance b = simular(t, c, fecha, altitud, suelo, hoy);
                int puntaje = puntuar(c, anterior, siembrasSinDescanso, prioridad, altitud, b);
                List<Motivo> razones = razones(c, anterior, fecha, hoy, b, t, altitud);
                ConsejoTemporada candidato = new ConsejoTemporada(c, fecha, puntaje,
                        b.anosEstres, b.anosAguaCompleta,
                        b.cobertura, b.helada, b.deficit, b.rendimiento, razones);
                // Entre las fechas del mismo cultivo gana la que mejor veredicto da,
                // no la de más puntaje: el agua y la helada mandan sobre la suma.
                if (mejor == null || MEJOR_PRIMERO.compare(candidato, mejor) < 0)
                    mejor = candidato;
            }
            opciones.add(mejor);
        }
        // La escalera va de una en una: así cada respuesta de "¿descansó?"
        // cambia algo, en vez de que solo cuente la última.
        int descanso = anterior == Cultivo.DESCANSO ? 0 : 20
                + (anterior.fijaNitrogeno ? 40 : 0)
                + (siembrasSinDescanso >= 4 ? 40 : siembrasSinDescanso == 3 ? 20
                        : siembrasSinDescanso == 2 ? 10 : 0)
                + (prioridad == Prioridad.CUIDAR_SUELO ? 10 : 0);
        List<Motivo> razones = new ArrayList<>();
        razones.add(new Motivo(anterior == Cultivo.DESCANSO ? "descanso_ya"
                : siembrasSinDescanso >= 4 ? "descanso_seguidas"
                : anterior.fijaNitrogeno ? "descanso_leguminosa"
                : "descanso_suelo"));
        opciones.add(new ConsejoTemporada(Cultivo.DESCANSO, null,
                Math.min(100, descanso), 0, 0, 1, 0, 0, razones));
        opciones.sort(MEJOR_PRIMERO);
        return opciones;
    }

    /**
     * El orden de la lista es el orden del veredicto, no el del puntaje: la
     * primera opción tiene que ser de verdad la mejor de las tres que se ven, y
     * nunca puede salir pintada peor que las alternativas que lleva al lado.
     */
    private static final Comparator<ConsejoTemporada> MEJOR_PRIMERO =
            Comparator.comparingInt((ConsejoTemporada r) -> Veredicto.de(r).nivel.ordinal()).reversed()
                    .thenComparing(Comparator.comparingInt((ConsejoTemporada r) -> r.puntaje).reversed())
                    .thenComparing(r -> r.fechaSiembra == null ? 0L : r.fechaSiembra.toEpochDay())
                    .thenComparing(r -> r.cultivo.ordinal());

    private static LocalDate proximaSiembra(LocalDate hoy, int mes) {
        LocalDate propuesta = LocalDate.of(hoy.getYear(), mes, 1);
        if (mes == hoy.getMonthValue()) return hoy;
        if (propuesta.isBefore(hoy)) propuesta = propuesta.plusYears(1);
        return propuesta;
    }

    private static Balance simular(Temporada t, Cultivo c, LocalDate fecha, int altitud, Suelo suelo, LocalDate hoy) {
        Balance b = new Balance();
        double capacidad = suelo == Suelo.SE_SECA_RAPIDO ? 40 : suelo == Suelo.RETIENE_AGUA ? 120 : 80;
        int bloques = c.cicloMeses * 3;
        for (int ano = 0; ano < Temporada.ESCENARIOS; ano++) {
            // Si se siembra hoy, el agua reciente inicia el balance solo como estimación, no suelo medido.
            double reserva = t.reciente(hoy) && fecha.equals(hoy)
                    ? Math.min(capacidad, lluviaEfectiva(t.lluvia15 * 10.0 / 15.0) * 1.5) : capacidad * .5;
            double necesidad = 0, cubierta = 0, deficit = 0;
            double peorHelada = 0;
            for (int i = 0; i < bloques; i++) {
                int bloque = Math.floorMod(Temporada.bloque(fecha) + i, Temporada.BLOQUES);
                double fraccion = i / (double) bloques;
                double kc = fraccion < .25 ? c.kcInicial
                        : fraccion < .75 ? c.kcMedio : c.kcFinal;
                double fraccionBloque = i==0 && fecha.equals(hoy) ? restante(fecha) : 1;
                double demanda = t.eto(ano, bloque) * kc * fraccionBloque
                        * factorAltura(t.tmedia(ano, bloque), t.elevacionCelda, altitud);
                // Solo la lluvia efectiva llega a las raíces (USDA SCS); lo que sobra de la
                // capacidad del suelo se pierde por drenaje.
                reserva = Math.min(capacidad, reserva + lluviaEfectiva(t.lluvia(ano, bloque))
                        * fraccionBloque);
                double entregada = Math.min(reserva, demanda);
                reserva -= entregada;
                necesidad += demanda;
                cubierta += entregada;
                // La floración y el inicio pesan más que la maduración ante falta de agua.
                if (entregada < demanda && (fraccion < .25 || fraccion >= .40 && fraccion < .75))
                    deficit += demanda - entregada;
                if (fraccion < .25 || fraccion >= .4 && fraccion < .75) {
                    double riesgo = Clima.probHelada(t.tmin(ano, bloque),
                            Math.max(.6, t.desviacion(ano, bloque)), t.elevacionCelda, altitud);
                    peorHelada = Math.max(peorHelada, riesgo);
                }
            }
            double ratio = necesidad <= 0 ? 1 : cubierta / necesidad;
            b.cobertura += ratio;
            // Los milímetros que pide el ciclo: es lo que se le enseña al agricultor.
            b.demanda += necesidad;
            // Cosecha de ese año por falta de agua (FAO-33): 1 − Ky·(1 − ETa/ETc).
            b.rendimiento += Math.max(0, 1 - c.ky() * (1 - ratio));
            // Solo decimos que el agua alcanzó si cubrió toda la demanda modelada.
            if (ratio >= 1 - 1e-6) b.anosAguaCompleta++;
            b.deficit += deficit;
            b.helada += peorHelada;
            if (ratio < .75 || peorHelada > c.toleranciaHelada + .15) b.anosEstres++;
        }
        b.cobertura /= Temporada.ESCENARIOS;
        b.deficit /= Temporada.ESCENARIOS;
        b.helada /= Temporada.ESCENARIOS;
        b.demanda /= Temporada.ESCENARIOS;
        b.rendimiento /= Temporada.ESCENARIOS;
        return b;
    }

    /**
     * La ETo del pack es la de la celda NASA, que puede estar 1.500 m por encima
     * o por debajo de la chacra. Con Hargreaves, ETo ∝ (T + 17,8): se corrige la
     * media con el gradiente de -6,5 °C/km (el mismo de la helada) y se escala la
     * demanda. El rango diario no cambia con la altura, así que se cancela.
     * Packs del formato 1 (sin media) no se corrigen.
     */
    static double factorAltura(double tmediaCelda, double elevacionCelda, double altitud) {
        if (!Double.isFinite(tmediaCelda) || tmediaCelda + 17.8 < 1) return 1;
        double tChacra = Clima.tminCorregida(tmediaCelda, elevacionCelda, altitud);
        return Math.max(.5, Math.min(1.6, (tChacra + 17.8) / (tmediaCelda + 17.8)));
    }

    /**
     * Lluvia efectiva de un bloque de diez días, por el método del USDA (SCS), el
     * que usa CROPWAT de la FAO: mensual Pe = P(125 − 0,2P)/125 si P ≤ 250 mm, y
     * Pe = 125 + 0,1P si no. Las lluvias pequeñas se aprovechan casi enteras y las
     * fuertes escurren más. El bloque se lleva a su equivalente mensual (×3).
     */
    static double lluviaEfectiva(double mmBloque) {
        double p = Math.max(0, mmBloque) * 3;
        double pe = p <= 250 ? p * (125 - 0.2 * p) / 125 : 125 + 0.1 * p;
        return pe / 3;
    }

    private static double restante(LocalDate fecha) {
        int primero=(fecha.getDayOfMonth()-1)/10*10+1;
        int ultimo=Math.min(primero+9,fecha.lengthOfMonth());
        if(primero==21)ultimo=fecha.lengthOfMonth();
        return (ultimo-fecha.getDayOfMonth()+1)/(double)(ultimo-primero+1);
    }

    private static int puntuar(Cultivo c, Cultivo anterior, int seguidas, Prioridad prioridad,
                                int altitud, Balance b) {
        // Agua: la cosecha esperada por falta de agua (FAO-33), de 0 a 1.
        double agua = Math.max(0, Math.min(1, b.rendimiento));
        double helada = Math.max(0, 1 - Math.max(0, b.helada - c.toleranciaHelada) / .4);
        double altura = Math.max(0, 1 - c.fueraDeRango(altitud) / (double) Cultivo.MARGEN_ALTURA);
        double rotacion = c == anterior ? 0 : c.familia == anterior.familia ? .15
                : c.fijaNitrogeno && anterior.consumeNitrogeno ? 1
                : altitud >= 1800 && c.altitudMin >= 1800
                    && anterior.etapa.siguiente() == c.etapa ? 1 : .5;
        int penalidadSuelo = seguidas >= 4 && c.consumeNitrogeno ? 12 : 0;
        double puntuacion = 35 * agua + 20 * helada + 20 * altura + 25 * rotacion - penalidadSuelo;
        if (prioridad == Prioridad.CUIDAR_AGUA) puntuacion += 8 * agua - 6 * (1 - agua);
        if (prioridad == Prioridad.CUIDAR_SUELO) puntuacion += 10 * rotacion - (c.consumeNitrogeno ? 6 : 0);
        if (prioridad == Prioridad.COSECHAR_PRONTO) puntuacion += Math.max(0, 7 - c.cicloMeses) * 2;
        // Repetir la misma especie arriesga plagas y fatiga; no debe salir verde por mucha lluvia.
        if(c==anterior)puntuacion=Math.min(puntuacion,34);
        return (int) Math.round(Math.max(0, Math.min(100, puntuacion)));
    }

    private static List<Motivo> razones(Cultivo c, Cultivo anterior, LocalDate fecha,
                                        LocalDate hoy, Balance b, Temporada t, int altitud) {
        List<Motivo> r = new ArrayList<>();
        if (c == anterior) r.add(new Motivo("repetir"));
        else if (c.familia == anterior.familia) r.add(new Motivo("parientes"));
        else if (c.fijaNitrogeno && anterior.consumeNitrogeno) r.add(new Motivo("leguminosa"));
        else if (altitud >= 1800 && c.altitudMin >= 1800
                && anterior.etapa.siguiente() == c.etapa)
            r.add(new Motivo("rotacion"));
        // Cifras de esta opción y de esta chacra: cobertura, milímetros que pide
        // el ciclo, campañas con estrés y la altura frente al rango del cultivo.
        r.add(new Motivo("cobertura", b.cobertura * 100, b.demanda));
        r.add(new Motivo("cosecha", b.rendimiento * 100));
        r.add(new Motivo("estres", b.anosEstres));
        r.add(new Motivo(altitud >= c.altitudMin && altitud <= c.altitudMax ? "altura_bien"
                : c.seDaA(altitud) ? "altura_limite"
                : "altura_fuera", altitud));
        if (b.helada > c.toleranciaHelada) r.add(new Motivo("helada"));
        if (fecha.isAfter(hoy.plusDays(45))) r.add(new Motivo("calendario", fecha.getMonthValue()));
        if (t.reciente(hoy) && t.promedio30 > 0 && t.lluvia30 > t.promedio30 * 1.8)
            r.add(new Motivo("lluvia_mucha"));
        return r;
    }
}
