package pe.yachayallpa.app;

import android.content.Context;
import android.net.Uri;
import pe.yachayallpa.core.PackInvalidoException;
import pe.yachayallpa.core.PowerActualizador;
import pe.yachayallpa.core.Temporada;
import pe.yachayallpa.core.TemporadaFormato;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Los datos de fábrica e importados pasan por la misma llave pública y límite de tamaño. */
public final class TemporadaRepositorio {
    private final Context contexto;
    private final android.content.SharedPreferences versiones;
    public TemporadaRepositorio(Context contexto) {
        this.contexto = contexto;
        versiones = contexto.getSharedPreferences("temporadas_versiones", Context.MODE_PRIVATE);
    }
    public Map<String, Temporada> cargar() throws Exception {
        Map<String, Temporada> r = new HashMap<>();
        String[] nombres = contexto.getAssets().list("temporadas");
        if (nombres != null) for (String n : nombres) if (n.endsWith(".season")) {
            try (InputStream in = contexto.getAssets().open("temporadas/" + n)) {
                Temporada t=verificarFirma(leer(in));
                if(t.version>=versiones.getInt(nombre(t.distrito),0))agregar(r,t);
            }
        }
        File carpeta = new File(contexto.getFilesDir(), "temporadas");
        File[] archivos = carpeta.listFiles();
        if (archivos != null) for (File f : archivos) if (f.getName().endsWith(".season")) {
            try (InputStream in = new FileInputStream(f)) {
                Temporada t=verificarFirma(leer(in));
                if(t.version>=versiones.getInt(nombre(t.distrito),0))agregar(r,t);
            }
        }
        // El CSV de NASA viaja por HTTPS y queda separado de los escenarios firmados.
        // Si el caché falla, la app conserva el último escenario válido del APK.
        for (Temporada firmada : new HashMap<>(r).values()) {
            File cache = archivoCsv(firmada);
            if (!cache.isFile()) continue;
            try (InputStream in = new FileInputStream(cache)) {
                Temporada actual = PowerActualizador.actualizar(firmada,
                        new String(leerLimitado(in, 2_000_000), StandardCharsets.UTF_8), LocalDate.now());
                r.put(firmada.distrito, actual);
            } catch (Exception invalido) {
                cache.delete();
            }
        }
        return r;
    }
    private Temporada verificarFirma(byte[] bytes) throws PackInvalidoException {
        return TemporadaFormato.verificar(bytes, PackRepositorio.PUBLICA, 0);
    }
    private static void agregar(Map<String, Temporada> r, Temporada t) {
        Temporada vieja = r.get(t.distrito);
        if (vieja == null || t.version > vieja.version) r.put(t.distrito, t);
    }
    public Temporada importar(Uri uri) throws Exception {
        byte[] bytes;
        try (InputStream in = contexto.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("No se pudo abrir la temporada");
            bytes = leer(in);
        }
        Temporada t = verificarFirma(bytes);
        if(t.version<versiones.getInt(nombre(t.distrito),0))
            throw new PackInvalidoException(PackInvalidoException.Motivo.VERSION_ANTIGUA,
                    "Temporada anterior a la instalada");
        File carpeta = new File(contexto.getFilesDir(), "temporadas");
        if (!carpeta.exists() && !carpeta.mkdirs()) throw new IOException("No se pudo crear carpeta");
        String nombre = nombre(t.distrito);
        File temporal = new File(carpeta, nombre + ".tmp"), finalArchivo = new File(carpeta, nombre);
        try (FileOutputStream out = new FileOutputStream(temporal)) { out.write(bytes); out.getFD().sync(); }
        if (!temporal.renameTo(finalArchivo)) throw new IOException("No se pudo guardar temporada");
        if (!versiones.edit().putInt(nombre, t.version).commit())
            throw new IOException("No se pudo guardar la versión");
        return t;
    }
    public Temporada guardarNasa(Temporada firmada, byte[] csv, LocalDate hoy) throws Exception {
        if (csv == null || csv.length > 2_000_000)
            throw new IOException("Respuesta NASA demasiado grande");
        Temporada actual = PowerActualizador.actualizar(firmada,
                new String(csv, StandardCharsets.UTF_8), hoy);
        File destino = archivoCsv(firmada);
        File carpeta = destino.getParentFile();
        if (!carpeta.exists() && !carpeta.mkdirs()) throw new IOException("No se pudo crear caché NASA");
        File temporal = new File(carpeta, destino.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temporal)) {
            out.write(csv); out.getFD().sync();
        }
        if (!temporal.renameTo(destino)) throw new IOException("No se pudo guardar lluvia NASA");
        return actual;
    }
    private File archivoCsv(Temporada t) {
        return new File(new File(contexto.getFilesDir(), "nasa_csv"), nombre(t.distrito) + ".csv");
    }
    private static String nombre(String distrito) {
        return distrito.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_") + ".season";
    }
    private static byte[] leer(InputStream in) throws IOException {
        return leerLimitado(in, 65536);
    }
    private static byte[] leerLimitado(InputStream in, int limite) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream(); byte[] bloque = new byte[4096]; int n;
        while ((n = in.read(bloque)) != -1) {
            if (b.size() + n > limite) throw new IOException("Archivo supera el límite permitido");
            b.write(bloque, 0, n);
        }
        return b.toByteArray();
    }
}
