package urbanroute.modelo;

import urbanroute.control.AnalizadorRed;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Grafo ponderado con listas de adyacencia.
 * Guarda la lista de estaciones y usa una TablaHash para buscarlas por id.
 * Por defecto no es dirigido: cada conexion se guarda en los dos sentidos.
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

    /** Agrega la estacion a la lista y a la tabla hash. Devuelve false si es null o el id ya existe. */
    public boolean agregarEstacion(Estacion e) {
        if (e == null || estaciones.buscar(e) != null) {
            return false;
        }
        estaciones.insertar(e);
        indice.insertar(e.getId(), e);
        return true;
    }

    /** Elimina la estacion de la tabla hash y del grafo, junto con todas las conexiones que la tocan. */
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

    /** Agrega la conexion. Si ya existe solo actualiza el tiempo. En un grafo no dirigido tambien agrega la de vuelta. */
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

    private void registrarConexion(Estacion a, Estacion b, double tiempo) {
        Conexion existente = (Conexion) a.getConexiones().buscar(new Conexion(a, b, 0));
        if (existente != null) {
            existente.setTiempo(tiempo);
        } else {
            a.getConexiones().insertar(new Conexion(a, b, tiempo));
        }
    }

    /** Elimina la conexion (y la de vuelta si el grafo no es dirigido). */
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

    /** Devuelve la lista de conexiones que salen de la estacion. */
    public ListaEnlazada obtenerVecinos(Estacion e) {
        return e == null ? new ListaEnlazada() : e.getConexiones();
    }

    /** Busca una estacion por id usando la tabla hash. */
    public Estacion buscarEstacion(String id) {
        return indice.buscar(id);
    }

    /** Devuelve la conexion origen-destino, o null si no existe. */
    public Conexion buscarConexion(Estacion origen, Estacion destino) {
        if (origen == null || destino == null) {
            return null;
        }
        return (Conexion) origen.getConexiones().buscar(new Conexion(origen, destino, 0));
    }

    /** Devuelve las lineas distintas de la red, en orden de aparicion. */
    public ListaEnlazada obtenerLineas() {
        ListaEnlazada lineas = new ListaEnlazada();
        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            String linea = ((Estacion) n.getDato()).getLinea();
            if (lineas.buscar(linea) == null) {
                lineas.insertar(linea);
            }
        }
        return lineas;
    }

    public int numEstaciones() { return estaciones.obtenerTamano(); }

    /** Cuenta las conexiones guardadas (en un grafo no dirigido cada via cuenta dos veces). */
    public int numConexiones() {
        int total = 0;
        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            total += ((Estacion) n.getDato()).getConexiones().obtenerTamano();
        }
        return total;
    }

    /** Elimina todas las conexiones pero deja las estaciones. */
    public void limpiarConexiones() {
        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            ((Estacion) n.getDato()).getConexiones().vaciar();
        }
    }

    /** Deja el grafo y la tabla hash vacios. */
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