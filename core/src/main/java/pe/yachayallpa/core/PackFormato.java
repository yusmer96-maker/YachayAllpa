package pe.yachayallpa.core;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PackFormato {
    private PackFormato() {}
    private static String limpio(String s){return s.replace('\r',' ').replace('\n',' ');}
    private static String numero(double d){return String.format(Locale.ROOT,"%.2f",d);}
    private static String serie(double[] a){StringJoiner j=new StringJoiner(",");for(double d:a)j.add(numero(d));return j.toString();}
    public static byte[] cuerpo(Pack p){
        StringBuilder b=new StringBuilder("YACHAY-PACK/1\n");
        b.append("version=").append(p.version).append('\n');
        b.append("distrito=").append(limpio(p.distrito)).append('\n');
        b.append("lat=").append(numero(p.lat)).append('\n');
        b.append("lon=").append(numero(p.lon)).append('\n');
        b.append("altitudReferencia=").append(numero(p.altitudReferencia)).append('\n');
        b.append("elevacionCelda=").append(numero(p.elevacionCelda)).append('\n');
        b.append("fuente=").append(limpio(p.fuente)).append('\n');
        perfil(b,"ahora",p.ahora);perfil(b,"antes",p.antes);
        return b.toString().getBytes(StandardCharsets.UTF_8);
    }
    private static void perfil(StringBuilder b,String nombre,PerfilClima p){
        b.append(nombre).append(".periodo=").append(limpio(p.periodo)).append('\n');
        b.append(nombre).append(".lluvia=").append(serie(p.lluvias())).append('\n');
        b.append(nombre).append(".tmin=").append(serie(p.tmins())).append('\n');
        b.append(nombre).append(".desviacion=").append(serie(p.desviaciones())).append('\n');
    }
    static Pack interpretar(byte[] cuerpo) throws PackInvalidoException {
        try {
            String texto=new String(cuerpo,StandardCharsets.UTF_8);
            if(!Arrays.equals(texto.getBytes(StandardCharsets.UTF_8),cuerpo))throw new IllegalArgumentException("UTF-8 inválido");
            String[] lineas=texto.split("\n",-1);
            if(!lineas[0].equals("YACHAY-PACK/1")||!lineas[lineas.length-1].isEmpty())throw new IllegalArgumentException("Cabecera");
            Map<String,String> m=new HashMap<>();
            for(int i=1;i<lineas.length-1;i++){
                String s=lineas[i];int igual=s.indexOf('=');
                if(igual<1||m.putIfAbsent(s.substring(0,igual),s.substring(igual+1))!=null)throw new IllegalArgumentException("Clave repetida o vacía");
            }
            Set<String> claves=new HashSet<>(Arrays.asList("version","distrito","lat","lon","altitudReferencia","elevacionCelda","fuente","ahora.periodo","ahora.lluvia","ahora.tmin","ahora.desviacion","antes.periodo","antes.lluvia","antes.tmin","antes.desviacion"));
            if(!m.keySet().equals(claves))throw new IllegalArgumentException("Claves inválidas");
            Pack p=new Pack(Integer.parseInt(m.get("version")),m.get("distrito"),Double.parseDouble(m.get("lat")),Double.parseDouble(m.get("lon")),Double.parseDouble(m.get("altitudReferencia")),Double.parseDouble(m.get("elevacionCelda")),m.get("fuente"),perfil(m,"ahora"),perfil(m,"antes"));
            if(!Arrays.equals(cuerpo(p),cuerpo))throw new IllegalArgumentException("Formato no canónico");
            return p;
        }catch(RuntimeException e){throw new PackInvalidoException(PackInvalidoException.Motivo.FORMATO,e.getMessage());}
    }
    private static PerfilClima perfil(Map<String,String> m,String prefijo){return new PerfilClima(m.get(prefijo+".periodo"),serie(m.get(prefijo+".lluvia")),serie(m.get(prefijo+".tmin")),serie(m.get(prefijo+".desviacion")));}
    private static double[] serie(String s){String[] a=s.split(",",-1);if(a.length!=12)throw new IllegalArgumentException("Serie incompleta");double[] d=new double[12];for(int i=0;i<12;i++)d[i]=Double.parseDouble(a[i]);return d;}
}
