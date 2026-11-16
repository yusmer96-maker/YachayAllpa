package pe.yachayallpa.app;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Los índices que el VPS calcula a diario con NASA POWER desde 1981: humedad de
 * la zona de raíces en percentil, SPI de sequía y tendencias.
 *
 * El teléfono NO hace este cálculo: descarga un JSON de menos de medio kilobyte
 * cuando hay señal y lo guarda en almacenamiento privado. Sin señal usa la copia
 * guardada, y si tampoco la hay, la app sigue funcionando igual con el `.season`
 * firmado: esto añade contexto, no lo sustituye.
 *
 * No viene firmado, igual que la renovación directa de lluvia, así que se valida
 * por rango antes de usarlo y se mantiene separado de las series firmadas.
 */
public final class DatosZona {
    public final String zona;

    /** Lluvia medida por satélite hasta ayer (NASA GPM IMERG) con su normal; null si no vino. */
    public static final class Reciente {
        public final java.time.LocalDate hasta;
        public final String fuente;
        public final double lluvia3, lluvia7, lluvia15, lluvia30, normal30;
        public final double[] semanas, normales;
        Reciente(java.time.LocalDate hasta, String fuente, double l3, double l7, double l15, double l30,
                 double n30, double[] semanas, double[] normales) {
            this.hasta = hasta; this.fuente = fuente; this.lluvia3 = l3;
            this.lluvia7 = l7; this.lluvia15 = l15; this.lluvia30 = l30; this.normal30 = n30;
            this.semanas = semanas; this.normales = normales;
        }
    }
    public Reciente reciente;

    /**
     * La zona con la lluvia medida hasta ayer, si es fresca (de hace 10 días o menos)
     * y más nueva que la del pack. Si no, la zona tal cual.
     */
    public pe.yachayallpa.core.Temporada aplicar(pe.yachayallpa.core.Temporada t, java.time.LocalDate hoy) {
        Reciente r = reciente;
        if (r == null || r.hasta.isBefore(hoy.minusDays(10)) || !r.hasta.isAfter(t.fechaCorte)) return t;
        try {
            return t.conReciente(r.hasta, r.fuente, r.lluvia7, r.lluvia15, r.lluvia30, r.normal30,
                    r.semanas, r.normales);
        } catch (IllegalArgumentException e) {
            return t;
        }
    }

    /** Un día del pronóstico de 7 días (Open-Meteo vía VPS), a la altura de la celda. */
    public static final class Dia {
        public final java.time.LocalDate fecha;
        public final double lluvia, tmin, tmax;
        public final int prob;
        Dia(java.time.LocalDate fecha, double lluvia, int prob, double tmin, double tmax) {
            this.fecha = fecha; this.lluvia = lluvia; this.prob = prob;
            this.tmin = tmin; this.tmax = tmax;
        }
    }

    /** Vacío si el VPS no publicó pronóstico o no pasó la validación. */
    public java.util.List<Dia> pronostico = java.util.Collections.emptyList();
    public double elevacionPronostico = Double.NaN;
    public String fuentePronostico = "";

    private DatosZona(String zona) { this.zona = zona; }

    /** "Zona climática 07" -> "zona_07". Si no se reconoce, no hay descarga. */
    public static String idDe(String distrito) {
        if (distrito == null) return null;
        StringBuilder d = new StringBuilder();
        for (char c : distrito.toCharArray()) if (Character.isDigit(c)) d.append(c);
        if (d.length() == 0) return null;
        int n = Integer.parseInt(d.toString());
        if (n < 1 || n > 99) return null;
        return String.format("zona_%02d", n);
    }

    private static File fichero(Context c, String id) {
        File dir = new File(c.getFilesDir(), "indices");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, id + ".json");
    }

    /** Lo guardado de la última vez, o null. No toca la red. */
    public static DatosZona guardado(Context c, String id) {
        if (id == null) return null;
        File f = fichero(c, id);
        if (!f.exists()) return null;
        try (InputStream in = new java.io.FileInputStream(f)) {
            return desde(new String(in.readAllBytes(), StandardCharsets.UTF_8), id);
        } catch (Exception e) {
            return null;
        }
    }

    /** Descarga y guarda. Pensado para un hilo de fondo. */
    public static DatosZona renovar(Context c, String id) {
        if (id == null || ConfigDatos.BASE_DATOS.isEmpty()) return null;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(ConfigDatos.BASE_DATOS + id + ".json").openConnection();
            con.setConnectTimeout(ConfigDatos.ESPERA_MS);
            con.setReadTimeout(ConfigDatos.ESPERA_MS);
            con.setRequestProperty("Accept", "application/json");
            if (con.getResponseCode() != 200) return null;
            byte[] bruto;
            try (InputStream in = con.getInputStream()) {
                bruto = in.readAllBytes();
            }
            if (bruto.length > 8192) return null;           // esto es un JSON diminuto
            String texto = new String(bruto, StandardCharsets.UTF_8);
            DatosZona d = desde(texto, id);
            if (d == null) return null;
            File tmp = new File(fichero(c, id).getPath() + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmp)) { fos.write(bruto); }
            if (!tmp.renameTo(fichero(c, id))) tmp.delete();
            return d;
        } catch (Exception e) {
            return null;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    /** Valida por rango: sin firma, nada se usa sin comprobar que es plausible. */
    private static DatosZona desde(String texto, String id) {
        try {
            JSONObject o = new JSONObject(texto);
            if (!id.equals(o.optString("zona"))) return null;
            DatosZona d = new DatosZona(id);
            leerReciente(o.optJSONObject("reciente"), d);
            leerPronostico(o.optJSONObject("pronostico"), d);
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * La lluvia medida entra solo si todo es plausible: 0..2000 mm por semana o mes y
     * las 6 semanas completas. Una cifra rara la descarta entera y se usa el pack.
     */
    private static void leerReciente(JSONObject r, DatosZona d) {
        if (r == null) return;
        try {
            org.json.JSONArray s = r.getJSONArray("semanas");
            if (s.length() != 6) return;
            double[] mm = new double[6], normal = new double[6];
            for (int i = 0; i < 6; i++) {
                JSONObject x = s.getJSONObject(i);
                mm[i] = x.getDouble("mm");
                normal[i] = x.getDouble("normal");
                if (!(mm[i] >= 0 && mm[i] < 2000 && normal[i] >= 0 && normal[i] < 2000)) return;
            }
            double l7 = r.getDouble("lluvia7"), l15 = r.getDouble("lluvia15"),
                    l30 = r.getDouble("lluvia30"), n30 = r.getDouble("normal30");
            for (double v : new double[]{l7, l15, l30, n30}) if (!(v >= 0 && v < 5000)) return;
            double l3 = r.optDouble("lluvia3", Double.NaN);
            if (!(l3 >= 0 && l3 <= l7)) l3 = Double.NaN;
            d.reciente = new Reciente(java.time.LocalDate.parse(r.getString("hasta")),
                    r.optString("fuente", "NASA GPM IMERG"), l3, l7, l15, l30, n30, mm, normal);
        } catch (Exception ignorado) {
            // Sin lluvia medida reciente la app usa la del pack.
        }
    }

    /**
     * El pronóstico entra solo si cada día es plausible: lluvia 0..300 mm,
     * probabilidad 0..100, mínima -30..40 °C y la celda a una altura real. Un día
     * raro descarta el pronóstico entero: mejor sin pronóstico que con uno malo.
     */
    private static void leerPronostico(JSONObject p, DatosZona d) {
        if (p == null) return;
        try {
            double elev = p.getDouble("elevacion");
            org.json.JSONArray dias = p.getJSONArray("dias");
            if (elev < 0 || elev > 6500 || dias.length() < 5 || dias.length() > 16) return;
            java.util.List<Dia> lista = new java.util.ArrayList<>();
            for (int i = 0; i < dias.length(); i++) {
                JSONObject x = dias.getJSONObject(i);
                double lluvia = x.getDouble("lluvia"), tmin = x.getDouble("tmin");
                double tmax = x.optDouble("tmax", tmin);
                int prob = x.optInt("prob", -1);
                if (lluvia < 0 || lluvia > 300 || tmin < -30 || tmin > 40 || prob > 100) return;
                lista.add(new Dia(java.time.LocalDate.parse(x.getString("fecha")), lluvia, prob,
                        tmin, tmax));
            }
            d.pronostico = java.util.Collections.unmodifiableList(lista);
            d.elevacionPronostico = elev;
            d.fuentePronostico = p.optString("fuente", "Open-Meteo");
        } catch (Exception ignorado) {
            // Sin pronóstico la app sigue con la historia NASA.
        }
    }

    /**
     * Los 7 días desde hoy, para la recomendación. Null si el pronóstico es viejo
     * (no cubre al menos 5 días desde hoy): un pronóstico pasado no se usa.
     */
    public pe.yachayallpa.core.Decision.Pronostico paraDecision(java.time.LocalDate hoy) {
        double lluvia = 0, minima = Double.POSITIVE_INFINITY;
        java.time.LocalDate frio = null;
        java.util.List<Double> porDia = new java.util.ArrayList<>();
        for (Dia x : pronostico) {
            if (x.fecha.isBefore(hoy) || x.fecha.isAfter(hoy.plusDays(6))) continue;
            porDia.add(x.lluvia);
            lluvia += x.lluvia;
            if (x.tmin < minima) { minima = x.tmin; frio = x.fecha; }
        }
        if (porDia.size() < 5 || frio == null) return null;
        // Los 3 días seguidos más lluviosos de la semana (criterio de inicio de siembra).
        double tres = 0;
        for (int i = 0; i + 2 < porDia.size(); i++)
            tres = Math.max(tres, porDia.get(i) + porDia.get(i + 1) + porDia.get(i + 2));
        // Lo medido en los últimos 3 días, solo si el dato llega hasta anteayer o ayer.
        double medida = reciente != null && !reciente.hasta.isBefore(hoy.minusDays(2))
                ? reciente.lluvia3 : Double.NaN;
        return new pe.yachayallpa.core.Decision.Pronostico(lluvia, Math.min(tres, lluvia), medida,
                minima, frio, elevacionPronostico);
    }

    /** Los días desde hoy, para la gráfica. */
    public java.util.List<Dia> desde(java.time.LocalDate hoy) {
        java.util.List<Dia> r = new java.util.ArrayList<>();
        for (Dia x : pronostico) if (!x.fecha.isBefore(hoy)) r.add(x);
        return r;
    }
}
