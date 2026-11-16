package pe.yachayallpa.core;
import static pe.yachayallpa.core.Razon.Icono.SUELO;
import static pe.yachayallpa.core.Razon.Icono.COSTUMBRE;
public final class ReglaRotacion implements Regla {
    public int peso(){return 30;}
    public Evaluacion evaluar(Cultivo c,int mes,Contexto x,PerfilClima p){
        Cultivo a=x.cultivoAnterior;
        if(c==a)return new Evaluacion(0,30,new Razon(SUELO,false,"mismo_cultivo","Repetir cansa el suelo y trae plagas"));
        if(c.familia==a.familia)return new Evaluacion(5,30,new Razon(SUELO,false,"misma_familia","Es pariente de lo que sembró: comparten plagas"));
        if(c.fijaNitrogeno&&a.consumeNitrogeno)return new Evaluacion(30,30,new Razon(SUELO,true,"nitrogeno","Devuelve al suelo la fuerza que se llevó "+a.articulo));
        if(c.etapa==a.etapa.siguiente())return new Evaluacion(30,30,new Razon(COSTUMBRE,true,"secuencia","Sigue la rotación de nuestros abuelos"));
        return new Evaluacion(15,30,new Razon(SUELO,true,"cambio","Cambiar de cultivo ayuda al suelo"));
    }
}
