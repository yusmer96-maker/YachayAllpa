package pe.yachayallpa.pipeline;

import pe.yachayallpa.core.PowerActualizador;
import pe.yachayallpa.core.Temporada;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Convierte el CSV NASA diario en escenarios compactos que caben en un pack offline. */
public final class AnalizadorPower {
    private static final String PARAMETROS = "PRECTOTCORR,T2M_MIN,T2M_MAX,T2M,T2MDEW,WS2M,PS,ALLSKY_SFC_SW_DWN";

    private static final class Dia {
        final double lluvia, tmin, eto, tmedia;
        Dia(double lluvia, double tmin, double eto, double tmedia) {
            this.lluvia = lluvia; this.tmin = tmin; this.eto = eto; this.tmedia = tmedia;
        }
    }

    public static String url(double lat, double lon, LocalDate hasta) {
        return String.format(Locale.ROOT,
                "https://power.larc.nasa.gov/api/temporal/daily/point?parameters=%s&community=AG&longitude=%.5f&latitude=%.5f&start=20110101&end=%s&format=CSV",
                PARAMETROS, lon, lat, hasta.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
    }

    public Temporada preparar(String csv, Distrito distrito, String url, int version,
                              LocalDate hoy) {
        NasaPower.Datos base = NasaPower.parsear(csv);
        Map<LocalDate, Dia> dias = leerDias(csv, distrito.lat, base);
        // Lo reciente se corta donde acaba MERRA-2: después viene GEOS-IT, que no
        // es comparable con la historia (ver PowerActualizador.confiableHasta).
        LocalDate limite = PowerActualizador.confiableHasta(hoy);
        dias.keySet().removeIf(f -> f.isAfter(limite));
        LocalDate corte = null;
        for (Map.Entry<LocalDate, Dia> e : dias.entrySet()) {
            Dia d = e.getValue();
            if (Double.isFinite(d.lluvia) && Double.isFinite(d.tmin)
                    && (corte == null || e.getKey().isAfter(corte))) corte = e.getKey();
        }
        if (corte == null) throw new IllegalArgumentException("NASA no entregó días recientes");
        int anioTemporada = corte.getMonthValue() >= 9 ? corte.getYear() : corte.getYear() - 1;
        LocalDate inicio = LocalDate.of(anioTemporada, 9, 1);
        if (ChronoUnit.DAYS.between(inicio, corte) < 29)
            throw new IllegalArgumentException("Aún no hay 30 días de la temporada actual");

        double[] lluvia = new double[Temporada.ESCENARIOS * Temporada.BLOQUES];
        double[] eto = new double[lluvia.length];
        double[] tmin = new double[lluvia.length];
        double[] desv = new double[lluvia.length];
        double[] tmed = new double[lluvia.length];
        for (int escenario = 0; escenario < Temporada.ESCENARIOS; escenario++) {
            int anio = Temporada.PRIMER_ANO + escenario;
            for (int bloque = 0; bloque < Temporada.BLOQUES; bloque++) {
                int mes = (bloque / 3 + 8) % 12 + 1;
                int anoReal = mes >= 9 ? anio : anio + 1;
                int diaInicial = (bloque % 3) * 10 + 1;
                int diaFinal = Math.min(diaInicial + 9, YearMonth.of(anoReal, mes).lengthOfMonth());
                if (bloque % 3 == 2) diaFinal = YearMonth.of(anoReal, mes).lengthOfMonth();
                int total = diaFinal - diaInicial + 1;
                double sumaLluvia = 0, sumaEto = 0, sumaTmin = 0, sumaCuadrados = 0, sumaTmed = 0;
                int validos = 0;
                for (int dia = diaInicial; dia <= diaFinal; dia++) {
                    Dia d = dias.get(LocalDate.of(anoReal, mes, dia));
                    if (d == null || !Double.isFinite(d.lluvia) || !Double.isFinite(d.tmin)
                            || !Double.isFinite(d.eto) || !Double.isFinite(d.tmedia)) continue;
                    sumaLluvia += d.lluvia;
                    sumaEto += d.eto;
                    sumaTmin += d.tmin;
                    sumaCuadrados += d.tmin * d.tmin;
                    sumaTmed += d.tmedia;
                    validos++;
                }
                if (validos < Math.ceil(total * .7))
                    throw new IllegalArgumentException("Datos insuficientes en " + anoReal + "-" + mes + ", bloque " + bloque);
                int indice = escenario * Temporada.BLOQUES + bloque;
                lluvia[indice] = sumaLluvia * total / validos;
                eto[indice] = sumaEto * total / validos;
                tmin[indice] = sumaTmin / validos;
                tmed[indice] = sumaTmed / validos;
                desv[indice] = Math.sqrt(Math.max(0, sumaCuadrados / validos - tmin[indice] * tmin[indice]));
            }
        }

        double[] semanasActual = new double[6], semanasHabitual = new double[6];
        for (int i = 0; i < 6; i++) {
            LocalDate fin = corte.minusDays(i * 7L), desde = fin.minusDays(6);
            semanasActual[5 - i] = lluviaExacta(dias, desde, fin);
            semanasHabitual[5 - i] = promedioHistorico(dias, desde, fin);
        }
        return new Temporada(version, distrito.nombre, corte, distrito.lat, distrito.lon,
                base.elevacionCelda, "NASA POWER PRECTOTCORR (MERRA-2)", url,
                lluviaExacta(dias, corte.minusDays(6), corte),
                lluviaExacta(dias, corte.minusDays(14), corte),
                lluviaExacta(dias, corte.minusDays(29), corte),
                promedioHistorico(dias, corte.minusDays(29), corte),
                lluvia, eto, tmin, desv, tmed, semanasActual, semanasHabitual);
    }

    private static double lluviaExacta(Map<LocalDate, Dia> datos, LocalDate desde, LocalDate hasta) {
        double total = 0;
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            Dia valor = datos.get(d);
            if (valor == null || !Double.isFinite(valor.lluvia))
                throw new IllegalArgumentException("Falta lluvia del " + d);
            total += valor.lluvia;
        }
        return total;
    }

    private static double promedioHistorico(Map<LocalDate, Dia> datos, LocalDate desde, LocalDate hasta) {
        double suma = 0;
        int anos = 0;
        for (int anio = Temporada.PRIMER_ANO; anio <= Temporada.ULTIMO_ANO; anio++) {
            LocalDate fechaInicial = moverAlEscenario(desde, anio);
            LocalDate fechaFinal = fechaInicial.plusDays(ChronoUnit.DAYS.between(desde, hasta));
            try {
                suma += lluviaExacta(datos, fechaInicial, fechaFinal);
                anos++;
            } catch (IllegalArgumentException ignorada) {
                // Un año con faltantes no debe sesgar el promedio hacia cero.
            }
        }
        if (anos < 10) throw new IllegalArgumentException("Menos de 10 años para ventana histórica");
        return suma / anos;
    }

    private static LocalDate moverAlEscenario(LocalDate fecha, int anioTemporada) {
        int anio = fecha.getMonthValue() >= 9 ? anioTemporada : anioTemporada + 1;
        return LocalDate.of(anio, fecha.getMonthValue(), fecha.getDayOfMonth());
    }

    private static Map<LocalDate, Dia> leerDias(String csv, double lat, NasaPower.Datos base) {
        String[] lineas = csv.replace("\r", "").split("\n");
        int cabecera = -1;
        for (int i = 0; i < lineas.length; i++) {
            if (lineas[i].startsWith("YEAR,")) { cabecera = i; break; }
        }
        if (cabecera < 0) throw new IllegalArgumentException("CSV sin columnas");
        String[] columnas = lineas[cabecera].split(",", -1);
        int yi = indice(columnas, "YEAR"), di = indice(columnas, "DOY");
        int pi = indice(columnas, "PRECTOTCORR"), ti = indice(columnas, "T2M_MIN");
        int maxi = indice(columnas, "T2M_MAX"), mediai = indice(columnas, "T2M");
        int dewi = indice(columnas, "T2MDEW"), vi = indice(columnas, "WS2M");
        int presi = indice(columnas, "PS"), radi = indice(columnas, "ALLSKY_SFC_SW_DWN");
        Map<LocalDate, Dia> resultado = new HashMap<>();
        for (int i = cabecera + 1; i < lineas.length; i++) {
            if (lineas[i].isBlank()) continue;
            String[] v = lineas[i].split(",", -1);
            if (v.length != columnas.length) throw new IllegalArgumentException("Fila incompleta " + i);
            LocalDate fecha = LocalDate.ofYearDay(Integer.parseInt(v[yi]), Integer.parseInt(v[di]));
            double lluvia = valor(v[pi]), tmin = valor(v[ti]);
            double tmax = valor(v[maxi]), media = valor(v[mediai]), dew = valor(v[dewi]);
            double viento = valor(v[vi]), presion = valor(v[presi]), radiacion = valor(v[radi]);
            double eto = Double.NaN;
            if (Double.isFinite(tmin) && Double.isFinite(tmax) && Double.isFinite(media)
                    && Double.isFinite(dew) && Double.isFinite(viento)
                    && Double.isFinite(presion) && Double.isFinite(radiacion)) {
                eto = etoFao56(fecha, lat, base.elevacionCelda, tmin, tmax, media,
                        dew, viento, presion, radiacion);
            }
            if (resultado.putIfAbsent(fecha, new Dia(lluvia, tmin, eto, media)) != null)
                throw new IllegalArgumentException("Día repetido: " + fecha);
        }
        return resultado;
    }

    private static int indice(String[] columnas, String nombre) {
        for (int i = 0; i < columnas.length; i++) if (columnas[i].equals(nombre)) return i;
        throw new IllegalArgumentException("Falta parámetro NASA " + nombre);
    }

    private static double valor(String texto) {
        double v = Double.parseDouble(texto.trim());
        if (!Double.isFinite(v)) throw new IllegalArgumentException("Número no finito");
        return v == -999 ? Double.NaN : v;
    }

    /** FAO-56 Penman-Monteith diario; ETo de la celda, no medición en suelo de la chacra. */
    static double etoFao56(LocalDate fecha, double lat, double elevacion,
                           double tmin, double tmax, double tmedia, double tdew,
                           double viento, double presion, double rs) {
        double phi = Math.toRadians(lat), j = fecha.getDayOfYear();
        double dr = 1 + .033 * Math.cos(2 * Math.PI * j / 365);
        double deltaSolar = .409 * Math.sin(2 * Math.PI * j / 365 - 1.39);
        double omega = Math.acos(Math.max(-1, Math.min(1, -Math.tan(phi) * Math.tan(deltaSolar))));
        double ra = 24 * 60 / Math.PI * .0820 * dr *
                (omega * Math.sin(phi) * Math.sin(deltaSolar)
                        + Math.cos(phi) * Math.cos(deltaSolar) * Math.sin(omega));
        double rso = (.75 + .00002 * elevacion) * ra;
        double ea = vapor(tdew), es = (vapor(tmin) + vapor(tmax)) / 2;
        double rnl = 4.903e-9 * (Math.pow(tmax + 273.16, 4) + Math.pow(tmin + 273.16, 4)) / 2
                * (.34 - .14 * Math.sqrt(Math.max(0, ea)))
                * (1.35 * Math.max(0, Math.min(1, rs / rso)) - .35);
        double rn = .77 * rs - rnl;
        double pendiente = 4098 * vapor(tmedia) / Math.pow(tmedia + 237.3, 2);
        double gamma = .000665 * presion;
        double numerador = .408 * pendiente * rn
                + gamma * 900 / (tmedia + 273) * viento * Math.max(0, es - ea);
        double denominador = pendiente + gamma * (1 + .34 * viento);
        double eto = Math.max(0, numerador / denominador);
        if (!Double.isFinite(eto) || eto > 25) throw new IllegalArgumentException("ETo fuera de rango: " + fecha);
        return eto;
    }

    private static double vapor(double t) {
        return .6108 * Math.exp(17.27 * t / (t + 237.3));
    }
}
