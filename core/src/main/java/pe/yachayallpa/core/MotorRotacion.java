package pe.yachayallpa.core;
import java.util.*;
import static pe.yachayallpa.core.Razon.Icono.*;
public final class MotorRotacion {
    private final List<Regla> reglas;
    private static final String[] MESES={"enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"};
    public MotorRotacion(){this(Arrays.asList(new ReglaAgua(),new ReglaAltitud(),new ReglaHelada(),new ReglaRotacion()));}
    public MotorRotacion(List<Regla> reglas){
        int total=0;for(Regla r:reglas)total+=r.peso();
        if(total!=100)throw new IllegalArgumentException("Los pesos deben sumar 100");
        this.reglas=Collections.unmodifiableList(new ArrayList<>(reglas));
    }
    private Recomendacion evaluar(Cultivo c,int mes,Contexto x,PerfilClima p){
        int puntos=0;List<Razon> razones=new ArrayList<>();
        for(Regla r:reglas){Evaluacion e=r.evaluar(c,mes,x,p);puntos+=e.puntos;razones.add(e.razon);}
        return new Recomendacion(c,puntos,0,mes,razones);
    }
    private Recomendacion mejor(Cultivo c,Contexto x,PerfilClima p){
        Recomendacion mejor=null;
        for(int mes:c.mesesSiembra()){Recomendacion r=evaluar(c,mes,x,p);if(mejor==null||r.puntaje>mejor.puntaje)mejor=r;}
        return mejor;
    }
    public List<Recomendacion> recomendar(Contexto x){
        List<Recomendacion> todas=new ArrayList<>();
        for(Cultivo c:Cultivo.values()){
            if(c==Cultivo.DESCANSO)continue;
            Recomendacion actual=mejor(c,x,x.pack.ahora), anterior=mejor(c,x,x.pack.antes);
            // Si el calendario de antes todavía funciona, se conserva la práctica local.
            Recomendacion mesTradicional=evaluar(c,anterior.mesSiembra,x,x.pack.ahora);
            if(actual.puntaje-mesTradicional.puntaje<=2)actual=mesTradicional;
            List<Razon> razones=new ArrayList<>(actual.razones);
            int deriva=actual.puntaje-anterior.puntaje;
            if(Math.abs(deriva)>15){
                boolean mejoro=deriva>0;
                // El puntaje compara condiciones, no rendimiento cosechado: evitar prometer una cosecha.
                razones.add(new Razon(CLIMA_CAMBIO,mejoro,mejoro?"clima_mejor":"clima_peor",mejoro?"El clima de ahora favorece más este cultivo":"El clima de ahora favorece menos este cultivo"));
                if(actual.mesSiembra!=anterior.mesSiembra)razones.add(new Razon(CALENDARIO,true,"mes_cambio","Antes convenía sembrar en "+MESES[anterior.mesSiembra-1]+"; ahora en "+MESES[actual.mesSiembra-1]));
            }
            razones.sort(Comparator.comparing((Razon r)->!r.favorable));
            todas.add(new Recomendacion(c,actual.puntaje,anterior.puntaje,actual.mesSiembra,razones));
        }
        todas.add(descanso(x));
        todas.sort(Comparator.comparingInt((Recomendacion r)->r.puntaje).reversed().thenComparing(r->r.cultivo.ordinal()));
        return todas;
    }
    private Recomendacion descanso(Contexto x){
        List<Razon> razones=new ArrayList<>();int puntos;
        if(x.cultivoAnterior==Cultivo.DESCANSO){puntos=0;razones.add(new Razon(SUELO,false,"descanso_repetido","La chacra ya descansó la vez pasada"));}
        else {
            puntos=20;
            if(x.cultivoAnterior.etapa==Etapa.LEGUMINOSA){puntos+=40;razones.add(new Razon(COSTUMBRE,true,"descanso_leguminosa","Después de "+x.cultivoAnterior.articulo+" la costumbre es descansar"));}
            if(x.campaniasSinDescanso>=4){puntos+=40;razones.add(new Razon(SUELO,true,"descanso_urgente","Esta chacra lleva varias siembras seguidas sin descansar"));}
            else if(x.campaniasSinDescanso==3){puntos+=20;razones.add(new Razon(SUELO,true,"descanso_pronto","Ya conviene dar descanso a la tierra"));}
            if(razones.isEmpty())razones.add(new Razon(SUELO,true,"descanso_posible","Descansar ayuda a recuperar el suelo"));
        }
        return new Recomendacion(Cultivo.DESCANSO,Math.min(100,puntos),Math.min(100,puntos),0,razones);
    }
}
