package urbanroute.modelo;

/**
 * Arista ponderada entre dos estaciones.
 * Dos conexiones son iguales si comparten origen y destino (por ID),
 * lo que permite buscarlas/eliminarlas en una ListaEnlazada con una "sonda".
 */
public class Conexion {
    private Estacion origen;
    private Estacion destino;
    private double tiempo;

    public Conexion(Estacion origen, Estacion destino, double tiempo) {
        this.origen = origen;
        this.destino = destino;
        this.tiempo = tiempo;
    }

    public Estacion getOrigen() { return origen; }
    public void setOrigen(Estacion origen) { this.origen = origen; }
    public Estacion getDestino() { return destino; }
    public void setDestino(Estacion destino) { this.destino = destino; }
    public double getTiempo() { return tiempo; }
    public void setTiempo(double tiempo) { this.tiempo = tiempo; }

    /** Dado un extremo, devuelve el otro. */
    public Estacion getOtroExtremo(Estacion e) {
        return e.equals(origen) ? destino : origen;
    }

    /** Formato de línea para grafo.csv: idOrigen,idDestino,tiempo */
    public String toCSVString() {
        return origen.getId() + "," + destino.getId() + "," + tiempo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conexion)) return false;
        Conexion otra = (Conexion) o;
        return origen.equals(otra.origen) && destino.equals(otra.destino);
    }

    @Override
    public int hashCode() {
        return 31 * origen.hashCode() + destino.hashCode();
    }

    @Override
    public String toString() {
        return origen.getId() + " -> " + destino.getId() + " (" + tiempo + ")";
    }
}
