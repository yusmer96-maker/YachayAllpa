package pe.yachayallpa.pipeline;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

public final class NasaPower {
    public static final class Dia {
        public final LocalDate fecha; public final double lluvia,tmin;
        Dia(LocalDate fecha,double lluvia,double tmin){this.fecha=fecha;this.lluvia=lluvia;this.tmin=tmin;}
    }
    public static final class Datos {
        public final double elevacionCelda; public final List<Dia> dias;
        Datos(double elevacion,List<Dia> dias){this.elevacionCelda=elevacion;this.dias=Collections.unmodifiableList(dias);}
    }
    private final HttpClient cliente=HttpClient.newHttpClient();
    public static String url(double lat,double lon,int inicio,int fin){return String.format(Locale.ROOT,"https://power.larc.nasa.gov/api/temporal/daily/point?parameters=PRECTOTCORR,T2M_MIN&community=AG&longitude=%.5f&latitude=%.5f&start=%d0101&end=%d1231&format=CSV",lon,lat,inicio,fin);}
    public String descargar(String url) throws IOException,InterruptedException {
        HttpRequest req=HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> res=cliente.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if(res.statusCode()!=200)throw new IOException("NASA POWER HTTP "+res.statusCode());
        return res.body();
    }
    public static Datos parsear(String csv){
        String[] lineas=csv.replace("\r","").split("\n");
        boolean dentro=false,termino=false;double elevacion=Double.NaN;int cabecera=-1;
        Pattern altura=Pattern.compile("(?i)(?:elevation|elevaci[oó]n)[^=]*=\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*meters");
        for(int i=0;i<lineas.length;i++){
            String s=lineas[i].trim();
            if(s.equals("-BEGIN HEADER-")){dentro=true;continue;}
            if(s.equals("-END HEADER-")){dentro=false;termino=true;continue;}
            if(dentro){Matcher m=altura.matcher(s);if(m.find())elevacion=Double.parseDouble(m.group(1));}
            if(termino&&s.startsWith("YEAR,")){cabecera=i;break;}
        }
        if(!Double.isFinite(elevacion)||cabecera<0)throw new IllegalArgumentException("Cabecera NASA POWER incompleta");
        String[] columnas=lineas[cabecera].split(",",-1);
        int yi=indice(columnas,"YEAR"), doy=indiceOpcional(columnas,"DOY"), mi=doy<0?indice(columnas,"MO"):-1, di=doy<0?indice(columnas,"DY"):-1, pi=indice(columnas,"PRECTOTCORR"), ti=indice(columnas,"T2M_MIN");
        List<Dia> dias=new ArrayList<>();Set<LocalDate> fechas=new HashSet<>();
        for(int i=cabecera+1;i<lineas.length;i++){
            String s=lineas[i].trim();if(s.isEmpty())continue;
            String[] a=s.split(",",-1);if(a.length<columnas.length)continue;
            try{
                LocalDate fecha=doy>=0?LocalDate.ofYearDay(Integer.parseInt(a[yi].trim()),Integer.parseInt(a[doy].trim())):LocalDate.of(Integer.parseInt(a[yi].trim()),Integer.parseInt(a[mi].trim()),Integer.parseInt(a[di].trim()));
                if(!fechas.add(fecha))throw new IllegalArgumentException("Fecha repetida en CSV NASA POWER: "+fecha);
                double lluvia=valor(a[pi]),tmin=valor(a[ti]);
                if(Double.isFinite(lluvia)&&lluvia<0)throw new IllegalArgumentException("Lluvia negativa: "+fecha);
                dias.add(new Dia(fecha,lluvia,tmin));
            }catch(NumberFormatException | java.time.DateTimeException e){throw new IllegalArgumentException("Fila CSV inválida: "+s,e);}
        }
        return new Datos(elevacion,dias);
    }
    private static int indiceOpcional(String[] c,String valor){for(int i=0;i<c.length;i++)if(c[i].trim().equals(valor))return i;return -1;}
    private static int indice(String[] c,String valor){for(int i=0;i<c.length;i++)if(c[i].trim().equals(valor))return i;throw new IllegalArgumentException("Falta columna "+valor);}
    private static double valor(String s){
        double d=Double.parseDouble(s.trim());
        if(!Double.isFinite(d))throw new IllegalArgumentException("Valor no finito en CSV NASA POWER");
        return d==-999?Double.NaN:d;
    }
}
