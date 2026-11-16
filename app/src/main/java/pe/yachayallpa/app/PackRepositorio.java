package pe.yachayallpa.app;
import android.content.*;
import android.net.Uri;
import pe.yachayallpa.core.*;
import java.io.*;
import java.util.*;

public final class PackRepositorio {
    // Llave pública de distribución. La privada jamás se incluye en el APK.
    public static final String PUBLICA="MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE9nXcqevKijnVTJcVqdp+8KxAs19Ykuofy78LjXnVTdTYgxzB3AwxxyWIer973M6MMaCY6+F8wTs0zlURceE6Cg==";
    private final Context contexto;
    private final PackVerificador verificador;
    private final android.content.SharedPreferences prefs;
    public PackRepositorio(Context contexto){this.contexto=contexto;verificador=new PackVerificador(PUBLICA);prefs=contexto.getSharedPreferences("versiones",Context.MODE_PRIVATE);}
    public Map<String,Pack> cargar() throws Exception {
        Map<String,Pack> resultado=new HashMap<>();
        for(String nombre:contexto.getAssets().list("packs"))if(nombre.endsWith(".pack")){
            try(InputStream in=contexto.getAssets().open("packs/"+nombre)){Pack p=verificador.verificar(leer(in),0);if(p.version>=version(nombreSeguro(p.distrito)))agregar(resultado,p);}
        }
        File dir=new File(contexto.getFilesDir(),"packs");File[] archivos=dir.listFiles();
        if(archivos!=null)for(File f:archivos)if(f.getName().endsWith(".pack")){
            try(InputStream in=new FileInputStream(f)){Pack p=verificador.verificar(leer(in),0);if(p.version>=version(nombreSeguro(p.distrito)))agregar(resultado,p);}
        }
        return resultado;
    }
    private int version(String nombre){return prefs.getInt(nombre,0);}
    private void agregar(Map<String,Pack> m,Pack p){Pack anterior=m.get(p.distrito);if(anterior==null||p.version>anterior.version)m.put(p.distrito,p);}
    public Pack importar(Uri uri) throws Exception {
        byte[] bytes;try(InputStream in=contexto.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("No se pudo abrir el archivo");bytes=leer(in);}
        // Se verifica antes de tocar el almacenamiento privado.
        Pack p=verificador.verificar(bytes,0);
        String nombre=nombreSeguro(p.distrito);int instalada=prefs.getInt(nombre,0);
        if(p.version<instalada)throw new PackInvalidoException(PackInvalidoException.Motivo.VERSION_ANTIGUA,"Versión antigua");
        File dir=new File(contexto.getFilesDir(),"packs");if(!dir.exists()&&!dir.mkdirs())throw new IOException("No se pudo crear la carpeta");
        File temporal=new File(dir,nombre+".tmp"),destino=new File(dir,nombre);
        try(FileOutputStream out=new FileOutputStream(temporal)){out.write(bytes);out.getFD().sync();}
        if(!temporal.renameTo(destino))throw new IOException("No se pudo instalar el pack");
        if(!prefs.edit().putInt(nombre,p.version).commit())throw new IOException("No se pudo guardar la versión");
        return p;
    }
    private static String nombreSeguro(String distrito){String s=distrito.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","_").replaceAll("_+","_");return s+".pack";}
    private static byte[] leer(InputStream in)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
        while((n=in.read(buffer))!=-1){if(out.size()+n>65536)throw new IOException("Pack supera 64 KB");out.write(buffer,0,n);}return out.toByteArray();
    }
}
