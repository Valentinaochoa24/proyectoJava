package co.edu.iudigital.tecnomovil.modelo;

/**
 * Resultado (inmutable) del análisis de ocupación de una ruta.
 *
 * @param ruta               código de la ruta
 * @param horaMasCargada     hora del día con más entradas en la ruta
 * @param entradasHoraPico   pasajeros que ingresaron en esa hora
 * @param capacidadPorHora   capacidad simulada de la ruta
 * @param ocupacion          entradasHoraPico / capacidadPorHora (1.0 = 100 %)
 * @param critica            true si la ocupación supera el umbral
 */
public record EstadoRuta(String ruta,
                         int horaMasCargada,
                         long entradasHoraPico,
                         int capacidadPorHora,
                         double ocupacion,
                         boolean critica) {

    public String etiqueta() {
        return critica ? "CRITICA" : "NORMAL";
    }
}
