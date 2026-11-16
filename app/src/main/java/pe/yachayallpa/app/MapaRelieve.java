package pe.yachayallpa.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapa de relieve de la zona de la chacra, dibujado en el teléfono con las
 * teselas Terrarium que ya viajan en el APK (zoom 9, unos 300 m por píxel).
 *
 * No pide nada a la red: la posición se usa solo en memoria para recortar el
 * relieve alrededor de la chacra. El sombreado ilumina desde el noroeste, como
 * los mapas de papel, y el color va por altura en tonos suaves (verde en el
 * valle, ocre en la ladera, pardo en la puna).
 */
public final class MapaRelieve {
    private static final int ZOOM = 9;
    private static final int N = 1 << ZOOM;
    private static final int TX0 = 146, TX1 = 150, TY0 = 267, TY1 = 271;

    /** El mapa ya dibujado y cómo pasar de lat/lon a sus píxeles. */
    public static final class Mapa {
        public final Bitmap imagen;
        private final double origenX, origenY, escala;
        Mapa(Bitmap imagen, double origenX, double origenY, double escala) {
            this.imagen = imagen; this.origenX = origenX; this.origenY = origenY; this.escala = escala;
        }
        /** Píxel {x, y} dentro de la imagen para una coordenada. */
        public float[] aPixel(double lat, double lon) {
            double[] g = global(lat, lon);
            return new float[]{(float) ((g[0] - origenX) * escala), (float) ((g[1] - origenY) * escala)};
        }
        /** Metros que mide un píxel de la imagen, para el círculo de precisión. */
        public double metrosPorPixel(double lat) {
            return 40075016.686 * Math.cos(Math.toRadians(lat)) / (256.0 * N) / escala;
        }
    }

    private MapaRelieve() { }

    private static double[] global(double lat, double lon) {
        double x = (lon + 180.0) / 360.0 * N * 256;
        double rad = Math.toRadians(lat);
        double y = (1.0 - Math.log(Math.tan(Math.PI / 4.0 + rad / 2.0)) / Math.PI) / 2.0 * N * 256;
        return new double[]{x, y};
    }

    /**
     * Dibuja `lado`×`lado` píxeles que cubren `radio` píxeles de tesela a cada
     * lado de la chacra (radio 56 ≈ 17 km). Null si la chacra cae fuera del
     * relieve incluido. Pensado para un hilo de fondo.
     */
    public static Mapa dibujar(Context contexto, double lat, double lon, int radio, int lado) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon)) return null;
        double[] c = global(lat, lon);
        double ox = c[0] - radio, oy = c[1] - radio;
        double escala = lado / (2.0 * radio);
        Map<String, int[]> teselas = new HashMap<>();
        float[] alt = new float[(lado + 2) * (lado + 2)];
        for (int j = -1; j <= lado; j++) {
            for (int i = -1; i <= lado; i++) {
                double gx = ox + i / escala, gy = oy + j / escala;
                float e = altura(contexto, teselas, gx, gy);
                if (Float.isNaN(e)) return null;
                alt[(j + 1) * (lado + 2) + (i + 1)] = e;
            }
        }
        double metros = 40075016.686 * Math.cos(Math.toRadians(lat)) / (256.0 * N) / escala;
        int[] px = new int[lado * lado];
        // Luz desde el noroeste (315°) a 45° de altura; relieve exagerado ×2 para que se lea.
        double az = Math.toRadians(315), zen = Math.toRadians(45);
        for (int j = 0; j < lado; j++) {
            for (int i = 0; i < lado; i++) {
                int k = (j + 1) * (lado + 2) + (i + 1);
                double dzdx = (alt[k + 1] - alt[k - 1]) / (2 * metros) * 2;
                double dzdy = (alt[k + lado + 2] - alt[k - lado - 2]) / (2 * metros) * 2;
                double pendiente = Math.atan(Math.hypot(dzdx, dzdy));
                double aspecto = Math.atan2(dzdy, -dzdx);
                double sombra = Math.cos(zen) * Math.cos(pendiente)
                        + Math.sin(zen) * Math.sin(pendiente) * Math.cos(az - Math.PI / 2 - aspecto);
                sombra = Math.max(0, Math.min(1, sombra));
                px[j * lado + i] = tinte(alt[k], .58 + .42 * sombra);
            }
        }
        Bitmap b = Bitmap.createBitmap(px, lado, lado, Bitmap.Config.ARGB_8888);
        return new Mapa(b, ox, oy, escala);
    }

    /** Altura bilineal en coordenadas globales de píxel a zoom 9; NaN fuera del relieve. */
    private static float altura(Context ctx, Map<String, int[]> cache, double gx, double gy) {
        int x0 = (int) Math.floor(gx), y0 = (int) Math.floor(gy);
        double fx = gx - x0, fy = gy - y0;
        float a = punto(ctx, cache, x0, y0), b = punto(ctx, cache, x0 + 1, y0);
        float c = punto(ctx, cache, x0, y0 + 1), d = punto(ctx, cache, x0 + 1, y0 + 1);
        if (Float.isNaN(a) || Float.isNaN(b) || Float.isNaN(c) || Float.isNaN(d)) return Float.NaN;
        return (float) ((a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy);
    }

    private static float punto(Context ctx, Map<String, int[]> cache, int gx, int gy) {
        int tx = Math.floorDiv(gx, 256), ty = Math.floorDiv(gy, 256);
        if (tx < TX0 || tx > TX1 || ty < TY0 || ty > TY1) return Float.NaN;
        String clave = tx + "_" + ty;
        int[] t = cache.get(clave);
        if (t == null) {
            try (InputStream in = ctx.getAssets().open("relieve/9_" + clave + ".png")) {
                Bitmap img = BitmapFactory.decodeStream(in);
                if (img == null || img.getWidth() != 256 || img.getHeight() != 256) return Float.NaN;
                t = new int[256 * 256];
                img.getPixels(t, 0, 256, 0, 0, 256, 256);
                img.recycle();
            } catch (Exception e) {
                return Float.NaN;
            }
            cache.put(clave, t);
        }
        int color = t[Math.floorMod(gy, 256) * 256 + Math.floorMod(gx, 256)];
        int r = (color >> 16) & 255, g = (color >> 8) & 255, bl = color & 255;
        return (float) (r * 256.0 + g + bl / 256.0 - 32768.0);
    }

    // Tonos por altura, claros para que el pin y el contorno se lean encima.
    private static final double[] COTAS = {0, 800, 1800, 2800, 3600, 4400, 5200};
    private static final int[] TONOS = {0xD2E6CC, 0xDCE8C7, 0xE8E4C2, 0xEBD9BB, 0xE2CDB6, 0xEDE6DE, 0xFAF8F5};

    private static int tinte(double e, double luz) {
        int i = 0;
        while (i < COTAS.length - 2 && e > COTAS[i + 1]) i++;
        double t = Math.max(0, Math.min(1, (e - COTAS[i]) / (COTAS[i + 1] - COTAS[i])));
        int a = TONOS[i], b = TONOS[i + 1];
        int r = (int) ((((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t) * luz);
        int g = (int) ((((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t) * luz);
        int bl = (int) (((a & 255) * (1 - t) + (b & 255) * t) * luz);
        return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, bl);
    }
}
