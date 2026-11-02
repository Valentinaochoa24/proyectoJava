package co.edu.iudigital.tecnomovil.modelo;

import java.time.Duration;

/**
 * Desplazamiento de un usuario entre dos estaciones consecutivas.
 *
 * @param origen   estación donde validó la entrada
 * @param destino  estación donde validó la salida
 * @param minutos  duración del desplazamiento en minutos
 */
public record Tramo(String origen, String destino, double minutos) {

    /** Función pura que construye un tramo a partir de dos registros consecutivos. */
    public static Tramo entre(Registro desde, Registro hasta) {
        double minutos = Duration.between(desde.timestamp(), hasta.timestamp()).toSeconds() / 60.0;
        return new Tramo(desde.estacion(), hasta.estacion(), minutos);
    }

    public String clave() {
        return origen + " -> " + destino;
    }
}
