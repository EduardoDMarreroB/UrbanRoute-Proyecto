package urbanroute.modelo;

import urbanroute.control.AnalizadorRed;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Representa un grafo ponderado mediante listas de adyacencia.
 *
 * Almacena estaciones y sus conexiones, utilizando una tabla hash
 * para localizar las estaciones mediante su identificador.
 *
 * En un grafo dirigido las conexiones tienen un único sentido.
 * En uno no dirigido se registran en ambos sentidos.
 */
public class GrafoPonderado {

    // Lista principal que almacena todas las estaciones del grafo.
    private final ListaEnlazada estaciones;

    // Tabla hash que permite localizar estaciones mediante su identificador.
    private final TablaHash<String, Estacion> indice;

    // Determina si el grafo es dirigido o no dirigido.
    private final boolean dirigido;

    // Analizador encargado de realizar operaciones sobre la red.
    private AnalizadorRed analizador;


    /**
     * Construye un grafo con una tabla hash y una configuración de dirección.
     *
     * @param indice Tabla hash utilizada para localizar estaciones.
     * @param dirigido Indica si las conexiones tienen dirección.
     */
    public GrafoPonderado(
            TablaHash<String, Estacion> indice,
            boolean dirigido) {

        if (indice == null) {
            throw new IllegalArgumentException(
                    "La tabla hash no puede ser null."
            );
        }

        this.estaciones = new ListaEnlazada();
        this.indice = indice;
        this.dirigido = dirigido;

        // Vinculamos la tabla hash con el grafo al que pertenece.
        this.indice.setPropietario(this);
    }


    /**
     * Construye un grafo no dirigido.
     *
     * @param indice Tabla hash utilizada para localizar estaciones.
     */
    public GrafoPonderado(TablaHash<String, Estacion> indice) {
        this(indice, false);
    }

    // GESTIÓN DE ESTACIONES
  
    /**
     * Agrega una estación a la lista principal y a la tabla hash.
     *
     * @param e Estación que se desea agregar.
     * @return true si se agregó; false si es inválida o ya existe.
     */
    public boolean agregarEstacion(Estacion e) {

        // Validamos que la estación tenga un identificador válido.
        if (e == null || e.getId() == null || e.getId().trim().isEmpty()) {
            return false;
        }

        // Evitamos registrar identificadores duplicados.
        if (indice.buscar(e.getId()) != null) {
            return false;
        }

        // Incorporamos la estación a ambas estructuras.
        estaciones.insertar(e);
        indice.insertar(e.getId(), e);

        return true;
    }


    /**
     * Elimina una estación y todas las conexiones relacionadas con ella.
     *
     * Primero elimina las conexiones entrantes y después las salientes.
     *
     * @param id Identificador de la estación.
     * @return true si se eliminó; false si no existe.
     */
    public boolean eliminarEstacion(String id) {

        Estacion e = indice.buscar(id);

        if (e == null) {
            return false;
        }

        // Eliminamos las conexiones entrantes desde las demás estaciones.
        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {

            Estacion otra = (Estacion) n.getDato();

            if (otra != e) {
                otra.getConexiones().eliminar(
                        new Conexion(otra, e, 0)
                );
            }
        }

        // Eliminamos las conexiones salientes de la estación.
        e.getConexiones().vaciar();

        // Retiramos la estación de la lista y de la tabla hash.
        estaciones.eliminar(e);
        indice.eliminar(id);

        return true;
    }

    // GESTIÓN DE CONEXIONES
  
    /**
     * Agrega una conexión entre dos estaciones.
     * Si ya existe, actualiza su tiempo.
     * En grafos no dirigidos también agrega la conexión inversa.
     *
     * @param origen Estación de origen.
     * @param destino Estación de destino.
     * @param tiempo Peso o tiempo de la conexión.
     * @return true si se agregó o actualizó correctamente.
     */
    public boolean agregarConexion(
            Estacion origen,
            Estacion destino,
            double tiempo) {

        // Verificamos que ambas estaciones pertenezcan al grafo.
        if (!perteneceAlGrafo(origen) || !perteneceAlGrafo(destino)) {
            return false;
        }

        // No permitimos conexiones propias ni pesos negativos o no finitos.
        if (origen == destino || !Double.isFinite(tiempo) || tiempo < 0) {
            return false;
        }

        registrarConexion(origen, destino, tiempo);

        // En un grafo no dirigido se almacena también el sentido contrario.
        if (!dirigido) {
            registrarConexion(destino, origen, tiempo);
        }

        return true;
    }


    /**
     * Agrega una conexión utilizando los identificadores de las estaciones.
     *
     * @param idOrigen Identificador de la estación inicial.
     * @param idDestino Identificador de la estación final.
     * @param tiempo Peso de la conexión.
     * @return true si la operación se realizó correctamente.
     */
    public boolean agregarConexion(
            String idOrigen,
            String idDestino,
            double tiempo) {

        return agregarConexion(
                indice.buscar(idOrigen),
                indice.buscar(idDestino),
                tiempo
        );
    }

    /**
     * Registra una conexión en la lista de adyacencia.
     *
     * Si ya existe, actualiza su peso para evitar duplicados.
     */
    private void registrarConexion(Estacion a, Estacion b, double tiempo) {

        Conexion existente = (Conexion) a.getConexiones().buscar(
                new Conexion(a, b, 0)
        );

        if (existente != null) {

            existente.setTiempo(tiempo);

        } else {

            a.getConexiones().insertar(new Conexion(a, b, tiempo));
        }
    }


    /**
     * Elimina una conexión entre dos estaciones.
     * En grafos no dirigidos también elimina la conexión inversa.
     *
     * @param origen Estación inicial.
     * @param destino Estación final.
     * @return true si se eliminó alguna conexión.
     */
    public boolean eliminarConexion(Estacion origen, Estacion destino) {

        if (!perteneceAlGrafo(origen) || !perteneceAlGrafo(destino)) {
            return false;
        }

        boolean eliminada = origen.getConexiones().eliminar(
                new Conexion(origen, destino, 0)
        );

        if (!dirigido) {

            boolean inversa = destino.getConexiones().eliminar(
                    new Conexion(destino, origen, 0)
            );

            eliminada = eliminada || inversa;
        }

        return eliminada;
    }

    // CONSULTAS DEL GRAFO
  
    /**
     * Devuelve las conexiones de una estación.
     *
     * @param e Estación cuyos vecinos se desean consultar.
     * @return Lista de conexiones o una lista vacía si no pertenece al grafo.
     */
    public ListaEnlazada obtenerVecinos(Estacion e) {

        return perteneceAlGrafo(e)
                ? e.getConexiones()
                : new ListaEnlazada();
    }

    /**
     * Busca una estación mediante su identificador.
     *
     * @param id Identificador de la estación.
     * @return Estación encontrada o null si no existe.
     */
    public Estacion buscarEstacion(String id) {
        return indice.buscar(id);
    }

    /**
     * Busca una conexión entre dos estaciones.
     *
     * @param origen Estación inicial.
     * @param destino Estación final.
     * @return Conexión encontrada o null si no existe.
     */
    public Conexion buscarConexion(Estacion origen, Estacion destino) {

        if (!perteneceAlGrafo(origen) || !perteneceAlGrafo(destino)) {
            return null;
        }

        return (Conexion) origen.getConexiones().buscar(
                new Conexion(origen, destino, 0)
        );
    }


    /**
     * Comprueba si una estación pertenece al grafo.
     *
     * Se verifica que el objeto almacenado en la tabla hash sea exactamente
     * la misma referencia recibida.
     */
    private boolean perteneceAlGrafo(Estacion e) {

        return e != null
                && e.getId() != null
                && indice.buscar(e.getId()) == e;
    }


    /**
     * Obtiene las líneas distintas de la red, respetando su orden de aparición.
     *
     * @return Lista de líneas sin duplicados.
     */
    public ListaEnlazada obtenerLineas() {

        ListaEnlazada lineas = new ListaEnlazada();

        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {

            Estacion e = (Estacion) n.getDato();
            String linea = e.getLinea();

            if (lineas.buscar(linea) == null) {
                lineas.insertar(linea);
            }
        }

        return lineas;
    }

    // INFORMACIÓN Y CONTADORES

    /**
     * Devuelve la cantidad de estaciones almacenadas.
     *
     * @return Número de estaciones.
     */
    public int numEstaciones() {
        return estaciones.obtenerTamano();
    }

    /**
     * Cuenta los registros de conexión almacenados en las listas de adyacencia.
     * En grafos no dirigidos cada conexión se cuenta dos veces.
     *
     * @return Cantidad de conexiones almacenadas.
     */
    public int numConexiones() {

        int total = 0;

        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {

            Estacion e = (Estacion) n.getDato();
            total += e.getConexiones().obtenerTamano();
        }

        return total;
    }

    // LIMPIEZA DEL GRAFO

    /**
     * Elimina todas las conexiones sin eliminar las estaciones.
     */
    public void limpiarConexiones() {

        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {

            Estacion e = (Estacion) n.getDato();
            e.getConexiones().vaciar();
        }
    }

    /**
     * Elimina todos los elementos del grafo y vacía la tabla hash.
     */
    public void limpiar() {

        limpiarConexiones();
        estaciones.vaciar();
        indice.limpiar();
    }

    // GETTERS Y SETTERS

    public ListaEnlazada getEstaciones() {
        return estaciones;
    }

    public TablaHash<String, Estacion> getIndice() {
        return indice;
    }

    public boolean esDirigido() {
        return dirigido;
    }

    public AnalizadorRed getAnalizador() {
        return analizador;
    }

    public void setAnalizador(AnalizadorRed analizador) {
        this.analizador = analizador;
    }
}
