package co.edu.iudigital.tecnomovil.modelo;

import java.util.List;

/**
 * Ruta del sistema de transporte (inmutable).
 *
 * @param codigo           código de la ruta
 * @param nombre           nombre descriptivo
 * @param estaciones       estaciones en orden de recorrido (lista inmutable)
 * @param capacidadPorHora pasajeros que la ruta puede mover en una hora (valor simulado)
 */
public record Ruta(String codigo, String nombre, List<String> estaciones, int capacidadPorHora) {

    public Ruta {
        estaciones = List.copyOf(estaciones); // copia defensiva inmutable
    }
}
