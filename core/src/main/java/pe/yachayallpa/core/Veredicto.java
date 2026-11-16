package pe.yachayallpa.core;

/**
 * El juicio unico de una opcion: lo que se pinta, lo que se dice y lo que se lee
 * en voz alta salen todos de aqui.
 *
 * POR QUE EXISTE. Antes la tarjeta se pintaba con el puntaje y las barras con el
 * agua, cada una con sus umbrales. Una opcion podia salir en verde y "muy buena"
 * encima de una barra roja de falta de agua. El puntaje mezcla agua, helada,
 * altura y rotacion: una buena rotacion tapaba un balance de agua malo.
 *
 * REGLA. El nivel final es el MENOR de los tres: puntaje, agua y helada. Si el
 * agua o la helada estan en rojo, la opcion no puede salir buena por mucho que
 * sume el resto. El mismo nivel ordena la lista, asi que la primera opcion es de
 * verdad la mejor y nunca queda por debajo de las dos alternativas.
 *
 * LAS CIFRAS. El agua se juzga por la COSECHA ESPERADA (FAO-33): cada campana
 * 1 − Ky·(1 − agua recibida / agua pedida), promedio de las 15 campanas NASA.
 * Desde 85 % va bien; entre 50 y 85 %, con cuidado (se siembra y conviene
 * riego); bajo 50 %, no compensa en secano. La helada se mide contra lo que
 * ese cultivo aguanta, porque una helada que no mata a la mashua si mata al maiz.
 */
public final class Veredicto {
    public enum Nivel { REVISAR, CUIDADO, BUENA }

    /**
     * Cosecha esperada (FAO-33) desde la que el agua no estorba: se pierde como
     * mucho un 15 %. Por debajo de AGUA_JUSTA se pierde más de la mitad: con esa
     * cosecha no compensa sembrar en secano.
     */
    public static final double AGUA_BUENA = .85;
    public static final double AGUA_JUSTA = .50;
    /** Margen de riesgo de helada sobre la tolerancia del cultivo que aun se acepta. */
    public static final double HELADA_MARGEN = .15;

    public final Nivel nivel, agua, helada;
    public final double faltaAgua, excesoHelada;

    private Veredicto(Nivel nivel, Nivel agua, Nivel helada,
                      double faltaAgua, double excesoHelada) {
        this.nivel = nivel;
        this.agua = agua;
        this.helada = helada;
        this.faltaAgua = faltaAgua;
        this.excesoHelada = excesoHelada;
    }

    private static Nivel porPuntaje(int puntaje) {
        return puntaje >= 70 ? Nivel.BUENA : puntaje >= 45 ? Nivel.CUIDADO : Nivel.REVISAR;
    }

    private static Nivel menor(Nivel a, Nivel b) {
        return a.ordinal() <= b.ordinal() ? a : b;
    }

    private static double recortar(double v) { return Math.max(0, Math.min(1, v)); }

    public static Veredicto de(ConsejoTemporada c) {
        if (c == null) throw new IllegalArgumentException("Consejo nulo");
        Nivel puntaje = porPuntaje(c.puntaje);
        // Descansar no necesita agua ni teme la helada: su nivel es solo su puntaje.
        if (c.cultivo == Cultivo.DESCANSO)
            return new Veredicto(puntaje, Nivel.BUENA, Nivel.BUENA, 0, 0);
        double cosecha = c.rendimientoEsperado;
        Nivel agua = cosecha >= AGUA_BUENA ? Nivel.BUENA
                : cosecha >= AGUA_JUSTA ? Nivel.CUIDADO : Nivel.REVISAR;
        // Índice 0..1 de lo que se pierde por agua, para quien lo quiera en barra.
        double faltaAgua = recortar((1 - cosecha) / (1 - AGUA_JUSTA));
        double exceso = c.riesgoHelada - c.cultivo.toleranciaHelada;
        Nivel helada = exceso <= 0 ? Nivel.BUENA
                : exceso <= HELADA_MARGEN ? Nivel.CUIDADO : Nivel.REVISAR;
        double excesoHelada = recortar(exceso / (HELADA_MARGEN * 2));
        return new Veredicto(menor(puntaje, menor(agua, helada)), agua, helada,
                faltaAgua, excesoHelada);
    }
}
