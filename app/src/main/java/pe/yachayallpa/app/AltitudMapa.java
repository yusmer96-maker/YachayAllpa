package pe.yachayallpa.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;

/** Relieve Terrarium incluido en el APK: solo se consulta en memoria con las coordenadas GPS. */
public final class AltitudMapa {
    private static final int ZOOM = 9;
    private static final int N = 1 << ZOOM;
    private AltitudMapa() { }

    /** Devuelve metros aproximados o -1 si la tesela no está disponible. */
    public static int metros(Context contexto, double lat, double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < -10.6 || lat > -8.2
                || lon < -77.4 || lon > -74.4) return -1;
        double x = (lon + 180.0) / 360.0 * N;
        double rad = Math.toRadians(lat);
        double y = (1.0 - Math.log(Math.tan(Math.PI / 4.0 + rad / 2.0)) / Math.PI)
                / 2.0 * N;
        int tx = (int) Math.floor(x), ty = (int) Math.floor(y);
        if (tx < 146 || tx > 150 || ty < 267 || ty > 271) return -1;
        int px = Math.max(0, Math.min(255, (int) Math.floor((x - tx) * 256)));
        int py = Math.max(0, Math.min(255, (int) Math.floor((y - ty) * 256)));
        try (InputStream in = contexto.getAssets().open("relieve/9_" + tx + "_" + ty + ".png")) {
            Bitmap imagen = BitmapFactory.decodeStream(in);
            if (imagen == null || imagen.getWidth() != 256 || imagen.getHeight() != 256)
                return -1;
            int color = imagen.getPixel(px, py);
            imagen.recycle();
            int rojo = (color >> 16) & 255, verde = (color >> 8) & 255, azul = color & 255;
            double altura = rojo * 256.0 + verde + azul / 256.0 - 32768.0;
            if (altura < 30 || altura > 6000) return -1;
            // A ~300 m por píxel, 50 m comunica mejor la incertidumbre que una cifra exacta.
            return (int) (Math.round(altura / 50.0) * 50);
        } catch (Exception e) {
            return -1;
        }
    }
}
