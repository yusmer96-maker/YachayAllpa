package pe.yachayallpa.core;

/**
 * Una razon del consejo, sin idioma: una clave y su cifra.
 *
 * El motor no escribe frases. Devuelve claves ("cobertura", "helada",
 * "calendario") con el numero que les toca, y la app las convierte al idioma
 * elegido. Asi la misma cuenta se lee en castellano o en quechua sin que el
 * motor tenga texto dentro.
 */
public final class Motivo {
    public final String clave;
    public final double valor, valor2;

    public Motivo(String clave, double valor, double valor2) {
        if (clave == null || clave.isEmpty()) throw new IllegalArgumentException("Clave vacía");
        this.clave = clave;
        this.valor = valor;
        this.valor2 = valor2;
    }

    public Motivo(String clave, double valor) { this(clave, valor, 0); }

    public Motivo(String clave) { this(clave, 0, 0); }

    public int entero() { return (int) Math.round(valor); }

    public int entero2() { return (int) Math.round(valor2); }

    @Override public String toString() { return clave + "=" + valor + "/" + valor2; }
}
