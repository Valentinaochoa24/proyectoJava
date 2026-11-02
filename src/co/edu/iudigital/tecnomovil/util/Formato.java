package co.edu.iudigital.tecnomovil.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Funciones puras que convierten resultados en texto. No imprimen nada:
 * solo devuelven String. La impresión (efecto secundario) queda aislada en Main.
 */
public final class Formato {

    private Formato() { }

    public static String titulo(String texto) {
        String linea = "=".repeat(72);
        return "\n" + linea + "\n " + texto + "\n" + linea;
    }

    public static String barra(long valor, long maximo, int ancho) {
        int largo = maximo == 0 ? 0 : (int) Math.round((double) valor / maximo * ancho);
        return "#".repeat(largo);
    }

    public static String miles(long n) {
        return String.format("%,d", n).replace(',', '.');
    }

    /** Tabla clave-valor con barra proporcional. */
    public static <K> String tablaConBarras(List<Map.Entry<K, Long>> filas, String encabezadoClave, int ancho) {
        long max = filas.stream().mapToLong(Map.Entry::getValue).max().orElse(0);
        String encabezado = String.format("  %-30s %10s", encabezadoClave, "Cantidad");
        String cuerpo = filas.stream()
            .map(e -> String.format("  %-30s %10s  %s", e.getKey(), miles(e.getValue()), barra(e.getValue(), max, ancho)))
            .collect(Collectors.joining("\n"));
        return encabezado + "\n" + cuerpo;
    }

    public static String sangria(String texto) {
        return texto.lines().map(l -> "  " + l).collect(Collectors.joining("\n"));
    }

    public static <T> List<T> primeros(List<T> lista, int n) {
        return Collections.unmodifiableList(lista.subList(0, Math.min(n, lista.size())));
    }
}
