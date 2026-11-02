package co.edu.iudigital.tecnomovil.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro inmutable generado por un validador, sensor o app móvil.
 * Un record de Java tiene todos sus campos "private final", no tiene setters
 * y genera equals/hashCode/toString automáticamente.
 *
 * @param idUsuario identificador del usuario (tarjeta)
 * @param ruta      código de la ruta (ej. "R1")
 * @param estacion  nombre de la estación
 * @param accion    ENTRADA o SALIDA
 * @param timestamp fecha y hora del evento
 */
public record Registro(String idUsuario,
                       String ruta,
                       String estacion,
                       Accion accion,
                       LocalDateTime timestamp) {

    /** Constructor compacto: valida los datos sin modificar nada externo. */
    public Registro {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(ruta, "ruta");
        Objects.requireNonNull(estacion, "estacion");
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    public boolean esEntrada() {
        return accion == Accion.ENTRADA;
    }

    public int hora() {
        return timestamp.getHour();
    }
}
