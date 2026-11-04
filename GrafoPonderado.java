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
    private double ultimoTiempoRuta;

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

        /**
     * Calcula la ruta de menor tiempo entre dos estaciones aplicando el algoritmo de Dijkstra.
     * <p>
     * La selección del vértice no visitado con menor distancia se realiza recorriendo un arreglo
     * primitivo, sin colas de prioridad. Complejidad: O(V² + E·V), que equivale a O(V²) en redes
     * dispersas como una red de metro.
     * </p>
     *
     * @param idONombreOrigen  ID (o, en su defecto, nombre) de la estación de partida
     * @param idONombreDestino ID (o, en su defecto, nombre) de la estación de llegada
     * @return {@link ListaEnlazada} de {@link Estacion} en orden desde el origen hasta el destino
     *         (ambos incluidos). Si alguna estación no existe o no hay ruta posible, devuelve una
     *         lista vacía. Si origen y destino coinciden, devuelve una lista con esa única estación.
     */
    public ListaEnlazada rutaMenorTiempo(String idONombreOrigen, String idONombreDestino) {
        ListaEnlazada ruta = new ListaEnlazada();

        Estacion estOrigen = resolverEstacion(idONombreOrigen);
        Estacion estDestino = resolverEstacion(idONombreDestino);
        if (estOrigen == null || estDestino == null) {
            return ruta;
        }

        // Instantánea de los vértices en un arreglo para trabajar con posiciones enteras.
        int n = estaciones.tamano();
        Estacion[] vertices = new Estacion[n];
        int i = 0;
        for (Nodo nodo = estaciones.getCabeza(); nodo != null; nodo = nodo.getSiguiente()) {
            vertices[i++] = (Estacion) nodo.getDato();
        }

        int posOrigen = posicionDe(vertices, estOrigen);
        int posDestino = posicionDe(vertices, estDestino);
        if (posOrigen < 0 || posDestino < 0) {
            return ruta;
        }

        double[] distancia = new double[n];
        int[] previo = new int[n];
        boolean[] visitado = new boolean[n];
        for (int k = 0; k < n; k++) {
            distancia[k] = Double.POSITIVE_INFINITY;
            previo[k] = -1;
        }
        distancia[posOrigen] = 0.0;

        for (int iteracion = 0; iteracion < n; iteracion++) {
            int u = seleccionarMinimo(distancia, visitado);
            if (u < 0) {
                break; // Los vértices restantes son inalcanzables.
            }
            visitado[u] = true;
            if (u == posDestino) {
                break; // Distancia mínima al destino ya definitiva.
            }

            // Relajación de las aristas salientes de u.
            for (Nodo nc = vertices[u].getConexiones().getCabeza(); nc != null; nc = nc.getSiguiente()) {
                Conexion conexion = (Conexion) nc.getDato();
                int v = posicionDe(vertices, conexion.getDestino());
                if (v < 0 || visitado[v]) {
                    continue;
                }
                double candidata = distancia[u] + conexion.getTiempo();
                if (candidata < distancia[v]) {
                    distancia[v] = candidata;
                    previo[v] = u;
                }
            }
        }

        if (distancia[posDestino] == Double.POSITIVE_INFINITY) {
            return ruta; // No existe camino entre origen y destino.
        }

        // Reconstrucción desde el destino hacia el origen; insertarInicio deja el orden correcto.
        for (int actual = posDestino; actual != -1; actual = previo[actual]) {
            ruta.insertarInicio(vertices[actual]);
        }
        return ruta;
    }

    /**
     * Selecciona el vértice no visitado con menor distancia acumulada, recorriendo los arreglos.
     *
     * @param distancia arreglo de distancias mínimas provisionales desde el origen
     * @param visitado  arreglo que indica qué vértices ya fueron procesados
     * @return posición del vértice elegido, o -1 si no quedan vértices alcanzables sin visitar
     */
    private int seleccionarMinimo(double[] distancia, boolean[] visitado) {
        int mejor = -1;
        double menor = Double.POSITIVE_INFINITY;
        for (int k = 0; k < distancia.length; k++) {
            if (!visitado[k] && distancia[k] < menor) {
                menor = distancia[k];
                mejor = k;
            }
        }
        return mejor;
    }

    /**
     * Busca la posición de una estación dentro del arreglo de vértices (comparación por ID).
     *
     * @param vertices arreglo con las estaciones del grafo
     * @param e        estación a localizar
     * @return posición de la estación en el arreglo, o -1 si no se encuentra
     */
    private int posicionDe(Estacion[] vertices, Estacion e) {
        for (int k = 0; k < vertices.length; k++) {
            if (vertices[k].equals(e)) {
                return k;
            }
        }
        return -1;
    }

    /**
     * Resuelve una estación a partir de su ID (búsqueda O(1) en el índice hash) o, si no
     * coincide ningún ID, a partir de su nombre (sin distinguir mayúsculas de minúsculas).
     *
     * @param idONombre ID o nombre de la estación
     * @return la estación encontrada, o {@code null} si no existe o el parámetro es nulo
     */
    private Estacion resolverEstacion(String idONombre) {
        if (idONombre == null) {
            return null;
        }
        String clave = idONombre.trim();
        Estacion porId = indice.buscar(clave);
        if (porId != null) {
            return porId;
        }
        for (Nodo n = estaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion e = (Estacion) n.getDato();
            if (e.getNombre() != null && e.getNombre().equalsIgnoreCase(clave)) {
                return e;
            }
        }
        return null;
    }
}
