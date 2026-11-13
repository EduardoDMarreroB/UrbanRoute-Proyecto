package urbanroute.modelo;

import urbanroute.control.AnalizadorRed;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Grafo ponderado con listas de adyacencia.
 * <p>
 * Guarda todas las estaciones en una {@link ListaEnlazada} y usa una
 * {@link TablaHash} para encontrarlas rápidamente por su ID. Cada estación
 * tiene su propia lista de conexiones, así que el grafo se representa como
 * una lista de adyacencia.
 * <p>
 * Por defecto el grafo no es dirigido: cuando se agrega una conexión, se
 * guarda también en el sentido contrario para que se pueda recorrer en ambos
 * sentidos.
 */
public class GrafoPonderado {

    /** Lista con todas las estaciones registradas en el grafo. */
    private ListaEnlazada<Estacion> estaciones;

    /** Tabla hash que permite buscar estaciones por su ID. */
    private TablaHash indice;

    /** Indica si el grafo es dirigido o no. */
    private boolean dirigido;

    /**
     * Crea un grafo ponderado indicando si es dirigido o no.
     *
     * @param indice   tabla hash que se usará para buscar estaciones por ID.
     * @param dirigido {@code true} si el grafo es dirigido; {@code false} si es no dirigido.
     * @throws IllegalArgumentException si la tabla hash es {@code null}.
     */
    public GrafoPonderado(TablaHash indice, boolean dirigido) {
        if (indice == null) {
            throw new IllegalArgumentException(
                    "La tabla hash no puede ser nula."
            );
        }
        this.estaciones = new ListaEnlazada();
        this.indice = indice;
        this.dirigido = dirigido;
    }

    /**
     * Construye un grafo ponderado no dirigido.
     * <p>
     * Usa el índice de la tabla hash para buscar las estaciones por su ID.
     *
     * @param indice tabla hash que se usará para buscar estaciones por ID.
     */
    public GrafoPonderado(TablaHash indice) {
        this(indice, false);
    }

    /**
     * Agrega una estación al grafo y a la tabla hash.
     * <p>
     * No se permite agregar una estación nula ni una cuyo ID ya exista.
     *
     * @param e estación que se quiere agregar.
     * @return {@code true} si se agregó; {@code false} si era {@code null} o el ID ya existía.
     */
    public boolean agregarEstacion(Estacion e) {
        if (e == null || estaciones.buscar(e) != null) {
            return false;
        }
        estaciones.insertar(e);
        indice.insertar(e.getId(), e);
        return true;
    }

    /**
     * Elimina una estación del grafo y de la tabla hash.
     * <p>
     * Antes de eliminarla se recorre el resto de estaciones para quitar las
     * conexiones que apuntaban a ella. De esta forma no quedan conexiones
     * "colgadas" apuntando a una estación que ya no existe.
     *
     * @param id ID de la estación que se quiere eliminar.
     * @return {@code true} si se eliminó; {@code false} si no existía.
     */
    public boolean eliminarEstacion(String id) {
        Estacion e = indice.buscar(id);
        if (e == null) {
            return false;
        }

        // Se usa tiempo 0 porque al comparar conexiones solo importan origen y destino.
        for (Nodo<Estacion> n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion otra = (Estacion) n.getDato();
            otra.getConexiones().eliminar(new Conexion(otra, e, 0));
        }
        e.getConexiones().vaciar();
        estaciones.eliminar(e);
        indice.eliminar(id);
        return true;
    }

    /**
     * Agrega una conexión entre dos estaciones con un tiempo determinado.
     * <p>
     * Si la conexión ya existía, se actualiza su tiempo. Si el grafo no es
     * dirigido, también se registra la conexión en sentido inverso.
     *
     * @param origen  estación de origen.
     * @param destino estación de destino.
     * @param tiempo  tiempo de viaje entre las dos estaciones.
     * @return {@code true} si la conexión se registró; {@code false} si los datos no son válidos.
     */
    public boolean agregarConexion(Estacion origen, Estacion destino,
            double tiempo) {
        if (origen == null || destino == null || origen.equals(destino) ||
                tiempo < 0) {
            return false;
        }

        registrarConexion(origen, destino, tiempo);

        // En un grafo no dirigido la conexión debe poder recorrerse en ambos sentidos.
        if (!dirigido) {
            registrarConexion(destino, origen, tiempo);
        }
        return true;
    }

    /**
     * Sobrecarga que agrega una conexión buscando las estaciones por sus ID.
     *
     * @param idOrigen  ID de la estación de origen.
     * @param idDestino ID de la estación de destino.
     * @param tiempo    tiempo de viaje entre las dos estaciones.
     * @return {@code true} si la conexión se registró; {@code false} si alguna estación no existe o los datos no son válidos.
     */
    public boolean agregarConexion(String idOrigen, String idDestino,
            double tiempo) {
        return agregarConexion(indice.buscar(idOrigen),
                indice.buscar(idDestino), tiempo);
    }

    /**
     * Guarda el tiempo entre dos estaciones.
     * <p>
     * Si la conexión ya existía, solo se actualiza su tiempo. Se busca con
     * tiempo 0 porque al comparar conexiones solo importan origen y destino.
     *
     * @param a      estación de origen.
     * @param b      estación de destino.
     * @param tiempo tiempo de viaje entre las dos estaciones.
     */
    private void registrarConexion(Estacion a, Estacion b, double tiempo) {
        Conexion existente = (Conexion)
                a.getConexiones().buscar(new Conexion(a, b, 0));
        if (existente != null) {
            existente.setTiempo(tiempo);
        } else {
            a.getConexiones().insertar(new Conexion(a, b, tiempo));
        }
    }

    /**
     * Elimina la conexión entre dos estaciones.
     * <p>
     * Si el grafo no es dirigido, también se elimina la conexión en sentido
     * inverso para que el grafo quede consistente.
     *
     * @param origen  estación de origen.
     * @param destino estación de destino.
     * @return {@code true} si se eliminó alguna de las dos conexiones; {@code false} en caso contrario.
     */
    public boolean eliminarConexion(Estacion origen, Estacion destino) {
        if (origen == null || destino == null) {
            return false;
        }
        boolean eliminada = origen.getConexiones().
                eliminar(new Conexion(origen, destino, 0));
        if (!dirigido) {
            eliminada = destino.getConexiones().
                    eliminar(new Conexion(destino, origen, 0)) || eliminada;
        }
        return eliminada;
    }

    /**
     * Devuelve la lista de conexiones que salen de una estación.
     *
     * @param e estación de la que se quieren obtener las conexiones.
     * @return lista de conexiones de la estación, o una lista vacía si la estación es {@code null}.
     */
    public ListaEnlazada obtenerVecinos(Estacion e) {
        return e == null ? new ListaEnlazada() : e.getConexiones();
    }

    /**
     * Busca una estación por su ID usando la tabla hash.
     *
     * @param id ID de la estación que se quiere buscar.
     * @return la estación encontrada, o {@code null} si no existe.
     */
    public Estacion buscarEstacion(String id) {
        return indice.buscar(id);
    }

    /**
     * Devuelve la conexión entre dos estaciones.
     *
     * @param origen  estación de origen.
     * @param destino estación de destino.
     * @return la conexión encontrada, o {@code null} si no existe.
     */
    public Conexion buscarConexion(Estacion origen, Estacion destino) {
        if (origen == null || destino == null) {
            return null;
        }
        return (Conexion) origen.getConexiones().
                buscar(new Conexion(origen, destino, 0));
    }

    /**
     * Devuelve las líneas distintas que aparecen en la red.
     * <p>
     * Se recorre la lista de estaciones y se van guardando las líneas sin
     * repetir, manteniendo el orden en que aparecen.
     *
     * @return lista enlazada con los nombres de las líneas.
     */
    public ListaEnlazada obtenerLineas() {
        ListaEnlazada lineas = new ListaEnlazada();
        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            String linea = ((Estacion) n.getDato()).getLinea();

            // Solo se agrega la línea si todavía no estaba en la lista.
            if (lineas.buscar(linea) == null) {
                lineas.insertar(linea);
            }
        }
        return lineas;
    }

    /**
     * Devuelve el número de estaciones del grafo.
     *
     * @return cantidad de estaciones registradas.
     */
    public int numEstaciones() { return estaciones.obtenerTamano(); }

    /**
     * Cuenta las conexiones guardadas.
     * <p>
     * En un grafo no dirigido cada viaje aparece dos veces, una por cada
     * sentido, porque así se guarda internamente.
     *
     * @return total de conexiones almacenadas.
     */
    public int numConexiones() {
        int total = 0;
        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            total += ((Estacion) n.getDato()).getConexiones().obtenerTamano();
        }
        return total;
    }

    /**
     * Elimina todas las conexiones pero mantiene las estaciones.
     */
    public void limpiarConexiones() {
        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            ((Estacion) n.getDato()).getConexiones().vaciar();
        }
    }

    /**
     * Deja vacíos el grafo y la tabla hash.
     */
    public void limpiar() {
        limpiarConexiones();
        estaciones.vaciar();
        indice.limpiar();
    }

    /**
     * Devuelve la lista con todas las estaciones del grafo.
     *
     * @return lista enlazada de estaciones.
     */
    public ListaEnlazada<Estacion> getEstaciones() { return estaciones; }

    /**
     * Devuelve la tabla hash que indexa las estaciones por ID.
     *
     * @return tabla hash de estaciones.
     */
    public TablaHash getIndice() { return indice; }

    /**
     * Indica si el grafo es dirigido.
     *
     * @return {@code true} si el grafo es dirigido; {@code false} si no lo es.
     */
    public boolean esDirigido() { return dirigido; }
}
