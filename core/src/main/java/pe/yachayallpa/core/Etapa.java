package pe.yachayallpa.core;
public enum Etapa {
    CABECERA, SEGUNDA, CEREAL, LEGUMINOSA, DESCANSO;
    public Etapa siguiente() { return values()[(ordinal()+1)%values().length]; }
}
