package pe.yachayallpa.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import pe.yachayallpa.core.Cultivo;

/** Cada cultivo ocupa una celda de la lámina botánica incluida en el APK. */
public final class CultivoLamina extends View {
    private static Bitmap lamina;
    private static Bitmap laminaBaja;
    private static Bitmap laminaTuberculos;
    private final Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Cultivo cultivo;

    public CultivoLamina(Context contexto, Cultivo cultivo) {
        super(contexto);
        this.cultivo = cultivo;
        setContentDescription(cultivo.nombre);
        if (lamina == null) lamina = BitmapFactory.decodeResource(
                getResources(), R.drawable.cultivos_transparentes);
        if (laminaBaja == null) laminaBaja = BitmapFactory.decodeResource(
                getResources(), R.drawable.cultivos_bajos_lamina);
        if (laminaTuberculos == null) laminaTuberculos = BitmapFactory.decodeResource(
                getResources(), R.drawable.tuberculos_curvos_transparentes);
    }

    @Override protected void onDraw(Canvas lienzo) {
        super.onDraw(lienzo);
        boolean baja=cultivo==Cultivo.MAIZ_AMARILLO || cultivo==Cultivo.FRIJOL
                || cultivo==Cultivo.YUCA;
        boolean tuberculo=cultivo==Cultivo.MASHUA || cultivo==Cultivo.CAMOTE
                || cultivo==Cultivo.YACON || cultivo==Cultivo.ARRACACHA;
        Bitmap hoja=tuberculo?laminaTuberculos:baja?laminaBaja:lamina;
        if (hoja == null) return;
        int columnas=tuberculo?4:baja?3:5;
        int columna=tuberculo?cultivo.ordinal()-Cultivo.MASHUA.ordinal()
                :baja?cultivo.ordinal()-Cultivo.MAIZ_AMARILLO.ordinal():cultivo.ordinal()%5;
        int fila=baja||tuberculo?0:cultivo.ordinal()/5;
        int ancho=hoja.getWidth()/columnas;
        int alto=baja||tuberculo?hoja.getHeight():hoja.getHeight()/2;
        Rect origen = new Rect(columna * ancho, fila * alto,
                columna == columnas-1 ? hoja.getWidth() : (columna + 1) * ancho,
                baja||tuberculo?hoja.getHeight():fila == 1 ? hoja.getHeight() : (fila + 1) * alto);
        float lado = Math.min(getWidth(), getHeight());
        float izquierda = (getWidth() - lado) / 2f;
        float arriba = (getHeight() - lado) / 2f;
        RectF destino = new RectF(izquierda, arriba, izquierda + lado, arriba + lado);
        lienzo.drawBitmap(hoja, origen, destino, pintura);
    }
}
