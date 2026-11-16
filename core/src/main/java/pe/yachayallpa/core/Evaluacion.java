package pe.yachayallpa.core;
public final class Evaluacion {
    public final int puntos, maximo;
    public final Razon razon;
    public Evaluacion(int puntos,int maximo,Razon razon){if(puntos<0||puntos>maximo)throw new IllegalArgumentException();this.puntos=puntos;this.maximo=maximo;this.razon=razon;}
}
