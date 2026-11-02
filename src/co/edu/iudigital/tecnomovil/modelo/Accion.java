package co.edu.iudigital.tecnomovil.modelo;

/**
 * Tipo de acción registrada por los validadores y sensores.
 * Un enum es inmutable por naturaleza y evita errores de escritura
 * con cadenas como "entrada" / "salida".
 */
public enum Accion {
    ENTRADA,
    SALIDA;

    /** Función pura: convierte el texto del sensor ("entrada"/"salida") en el enum. */
    public static Accion desdeTexto(String texto) {
        return Accion.valueOf(texto.trim().toUpperCase());
    }
}
