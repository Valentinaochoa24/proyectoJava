package co.edu.iudigital.tecnomovil;

import co.edu.iudigital.tecnomovil.datos.CatalogoRutas;
import co.edu.iudigital.tecnomovil.datos.GeneradorDatos;
import co.edu.iudigital.tecnomovil.modelo.EstadoRuta;
import co.edu.iudigital.tecnomovil.modelo.Registro;
import co.edu.iudigital.tecnomovil.servicio.AnalizadorTransporte;
import co.edu.iudigital.tecnomovil.util.Formato;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static co.edu.iudigital.tecnomovil.util.Formato.miles;
import static co.edu.iudigital.tecnomovil.util.Formato.titulo;

/**
 * TecnoMovil Data - Módulo de procesamiento funcional de datos de transporte urbano.
 *
 * Main es la ÚNICA capa con efectos secundarios (imprimir en consola y medir tiempo).
 * Toda la lógica de negocio está en funciones puras dentro de AnalizadorTransporte.
 *
 * Uso: java -cp out co.edu.iudigital.tecnomovil.Main [usuarios] [umbral]
 *   usuarios: cantidad de usuarios simulados (por defecto 200000)
 *   umbral  : ocupación para marcar ruta crítica (por defecto 0.85)
 */
public final class Main {

    public static void main(String[] args) {
        int usuarios = args.length > 0 ? Integer.parseInt(args[0]) : 200_000;
        double umbral = args.length > 1 ? Double.parseDouble(args[1]) : 0.85;
        long semilla = 2026L;

        System.out.println(titulo("TECNOMOVIL DATA - Alcaldía de TecnoValle"));
        System.out.println("  Procesamiento funcional de datos de transporte urbano (Java "
            + System.getProperty("java.version") + ")");

        // ------------------ Generación de datos ------------------
        Resultado<List<Registro>> gen = medir(() -> GeneradorDatos.generar(usuarios, semilla));
        List<Registro> registros = gen.valor();
        System.out.printf("  Fecha simulada: %s | Usuarios: %s | Registros generados: %s (%d ms)%n",
            GeneradorDatos.FECHA, miles(usuarios), miles(registros.size()), gen.ms());
        System.out.println("  Ejemplo de registro: " + registros.get(0));

        AnalizadorTransporte analizador = new AnalizadorTransporte(registros, true);

        // ------------------ a) Afluencia por estación ------------------
        System.out.println(titulo("a) AFLUENCIA POR ESTACIÓN (ingresos en el día)"));
        List<Map.Entry<String, Long>> afluencia = analizador.afluenciaPorEstacion().entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .toList();
        System.out.println(Formato.tablaConBarras(afluencia, "Estación", 30));
        long totalIngresos = afluencia.stream().mapToLong(Map.Entry::getValue).sum();
        System.out.println("  Total de ingresos: " + miles(totalIngresos));

        // ------------------ b) Horas pico ------------------
        System.out.println(titulo("b) FLUJO POR HORA Y HORAS PICO (entradas + salidas)"));
        List<Map.Entry<Integer, Long>> porHora = analizador.flujoPorHora().entrySet().stream().toList();
        List<Map.Entry<String, Long>> porHoraTexto = porHora.stream()
            .map(e -> Map.entry(String.format("%02d:00 - %02d:59", e.getKey(), e.getKey()), e.getValue()))
            .toList();
        System.out.println(Formato.tablaConBarras(porHoraTexto, "Hora", 35));
        System.out.println("\n  Top 3 horas pico:");
        analizador.horasPico(3).forEach(e ->
            System.out.printf("    %02d:00  ->  %s registros%n", e.getKey(), miles(e.getValue())));

        // ------------------ c) Rutas más utilizadas ------------------
        System.out.println(titulo("c) RUTAS MÁS UTILIZADAS (ingresos por ruta)"));
        List<Map.Entry<String, Long>> rutas = analizador.rutasMasUtilizadas().stream()
            .map(e -> Map.entry(e.getKey() + " " + CatalogoRutas.POR_CODIGO.get(e.getKey()).nombre(), e.getValue()))
            .toList();
        System.out.println(Formato.tablaConBarras(rutas, "Ruta", 25));

        // ------------------ d) Patrones de viaje ------------------
        System.out.println(titulo("d) PATRONES DE VIAJE POR USUARIO (orden cronológico)"));
        Map<String, List<String>> patrones = analizador.patronesDeViaje();
        System.out.println("  Usuarios con patrón calculado: " + miles(patrones.size()));
        System.out.println("  Muestra de 5 usuarios:");
        patrones.entrySet().stream()
            .limit(5)
            .map(e -> String.format("    %s: %s", e.getKey(), String.join(" > ", e.getValue())))
            .forEach(System.out::println);
        System.out.println("\n  Patrones más frecuentes:");
        analizador.patronesMasFrecuentes(3).forEach(e ->
            System.out.printf("    [%s usuarios] %s%n", miles(e.getValue()), e.getKey()));

        // ------------------ e) Tiempo promedio ------------------
        System.out.println(titulo("e) TIEMPO PROMEDIO ENTRE ESTACIONES CONSECUTIVAS"));
        System.out.printf("  Tramos analizados: %s%n", miles(analizador.tramos().size()));
        System.out.printf("  Tiempo promedio general: %.2f minutos%n", analizador.tiempoPromedioGeneral());
        System.out.println("  Los 5 tramos más lentos:");
        analizador.tiempoPromedioPorTramo().entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> System.out.printf("    %-44s %6.2f min%n", e.getKey(), e.getValue()));
        System.out.println("  Los 5 tramos más rápidos:");
        analizador.tiempoPromedioPorTramo().entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .limit(5)
            .forEach(e -> System.out.printf("    %-44s %6.2f min%n", e.getKey(), e.getValue()));

        // ------------------ f) Sobrecarga ------------------
        System.out.println(titulo(String.format("f) DETECCIÓN DE SOBRECARGA (umbral %.0f %% de capacidad)", umbral * 100)));
        System.out.printf("  %-5s %-11s %14s %12s %10s  %s%n", "Ruta", "Hora pico", "Ingresos/hora", "Capacidad", "Ocupación", "Estado");
        List<EstadoRuta> estados = analizador.detectarSobrecarga(CatalogoRutas.RUTAS, umbral);
        estados.forEach(e -> System.out.printf("  %-5s %02d:00%6s %14s %12s %9.1f%%  %s%n",
            e.ruta(), e.horaMasCargada(), "", miles(e.entradasHoraPico()), miles(e.capacidadPorHora()),
            e.ocupacion() * 100, e.etiqueta()));
        System.out.println("  Rutas críticas: " + analizador.rutasCriticas(CatalogoRutas.RUTAS, umbral));

        // ------------------ Inmutabilidad y paralelismo ------------------
        System.out.println(titulo("VERIFICACIÓN DE INMUTABILIDAD Y PARALELISMO"));
        Registro primeroAntes = registros.get(0);
        System.out.println("  Registros antes y después del análisis: " + miles(registros.size())
            + " / " + miles(analizador.totalRegistros()) + " | primer registro sin cambios: "
            + primeroAntes.equals(registros.get(0)));
        System.out.println("  Intento de modificar la lista original -> " + intentarModificar(registros));
        System.out.println("  Intento de modificar un resultado        -> "
            + intentarModificar(analizador.afluenciaPorEstacion()));

        AnalizadorTransporte secuencial = new AnalizadorTransporte(registros, false);
        Resultado<Map<String, Long>> rSec = medir(secuencial::afluenciaPorEstacion);
        Resultado<Map<String, Long>> rPar = medir(analizador::afluenciaPorEstacion);
        Resultado<Map<String, List<String>>> pSec = medir(secuencial::patronesDeViaje);
        Resultado<Map<String, List<String>>> pPar = medir(analizador::patronesDeViaje);
        System.out.printf("  Afluencia  - secuencial: %4d ms | paralelo: %4d ms | resultados iguales: %s%n",
            rSec.ms(), rPar.ms(), rSec.valor().equals(rPar.valor()));
        System.out.printf("  Patrones   - secuencial: %4d ms | paralelo: %4d ms | resultados iguales: %s%n",
            pSec.ms(), pPar.ms(), pSec.valor().equals(pPar.valor()));
        System.out.printf("  Núcleos disponibles: %d%n", Runtime.getRuntime().availableProcessors());
        System.out.println("\n  Procesamiento finalizado correctamente.");
    }

    /** Resultado de una operación junto con su tiempo de ejecución. */
    private record Resultado<T>(T valor, long ms) { }

    /** Función de orden superior: recibe una operación (Supplier) y la ejecuta midiendo el tiempo. */
    private static <T> Resultado<T> medir(Supplier<T> operacion) {
        Instant inicio = Instant.now();
        T valor = operacion.get();
        return new Resultado<>(valor, Duration.between(inicio, Instant.now()).toMillis());
    }

    @SuppressWarnings("unchecked")
    private static String intentarModificar(Object coleccion) {
        try {
            if (coleccion instanceof List<?> l) {
                ((List<Object>) l).add(null);
            } else if (coleccion instanceof Map<?, ?> m) {
                ((Map<Object, Object>) m).put("X", 0L);
            }
            return "se modificó (ERROR)";
        } catch (UnsupportedOperationException | NullPointerException ex) {
            return "rechazado con " + ex.getClass().getSimpleName() + " (colección inmutable)";
        }
    }
}
