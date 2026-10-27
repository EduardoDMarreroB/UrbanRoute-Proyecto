package urbanroute.modelo;

import urbanroute.tda.ListaEnlazada;

/**
 * Estación de la red. Mantiene su lista de adyacencia (conexiones salientes),
 * y cada Conexion mantiene a su vez referencias a sus Estaciones: dependencia bidireccional.
 */
public class Estacion {
    private String id;
    private String nombre;
    private String linea;
    private int capacidad;
    private ListaEnlazada conexiones; // elementos: Conexion

    public Estacion(String id, String nombre, String linea, int capacidad) {
        this.id = id;
        this.nombre = nombre;
        this.linea = linea;
        this.capacidad = capacidad;
        this.conexiones = new ListaEnlazada();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getLinea() { return linea; }
    public void setLinea(String linea) { this.linea = linea; }
    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
    public ListaEnlazada getConexiones() { return conexiones; }

    /** Grado de salida (número de conexiones). */
    public int getGrado() { return conexiones.tamano(); }

    /** Compara por ID (orden lexicográfico). Negativo, 0 o positivo. */
    public int comparar(Estacion otra) {
        return this.id.compareTo(otra.id);
    }

    /** Formato de línea para diccionario.csv: id,nombre,linea,capacidad */
    public String toCSVString() {
        return id + "," + nombre + "," + linea + "," + capacidad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Estacion)) return false;
        return id.equals(((Estacion) o).id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() { return nombre + " [" + id + "]"; }
}
