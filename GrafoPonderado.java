package urbanroute.modelo;

import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Implementa un grafo ponderado mediante listas de adyacencia propias.
 *
 * Las estaciones se almacenan en una lista enlazada y se localizan
 * mediante una tabla hash indexada por su identificador.
 *
 * El grafo puede ser dirigido o no dirigido. En un grafo no dirigido,
 * cada conexión se registra en las listas de adyacencia de ambos extremos.
 *
 * La clase administra las estaciones y conexiones, manteniendo
 * la consistencia entre la lista principal, el índice hash
 * y las listas de adyacencia.
 */
public class GrafoPonderado {

    /** Lista enlazada propia que almacena las estaciones del grafo. */
    private final ListaEnlazada<Estacion> estaciones;

    /** Tabla hash propia que indexa las estaciones por su identificador. */
    private final TablaHash indice;

    /** Indica si el grafo es dirigido. */
    private final boolean dirigido;

    /**
     * Construye un grafo ponderado utilizando una tabla hash existente.
     *
     * @param indice Tabla hash que utilizará el grafo para localizar
     *               las estaciones.
     * @param dirigido true si el grafo es dirigido;
     *                 false si es no dirigido.
     * @throws IllegalArgumentException Si la tabla hash es nula.
     */
    public GrafoPonderado(TablaHash indice, boolean dirigido) {

        if (indice == null) {
            throw new IllegalArgumentException(
                    "La tabla hash no puede ser nula."
            );
        }

        this.estaciones = new ListaEnlazada<>();
        this.indice = indice;
        this.dirigido = dirigido;

        // Conserva la relación utilizada por la versión actual de TablaHash.
        this.indice.setPropietario(this);
    }

    /**
     * Construye un grafo ponderado no dirigido.
     *
     * @param indice Tabla hash que utilizará el grafo.
     * @throws IllegalArgumentException Si la tabla hash es nula.
     */
    public GrafoPonderado(TablaHash indice) {
        this(indice, false);
    }

    /**
     * Comprueba si una estación pertenece al grafo.
     *
     * Se verifica que la estación recuperada mediante su identificador
     * sea exactamente la misma instancia recibida.
     *
     * Esto evita aceptar objetos externos que tengan el mismo ID
     * que una estación registrada.
     *
     * @param estacion Estación que se desea comprobar.
     * @return true si pertenece al grafo; false en caso contrario.
     */
    private boolean perteneceAlGrafo(Estacion estacion) {

        if (estacion == null || estacion.getId() == null) {
            return false;
        }

        return indice.buscar(estacion.getId()) == estacion;
    }

    /**
     * Agrega una estación al grafo y la registra en la tabla hash.
     *
     * Antes de incorporarla, comprueba que no exista otra estación
     * con el mismo identificador.
     *
     * @param estacion Estación que se desea incorporar.
     * @return true si se agregó correctamente; false si es nula
     *         o su identificador ya existe.
     */
    public boolean agregarEstacion(Estacion estacion) {

        if (estacion == null
                || estacion.getId() == null
                || estacion.getId().trim().isEmpty()) {
            return false;
        }

        String id = estacion.getId();

        // La tabla hash determina si el identificador ya está registrado.
        if (indice.buscar(id) != null) {
            return false;
        }

        // Se registra primero en el índice.
        indice.insertar(id, estacion);

        // Se verifica que la tabla haya almacenado la instancia correcta.
        if (indice.buscar(id) != estacion) {
            indice.eliminar(id);
            return false;
        }

        // Se incorpora a la lista principal.
        estaciones.insertar(estacion);

        return true;
    }

    /**
     * Elimina una estación del grafo y todas las conexiones relacionadas
     * con ella.
     *
     * También elimina su registro de la tabla hash.
     *
     * @param id Identificador de la estación que se desea eliminar.
     * @return true si la estación fue eliminada; false si no existe.
     */
    public boolean eliminarEstacion(String id) {

        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        Estacion estacionEliminada = indice.buscar(id.trim());

        if (estacionEliminada == null) {
            return false;
        }

        /*
         * Se eliminan las conexiones que llegan a la estación.
         * Sus conexiones salientes se eliminan posteriormente
         * al vaciar su propia lista de adyacencia.
         */
        for (Nodo<Estacion> nodoEstacion = estaciones.getCabeza();
             nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {

            Estacion otraEstacion = nodoEstacion.getDato();

            otraEstacion.getConexiones().eliminar(
                    new Conexion(otraEstacion, estacionEliminada, 0.0)
            );
        }

        // Se eliminan todas las conexiones salientes de la estación.
        estacionEliminada.getConexiones().vaciar();

        // Se elimina de la lista de vértices.
        boolean eliminadaLista = estaciones.eliminar(estacionEliminada);

        // Se elimina del índice hash.
        boolean eliminadaIndice = indice.eliminar(estacionEliminada.getId());

        return eliminadaLista && eliminadaIndice;
    }

    /**
     * Agrega una conexión ponderada entre dos estaciones.
     *
     * Actualiza el tiempo si la conexión ya existe.
     * Si el grafo es no dirigido, también registra la conexión inversa.
     *
     * Ambas estaciones deben pertenecer al grafo.
     *
     * @param origen Estación desde la que comienza la conexión.
     * @param destino Estación hacia la que se dirige.
     * @param tiempo Tiempo de recorrido en minutos.
     * @return true si la conexión fue agregada o actualizada;
     *         false si los extremos no pertenecen al grafo,
     *         son la misma estación o el tiempo no es válido.
     */
    public boolean agregarConexion(
            Estacion origen, Estacion destino, double tiempo) {

        if (!perteneceAlGrafo(origen) || !perteneceAlGrafo(destino)) {
            return false;
        }

        if (origen.getId().equals(destino.getId())) {
            return false;
        }

        if (!Double.isFinite(tiempo) || tiempo < 0) {
            return false;
        }

        registrarConexion(origen, destino, tiempo);

        if (!dirigido) {
            registrarConexion(destino, origen, tiempo);
        }

        return true;
    }

    /**
     * Agrega una conexión mediante los identificadores de las estaciones.
     *
     * @param idOrigen Identificador de la estación de origen.
     * @param idDestino Identificador de la estación de destino.
     * @param tiempo Tiempo de recorrido en minutos.
     * @return true si la conexión fue agregada o actualizada;
     *         false si alguna estación no existe o los datos son inválidos.
     */
    public boolean agregarConexion(
            String idOrigen, String idDestino, double tiempo) {

        if (idOrigen == null || idDestino == null) {
            return false;
        }

        Estacion origen = indice.buscar(idOrigen.trim());
        Estacion destino = indice.buscar(idDestino.trim());

        return agregarConexion(origen, destino, tiempo);
    }

    /**
     * Registra una conexión en la lista de adyacencia del origen.
     *
     * Si ya existe una conexión con el mismo destino, actualiza
     * su tiempo. De lo contrario, crea una nueva conexión.
     *
     * @param origen Estación de origen.
     * @param destino Estación de destino.
     * @param tiempo Tiempo de recorrido en minutos.
     */
    private void registrarConexion(
            Estacion origen, Estacion destino, double tiempo) {

        Conexion conexionExistente = buscarConexionInterna(origen, destino);

        if (conexionExistente != null) {
            conexionExistente.setTiempo(tiempo);
        } else {
            origen.getConexiones().insertar(
                    new Conexion(origen, destino, tiempo)
            );
        }
    }

    /**
     * Busca una conexión directamente en la lista de adyacencia
     * de la estación de origen.
     *
     * @param origen Estación desde la que parte la conexión.
     * @param destino Estación hacia la que se dirige.
     * @return Conexión encontrada o null si no existe.
     */
    private Conexion buscarConexionInterna(
            Estacion origen, Estacion destino) {

        if (origen == null || destino == null) {
            return null;
        }

        return origen.getConexiones().buscar(
                new Conexion(origen, destino, 0.0)
        );
    }

    /**
     * Elimina la conexión entre dos estaciones.
     *
     * Si el grafo es no dirigido, también elimina la conexión inversa.
     *
     * @param origen Estación de origen.
     * @param destino Estación de destino.
     * @return true si se eliminó al menos una conexión;
     *         false si los extremos no pertenecen al grafo
     *         o la conexión no existe.
     */
    public boolean eliminarConexion(Estacion origen, Estacion destino) {

        if (!perteneceAlGrafo(origen)
                || !perteneceAlGrafo(destino)) {
            return false;
        }

        if (origen.getId().equals(destino.getId())) {
            return false;
        }

        boolean eliminada = origen.getConexiones().eliminar(
                new Conexion(origen, destino, 0.0)
        );

        if (!dirigido) {
            boolean inversaEliminada = destino.getConexiones().eliminar(
                    new Conexion(destino, origen, 0.0)
            );

            eliminada = eliminada || inversaEliminada;
        }

        return eliminada;
    }

    /**
     * Elimina una conexión utilizando los identificadores
     * de las estaciones.
     *
     * @param idOrigen Identificador de la estación de origen.
     * @param idDestino Identificador de la estación de destino.
     * @return true si se eliminó alguna conexión; false en caso contrario.
     */
    public boolean eliminarConexion(String idOrigen, String idDestino) {

        if (idOrigen == null || idDestino == null) {
            return false;
        }

        Estacion origen = indice.buscar(idOrigen.trim());
        Estacion destino = indice.buscar(idDestino.trim());

        return eliminarConexion(origen, destino);
    }

    /**
     * Obtiene una copia de las conexiones salientes de una estación.
     *
     * Se crea una lista independiente y se copian las conexiones
     * para evitar que las modificaciones sobre el resultado
     * alteren directamente los tiempos almacenados en el grafo.
     *
     * @param estacion Estación cuyos vecinos se desean consultar.
     * @return Lista enlazada con copias de sus conexiones;
     *         una lista vacía si la estación no pertenece al grafo.
     */
    public ListaEnlazada<Conexion> obtenerVecinos(Estacion estacion) {

        ListaEnlazada<Conexion> vecinos = new ListaEnlazada<>();

        if (!perteneceAlGrafo(estacion)) {
            return vecinos;
        }

        for (Nodo<Conexion> nodoConexion =
                estacion.getConexiones().getCabeza();
             nodoConexion != null;
             nodoConexion = nodoConexion.getSiguiente()) {

            Conexion conexion = nodoConexion.getDato();

            vecinos.insertar(
                    new Conexion(
                            conexion.getOrigen(),
                            conexion.getDestino(),
                            conexion.getTiempo()
                    )
            );
        }

        return vecinos;
    }

    /**
     * Busca una estación mediante su identificador.
     *
     * Delega la búsqueda en la tabla hash propia.
     *
     * @param id Identificador de la estación.
     * @return Estación encontrada o null si no existe.
     */
    public Estacion buscarEstacion(String id) {
        if (id == null) {
            return null;
        }

        return indice.buscar(id.trim());
    }

    /**
     * Busca una conexión entre dos estaciones.
     *
     * Devuelve una copia para evitar modificaciones externas
     * sobre el peso almacenado en el grafo.
     *
     * @param origen Estación de origen.
     * @param destino Estación de destino.
     * @return Copia de la conexión encontrada o null si no existe.
     */
    public Conexion buscarConexion(Estacion origen, Estacion destino) {

        if (!perteneceAlGrafo(origen)
                || !perteneceAlGrafo(destino)) {
            return null;
        }

        Conexion conexion = buscarConexionInterna(origen, destino);

        if (conexion == null) {
            return null;
        }

        return new Conexion(
                conexion.getOrigen(),
                conexion.getDestino(),
                conexion.getTiempo()
        );
    }

    /**
     * Obtiene las líneas de transporte distintas presentes en el grafo.
     *
     * Las líneas se devuelven en el orden en que aparecen
     * por primera vez en la lista de estaciones.
     *
     * @return Lista enlazada con los nombres de las líneas.
     */
    public ListaEnlazada<String> obtenerLineas() {

        ListaEnlazada<String> lineas = new ListaEnlazada<>();

        for (Nodo<Estacion> nodoEstacion = estaciones.getCabeza();
             nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {

            String lineaActual = nodoEstacion.getDato().getLinea();

            if (lineas.buscar(lineaActual) == null) {
                lineas.insertar(lineaActual);
            }
        }

        return lineas;
    }

    /**
     * Obtiene la cantidad de estaciones registradas.
     *
     * @return Número de vértices del grafo.
     */
    public int numEstaciones() {
        return estaciones.obtenerTamano();
    }

    /**
     * Cuenta las conexiones almacenadas en las listas de adyacencia.
     *
     * En un grafo no dirigido, cada conexión física se almacena
     * en ambos sentidos, por lo que se contabiliza dos veces.
     *
     * @return Cantidad total de arcos almacenados.
     */
    public int numConexiones() {

        int total = 0;

        for (Nodo<Estacion> nodoEstacion = estaciones.getCabeza();
             nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {

            total += nodoEstacion.getDato()
                    .getConexiones()
                    .obtenerTamano();
        }

        return total;
    }

    /**
     * Cuenta las conexiones físicas de la red.
     *
     * En un grafo no dirigido, divide entre dos el número de arcos
     * almacenados porque cada conexión aparece en ambos sentidos.
     *
     * @return Cantidad de conexiones físicas del grafo.
     */
    public int numConexionesFisicas() {

        int total = numConexiones();

        return dirigido ? total : total / 2;
    }

    /**
     * Elimina todas las conexiones del grafo, pero conserva
     * las estaciones registradas.
     */
    public void limpiarConexiones() {

        for (Nodo<Estacion> nodoEstacion = estaciones.getCabeza();
             nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {

            nodoEstacion.getDato().getConexiones().vaciar();
        }
    }

    /**
     * Elimina todas las estaciones y conexiones del grafo.
     *
     * También limpia el índice hash para mantener la coherencia
     * entre ambas estructuras.
     */
    public void limpiar() {

        limpiarConexiones();

        estaciones.vaciar();

        indice.limpiar();
    }

    /**
     * Obtiene una copia de la lista de estaciones.
     *
     * La lista devuelta es independiente de la lista interna,
     * por lo que insertar o eliminar elementos en ella no altera
     * los vértices registrados en el grafo.
     *
     * Las estaciones contenidas siguen siendo las mismas instancias.
     *
     * @return Copia de la lista de estaciones.
     */
    public ListaEnlazada<Estacion> getEstaciones() {

        ListaEnlazada<Estacion> copia = new ListaEnlazada<>();

        for (Nodo<Estacion> nodoEstacion = estaciones.getCabeza();
             nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {

            copia.insertar(nodoEstacion.getDato());
        }

        return copia;
    }

    /**
     * Indica si el grafo es dirigido.
     *
     * @return true si el grafo es dirigido;
     *         false si es no dirigido.
     */
    public boolean esDirigido() {
        return dirigido;
    }
}
