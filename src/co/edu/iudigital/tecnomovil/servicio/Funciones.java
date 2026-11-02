package co.edu.iudigital.tecnomovil.servicio;

import co.edu.iudigital.tecnomovil.modelo.Registro;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Funciones reutilizables: predicados, extractores, comparadores y
 * funciones de orden superior (reciben y/o devuelven funciones).
 * Todas son puras: no leen ni modifican estado externo.
 */
public final class Funciones {

    private Funciones() { }

    // ---------- Predicados (lambdas) que se pueden componer con and / or / negate ----------

    public static final Predicate<Registro> ES_ENTRADA = Registro::esEntrada;
    public static final Predicate<Registro> ES_SALIDA = ES_ENTRADA.negate();

    /** Función de orden superior: recibe una ruta y DEVUELVE un predicado. */
    public static Predicate<Registro> deRuta(String ruta) {
        return r -> r.ruta().equals(ruta);
    }

    /** Función de orden superior: devuelve un predicado para un rango de horas [desde, hasta). */
    public static Predicate<Registro> entreHoras(int desde, int hasta) {
        return r -> r.hora() >= desde && r.hora() < hasta;
    }

    // ---------- Extractores (Function) ----------

    public static final Function<Registro, Integer> HORA = Registro::hora;
    public static final Function<Registro, String> ESTACION = Registro::estacion;
    public static final Function<Registro, String> RUTA = Registro::ruta;

    /** Composición: Registro -> hora -> franja horaria en texto (ej. "07:00-07:59"). */
    public static final Function<Registro, String> FRANJA_HORARIA =
        HORA.andThen(h -> String.format("%02d:00-%02d:59", h, h));

    // ---------- Utilidades genéricas de orden superior ----------

    /** Comparador genérico por valor descendente y, en empate, por clave ascendente. */
    public static <K extends Comparable<K>> Comparator<Map.Entry<K, Long>> porValorDesc() {
        return Map.Entry.<K, Long>comparingByValue().reversed()
            .thenComparing(Map.Entry.comparingByKey());
    }

    /** Devuelve las N entradas con mayor valor de un mapa de conteos (lista inmutable). */
    public static <K extends Comparable<K>> List<Map.Entry<K, Long>> topN(Map<K, Long> conteos, int n) {
        return conteos.entrySet().stream()
            .sorted(porValorDesc())
            .limit(n)
            .map(e -> Map.entry(e.getKey(), e.getValue())) // copia inmutable de cada par
            .toList();
    }

    /**
     * Función de orden superior que DEVUELVE una función: a partir del umbral
     * construye un clasificador de ocupación reutilizable.
     */
    public static Predicate<Double> superaUmbral(double umbral) {
        return ocupacion -> ocupacion > umbral;
    }
}
