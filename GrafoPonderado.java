package urbanroute.modelo;

import urbanroute.control.AnalizadorRed;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Grafo ponderado con listas de adyacencia.
 * Guarda las estaciones en una lista enlazada y usa una TablaHash para buscarlas por id.
 * Por defecto no es dirigido: cada conexión se almacena en los dos sentidos.
 */
public class GrafoPonderado {
    private ListaEnlazada<Estacion> estaciones; // Vértices almacenados en orden de incorporación.
    private TablaHash<String, Estacion> indice;  // Índice de búsqueda por identificador.
    private boolean dirigido;
    private AnalizadorRed analizador;

    public GrafoPonderado(TablaHash<String, Estacion> indice, boolean dirigido) {
        this.estaciones = new ListaEnlazada<>();
        this.indice = indice;
        this.dirigido = dirigido;
        this.indice.setPropietario(this);
    }

    public GrafoPonderado(TablaHash<String, Estacion> indice) {
        this(indice, false);
    }

    /** Agrega la estación a la lista y a la tabla hash. */
    public boolean agregarEstacion(Estacion estacion) {
        if (estacion == null || estaciones.buscar(estacion) != null) {
            return false;
        }
        estaciones.insertar(estacion);
        indice.insertar(estacion.getId(), estacion);
        return true;
    }

    /** Elimina una estación y todas las conexiones que la involucren. */
    public boolean eliminarEstacion(String id) {
        Estacion estacionEliminada = indice.buscar(id);
        if (estacionEliminada == null) {
            return false;
        }

        for (Nodo<Estacion> nodoEstacion = estaciones.getCabeza(); nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {
            Estacion otraEstacion = nodoEstacion.getDato();
            otraEstacion.getConexiones().eliminar(new Conexion(otraEstacion, estacionEliminada, 0));
        }

        estacionEliminada.getConexiones().vaciar();
        estaciones.eliminar(estacionEliminada);
        indice.eliminar(id);
        return true;
    }

    /** Agrega una conexión; si ya existe, actualiza el tiempo. */
    public boolean agregarConexion(Estacion origen, Estacion destino, double tiempo) {
        if (origen == null || destino == null || origen.equals(destino) || tiempo < 0) {
            return false;
        }
        registrarConexion(origen, destino, tiempo);
        if (!dirigido) {
            registrarConexion(destino, origen, tiempo);
        }
        return true;
    }

    public boolean agregarConexion(String idOrigen, String idDestino, double tiempo) {
        return agregarConexion(indice.buscar(idOrigen), indice.buscar(idDestino), tiempo);
    }

    private void registrarConexion(Estacion origen, Estacion destino, double tiempo) {
        Conexion conexionExistente = origen.getConexiones().buscar(new Conexion(origen, destino, 0));
        if (conexionExistente != null) {
            conexionExistente.setTiempo(tiempo);
        } else {
            origen.getConexiones().insertar(new Conexion(origen, destino, tiempo));
        }
    }

    /** Elimina la conexión y su inversa cuando el grafo no es dirigido. */
    public boolean eliminarConexion(Estacion origen, Estacion destino) {
        if (origen == null || destino == null) {
            return false;
        }
        boolean eliminada = origen.getConexiones().eliminar(new Conexion(origen, destino, 0));
        if (!dirigido) {
            eliminada = destino.getConexiones().eliminar(new Conexion(destino, origen, 0)) || eliminada;
        }
        return eliminada;
    }

    /** Devuelve la lista tipada de conexiones que salen de la estación. */
    public ListaEnlazada<Conexion> obtenerVecinos(Estacion estacion) {
        return estacion == null ? new ListaEnlazada<>() : estacion.getConexiones();
    }

    public Estacion buscarEstacion(String id) { return indice.buscar(id); }

    /** Devuelve la conexión origen-destino, o null si no existe. */
    public Conexion buscarConexion(Estacion origen, Estacion destino) {
        if (origen == null || destino == null) {
            return null;
        }
        return origen.getConexiones().buscar(new Conexion(origen, destino, 0));
    }

    /** Devuelve las líneas distintas de la red, en orden de aparición. */
    public ListaEnlazada<String> obtenerLineas() {
        ListaEnlazada<String> lineas = new ListaEnlazada<>();
        for (Nodo<Estacion> nodoEstacion = estaciones.getCabeza(); nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {
            String lineaActual = nodoEstacion.getDato().getLinea();
            if (lineas.buscar(lineaActual) == null) {
                lineas.insertar(lineaActual);
            }
        }
        return lineas;
    }

    public int numEstaciones() { return estaciones.obtenerTamano(); }

    /** Cuenta las conexiones guardadas (en un grafo no dirigido cada vía cuenta dos veces). */
    public int numConexiones() {
        int total = 0;
        for (Nodo<Estacion> nodoEstacion = estaciones.getCabeza(); nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {
            total += nodoEstacion.getDato().getConexiones().obtenerTamano();
        }
        return total;
    }

    /** Elimina todas las conexiones, pero conserva las estaciones. */
    public void limpiarConexiones() {
        for (Nodo<Estacion> nodoEstacion = estaciones.getCabeza(); nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {
            nodoEstacion.getDato().getConexiones().vaciar();
        }
    }

    /** Deja vacíos el grafo y su índice hash. */
    public void limpiar() {
        limpiarConexiones();
        estaciones.vaciar();
        indice.limpiar();
    }

    public ListaEnlazada<Estacion> getEstaciones() { return estaciones; }
    public TablaHash<String, Estacion> getIndice() { return indice; }
    public boolean esDirigido() { return dirigido; }
    public AnalizadorRed getAnalizador() { return analizador; }
    public void setAnalizador(AnalizadorRed analizador) { this.analizador = analizador; }
}
