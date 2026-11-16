package pe.yachayallpa.core;
import java.util.*;
public final class Recomendacion {
    public enum Semaforo { VERDE, AMARILLO, ROJO }
    public final Cultivo cultivo;
    public final int puntaje, puntajeAntes, mesSiembra;
    public final Semaforo semaforo;
    public final List<Razon> razones;
    public Recomendacion(Cultivo cultivo,int puntaje,int puntajeAntes,int mesSiembra,List<Razon> razones){
        this.cultivo=cultivo;this.puntaje=puntaje;this.puntajeAntes=puntajeAntes;this.mesSiembra=mesSiembra;
        semaforo=puntaje>=70?Semaforo.VERDE:puntaje>=45?Semaforo.AMARILLO:Semaforo.ROJO;
        this.razones=Collections.unmodifiableList(new ArrayList<>(razones));
    }
    public int circulos(){return Math.max(1,Math.min(5,(int)Math.ceil(puntaje/20.0)));}
}
