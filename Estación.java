package urbanroute.modelo;

import urbanroute.tda.ListaEnlazada;

/**
 * Representa una estación de la red de transporte.
 */
public class Estacion {

    private final String id; // ID único de la estación que no cambia
    private String nombre; // Nombre de la estación
    private String linea; // Línea a la que pertenece
    private String zona; // Zona geográfica donde está ubicada
    private int capacidad; // Cantidad máxima de pasajeros por hora

    // Lista con las conexiones de la estación
    private final ListaEnlazada<Conexion> conexiones;

    /**
     * Crea una estación con sus datos y prepara su lista de conexiones.
     */
    public Estacion(String id, String nombre, String linea, String zona,
                    int capacidad) {

        // Verificamos que los textos ingresados sean válidos
        validarTexto(id, "El ID");
        validarTexto(nombre, "El nombre");
        validarTexto(linea, "La línea");
        validarTexto(zona, "La zona");
        validarCapacidad(capacidad);

        this.id = id.trim();
        this.nombre = nombre.trim();
        this.linea = linea.trim();
        this.zona = zona.trim();
        this.capacidad = capacidad;
        this.conexiones = new ListaEnlazada<>(); // Empezamos con lista vacía
    }

    /**
     * Revisa que un texto no sea nulo ni esté vacío.
     */
    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) { // Si el texto no sirve
            throw new IllegalArgumentException(
                    campo + " no puede ser nulo ni estar vacío."
            );
        }
    }

    /**
     * Revisa que la capacidad sea un número positivo.
     */
    private void validarCapacidad(int capacidad) {
        if (capacidad < 0) { // No aceptamos números negativos
            throw new IllegalArgumentException(
                    "La capacidad no puede ser negativa."
            );
        }
    }

    /**
     * Obtiene el ID de la estación.
     */
    public String getId() {
        return id;
    }

    /**
     * Obtiene el nombre de la estación.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Cambia el nombre de la estación.
     */
    public void setNombre(String nombre) {
        validarTexto(nombre, "El nombre");
        this.nombre = nombre.trim();
    }

    /**
     * Obtiene la línea a la que pertenece.
     */
    public String getLinea() {
        return linea;
    }

    /**
     * Cambia la línea de la estación.
     */
    public void setLinea(String linea) {
        validarTexto(linea, "La línea");
        this.linea = linea.trim();
    }

    /**
     * Obtiene la zona donde está la estación.
     */
    public String getZona() {
        return zona;
    }

    /**
     * Cambia la zona de la estación.
     */
    public void setZona(String zona) {
        validarTexto(zona, "La zona");
        this.zona = zona.trim();
    }

    /**
     * Obtiene la capacidad de pasajeros.
     */
    public int getCapacidad() {
        return capacidad;
    }

    /**
     * Cambia la capacidad de pasajeros.
     */
    public void setCapacidad(int capacidad) {
        validarCapacidad(capacidad);
        this.capacidad = capacidad;
    }

    /**
     * Obtiene la lista de conexiones de la estación.
     */
    public ListaEnlazada<Conexion> getConexiones() {
        return conexiones;
    }

    /**
     * Obtiene la cantidad de conexiones que tiene la estación.
     */
    public int getGrado() {
        return conexiones.obtenerTamano();
    }

    /**
     * Compara esta estación con otra usando el ID alfabéticamente.
     */
    public int comparar(Estacion otra) {
        if (otra == null) { // Si la otra estación no existe
            throw new IllegalArgumentException(
                    "La estación a comparar no existe."
            );
        }
        return id.compareTo(otra.id);
    }

    /**
     * Arregla el texto para no dar errores al guardarlo en un archivo CSV.
     */
    private String escaparCSV(String valor) {
        if (valor == null) { // Si no hay texto, devolvemos vacío
            return "";
        }

        // Si tiene comas o saltos de línea, lo encerramos en comillas dobles
        if (valor.contains(",") || valor.contains("\"")
                || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor; // Devolvemos el texto normal si no hay caracteres raros
    }

    /**
     * Convierte los datos de la estación en una línea separada por comas.
     */
    public String toCSVString() {
        return escaparCSV(id) + ","
                + escaparCSV(nombre) + ","
                + escaparCSV(linea) + ","
                + escaparCSV(zona) + ","
                + capacidad;
    }

    /**
     * Revisa si dos estaciones son la misma usando su ID.
     */
    public boolean equals(Object objeto) {
        if (this == objeto) { // Si es exactamente la misma en memoria
            return true;
        }

        if (!(objeto instanceof Estacion)) { // Si no es una estación
            return false;
        }

        Estacion otra = (Estacion) objeto; // Conversión segura
        return id.equals(otra.id); // Comparamos los IDs
    }

    /**
     * Muestra el nombre de la estación junto con su ID.
     */
    public String toString() {
        return nombre + " [" + id + "]";
    }
}
