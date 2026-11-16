package pe.yachayallpa.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import pe.yachayallpa.core.Temporada;
import java.util.Locale;

/** Dos barras por semana; el dato comparable evita confundir una lluvia alta con una predicción. */
public final class LluviaGrafico extends View {
    private final double[] actual, habitual;
    private final Paint p = new Paint(3);
    private final float dp;
    public LluviaGrafico(Context contexto, Temporada t) {
        super(contexto);
        actual=t.semanasActuales(); habitual=t.semanasHabituales();
        dp=getResources().getDisplayMetrics().density;
        try {
            p.setTypeface(new Typeface.Builder(getResources().getAssets(), "fonts/DMSans.ttf")
                    .setFontVariationSettings("'wght' 700").build());
        } catch (RuntimeException e) { p.setTypeface(Typeface.DEFAULT_BOLD); }
        setContentDescription(String.format(Locale.ROOT,
                "Lluvia observada de seis semanas frente a lo habitual. Última semana %.0f frente a %.0f milímetros",
                actual[5],habitual[5]));
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w=getWidth(), h=getHeight();
        float izquierda=38*dp, derecha=w-5*dp, arriba=9*dp, base=h-24*dp;
        double pico=1;
        for(int i=0;i<6;i++)pico=Math.max(pico,Math.max(actual[i],habitual[i]));
        pico=Math.ceil(pico/25)*25;
        p.setTextSize(10*dp);
        for(int n=0;n<=2;n++) {
            float y=base-n*(base-arriba)/2;
            p.setColor(0xFFE6EAE1);p.setStrokeWidth(dp);
            c.drawLine(izquierda,y,derecha,y,p);
            p.setColor(0xFF7B887E);
            String v=String.valueOf((int)Math.round(pico*n/2));
            c.drawText(v,izquierda-5*dp-p.measureText(v),y+3*dp,p);
        }
        float paso=(derecha-izquierda)/6f, barra=Math.min(11*dp,paso*.19f);
        for(int i=0;i<6;i++) {
            float x=izquierda+i*paso+paso/2;
            float altHabitual=(float)(habitual[i]/pico*(base-arriba));
            float altActual=(float)(actual[i]/pico*(base-arriba));
            p.setColor(0xFFE2D3B8);
            c.drawRoundRect(x-barra-2*dp,base-altHabitual,x-2*dp,base,3*dp,3*dp,p);
            p.setColor(0xFF4E8F76);
            c.drawRoundRect(x+2*dp,base-altActual,x+barra+2*dp,base,3*dp,3*dp,p);
            p.setColor(0xFF6D7F71);p.setTextSize(10*dp);
            String texto=i==5?"Última":"-"+(5-i);
            c.drawText(texto,x-p.measureText(texto)/2,h-4*dp,p);
        }
    }
}
