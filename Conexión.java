package urbanroute.modelo;

// Representa un viaje o ruta directa que une a dos estaciones
public class Conexion {
    private final Estacion origen; // Estacion desde donde arranca el viaje
    private final Estacion destino; // Estacion a la que llega el viaje
    private double tiempo; // Minutos que tarda en hacer ese recorrido


// Crea una conexion entre dos estaciones indicando el tiempo que tardan
    public Conexion(Estacion origen, Estacion destino, double tiempo) {
        if (origen == null || destino == null) { // Si nos falta una estacion
            throw new IllegalArgumentException(
                    "Las estaciones no pueden ser nulas."
            );
        }

        validarTiempo(tiempo); // Revisamos que el tiempo tenga sentido

        this.origen = origen;
        this.destino = destino;
        this.tiempo = tiempo;
    }


// Te dice de que estacion sale esta conexion
    public Estacion getOrigen() {
        return origen;
    }


// Te dice a que estacion llega esta conexion
    public Estacion getDestino() {
        return destino;
    }


// Te dice cuantos minutos dura el viaje
    public double getTiempo() {
        return tiempo;
    }


// Cambia el tiempo que dura el viaje por uno nuevo
    public void setTiempo(double tiempo) {
        validarTiempo(tiempo); // Volvemos a revisar que no pongan locuras
        this.tiempo = tiempo;
    }


// Revisa que el tiempo no sea negativo, infinito o un error matematico
    private void validarTiempo(double tiempo) {
        // Si el tiempo es un error, es infinito o es menor a cero
        if (Double.isNaN(tiempo) || Double.isInfinite(tiempo)
                || tiempo < 0) {
            
            throw new IllegalArgumentException(
                    "El tiempo debe ser finito y no negativo."
            );
        }
    }


// Le pasas una de las dos estaciones y te devuelve la otra
    public Estacion getOtroExtremo(Estacion estacion) {
        if (estacion == null) { // Si no pasaron ninguna estacion
            throw new IllegalArgumentException(
                    "La estación no puede ser nula."
            );
        }

        if (estacion.equals(origen)) { // Si le pasas el origen, te da el destino
            return destino;
        }

        if (estacion.equals(destino)) { // Si le pasas el destino, te da el origen
            return origen;
        }

        // Si pasaste una estacion que no tiene nada que ver con este viaje
        throw new IllegalArgumentException(
                "La estación no pertenece a esta conexión."
        );
    }


// Prepara los datos separados por comas para guardarlos en un archivo
    public String toCSVString() {
        return origen.getId() + ","
                + destino.getId() + ","
                + tiempo;
    }


// Compara si dos conexiones son la misma (el tiempo no importa aqui)
    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) { // Si es exactamente la misma en memoria
            return true;
        }

        if (!(objeto instanceof Conexion)) { // Si ni siquiera es una conexion
            return false;
        }

        Conexion otra = (Conexion) objeto; // Hacemos la conversion segura

        // Solo son iguales si salen del mismo lugar y llegan al mismo lugar
        return origen.equals(otra.origen)
                && destino.equals(otra.destino);
    }


// Muestra la conexion de forma linda, ejemplo: A -> B (5.0)
    @Override
    public String toString() {
        return origen.getId() + " -> "
                + destino.getId() + " ("
                + tiempo + ")";
    }
}
