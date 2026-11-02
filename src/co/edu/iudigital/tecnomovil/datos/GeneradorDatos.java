package co.edu.iudigital.tecnomovil.datos;

import co.edu.iudigital.tecnomovil.modelo.Accion;
import co.edu.iudigital.tecnomovil.modelo.Registro;
import co.edu.iudigital.tecnomovil.modelo.Ruta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Genera un conjunto grande de registros simulados de forma 100 % funcional.
 *
 * No usa java.util.Random (que guarda estado interno y no es puro). En su lugar
 * usa una función hash determinística: el mismo (semilla, clave) siempre produce
 * el mismo número. Así la generación es reproducible y se puede paralelizar sin
 * condiciones de carrera.
 */
public final class GeneradorDatos {

    private GeneradorDatos() { }

    public static final LocalDate FECHA = LocalDate.of(2026, 10, 30);

    /** Pesos por hora para viajes de la mañana, mediodía y tarde (simulan horas pico y valle). */
    private static final int[][] HORAS_MANANA  = {{5, 8}, {6, 22}, {7, 32}, {8, 20}, {9, 11}, {10, 7}};
    private static final int[][] HORAS_MEDIODIA = {{11, 20}, {12, 25}, {13, 25}, {14, 18}, {15, 12}};
    /** {índice de ruta, peso}: derivado del catálogo de forma declarativa. */
    private static final int[][] PESOS_RUTA = IntStream.range(0, CatalogoRutas.RUTAS.size())
        .mapToObj(i -> new int[]{i, CatalogoRutas.PESO_DEMANDA.get(CatalogoRutas.RUTAS.get(i).codigo())})
        .toArray(int[][]::new);
    private static final int TOTAL_PESO_RUTA = CatalogoRutas.PESO_DEMANDA.values().stream()
        .mapToInt(Integer::intValue).sum();

    private static final int[][] HORAS_TARDE   = {{16, 10}, {17, 24}, {18, 30}, {19, 18}, {20, 11}, {21, 7}};

    /**
     * Genera todos los registros del día para la cantidad de usuarios indicada.
     * La lista resultante es inmutable (Stream.toList()).
     */
    public static List<Registro> generar(int cantidadUsuarios, long semilla) {
        return IntStream.range(0, cantidadUsuarios)
            .parallel()
            .boxed()
            .flatMap(usuario -> registrosDeUsuario(usuario, semilla))
            .toList();
    }

    /** Función pura: produce el recorrido completo de un usuario (2, 4 o 6 registros). */
    static Stream<Registro> registrosDeUsuario(int usuario, long semilla) {
        String id = String.format("U%06d", usuario);
        long base = semilla * 1_000_003L + usuario;
        int tiro = aleatorio(base, 1, 100);
        int viajes = tiro < 10 ? 1 : (tiro < 70 ? 2 : 3); // 10 % uno, 60 % dos, 30 % tres viajes

        Ruta principal = elegirRuta(base, 3);
        int n = principal.estaciones().size();
        int origen = aleatorio(base, 4, n);
        int destino = (origen + 1 + aleatorio(base, 5, n - 1)) % n;

        Ruta otra = elegirRuta(base, 20);
        int m = otra.estaciones().size();
        int origenOtra = aleatorio(base, 21, m);
        int destinoOtra = (origenOtra + 1 + aleatorio(base, 22, m - 1)) % m;

        // Horarios: cada viaje empieza al menos 15 minutos después de terminar el anterior
        LocalDateTime inicioIda = momento(base, 6, HORAS_MANANA);
        LocalDateTime finIda = inicioIda.plusSeconds(duracion(base, 7, origen, destino));
        LocalDateTime inicioMedio = posterior(momento(base, 23, HORAS_MEDIODIA), finIda);
        LocalDateTime finMedio = inicioMedio.plusSeconds(duracion(base, 24, origenOtra, destinoOtra));
        LocalDateTime inicioRegreso = posterior(momento(base, 30, HORAS_TARDE), viajes == 3 ? finMedio : finIda);

        // Viaje 1 (mañana): origen -> destino en la ruta principal
        Stream<Registro> ida = viaje(id, principal, origen, destino, inicioIda, base, 7);
        // Viaje 2 (solo si hace 3 viajes, mediodía): otra ruta
        Stream<Registro> intermedio = viajes == 3
            ? viaje(id, otra, origenOtra, destinoOtra, inicioMedio, base, 24)
            : Stream.empty();
        // Último viaje (tarde): regreso destino -> origen en la ruta principal
        Stream<Registro> regreso = viajes >= 2
            ? viaje(id, principal, destino, origen, inicioRegreso, base, 31)
            : Stream.empty();

        return Stream.of(ida, intermedio, regreso).flatMap(s -> s);
    }

    /** Función pura: devuelve el momento propuesto o, si choca, 15 min después del límite. */
    private static LocalDateTime posterior(LocalDateTime propuesto, LocalDateTime limite) {
        LocalDateTime minimo = limite.plusMinutes(15);
        return propuesto.isAfter(minimo) ? propuesto : minimo;
    }

    /** Función pura: duración simulada (segundos) según la cantidad de paradas recorridas. */
    private static long duracion(long base, int sal, int origen, int destino) {
        int paradas = Math.abs(destino - origen);
        return paradas * (180L + aleatorio(base, sal, 150)) + aleatorio(base, sal + 1, 240);
    }

    /** Un viaje = un registro de ENTRADA y uno de SALIDA. */
    private static Stream<Registro> viaje(String id, Ruta ruta, int origen, int destino,
                                          LocalDateTime inicio, long base, int sal) {
        long segundos = duracion(base, sal, origen, destino);
        return Stream.of(
            new Registro(id, ruta.codigo(), ruta.estaciones().get(origen), Accion.ENTRADA, inicio),
            new Registro(id, ruta.codigo(), ruta.estaciones().get(destino), Accion.SALIDA, inicio.plusSeconds(segundos))
        );
    }

    private static LocalDateTime momento(long base, int sal, int[][] horas) {
        int hora = elegirPonderado(horas, aleatorio(base, sal, 100));
        return FECHA.atTime(hora, aleatorio(base, sal + 1, 60), aleatorio(base, sal + 2, 60));
    }

    private static Ruta elegirRuta(long base, int sal) {
        int indice = elegirPonderado(PESOS_RUTA, aleatorio(base, sal, TOTAL_PESO_RUTA));
        return CatalogoRutas.RUTAS.get(indice);
    }

    /** Función pura: elige un valor según pesos acumulados (sin bucles mutables). */
    private static int elegirPonderado(int[][] valoresYPesos, int tiro) {
        IntFunction<Integer> acumulado = i -> IntStream.rangeClosed(0, i).map(k -> valoresYPesos[k][1]).sum();
        return IntStream.range(0, valoresYPesos.length)
            .filter(i -> tiro < acumulado.apply(i))
            .mapToObj(i -> valoresYPesos[i][0])
            .findFirst()
            .orElse(valoresYPesos[valoresYPesos.length - 1][0]);
    }

    /** Hash determinístico (SplitMix64): función pura que reemplaza a Random. */
    static int aleatorio(long base, int sal, int limite) {
        long x = base * 0x9E3779B97F4A7C15L + sal * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        x = x ^ (x >>> 31);
        return (int) Math.floorMod(x, (long) limite);
    }
}
