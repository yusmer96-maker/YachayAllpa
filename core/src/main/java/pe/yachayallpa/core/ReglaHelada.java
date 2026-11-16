package pe.yachayallpa.core;
import static pe.yachayallpa.core.Razon.Icono.HELADA;
public final class ReglaHelada implements Regla {
    public int peso(){return 20;}
    public Evaluacion evaluar(Cultivo c,int mes,Contexto x,PerfilClima p){
        double riesgo=Clima.peorHelada(p,mes,c.cicloMeses,x.pack.elevacionCelda,x.altitudChacra);
        double exceso=Math.max(0,riesgo-c.toleranciaHelada);
        int puntos=(int)Math.round(20*Math.max(0,1-exceso/.30));
        boolean bien=exceso==0;
        String clave=bien?"helada_baja":exceso<.15?"helada_media":"helada_alta";
        String texto=bien?"Tolera el frío habitual de esta zona":exceso<.15?"Cuidado con las heladas en esta zona":"Las heladas pueden dañar este cultivo";
        return new Evaluacion(puntos,20,new Razon(HELADA,bien,clave,texto));
    }
}
