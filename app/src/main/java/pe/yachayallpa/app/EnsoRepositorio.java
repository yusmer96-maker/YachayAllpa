package pe.yachayallpa.app;

import android.content.Context;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import pe.yachayallpa.core.Enso;

/**
 * Lee el estado de El Nino / La Nina del fichero `assets/enso.json` que viaja en
 * el APK (fuente NOAA CPC, con su fecha). Es un dato global, sin coordenadas.
 *
 * Mas adelante, igual que la lluvia, se puede refrescar por HTTPS desde el VPS
 * una vez al dia y guardar en almacenamiento privado; mientras tanto, el valor
 * del paquete sirve y la tarjeta muestra su fecha para que se vea si esta al dia.
 */
public final class EnsoRepositorio {
    private EnsoRepositorio() {}

    public static Enso cargar(Context contexto) {
        try (InputStream in = contexto.getAssets().open("enso.json")) {
            byte[] bruto = in.readAllBytes();
            JSONObject o = new JSONObject(new String(bruto, StandardCharsets.UTF_8));
            Enso.Fase fase;
            switch (o.optString("fase", "NEUTRAL")) {
                case "EL_NINO": fase = Enso.Fase.EL_NINO; break;
                case "LA_NINA": fase = Enso.Fase.LA_NINA; break;
                default: fase = Enso.Fase.NEUTRAL;
            }
            double oni = o.optDouble("oni", 0.0);
            String temporada = o.optString("temporada", "");
            String alerta = o.optString("alerta", "");
            int tendencia = o.optInt("tendencia", 0);
            LocalDate fecha;
            try { fecha = LocalDate.parse(o.optString("fecha")); }
            catch (Exception e) { fecha = null; }
            String fuente = o.optString("fuente", "NOAA CPC");
            return new Enso(fase, oni, temporada, alerta, tendencia, fecha, fuente);
        } catch (Exception e) {
            return null; // Sin ENSO la app sigue igual; la tarjeta de campana no se muestra.
        }
    }
}
