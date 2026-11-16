package pe.yachayallpa.core;

import java.util.Arrays;

public final class PerfilClima {
    public final String periodo;
    private final double[] lluvia, tmin, desviacion;
    public PerfilClima(String periodo,double[] lluvia,double[] tmin,double[] desviacion) {
        if (periodo==null || periodo.isEmpty()) throw new IllegalArgumentException("Periodo vacío");
        this.periodo=periodo; this.lluvia=validar(lluvia); this.tmin=validar(tmin); this.desviacion=validar(desviacion);
        for (double d:this.desviacion) if (d<0) throw new IllegalArgumentException("Desviación negativa");
        for (double d:this.lluvia) if (d<0) throw new IllegalArgumentException("Lluvia negativa");
    }
    private static double[] validar(double[] a) {
        if(a==null || a.length!=12) throw new IllegalArgumentException("Se requieren 12 meses");
        for(double d:a) if(!Double.isFinite(d)) throw new IllegalArgumentException("Valor no finito");
        return a.clone();
    }
    public double lluvia(int mes){return lluvia[Math.floorMod(mes-1,12)];}
    public double tmin(int mes){return tmin[Math.floorMod(mes-1,12)];}
    public double desviacion(int mes){return desviacion[Math.floorMod(mes-1,12)];}
    public double[] lluvias(){return lluvia.clone();}
    public double[] tmins(){return tmin.clone();}
    public double[] desviaciones(){return desviacion.clone();}
}
