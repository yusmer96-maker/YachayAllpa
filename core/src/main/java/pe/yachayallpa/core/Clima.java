package pe.yachayallpa.core;
public final class Clima {
    private Clima() {}
    public static double normalCdf(double x) {
        if(Double.isNaN(x)) throw new IllegalArgumentException("NaN");
        double z=Math.abs(x), t=1.0/(1.0+0.2316419*z);
        double phi=Math.exp(-z*z/2.0)/Math.sqrt(2.0*Math.PI);
        // Abramowitz y Stegun 7.1.26: aproximación suficiente para un semáforo, no para una predicción clínica.
        double c=1.0-phi*t*(0.319381530+t*(-0.356563782+t*(1.781477937+t*(-1.821255978+t*1.330274429))));
        return x>=0?c:1.0-c;
    }
    public static double tminCorregida(double tminCelda,double elevacionCelda,double altitudChacra) {
        return tminCelda-0.0065*(altitudChacra-elevacionCelda);
    }
    public static double probHelada(double mediaCelda,double desviacion,double elevacionCelda,double altitudChacra) {
        double media=tminCorregida(mediaCelda,elevacionCelda,altitudChacra);
        if(desviacion<0) throw new IllegalArgumentException("Desviación negativa");
        if(desviacion==0) return media<0?1:0;
        return normalCdf(-media/desviacion);
    }
    public static double lluviaEfectiva(PerfilClima p,int mes,int ciclo) {
        double suma=0; for(int i=0;i<ciclo;i++) suma+=p.lluvia(mes+i);
        return .8*suma;
    }
    public static double peorHelada(PerfilClima p,int mes,int ciclo,double elevacion,double altitud) {
        double max=0;
        // Primeras etapas y floración quedan expuestas; usar el peor mes evita esconder una helada en el promedio.
        for(int i=0;i<ciclo;i++) max=Math.max(max,probHelada(p.tmin(mes+i),p.desviacion(mes+i),elevacion,altitud));
        return max;
    }
}
