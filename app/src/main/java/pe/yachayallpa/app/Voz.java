package pe.yachayallpa.app;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/**
 * La voz de Kusi: el lector de Android (TTS) leyendo la MISMA frase que esta
 * escrita en la pantalla.
 *
 * No hay servidor de voz ni audio pregrabado: la app no manda nada a ningun
 * sitio para hablar, y por eso tampoco hay un catalogo de frases que pueda
 * quedarse desfasado del texto. Quien llama pasa la frase ya en castellano
 * (`Textos.ES`); en quechua no se habla, porque Android no trae esa voz, y la
 * pantalla oculta el altavoz.
 *
 * `decir` no repite la misma frase dos veces seguidas: al recomponer la
 * pantalla o girar el telefono no vuelve a sonar. `repetir` es el boton del
 * altavoz: manda el usuario, suena siempre, y si ya estaba hablando, calla.
 */
public final class Voz {
    /** Mientras el TTS habla, la mascota mueve la boca. */
    public interface Oyente { void estado(KusiEstado e); }

    /** Una locución es una frase, no un párrafo: lo largo se queda escrito. */
    private static final int MAX_FRASE = 240;

    private final Oyente oyente;
    private TextToSpeech tts;
    private boolean lista;
    private String pendiente, ultimo = "";
    private long secuencia;

    public Voz(Context contexto, Oyente oyente) {
        this.oyente = oyente;
        tts = new TextToSpeech(contexto.getApplicationContext(), estado -> {
            if (estado == TextToSpeech.SUCCESS && tts != null) {
                lista = tts.setLanguage(new Locale("es", "PE")) >= 0;
                tts.setSpeechRate(.85f);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { avisar(KusiEstado.Hablando); }
                    @Override public void onDone(String id) { avisar(KusiEstado.Quieto); }
                    @Override public void onError(String id) { avisar(KusiEstado.Quieto); }
                });
                if (lista && pendiente != null) {
                    String frase = pendiente;
                    pendiente = null;
                    hablar(frase);
                }
            }
        });
    }

    private void avisar(KusiEstado e) { if (oyente != null) oyente.estado(e); }

    /** Lee la frase si es nueva; si es la que ya se dijo, no hace nada. */
    public void decir(String texto) {
        if (texto == null || texto.isEmpty() || texto.equals(ultimo)) return;
        ultimo = texto;
        hablar(texto);
    }

    /** El altavoz: habla siempre; si ya estaba hablando, corta. */
    public void repetir(String texto) {
        if (texto == null || texto.isEmpty()) return;
        if (tts != null && tts.isSpeaking()) {
            secuencia++;
            tts.stop();
            avisar(KusiEstado.Quieto);
            return;
        }
        ultimo = texto;
        hablar(texto);
    }

    private void hablar(String texto) {
        String frase = texto.length() > MAX_FRASE ? texto.substring(0, MAX_FRASE) : texto;
        if (!lista) { pendiente = frase; return; }
        secuencia++;
        tts.stop();
        tts.speak(frase, TextToSpeech.QUEUE_FLUSH, null, "frase-" + secuencia);
    }

    public void cerrar() {
        pendiente = null;
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
    }
}
