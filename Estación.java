package urbanroute.modelo;

import urbanroute.tda.ListaEnlazada;

/**
 * Representa una estación perteneciente a la red de transporte.
 *
 * Almacena la información de identificación, ubicación, línea,
 * capacidad de pasajeros y las conexiones asociadas a la estación.
 *
 * Dos estaciones se consideran iguales cuando tienen el mismo ID,
 * independientemente de sus demás atributos.
 */
public class Estacion {

    /** Identificador único e inmutable de la estación. */
    private final String id;

    /** Nombre de la estación. */
    private String nombre;

    /** Línea de transporte a la que pertenece. */
    private String linea;

    /** Zona geográfica donde se encuentra la estación. */
    private String zona;

    /** Capacidad máxima de pasajeros por hora. */
    private int capacidad;

    /** Lista propia de conexiones asociadas a la estación. */
    private final ListaEnlazada<Conexion> conexiones;

    /**
     * Crea una estación con sus datos y una lista de conexiones vacía.
     *
     * El identificador se establece durante la construcción y no
     * puede modificarse posteriormente, ya que determina la identidad
     * de la estación dentro de la red.
     *
     * @param id Identificador único de la estación.
     * @param nombre Nombre de la estación.
     * @param linea Línea de transporte a la que pertenece.
     * @param zona Zona geográfica donde se encuentra.
     * @param capacidad Capacidad de pasajeros por hora.
     * @throws IllegalArgumentException Si algún texto es nulo o está
     * vacío, o si la capacidad es negativa.
     */
    public Estacion(String id, String nombre, String linea,
                    String zona, int capacidad) {

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
        this.conexiones = new ListaEnlazada<>();
    }

    /**
     * Comprueba que un texto no sea nulo ni esté vacío.
     *
     * @param valor Texto que se desea validar.
     * @param campo Nombre del campo que se está comprobando.
     * @throws IllegalArgumentException Si el texto es nulo o está vacío.
     */
    private void validarTexto(String valor, String campo) {

        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    campo + " no puede ser nulo ni estar vacío."
            );
        }
    }

    /**
     * Comprueba que la capacidad de pasajeros sea válida.
     *
     * @param capacidad Capacidad que se desea validar.
     * @throws IllegalArgumentException Si la capacidad es negativa.
     */
    private void validarCapacidad(int capacidad) {

        if (capacidad < 0) {
            throw new IllegalArgumentException(
                    "La capacidad no puede ser negativa."
            );
        }
    }

    /**
     * Obtiene el identificador único de la estación.
     *
     * @return Identificador de la estación.
     */
    public String getId() {
        return id;
    }

    /**
     * Obtiene el nombre de la estación.
     *
     * @return Nombre de la estación.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Modifica el nombre de la estación.
     *
     * @param nombre Nuevo nombre de la estación.
     * @throws IllegalArgumentException Si el nombre es nulo o está vacío.
     */
    public void setNombre(String nombre) {
        validarTexto(nombre, "El nombre");
        this.nombre = nombre.trim();
    }

    /**
     * Obtiene la línea de transporte de la estación.
     *
     * @return Línea a la que pertenece la estación.
     */
    public String getLinea() {
        return linea;
    }

    /**
     * Modifica la línea de transporte de la estación.
     *
     * @param linea Nueva línea de transporte.
     * @throws IllegalArgumentException Si la línea es nula o está vacía.
     */
    public void setLinea(String linea) {
        validarTexto(linea, "La línea");
        this.linea = linea.trim();
    }

    /**
     * Obtiene la zona geográfica de la estación.
     *
     * @return Zona donde se encuentra la estación.
     */
    public String getZona() {
        return zona;
    }

    /**
     * Modifica la zona geográfica de la estación.
     *
     * @param zona Nueva zona de ubicación.
     * @throws IllegalArgumentException Si la zona es nula o está vacía.
     */
    public void setZona(String zona) {
        validarTexto(zona, "La zona");
        this.zona = zona.trim();
    }

    /**
     * Obtiene la capacidad de pasajeros por hora.
     *
     * @return Capacidad de la estación.
     */
    public int getCapacidad() {
        return capacidad;
    }

    /**
     * Modifica la capacidad de pasajeros por hora.
     *
     * @param capacidad Nueva capacidad de la estación.
     * @throws IllegalArgumentException Si la capacidad es negativa.
     */
    public void setCapacidad(int capacidad) {
        validarCapacidad(capacidad);
        this.capacidad = capacidad;
    }

    /**
     * Obtiene la lista de conexiones asociadas a la estación.
     *
     * La lista está implementada mediante una estructura propia
     * y solamente almacena objetos de tipo Conexion.
     *
     * @return Lista enlazada de conexiones de la estación.
     *
     * @apiNote Se devuelve la referencia a la lista interna.
     * Las modificaciones de sus elementos deben realizarse
     * coordinadamente con GrafoPonderado para mantener la
     * consistencia de la red.
     */
    public ListaEnlazada<Conexion> getConexiones() {
        return conexiones;
    }

    /**
     * Obtiene la cantidad de conexiones almacenadas en la estación.
     *
     * En una representación dirigida corresponde al grado de salida.
     * En una representación no dirigida, corresponde al grado si
     * cada conexión se registra en las listas de ambos extremos.
     *
     * La operación se realiza en tiempo O(1), porque ListaEnlazada
     * mantiene un contador de elementos.
     *
     * @return Cantidad de conexiones asociadas a la estación.
     */
    public int getGrado() {
        return conexiones.obtenerTamano();
    }

    /**
     * Compara dos estaciones mediante sus identificadores.
     *
     * El resultado permite establecer el orden alfabético
     * de sus identificadores.
     *
     * @param otra Estación con la que se desea comparar.
     * @return Valor negativo, cero o positivo según el orden
     * de los identificadores.
     * @throws IllegalArgumentException Si la estación recibida es null.
     */
    public int comparar(Estacion otra) {

        if (otra == null) {
            throw new IllegalArgumentException(
                    "La estación con la que se desea comparar no puede ser nula."
            );
        }

        return id.compareTo(otra.id);
    }

    /**
     * Escapa un valor de texto para su almacenamiento en formato CSV.
     *
     * Si contiene comas, comillas o saltos de línea, se encierra
     * entre comillas y se duplican las comillas internas.
     *
     * @param valor Texto que se desea convertir.
     * @return Texto preparado para su representación CSV.
     */
    private String escaparCSV(String valor) {

        if (valor == null) {
            return "";
        }

        if (valor.contains(",") || valor.contains("\"")
                || valor.contains("\n") || valor.contains("\r")) {

            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }

        return valor;
    }

    /**
     * Genera la representación CSV de la estación.
     *
     * El formato es: id,nombre,linea,zona,capacidad.
     *
     * Los campos de texto se escapan para evitar errores
     * cuando contienen comas, comillas o saltos de línea.
     *
     * @return Cadena con los datos de la estación separados por comas.
     */
    public String toCSVString() {
        return escaparCSV(id) + ","
                + escaparCSV(nombre) + ","
                + escaparCSV(linea) + ","
                + escaparCSV(zona) + ","
                + capacidad;
    }

    /**
     * Compara esta estación con otro objeto.
     *
     * Dos estaciones son iguales cuando tienen el mismo identificador,
     * independientemente de sus nombres, líneas, zonas o capacidades.
     *
     * @param objeto Objeto con el que se desea comparar.
     * @return true si representa una estación con el mismo ID;
     * false en caso contrario.
     */
    @Override
    public boolean equals(Object objeto) {

        if (this == objeto) {
            return true;
        }

        if (!(objeto instanceof Estacion)) {
            return false;
        }

        Estacion otra = (Estacion) objeto;

        return id.equals(otra.id);
    }

    /**
     * Genera una representación textual de la estación.
     *
     * @return Nombre de la estación seguido de su identificador
     * entre corchetes.
     */
    @Override
    public String toString() {
        return nombre + " [" + id + "]";
    }
}
