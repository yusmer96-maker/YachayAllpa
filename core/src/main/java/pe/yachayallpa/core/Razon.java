package pe.yachayallpa.core;
public final class Razon {
    public enum Icono { AGUA, HELADA, ALTURA, SUELO, COSTUMBRE, CALENDARIO, CLIMA_CAMBIO }
    public final Icono icono;
    public final boolean favorable;
    public final String clave, texto;
    public Razon(Icono icono,boolean favorable,String clave,String texto){this.icono=icono;this.favorable=favorable;this.clave=clave;this.texto=texto;}
}
