package co.edu.iudigital.tecnomovil;

import co.edu.iudigital.tecnomovil.modelo.Accion;
import co.edu.iudigital.tecnomovil.modelo.Registro;
import co.edu.iudigital.tecnomovil.modelo.Ruta;
import co.edu.iudigital.tecnomovil.servicio.AnalizadorTransporte;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Pruebas sin frameworks externos: un conjunto pequeño de datos con resultados
 * conocidos permite verificar que cada función pura devuelve lo esperado,
 * tanto en modo secuencial como paralelo.
 */
public final class PruebasFuncionales {

    private static Registro r(String u, String ruta, String est, String accion, int h, int m) {
        return new Registro(u, ruta, est, Accion.desdeTexto(accion), LocalDateTime.of(2026, 10, 30, h, m));
    }

    static final List<Registro> DATOS = List.of(
        r("U1", "R1", "A", "entrada", 7, 0),  r("U1", "R1", "C", "salida", 7, 20),
        r("U1", "R1", "C", "entrada", 18, 0), r("U1", "R1", "A", "salida", 18, 30),
        r("U2", "R1", "A", "entrada", 7, 10), r("U2", "R1", "B", "salida", 7, 20),
        r("U3", "R2", "B", "entrada", 7, 40), r("U3", "R2", "D", "salida", 8, 0)
    );

    static final List<Ruta> RUTAS = List.of(
        new Ruta("R1", "Prueba 1", List.of("A", "B", "C"), 2),
        new Ruta("R2", "Prueba 2", List.of("B", "D"), 10)
    );

    public static void main(String[] args) {
        long fallos = Stream.of(false, true)
            .flatMap(paralelo -> casos(new AnalizadorTransporte(DATOS, paralelo), paralelo))
            .filter(ok -> !ok)
            .count();
        System.out.println(fallos == 0 ? "\nTODAS LAS PRUEBAS PASARON" : "\nPRUEBAS FALLIDAS: " + fallos);
    }

    private static Stream<Boolean> casos(AnalizadorTransporte a, boolean paralelo) {
        String modo = paralelo ? "[paralelo]  " : "[secuencial]";
        return Stream.of(
            prueba(modo + " a) afluencia por estación", () -> a.afluenciaPorEstacion(),
                Map.of("A", 2L, "B", 1L, "C", 1L)),
            prueba(modo + " b) hora pico es las 07", () -> a.horasPico(1).get(0).getKey(), 7),
            prueba(modo + " b) registros a las 07", () -> a.flujoPorHora().get(7), 5L),
            prueba(modo + " c) ruta más usada R1 (3)", () -> a.rutasMasUtilizadas().get(0), Map.entry("R1", 3L)),
            prueba(modo + " d) patrón de U1", () -> a.patronesDeViaje().get("U1"), List.of("A", "C", "C", "A")),
            prueba(modo + " e) promedio A -> C (min)", () -> a.tiempoPromedioPorTramo().get("A -> C"), 20.0),
            prueba(modo + " e) promedio general (min)", () -> a.tiempoPromedioGeneral(), 20.0),
            prueba(modo + " f) R1 crítica (umbral 80%)", () -> a.rutasCriticas(RUTAS, 0.8), List.of("R1")),
            prueba(modo + " lista original intacta", () -> DATOS.size(), 8)
        );
    }

    private static <T> boolean prueba(String nombre, Supplier<T> obtenido, T esperado) {
        T valor = obtenido.get();
        boolean ok = valor.equals(esperado);
        System.out.printf("%-48s %s  (esperado=%s, obtenido=%s)%n", nombre, ok ? "OK   " : "FALLO", esperado, valor);
        return ok;
    }
}
