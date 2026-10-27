package urbanroute.modelo;

import urbanroute.control.AnalizadorRed;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Grafo ponderado con listas de adyacencia propias.
 * - 'estaciones': ListaEnlazada de Estacion (vértices); cada Estacion guarda su lista de Conexion.
 * - 'indice': TablaHash compartida para buscar una estación por ID en O(1) promedio.
 * Por defecto es NO dirigido (una vía de metro se recorre en ambos sentidos):
 * agregarConexion crea la arista de ida y de vuelta.
 * Colabora con TablaHash (propietario) y AnalizadorRed (analizador): dependencia bidireccional.
 */
public class GrafoPonderado {
    private ListaEnlazada estaciones;
    private TablaHash indice;
    private boolean dirigido;
    private AnalizadorRed analizador;

    public GrafoPonderado(TablaHash indice, boolean dirigido) {
        this.estaciones = new ListaEnlazada();
        this.indice = indice;
        this.dirigido = dirigido;
        this.indice.setPropietario(this);
    }

    public GrafoPonderado(TablaHash indice) {
        this(indice, false);
    }

    /** Agrega el vértice. Devuelve false si ya existía una estación con ese ID. */
    public boolean agregarEstacion(Estacion e) {
        if (e == null || estaciones.buscar(e) != null) {
            return false;
        }
        estaciones.insertar(e);
        indice.insertar(e.getId(), e);
        return true;
    }

    /** Elimina la estación y todas las conexiones que la tocan. */
    public boolean eliminarEstacion(String id) {
        Estacion e = indice.buscar(id);
        if (e == null) {
            return false;
        }
        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion otra = (Estacion) n.getDato();
            otra.getConexiones().eliminar(new Conexion(otra, e, 0));
        }
        e.getConexiones().vaciar();
        estaciones.eliminar(e);
        indice.eliminar(id);
        return true;
    }

    /** Agrega (o actualiza el tiempo de) la conexión origen-destino. */
    public boolean agregarConexion(Estacion origen, Estacion destino, double tiempo) {
        if (origen == null || destino == null || origen.equals(destino) || tiempo < 0) {
            return false;
        }
        insertarOActualizar(origen, destino, tiempo);
        if (!dirigido) {
            insertarOActualizar(destino, origen, tiempo);
        }
        return true;
    }

    public boolean agregarConexion(String idOrigen, String idDestino, double tiempo) {
        return agregarConexion(indice.buscar(idOrigen), indice.buscar(idDestino), tiempo);
    }

    private void insertarOActualizar(Estacion a, Estacion b, double tiempo) {
        Conexion existente = (Conexion) a.getConexiones().buscar(new Conexion(a, b, 0));
        if (existente != null) {
            existente.setTiempo(tiempo);
        } else {
            a.getConexiones().insertar(new Conexion(a, b, tiempo));
        }
    }

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

    /** Devuelve la lista de adyacencia de la estación: ListaEnlazada de Conexion. */
    public ListaEnlazada obtenerAdyacentes(Estacion e) {
        return e == null ? new ListaEnlazada() : e.getConexiones();
    }

    public Estacion buscarEstacion(String id) { return indice.buscar(id); }

    public int numEstaciones() { return estaciones.tamano(); }

    /** Cuenta aristas almacenadas (en no dirigido cada vía cuenta dos veces). */
    public int numConexiones() {
        int total = 0;
        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            total += ((Estacion) n.getDato()).getConexiones().tamano();
        }
        return total;
    }

    /** Elimina solo las aristas, conservando las estaciones. */
    public void limpiarConexiones() {
        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            ((Estacion) n.getDato()).getConexiones().vaciar();
        }
    }

    /** Vacía el grafo y el índice hash. */
    public void limpiar() {
        limpiarConexiones();
        estaciones.vaciar();
        indice.limpiar();
    }

    public ListaEnlazada getEstaciones() { return estaciones; }
    public TablaHash getIndice() { return indice; }
    public boolean esDirigido() { return dirigido; }
    public AnalizadorRed getAnalizador() { return analizador; }
    public void setAnalizador(AnalizadorRed analizador) { this.analizador = analizador; }
}
