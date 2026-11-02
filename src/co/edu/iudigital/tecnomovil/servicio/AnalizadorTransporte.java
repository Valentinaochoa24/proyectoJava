package co.edu.iudigital.tecnomovil.servicio;

import co.edu.iudigital.tecnomovil.modelo.EstadoRuta;
import co.edu.iudigital.tecnomovil.modelo.Registro;
import co.edu.iudigital.tecnomovil.modelo.Ruta;
import co.edu.iudigital.tecnomovil.modelo.Tramo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static co.edu.iudigital.tecnomovil.servicio.Funciones.ES_ENTRADA;
import static co.edu.iudigital.tecnomovil.servicio.Funciones.ESTACION;
import static co.edu.iudigital.tecnomovil.servicio.Funciones.HORA;
import static co.edu.iudigital.tecnomovil.servicio.Funciones.RUTA;

/**
 * Módulo de procesamiento y agregación diaria de TecnoMovil Data.
 *
 * Diseño funcional:
 *  - La clase es inmutable: sus campos son final y la lista de registros es una copia inmutable.
 *  - Cada método es una función pura: el resultado depende solo de los registros recibidos,
 *    no modifica la lista original ni ningún estado externo, y no imprime nada.
 *  - Todos los resultados son colecciones inmutables.
 *  - El parámetro "paralelo" decide si los pipelines usan parallelStream(); como no hay
 *    estado compartido mutable, el resultado es idéntico en ambos modos.
 */
public final class AnalizadorTransporte {

    private final List<Registro> registros;
    private final boolean paralelo;

    public AnalizadorTransporte(List<Registro> registros, boolean paralelo) {
        this.registros = List.copyOf(registros); // si ya es inmutable, no se copia de nuevo
        this.paralelo = paralelo;
    }

    /** Punto único de creación del stream: secuencial o paralelo según configuración. */
    private Stream<Registro> flujo() {
        return paralelo ? registros.parallelStream() : registros.stream();
    }

    // ------------------------------------------------------------------------------------
    // a) Afluencia por estación
    // ------------------------------------------------------------------------------------

    /** Cuenta cuántos ingresos (validaciones de ENTRADA) tiene cada estación en el día. */
    public Map<String, Long> afluenciaPorEstacion() {
        return flujo()
            .filter(ES_ENTRADA)
            .collect(Collectors.collectingAndThen(
                Collectors.groupingBy(ESTACION, TreeMap::new, Collectors.counting()),
                Collections::unmodifiableMap));
    }

    /** Variante: usuarios DISTINTOS que ingresaron a cada estación. */
    public Map<String, Long> usuariosDistintosPorEstacion() {
        return flujo()
            .filter(ES_ENTRADA)
            .collect(Collectors.collectingAndThen(
                Collectors.groupingBy(ESTACION, TreeMap::new,
                    Collectors.collectingAndThen(
                        Collectors.mapping(Registro::idUsuario, Collectors.toSet()),
                        s -> (long) s.size())),
                Collections::unmodifiableMap));
    }

    // ------------------------------------------------------------------------------------
    // b) Horas pico
    // ------------------------------------------------------------------------------------

    /** Agrupa TODOS los registros (entradas y salidas) por hora del día (0-23). */
    public Map<Integer, Long> flujoPorHora() {
        return flujo()
            .collect(Collectors.collectingAndThen(
                Collectors.groupingBy(HORA, TreeMap::new, Collectors.counting()),
                Collections::unmodifiableMap));
    }

    /** Las N horas de mayor flujo, ordenadas de mayor a menor. */
    public List<Map.Entry<Integer, Long>> horasPico(int n) {
        return Funciones.topN(flujoPorHora(), n);
    }

    // ------------------------------------------------------------------------------------
    // c) Rutas más utilizadas
    // ------------------------------------------------------------------------------------

    /** Ranking de rutas por número de ingresos (de mayor a menor). */
    public List<Map.Entry<String, Long>> rutasMasUtilizadas() {
        Map<String, Long> usoPorRuta = flujo()
            .filter(ES_ENTRADA)
            .collect(Collectors.groupingBy(RUTA, Collectors.counting()));
        return Funciones.topN(usoPorRuta, usoPorRuta.size());
    }

    // ------------------------------------------------------------------------------------
    // d) Patrones de viaje por usuario
    // ------------------------------------------------------------------------------------

    /**
     * Para cada usuario, la lista de estaciones visitadas en orden cronológico.
     * groupingBy conserva el orden de encuentro del stream (incluso en paralelo),
     * por eso primero se ordena por timestamp.
     */
    public Map<String, List<String>> patronesDeViaje() {
        return flujo()
            .sorted(Comparator.comparing(Registro::timestamp))
            .collect(Collectors.collectingAndThen(
                Collectors.groupingBy(Registro::idUsuario, TreeMap::new,
                    Collectors.mapping(ESTACION, Collectors.toUnmodifiableList())),
                Collections::unmodifiableMap));
    }

    /** Patrones más frecuentes: secuencias de estaciones repetidas por más usuarios. */
    public List<Map.Entry<String, Long>> patronesMasFrecuentes(int n) {
        Map<String, Long> frecuencia = patronesDeViaje().values().stream()
            .map(estaciones -> String.join(" > ", estaciones))
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        return Funciones.topN(frecuencia, n);
    }

    // ------------------------------------------------------------------------------------
    // e) Tiempo promedio entre estaciones
    // ------------------------------------------------------------------------------------

    /**
     * Construye los tramos recorridos: para cada usuario se ordenan sus registros por
     * tiempo y se emparejan registros consecutivos ENTRADA -> SALIDA (el viaje real
     * dentro del sistema). Los pares SALIDA -> ENTRADA son tiempos de espera, no de
     * desplazamiento, por eso se descartan.
     */
    public List<Tramo> tramos() {
        return flujo()
            .collect(Collectors.groupingBy(Registro::idUsuario))
            .values().stream()
            .flatMap(AnalizadorTransporte::tramosDeUsuario)
            .toList();
    }

    /** Función pura auxiliar: tramos de un solo usuario. */
    private static Stream<Tramo> tramosDeUsuario(List<Registro> registrosUsuario) {
        List<Registro> ordenados = registrosUsuario.stream()
            .sorted(Comparator.comparing(Registro::timestamp))
            .toList();
        return IntStream.range(0, ordenados.size() - 1)
            .filter(i -> ordenados.get(i).esEntrada() && !ordenados.get(i + 1).esEntrada())
            .mapToObj(i -> Tramo.entre(ordenados.get(i), ordenados.get(i + 1)))
            .filter(t -> !t.origen().equals(t.destino()));
    }

    /** Tiempo promedio (minutos) por cada par origen -> destino. */
    public Map<String, Double> tiempoPromedioPorTramo() {
        return tramos().stream()
            .collect(Collectors.collectingAndThen(
                Collectors.groupingBy(Tramo::clave, TreeMap::new, Collectors.averagingDouble(Tramo::minutos)),
                Collections::unmodifiableMap));
    }

    /** Tiempo promedio general de desplazamiento entre estaciones (minutos). */
    public double tiempoPromedioGeneral() {
        return tramos().stream()
            .mapToDouble(Tramo::minutos)
            .average()
            .orElse(0.0);
    }

    // ------------------------------------------------------------------------------------
    // f) Detección de sobrecarga en rutas
    // ------------------------------------------------------------------------------------

    /**
     * Calcula, para cada ruta, la hora con más ingresos y la compara con su capacidad
     * por hora (simulada). Si la ocupación supera el umbral, la ruta se marca CRITICA.
     *
     * @param catalogo rutas con su capacidad simulada
     * @param umbral   fracción de ocupación permitida (ej. 0.85 = 85 %)
     */
    public List<EstadoRuta> detectarSobrecarga(List<Ruta> catalogo, double umbral) {
        Predicate<Double> esCritica = Funciones.superaUmbral(umbral);

        Map<String, Map<Integer, Long>> entradasPorRutaYHora = flujo()
            .filter(ES_ENTRADA)
            .collect(Collectors.groupingBy(RUTA, Collectors.groupingBy(HORA, Collectors.counting())));

        return catalogo.stream()
            .map(ruta -> {
                Map.Entry<Integer, Long> pico = entradasPorRutaYHora
                    .getOrDefault(ruta.codigo(), Map.of())
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(Map.entry(0, 0L));
                double ocupacion = (double) pico.getValue() / ruta.capacidadPorHora();
                return new EstadoRuta(ruta.codigo(), pico.getKey(), pico.getValue(),
                    ruta.capacidadPorHora(), ocupacion, esCritica.test(ocupacion));
            })
            .sorted(Comparator.comparingDouble(EstadoRuta::ocupacion).reversed())
            .toList();
    }

    /** Solo las rutas críticas (reutiliza la función anterior: composición). */
    public List<String> rutasCriticas(List<Ruta> catalogo, double umbral) {
        return detectarSobrecarga(catalogo, umbral).stream()
            .filter(EstadoRuta::critica)
            .map(EstadoRuta::ruta)
            .toList();
    }

    public int totalRegistros() {
        return registros.size();
    }
}
