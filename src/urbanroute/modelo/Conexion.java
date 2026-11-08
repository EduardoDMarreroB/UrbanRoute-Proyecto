package urbanroute.modelo;

/**
 * Conexion entre dos estaciones con su tiempo de viaje en minutos.
 * Dos conexiones son iguales si tienen el mismo origen y el mismo destino.
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

    /** Dada una de las dos estaciones, devuelve la otra. */
    public Estacion getOtroExtremo(Estacion e) {
        return e.equals(origen) ? destino : origen;
    }

    /** Linea para grafo.csv: origen,destino,tiempo. */
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
