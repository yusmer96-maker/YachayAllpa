package pe.yachayallpa.core;

import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/** Formato independiente: una temporada puede actualizarse sin alterar el pack histórico. */
public final class TemporadaFormato {
    private static final int LIMITE = 65536;
    private TemporadaFormato() { }

    private static String limpio(String valor) {
        return valor.replace('\r', ' ').replace('\n', ' ');
    }

    private static String numero(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    private static String coordenada(double valor) {
        // Un centro POWER a 0,625° no puede expresarse fielmente con dos decimales.
        return String.format(Locale.ROOT, "%.5f", valor);
    }

    private static String serie(double[] valores) {
        StringJoiner j = new StringJoiner(",");
        for (double v : valores) j.add(numero(v));
        return j.toString();
    }

    public static byte[] cuerpo(Temporada t) {
        // Formato 2 añade la temperatura media por bloque (corrige la demanda por
        // altura). Un pack sin ella se sigue escribiendo y leyendo como formato 1.
        StringBuilder b = new StringBuilder(t.tieneTmedia() ? "YACHAY-TEMPORADA/2\n"
                : "YACHAY-TEMPORADA/1\n");
        b.append("version=").append(t.version).append('\n');
        b.append("distrito=").append(limpio(t.distrito)).append('\n');
        b.append("fechaCorte=").append(t.fechaCorte).append('\n');
        b.append("lat=").append(coordenada(t.lat)).append('\n');
        b.append("lon=").append(coordenada(t.lon)).append('\n');
        b.append("elevacionCelda=").append(numero(t.elevacionCelda)).append('\n');
        b.append("productoLluvia=").append(limpio(t.productoLluvia)).append('\n');
        b.append("fuente=").append(limpio(t.fuente)).append('\n');
        b.append("lluvia7=").append(numero(t.lluvia7)).append('\n');
        b.append("lluvia15=").append(numero(t.lluvia15)).append('\n');
        b.append("lluvia30=").append(numero(t.lluvia30)).append('\n');
        b.append("promedio30=").append(numero(t.promedio30)).append('\n');
        b.append("historico.lluvia=").append(serie(t.lluviasHistoricas())).append('\n');
        b.append("historico.eto=").append(serie(t.etosHistoricos())).append('\n');
        b.append("historico.tmin=").append(serie(t.tminsHistoricas())).append('\n');
        b.append("historico.desviacion=").append(serie(t.desviacionesHistoricas())).append('\n');
        if (t.tieneTmedia())
            b.append("historico.tmedia=").append(serie(t.tmediasHistoricas())).append('\n');
        b.append("grafico.actual=").append(serie(t.semanasActuales())).append('\n');
        b.append("grafico.habitual=").append(serie(t.semanasHabituales())).append('\n');
        return b.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] firmar(Temporada temporada, PrivateKey privada) throws Exception {
        byte[] cuerpo = cuerpo(temporada);
        Signature firma = Signature.getInstance("SHA256withECDSA");
        firma.initSign(privada);
        firma.update(cuerpo);
        byte[] pie = ("firma=" + Base64.getEncoder().encodeToString(firma.sign()) + "\n")
                .getBytes(StandardCharsets.US_ASCII);
        if (cuerpo.length + pie.length > LIMITE) throw new IllegalArgumentException("Temporada supera 64 KB");
        byte[] archivo = Arrays.copyOf(cuerpo, cuerpo.length + pie.length);
        System.arraycopy(pie, 0, archivo, cuerpo.length, pie.length);
        return archivo;
    }

    public static Temporada verificar(byte[] archivo, String publicaBase64, int versionInstalada)
            throws PackInvalidoException {
        if (archivo == null || archivo.length < 40 || archivo.length > LIMITE
                || archivo[archivo.length - 1] != '\n') {
            throw error(PackInvalidoException.Motivo.FORMATO, "Tamaño o terminación inválida");
        }
        byte[] marca = "\nfirma=".getBytes(StandardCharsets.US_ASCII);
        int inicio = -1;
        for (int i = 0; i <= archivo.length - marca.length; i++) {
            boolean coincide = true;
            for (int j = 0; j < marca.length; j++) {
                if (archivo[i + j] != marca[j]) { coincide = false; break; }
            }
            if (coincide) inicio = i;
        }
        if (inicio < 0) throw error(PackInvalidoException.Motivo.FORMATO, "Falta firma");
        // Incluye el salto anterior a firma=, que forma parte de los bytes firmados.
        byte[] cuerpo = Arrays.copyOfRange(archivo, 0, inicio + 1);
        String textoFirma = new String(archivo, inicio + marca.length,
                archivo.length - inicio - marca.length - 1, StandardCharsets.US_ASCII);
        if (textoFirma.indexOf('\n') >= 0 || textoFirma.indexOf('\r') >= 0)
            throw error(PackInvalidoException.Motivo.FORMATO, "Firma no final");
        try {
            byte[] bytesFirma = Base64.getDecoder().decode(textoFirma);
            PublicKey publica = KeyFactory.getInstance("EC").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(publicaBase64.trim())));
            AlgorithmParameters parametros = AlgorithmParameters.getInstance("EC");
            parametros.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec esperada = parametros.getParameterSpec(ECParameterSpec.class);
            ECParameterSpec actual = ((ECPublicKey) publica).getParams();
            if (!actual.getCurve().equals(esperada.getCurve())
                    || !actual.getGenerator().equals(esperada.getGenerator())
                    || !actual.getOrder().equals(esperada.getOrder())) {
                throw error(PackInvalidoException.Motivo.FIRMA, "Llave no P-256");
            }
            Signature firma = Signature.getInstance("SHA256withECDSA");
            firma.initVerify(publica);
            firma.update(cuerpo);
            if (!firma.verify(bytesFirma)) throw error(PackInvalidoException.Motivo.FIRMA, "Firma no coincide");
        } catch (PackInvalidoException e) {
            throw e;
        } catch (Exception e) {
            throw error(PackInvalidoException.Motivo.FIRMA, "Firma inválida");
        }
        // Solo después de autenticar el cuerpo se interpretan fecha, cifras y fuentes.
        Temporada t = interpretar(cuerpo);
        if (t.version < versionInstalada)
            throw error(PackInvalidoException.Motivo.VERSION_ANTIGUA, "Temporada anterior a la instalada");
        return t;
    }

    private static Temporada interpretar(byte[] cuerpo) throws PackInvalidoException {
        try {
            String texto = new String(cuerpo, StandardCharsets.UTF_8);
            if (!Arrays.equals(texto.getBytes(StandardCharsets.UTF_8), cuerpo))
                throw new IllegalArgumentException("UTF-8 inválido");
            String[] lineas = texto.split("\n", -1);
            boolean v2 = lineas[0].equals("YACHAY-TEMPORADA/2");
            if (!(v2 || lineas[0].equals("YACHAY-TEMPORADA/1")) || !lineas[lineas.length - 1].isEmpty())
                throw new IllegalArgumentException("Cabecera inválida");
            Map<String, String> m = new HashMap<>();
            for (int i = 1; i < lineas.length - 1; i++) {
                int igual = lineas[i].indexOf('=');
                if (igual < 1 || m.putIfAbsent(lineas[i].substring(0, igual),
                        lineas[i].substring(igual + 1)) != null)
                    throw new IllegalArgumentException("Clave repetida o vacía");
            }
            Set<String> claves = new HashSet<>(Arrays.asList("version", "distrito", "fechaCorte",
                    "lat", "lon", "elevacionCelda", "productoLluvia", "fuente", "lluvia7",
                    "lluvia15", "lluvia30", "promedio30", "historico.lluvia", "historico.eto",
                    "historico.tmin", "historico.desviacion", "grafico.actual", "grafico.habitual"));
            if (v2) claves.add("historico.tmedia");
            if (!m.keySet().equals(claves)) throw new IllegalArgumentException("Claves inválidas");
            Temporada t = new Temporada(Integer.parseInt(m.get("version")), m.get("distrito"),
                    LocalDate.parse(m.get("fechaCorte")), Double.parseDouble(m.get("lat")),
                    Double.parseDouble(m.get("lon")), Double.parseDouble(m.get("elevacionCelda")),
                    m.get("productoLluvia"), m.get("fuente"), Double.parseDouble(m.get("lluvia7")),
                    Double.parseDouble(m.get("lluvia15")), Double.parseDouble(m.get("lluvia30")),
                    Double.parseDouble(m.get("promedio30")), serie(m.get("historico.lluvia"), 540),
                    serie(m.get("historico.eto"), 540), serie(m.get("historico.tmin"), 540),
                    serie(m.get("historico.desviacion"), 540),
                    v2 ? serie(m.get("historico.tmedia"), 540) : null,
                    serie(m.get("grafico.actual"), 6), serie(m.get("grafico.habitual"), 6));
            if (!Arrays.equals(cuerpo(t), cuerpo)) throw new IllegalArgumentException("Formato no canónico");
            return t;
        } catch (RuntimeException e) {
            throw error(PackInvalidoException.Motivo.FORMATO, e.getMessage());
        }
    }

    private static double[] serie(String valor, int longitud) {
        String[] partes = valor.split(",", -1);
        if (partes.length != longitud) throw new IllegalArgumentException("Serie incompleta");
        double[] datos = new double[longitud];
        for (int i = 0; i < longitud; i++) datos[i] = Double.parseDouble(partes[i]);
        return datos;
    }

    private static PackInvalidoException error(PackInvalidoException.Motivo motivo, String texto) {
        return new PackInvalidoException(motivo, texto);
    }
}
