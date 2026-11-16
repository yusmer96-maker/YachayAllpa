package pe.yachayallpa.app;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.util.Log;
import pe.yachayallpa.core.PowerActualizador;
import pe.yachayallpa.core.Temporada;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import javax.net.ssl.HttpsURLConnection;

/** Renovación en segundo plano: la API ve el centro de la celda, jamás el GPS de la parcela. */
public final class ActualizacionNasa extends JobService {
    public static final String CAMBIO = "pe.yachayallpa.app.NASA_ACTUALIZADA";
    private static final int INMEDIATA = 23101, PERIODICA = 23102;
    private static final long DIA = 24L * 60 * 60 * 1000;
    private volatile boolean detenido;

    public static void programar(Context contexto, Temporada zona) {
        if (zona == null) return;
        try {
        JobScheduler gestor = (JobScheduler) contexto.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (gestor == null) return;
        PersistableBundle datos = new PersistableBundle();
        datos.putString("zona", zona.distrito);
        ComponentName servicio = new ComponentName(contexto, ActualizacionNasa.class);
        JobInfo anterior = gestor.getPendingJob(PERIODICA);
        String zonaAnterior = anterior == null ? null : anterior.getExtras().getString("zona");
        boolean otraZona = !zona.distrito.equals(zonaAnterior);
        if (otraZona) {
            gestor.schedule(new JobInfo.Builder(PERIODICA, servicio)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(DIA, 2L * 60 * 60 * 1000)
                    .setExtras(datos).build());
        }
        long ultimo = contexto.getSharedPreferences("nasa_sync", MODE_PRIVATE)
                .getLong(zona.distrito, 0);
        JobInfo pendiente = gestor.getPendingJob(INMEDIATA);
        boolean mismoPendiente = pendiente != null
                && zona.distrito.equals(pendiente.getExtras().getString("zona"));
        if ((otraZona || System.currentTimeMillis() - ultimo >= DIA) && !mismoPendiente) {
            gestor.schedule(new JobInfo.Builder(INMEDIATA, servicio)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(60L * 60 * 1000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                    .setExtras(datos).build());
        }
        } catch (RuntimeException error) {
            // Un fallo del planificador nunca debe impedir el consejo local.
            Log.w("YachayNASA", "No se pudo programar la actualización", error);
        }
    }

    @Override public boolean onStartJob(JobParameters parametros) {
        detenido = false;
        new Thread(() -> {
            boolean reintentar = false;
            try {
                synchronized (ActualizacionNasa.class) {
                String nombre = parametros.getExtras().getString("zona");
                long ultimaDescarga = getSharedPreferences("nasa_sync", MODE_PRIVATE)
                        .getLong(nombre, 0);
                long ahoraMs = System.currentTimeMillis();
                if (ultimaDescarga > 0 && ahoraMs >= ultimaDescarga
                        && ahoraMs - ultimaDescarga < 20L * 60 * 60 * 1000) return;
                TemporadaRepositorio repo = new TemporadaRepositorio(this);
                Temporada base = repo.cargar().get(nombre);
                if (base == null) throw new IllegalArgumentException("Zona NASA desconocida");
                LocalDate hoy = LocalDate.now();
                String direccion = PowerActualizador.url(base, hoy);
                HttpsURLConnection conexion = (HttpsURLConnection) new URL(direccion).openConnection();
                conexion.setConnectTimeout(15_000);
                conexion.setReadTimeout(30_000);
                conexion.setInstanceFollowRedirects(false);
                conexion.setRequestProperty("Accept", "text/csv");
                byte[] csv;
                try {
                    if (conexion.getResponseCode() != 200)
                        throw new IllegalStateException("NASA POWER HTTP " + conexion.getResponseCode());
                    try (InputStream entrada = conexion.getInputStream()) {
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        byte[] bloque = new byte[8192]; int n;
                        while ((n = entrada.read(bloque)) != -1) {
                            if (buffer.size() + n > 2_000_000)
                                throw new IllegalArgumentException("CSV NASA demasiado grande");
                            buffer.write(bloque, 0, n);
                        }
                        csv = buffer.toByteArray();
                    }
                } finally { conexion.disconnect(); }
                if (detenido) return;
                Temporada actual = repo.guardarNasa(base, csv, hoy);
                getSharedPreferences("nasa_sync", MODE_PRIVATE).edit()
                        .putLong(nombre, System.currentTimeMillis()).apply();
                Intent cambio = new Intent(CAMBIO);
                cambio.setPackage(getPackageName());
                cambio.putExtra("zona", actual.distrito);
                sendBroadcast(cambio);
                }
            } catch (Exception error) {
                reintentar = true;
            } finally {
                if (!detenido) jobFinished(parametros, reintentar);
            }
        }, "nasa-power-actualizar").start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters parametros) {
        detenido = true;
        return true;
    }
}
