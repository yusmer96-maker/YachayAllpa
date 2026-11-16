package pe.yachayallpa.core;
import static pe.yachayallpa.core.Razon.Icono.AGUA;
public final class ReglaAgua implements Regla {
    public int peso(){return 30;}
    public Evaluacion evaluar(Cultivo c,int mes,Contexto x,PerfilClima p){
        double fraccion=Math.min(1,Clima.lluviaEfectiva(p,mes,c.cicloMeses)/c.aguaCicloMm);
        int puntos=(int)Math.round(30*fraccion);
        boolean bien=fraccion>=.8;
        return new Evaluacion(puntos,30,new Razon(AGUA,bien,bien?"agua_bien":"agua_poca",bien?"La lluvia habitual podría alcanzar":"La lluvia habitual puede quedarse corta"));
    }
}
