package urbanroute.modelo;

/**
 * Representa un viaje o ruta directa que une a dos estaciones.
 */
public class Conexion {

    private final Estacion origen; // Estación desde donde arranca el viaje
    private final Estacion destino; // Estación a la que llega el viaje
    private double tiempo; // Minutos que tarda en hacer ese recorrido

    /**
     * Crea una conexión entre dos estaciones indicando el tiempo que tardan.
     */
    public Conexion(Estacion origen, Estacion destino, double tiempo) {
        if (origen == null || destino == null) { // Si falta una estación
            throw new IllegalArgumentException(
                    "Las estaciones no pueden ser nulas."
            );
        }

        validarTiempo(tiempo); // Revisamos que el tiempo tenga sentido

        this.origen = origen;
        this.destino = destino;
        this.tiempo = tiempo;
    }

    /**
     * Te dice de qué estación sale esta conexión.
     */
    public Estacion getOrigen() {
        return origen;
    }

    /**
     * Te dice a qué estación llega esta conexión.
     */
    public Estacion getDestino() {
        return destino;
    }

    /**
     * Te dice cuántos minutos dura el viaje.
     */
    public double getTiempo() {
        return tiempo;
    }

    /**
     * Cambia el tiempo que dura el viaje por uno nuevo.
     */
    public void setTiempo(double tiempo) {
        validarTiempo(tiempo); // Volvemos a revisar el nuevo valor
        this.tiempo = tiempo;
    }

    /**
     * Revisa que el tiempo no sea negativo, infinito o un error matemático.
     */
    private void validarTiempo(double tiempo) {
        // Si el tiempo es un error, es infinito o es menor a cero
        if (Double.isNaN(tiempo) || Double.isInfinite(tiempo)
                || tiempo < 0) {
            throw new IllegalArgumentException(
                    "El tiempo debe ser finito y no negativo."
            );
        }
    }

    /**
     * Le pasas una de las dos estaciones y te devuelve la otra.
     */
    public Estacion getOtroExtremo(Estacion estacion) {
        if (estacion == null) { // Si no pasaron ninguna estación
            throw new IllegalArgumentException(
                    "La estación no puede ser nula."
            );
        }

        if (estacion.equals(origen)) { // Si pasas el origen, da el destino
            return destino;
        }

        if (estacion.equals(destino)) { // Si pasas el destino, da el origen
            return origen;
        }

        // Si la estación no tiene que ver con este viaje
        throw new IllegalArgumentException(
                "La estación no pertenece a esta conexión."
        );
    }

    /**
     * Prepara los datos separados por comas para guardarlos en un archivo.
     */
    public String toCSVString() {
        return origen.getId() + ","
                + destino.getId() + ","
                + tiempo;
    }

    /**
     * Compara si dos conexiones son la misma (el tiempo no importa aquí).
     */
    public boolean equals(Object objeto) {
        if (this == objeto) { // Si es exactamente la misma en memoria
            return true;
        }

        if (!(objeto instanceof Conexion)) { // Si no es una conexión
            return false;
        }

        Conexion otra = (Conexion) objeto; // Conversión segura

        // Solo son iguales si salen y llegan al mismo lugar
        return origen.equals(otra.origen)
                && destino.equals(otra.destino);
    }

    /**
     * Muestra la conexión de forma limpia, ejemplo: A -> B (5.0).
     */
    public String toString() {
        return origen.getId() + " -> "
                + destino.getId() + " ("
                + tiempo + ")";
    }
}
