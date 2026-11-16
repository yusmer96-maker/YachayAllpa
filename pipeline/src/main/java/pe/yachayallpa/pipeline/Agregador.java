package pe.yachayallpa.pipeline;
import pe.yachayallpa.core.PerfilClima;
import java.time.YearMonth;
import java.util.*;

public final class Agregador {
    private static final class Mes { double lluvia;int diasLluvia;List<Double> tmins=new ArrayList<>(); }
    public PerfilClima agregar(NasaPower.Datos datos,int desde,int hasta){
        if(datos==null||desde>hasta)throw new IllegalArgumentException("Periodo inválido");
        // Un solo año completo puede parecer una climatología: exigimos al menos dos tercios del periodo.
        int minimoAnos=(int)Math.ceil((hasta-desde+1)*2.0/3.0);
        Map<YearMonth,Mes> meses=new HashMap<>();
        for(NasaPower.Dia dia:datos.dias){
            int ano=dia.fecha.getYear();if(ano<desde||ano>hasta)continue;
            YearMonth ym=YearMonth.from(dia.fecha);Mes m=meses.computeIfAbsent(ym,k->new Mes());
            if(Double.isFinite(dia.lluvia)&&dia.lluvia>=0){m.lluvia+=dia.lluvia;m.diasLluvia++;}
            if(Double.isFinite(dia.tmin))m.tmins.add(dia.tmin);
        }
        double[] lluvia=new double[12],media=new double[12],desv=new double[12];int[] n=new int[12];
        for(Map.Entry<YearMonth,Mes> e:meses.entrySet()){
            Mes m=e.getValue();if(m.diasLluvia<25||m.tmins.size()<25)continue;
            int i=e.getKey().getMonthValue()-1;
            lluvia[i]+=m.lluvia*e.getKey().lengthOfMonth()/m.diasLluvia;
            double promedio=m.tmins.stream().mapToDouble(v->v).average().orElseThrow();
            double varianza=0;for(double v:m.tmins)varianza+=(v-promedio)*(v-promedio);
            media[i]+=promedio;desv[i]+=varianza/m.tmins.size()+promedio*promedio;n[i]++;
        }
        for(int i=0;i<12;i++){
            if(n[i]<minimoAnos)throw new IllegalArgumentException("Solo "+n[i]+" años válidos en mes "+(i+1)+"; se necesitan "+minimoAnos);
            lluvia[i]/=n[i];media[i]/=n[i];desv[i]=Math.sqrt(Math.max(0,desv[i]/n[i]-media[i]*media[i]));
        }
        return new PerfilClima(desde+"-"+hasta,lluvia,media,desv);
    }
}
