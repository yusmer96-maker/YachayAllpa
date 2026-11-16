package pe.yachayallpa.core;
import static pe.yachayallpa.core.Razon.Icono.ALTURA;
public final class ReglaAltitud implements Regla {
    public int peso(){return 20;}
    public Evaluacion evaluar(Cultivo c,int mes,Contexto x,PerfilClima p){
        int fuera=Math.max(c.altitudMin-x.altitudChacra,Math.max(0,x.altitudChacra-c.altitudMax));
        int puntos=(int)Math.round(20*Math.max(0,1-fuera/500.0));
        return new Evaluacion(puntos,20,new Razon(ALTURA,fuera==0,fuera==0?"altura_bien":"altura_fuera",fuera==0?"Esta altura le conviene":"Esta altura no le conviene mucho"));
    }
}
