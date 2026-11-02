package co.edu.iudigital.tecnomovil.datos;

import co.edu.iudigital.tecnomovil.modelo.Ruta;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catálogo fijo e inmutable de rutas simuladas de TecnoValle.
 * La capacidad por hora es un valor simulado usado para detectar sobrecarga.
 */
public final class CatalogoRutas {

    private CatalogoRutas() { }

    public static final List<Ruta> RUTAS = List.of(
        new Ruta("R1", "Troncal Norte-Sur",
            List.of("Terminal Norte", "Parque Central", "Plaza Mayor", "Universidad", "Hospital General", "Terminal Sur"), 18_000),
        new Ruta("R2", "Troncal Oriente-Occidente",
            List.of("Portal Oriente", "Mercado Central", "Parque Central", "Centro Administrativo", "Estadio", "Portal Occidente"), 20_000),
        new Ruta("R3", "Alimentadora La Loma",
            List.of("La Loma", "Barrio Nuevo", "Colegio Mayor", "Terminal Norte"), 9_000),
        new Ruta("R4", "Alimentadora El Bosque",
            List.of("El Bosque", "Villa Verde", "Mirador", "Portal Oriente"), 10_000),
        new Ruta("R5", "Circular Centro",
            List.of("Plaza Mayor", "Centro Administrativo", "Mercado Central", "Biblioteca", "Plaza Mayor Sur"), 10_000),
        new Ruta("R6", "Expresa Aeropuerto",
            List.of("Terminal Sur", "Zona Industrial", "Aeropuerto"), 5_500)
    );

    /** Peso relativo de demanda de cada ruta (simulado). */
    public static final Map<String, Integer> PESO_DEMANDA = Map.of(
        "R1", 30, "R2", 25, "R3", 13, "R4", 12, "R5", 12, "R6", 8
    );

    /** Índice inmutable codigo -> Ruta, construido de forma declarativa. */
    public static final Map<String, Ruta> POR_CODIGO = RUTAS.stream()
        .collect(Collectors.toUnmodifiableMap(Ruta::codigo, Function.identity()));
}
