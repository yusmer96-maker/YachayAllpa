package pe.yachayallpa.core;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Actualiza lluvia observada sin alterar las 15 temporadas históricas verificadas del APK. */
public final class PowerActualizador {
    private static final Pattern UBICACION = Pattern.compile(
            "(?i)Location:\\s*latitude\\s+([+-]?\\d+(?:\\.\\d+)?)\\s+longitude\\s+([+-]?\\d+(?:\\.\\d+)?)");

    private PowerActualizador() { }

    /**
     * Último día de lluvia NASA en que se confía para compararlo con la historia.
     *
     * NASA POWER sirve PRECTOTCORR de MERRA-2 (el reanálisis) hasta el último mes
     * publicado y, después, rellena con GEOS-IT casi en tiempo real; el CSV no lo
     * distingue y su cabecera dice "MERRA-2" para todo. En Huánuco GEOS-IT moja de
     * más: en la celda de zona_03 septiembre de 2026 (MERRA-2) dio 9 mm con 2 días
     * de lluvia y octubre (GEOS-IT) 431 mm con lluvia los 31 días; el salto cae el
     * 1-oct en las 23 celdas a la vez. La historia de la app es toda MERRA-2, así
     * que lo reciente solo se puede comparar con ella hasta donde llega MERRA-2.
     *
     * Regla única, la usan el pipeline y el teléfono: se corta al final del mes de
     * hace dos meses, que MERRA-2 ya tiene publicado (sale unas tres semanas
     * después de cerrar cada mes). El 15-nov corta el 30-sep.
     */
    public static LocalDate confiableHasta(LocalDate hoy) {
        return hoy.withDayOfMonth(1).minusMonths(1).minusDays(1);
    }

    public static String url(Temporada base, LocalDate hasta) {
        return String.format(Locale.ROOT,
                "https://power.larc.nasa.gov/api/temporal/daily/point?parameters=PRECTOTCORR&community=AG&longitude=%.5f&latitude=%.5f&start=20110101&end=%s&format=CSV",
                base.lon, base.lat, hasta.format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    public static Temporada actualizar(Temporada base, String csv, LocalDate hoy) {
        if (base == null || csv == null || hoy == null || csv.length() > 2_000_000)
            throw new IllegalArgumentException("CSV NASA ausente o demasiado grande");
        String[] lineas = csv.replace("\r", "").split("\n");
        boolean inicio = false, fin = false;
        int encabezado = -1;
        double lat = Double.NaN, lon = Double.NaN;
        for (int i = 0; i < lineas.length; i++) {
            String fila = lineas[i].trim();
            if (fila.equals("-BEGIN HEADER-")) { inicio = true; continue; }
            if (fila.equals("-END HEADER-")) { fin = inicio; continue; }
            if (inicio && !fin) {
                Matcher m = UBICACION.matcher(fila);
                if (m.find()) { lat = Double.parseDouble(m.group(1)); lon = Double.parseDouble(m.group(2)); }
            }
            if (fin && fila.startsWith("YEAR,")) { encabezado = i; break; }
        }
        // Pedimos el centro de una celda fija, nunca el GPS exacto de la chacra.
        if (encabezado < 0 || !Double.isFinite(lat) || !Double.isFinite(lon)
                || Math.abs(lat - base.lat) > .001 || Math.abs(lon - base.lon) > .001)
            throw new IllegalArgumentException("Respuesta NASA de otra zona o sin cabecera");
        String[] columnas = lineas[encabezado].split(",", -1);
        int yi = indice(columnas, "YEAR"), pi = indice(columnas, "PRECTOTCORR");
        int doy = indiceOpcional(columnas, "DOY");
        int mi = doy < 0 ? indice(columnas, "MO") : -1;
        int di = doy < 0 ? indice(columnas, "DY") : -1;
        Map<LocalDate, Double> lluvia = new HashMap<>();
        LocalDate corte = null;
        LocalDate limite = confiableHasta(hoy);
        for (int i = encabezado + 1; i < lineas.length; i++) {
            if (lineas[i].isBlank()) continue;
            String[] datos = lineas[i].split(",", -1);
            if (datos.length != columnas.length) throw new IllegalArgumentException("Fila NASA incompleta");
            try {
                LocalDate fecha = doy >= 0
                        ? LocalDate.ofYearDay(Integer.parseInt(datos[yi]), Integer.parseInt(datos[doy]))
                        : LocalDate.of(Integer.parseInt(datos[yi]), Integer.parseInt(datos[mi]),
                                Integer.parseInt(datos[di]));
                double mm = Double.parseDouble(datos[pi]);
                if (!Double.isFinite(mm) || mm < 0 && mm != -999)
                    throw new IllegalArgumentException("Lluvia NASA inválida");
                // Lo posterior al límite es GEOS-IT: ni se guarda ni cuenta.
                if (mm != -999 && !fecha.isAfter(limite)) {
                    if (lluvia.putIfAbsent(fecha, mm) != null)
                        throw new IllegalArgumentException("Fecha NASA repetida");
                    if (corte == null || fecha.isAfter(corte)) corte = fecha;
                }
            } catch (DateTimeException | NumberFormatException e) {
                throw new IllegalArgumentException("Fecha o valor NASA inválido", e);
            }
        }
        if (corte == null || ChronoUnit.DAYS.between(corte, limite) > 7)
            throw new IllegalArgumentException("NASA no entregó lluvia hasta " + limite);
        if (corte.isBefore(base.fechaCorte))
            throw new IllegalArgumentException("Respuesta NASA anterior a la instalada");
        double[] actual = new double[6], habitual = new double[6];
        for (int i = 0; i < 6; i++) {
            LocalDate finSemana = corte.minusDays(i * 7L);
            LocalDate inicioSemana = finSemana.minusDays(6);
            actual[5 - i] = suma(lluvia, inicioSemana, finSemana);
            habitual[5 - i] = promedio(lluvia, inicioSemana, finSemana);
        }
        int version = Math.max(base.version,
                Integer.parseInt(corte.format(DateTimeFormatter.BASIC_ISO_DATE)));
        return new Temporada(version, base.distrito, corte, base.lat, base.lon,
                base.elevacionCelda, "NASA POWER PRECTOTCORR (HTTPS)", url(base, hoy),
                suma(lluvia, corte.minusDays(6), corte),
                suma(lluvia, corte.minusDays(14), corte),
                suma(lluvia, corte.minusDays(29), corte),
                promedio(lluvia, corte.minusDays(29), corte),
                base.lluviasHistoricas(), base.etosHistoricos(),
                base.tminsHistoricas(), base.desviacionesHistoricas(), base.tmediasHistoricas(),
                actual, habitual);
    }

    private static double suma(Map<LocalDate, Double> lluvia, LocalDate desde, LocalDate hasta) {
        double total = 0;
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            Double mm = lluvia.get(fecha);
            if (mm == null) throw new IllegalArgumentException("Falta lluvia NASA del " + fecha);
            total += mm;
        }
        return total;
    }

    private static double promedio(Map<LocalDate, Double> lluvia, LocalDate desde, LocalDate hasta) {
        double total = 0;
        int validos = 0;
        for (int ano = Temporada.PRIMER_ANO; ano <= Temporada.ULTIMO_ANO; ano++) {
            try {
                LocalDate d = LocalDate.of(ano, desde.getMonthValue(), desde.getDayOfMonth());
                if (desde.getMonthValue() <= 8) d = d.plusYears(1);
                LocalDate h = d.plusDays(ChronoUnit.DAYS.between(desde, hasta));
                total += suma(lluvia, d, h);
                validos++;
            } catch (DateTimeException | IllegalArgumentException ignorado) {
                // Un año con día faltante no debe reducir falsamente el promedio.
            }
        }
        if (validos < 10) throw new IllegalArgumentException("Años insuficientes para comparar lluvia");
        return total / validos;
    }

    private static int indice(String[] columnas, String nombre) {
        int i = indiceOpcional(columnas, nombre);
        if (i < 0) throw new IllegalArgumentException("Falta columna NASA " + nombre);
        return i;
    }

    private static int indiceOpcional(String[] columnas, String nombre) {
        for (int i = 0; i < columnas.length; i++)
            if (columnas[i].trim().equals(nombre)) return i;
        return -1;
    }
}
