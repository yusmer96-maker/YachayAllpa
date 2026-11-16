package pe.yachayallpa.pipeline;
import pe.yachayallpa.core.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.PrivateKey;
import java.time.LocalDate;

public final class Main {
    public static void main(String[] args) throws Exception {
        if(args.length==2&&args[0].equals("llaves")){Firmador.crearLlaves(Path.of(args[1]));return;}
        if(args.length>=3&&args.length<=4&&args[0].equals("actualizar")) {
            PrivateKey llave=Firmador.leerPrivada(Path.of(args[1]));
            Path salida=Path.of(args[2]); Files.createDirectories(salida);
            Path locales=args.length==4?Path.of(args[3]):null;
            NasaPower nasa=new NasaPower(); AnalizadorPower analizador=new AnalizadorPower();
            LocalDate hasta=LocalDate.now().minusDays(2);
            int version=Integer.parseInt(LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
            for(Distrito d:Distrito.values()) {
                String nombre=d.name().toLowerCase(java.util.Locale.ROOT);
                String url=AnalizadorPower.url(d.lat,d.lon,hasta);
                String csv=obtener(nasa,locales,salida,nombre+"-2011-reciente.csv",url);
                Temporada temporada=analizador.preparar(csv,d,url,version,LocalDate.now());
                Files.write(salida.resolve(nombre+".season"),TemporadaFormato.firmar(temporada,llave));
                System.out.println(nombre+": datos hasta "+temporada.fechaCorte+"; 30 días "
                        +Math.round(temporada.lluvia30)+" mm frente a "
                        +Math.round(temporada.promedio30)+" mm habituales");
            }
            return;
        }
        if(args.length>=3&&args.length<=4&&args[0].equals("construir")){
            PrivateKey llave=Firmador.leerPrivada(Path.of(args[1]));Path salida=Path.of(args[2]);Files.createDirectories(salida);
            Path locales=args.length==4?Path.of(args[3]):null;NasaPower nasa=new NasaPower();Agregador agregador=new Agregador();
            for(Distrito d:Distrito.values()){
                if(d.name().startsWith("ZONA_"))continue; // El formato mensual antiguo conserva solo sus seis distritos.
                String nombre=d.name().toLowerCase(java.util.Locale.ROOT);
                String urlAntes=NasaPower.url(d.lat,d.lon,1991,2005),urlAhora=NasaPower.url(d.lat,d.lon,2011,2025);
                String csvAntes=obtener(nasa,locales,salida,nombre+"-1991-2005.csv",urlAntes);
                String csvAhora=obtener(nasa,locales,salida,nombre+"-2011-2025.csv",urlAhora);
                NasaPower.Datos a=NasaPower.parsear(csvAntes),b=NasaPower.parsear(csvAhora);
                if(Math.abs(a.elevacionCelda-b.elevacionCelda)>1)throw new IllegalArgumentException("Elevaciones de celda distintas");
                Pack pack=new Pack(Integer.parseInt(LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)),d.nombre,d.lat,d.lon,d.altitud,b.elevacionCelda,urlAntes+" | "+urlAhora,agregador.agregar(b,2011,2025),agregador.agregar(a,1991,2005));
                Files.write(salida.resolve(nombre+".pack"),Firmador.firmar(pack,llave));
                System.out.println("Creado "+nombre+".pack");
            }return;
        }
        System.err.println("Uso: llaves <dir> | construir <privada.key> <dirSalida> [dirCsvLocal] | actualizar <privada.key> <dirSalida> [dirCsvLocal]");System.exit(2);
    }
    private static String obtener(NasaPower nasa,Path locales,Path salida,String nombre,String url) throws Exception {
        String csv=locales==null?nasa.descargar(url):Files.readString(locales.resolve(nombre),StandardCharsets.UTF_8);
        Files.writeString(salida.resolve(nombre),csv,StandardCharsets.UTF_8);return csv;
    }
}
