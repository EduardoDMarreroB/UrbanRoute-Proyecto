package urbanroute.modelo;

/**
 * Representa una conexión entre dos estaciones de la red de transporte.
 *
 * Cada conexión almacena una estación de origen, una estación de destino
 * y el tiempo de recorrido expresado en minutos.
 *
 * Dos conexiones son iguales cuando tienen el mismo origen
 * y el mismo destino, independientemente de su tiempo.
 *
 * @author UrbanRoute
 */
public class Conexion {

    /** Estación desde la que comienza la conexión. */
    private final Estacion origen;

    /** Estación hacia la que se dirige la conexión. */
    private final Estacion destino;

    /** Tiempo de recorrido entre las estaciones, expresado en minutos. */
    private double tiempo;

    /**
     * Construye una conexión entre dos estaciones.
     *
     * @param origen Estación de origen.
     * @param destino Estación de destino.
     * @param tiempo Tiempo de recorrido en minutos.
     * @throws IllegalArgumentException Si alguna estación es null
     * o el tiempo es negativo, infinito o NaN.
     */
    public Conexion(Estacion origen, Estacion destino, double tiempo) {

        if (origen == null || destino == null) {
            throw new IllegalArgumentException(
                    "Las estaciones no pueden ser nulas."
            );
        }

        validarTiempo(tiempo);

        this.origen = origen;
        this.destino = destino;
        this.tiempo = tiempo;
    }

    /**
     * Obtiene la estación de origen.
     *
     * @return Estación de origen.
     */
    public Estacion getOrigen() {
        return origen;
    }

    /**
     * Obtiene la estación de destino.
     *
     * @return Estación de destino.
     */
    public Estacion getDestino() {
        return destino;
    }

    /**
     * Obtiene el tiempo de recorrido de la conexión.
     *
     * @return Tiempo de recorrido expresado en minutos.
     */
    public double getTiempo() {
        return tiempo;
    }

    /**
     * Modifica el tiempo de recorrido de la conexión.
     *
     * @param tiempo Nuevo tiempo de recorrido en minutos.
     * @throws IllegalArgumentException Si el tiempo es negativo,
     * infinito o NaN.
     */
    public void setTiempo(double tiempo) {
        validarTiempo(tiempo);
        this.tiempo = tiempo;
    }

    /**
     * Comprueba que el tiempo de recorrido sea válido.
     *
     * El tiempo debe ser un número finito y no negativo.
     * Esta condición permite utilizar la conexión correctamente
     * en el algoritmo de Dijkstra.
     *
     * @param tiempo Tiempo que se desea validar.
     * @throws IllegalArgumentException Si el tiempo no es válido.
     */
    private void validarTiempo(double tiempo) {

        if (Double.isNaN(tiempo)
                || Double.isInfinite(tiempo)
                || tiempo < 0) {

            throw new IllegalArgumentException(
                    "El tiempo debe ser finito y no negativo."
            );
        }
    }

    /**
     * Obtiene la estación situada en el extremo opuesto
     * al de la estación recibida.
     *
     * @param estacion Estación desde la que se consulta la conexión.
     * @return Estación ubicada en el extremo contrario.
     * @throws IllegalArgumentException Si la estación es null
     * o no pertenece a esta conexión.
     */
    public Estacion getOtroExtremo(Estacion estacion) {

        if (estacion == null) {
            throw new IllegalArgumentException(
                    "La estación no puede ser nula."
            );
        }

        if (estacion.equals(origen)) {
            return destino;
        }

        if (estacion.equals(destino)) {
            return origen;
        }

        throw new IllegalArgumentException(
                "La estación no pertenece a esta conexión."
        );
    }

    /**
     * Genera la representación CSV de la conexión.
     *
     * El formato es: origen,destino,tiempo.
     *
     * @return Cadena con los identificadores de las estaciones
     * y el tiempo de recorrido.
     */
    public String toCSVString() {
        return origen.getId() + ","
                + destino.getId() + ","
                + tiempo;
    }

    /**
     * Compara esta conexión con otro objeto.
     *
     * Dos conexiones se consideran iguales cuando tienen
     * el mismo origen y el mismo destino.
     * El tiempo de recorrido no interviene en la comparación.
     *
     * La comparación conserva la dirección de la conexión.
     *
     * @param objeto Objeto con el que se desea comparar.
     * @return true si ambos objetos representan la misma conexión;
     * false en caso contrario.
     */
    @Override
    public boolean equals(Object objeto) {

        if (this == objeto) {
            return true;
        }

        if (!(objeto instanceof Conexion)) {
            return false;
        }

        Conexion otra = (Conexion) objeto;

        return origen.equals(otra.origen)
                && destino.equals(otra.destino);
    }

    /**
     * Genera una representación textual de la conexión.
     *
     * @return Texto con el origen, destino y tiempo de recorrido.
     */
    @Override
    public String toString() {
        return origen.getId() + " -> "
                + destino.getId() + " ("
                + tiempo + ")";
    }
}
