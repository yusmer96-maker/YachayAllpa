package pe.yachayallpa.core;
public final class PackInvalidoException extends Exception {
    public enum Motivo { FORMATO, FIRMA, VERSION_ANTIGUA }
    public final Motivo motivo;
    public PackInvalidoException(Motivo motivo,String mensaje){super(mensaje);this.motivo=motivo;}
}
