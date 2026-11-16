package pe.yachayallpa.app;

import android.content.Context;
import pe.yachayallpa.core.Temporada;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Límites provinciales simplificados de geoBoundaries; identificación local sin geocodificador en red. */
public final class RegionHuanuco {
    private static final class Provincia {
        String nombre; double[] lon,lat; double centroLat,centroLon;
    }
    private final List<Provincia> provincias = new ArrayList<>();
    public RegionHuanuco(Context contexto) throws Exception {
        try(BufferedReader br=new BufferedReader(new InputStreamReader(
                contexto.getAssets().open("provincias.txt"),StandardCharsets.UTF_8))) {
            String linea;
            while((linea=br.readLine())!=null) {
                String[] partes=linea.split("\\|",2);
                String[] puntos=partes[1].split(";");
                Provincia p=new Provincia();p.nombre=partes[0];
                p.lon=new double[puntos.length];p.lat=new double[puntos.length];
                for(int i=0;i<puntos.length;i++) {
                    String[] xy=puntos[i].split(",");
                    p.lon[i]=Double.parseDouble(xy[0]);p.lat[i]=Double.parseDouble(xy[1]);
                    p.centroLon+=p.lon[i];p.centroLat+=p.lat[i];
                }
                p.centroLon/=puntos.length;p.centroLat/=puntos.length;
                provincias.add(p);
            }
        }
    }
    public List<String> nombres() {
        List<String> n=new ArrayList<>();for(Provincia p:provincias)n.add(p.nombre);
        n.sort(String::compareTo);return n;
    }
    public String provincia(double lat,double lon) {
        for(Provincia p:provincias)if(dentro(p,lat,lon))return p.nombre;
        return null;
    }
    /** Contorno de la provincia como {lat[], lon[]}, para dibujarlo sobre el relieve. */
    public double[][] contorno(String nombre) {
        for(Provincia p:provincias)if(p.nombre.equals(nombre))return new double[][]{p.lat.clone(),p.lon.clone()};
        return null;
    }
    public double[] centro(String nombre) {
        for(Provincia p:provincias)if(p.nombre.equals(nombre))return new double[]{p.centroLat,p.centroLon};
        throw new IllegalArgumentException("Provincia desconocida");
    }
    private static boolean dentro(Provincia p,double lat,double lon) {
        boolean si=false;
        for(int i=0,j=p.lon.length-1;i<p.lon.length;j=i++) {
            if((p.lat[i]>lat)!=(p.lat[j]>lat)
                    && lon<(p.lon[j]-p.lon[i])*(lat-p.lat[i])/(p.lat[j]-p.lat[i])+p.lon[i])si=!si;
        }
        return si;
    }
    public Temporada cercana(Map<String,Temporada> zonas,double lat,double lon) {
        Temporada mejor=null;double distancia=Double.POSITIVE_INFINITY;
        for(Temporada t:zonas.values()) {
            if(!t.distrito.startsWith("Zona climática"))continue;
            double dx=(t.lon-lon)*Math.cos(Math.toRadians(lat));
            double dy=t.lat-lat;
            double d=dx*dx+dy*dy;
            if(d<distancia){distancia=d;mejor=t;}
        }
        return mejor;
    }
}
