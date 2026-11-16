package pe.yachayallpa.core;
public final class Contexto {
    public final Pack pack;
    public final int altitudChacra, campaniasSinDescanso;
    public final Cultivo cultivoAnterior;
    public Contexto(Pack pack,int altitudChacra,Cultivo anterior,int campanias){if(pack==null||anterior==null||altitudChacra<0||campanias<0)throw new IllegalArgumentException();this.pack=pack;this.altitudChacra=altitudChacra;cultivoAnterior=anterior;campaniasSinDescanso=campanias;}
}
