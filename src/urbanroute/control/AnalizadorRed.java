package urbanroute.control;

import urbanroute.interfaz.VisualizadorGrafo;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.tda.Cola;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Algoritmos de análisis sobre el GrafoPonderado: ruta mínima (Dijkstra),
 * subredes aisladas (BFS) y hubs (centralidad de grado).
 * Colabora con GrafoPonderado (lo recibe y se registra en él) y con VisualizadorGrafo
 * (para resaltar resultados): dependencia bidireccional.
 * Prohibido usar Collections: se usan ListaEnlazada, Cola y arreglos.
 */
public class AnalizadorRed {
    private GrafoPonderado grafo;
    private VisualizadorGrafo visualizador;
    private double ultimoTiempoRuta;

    /**
     * Crea el analizador y se registra en el grafo (relación bidireccional).
     * @param grafo grafo sobre el que se ejecutarán los algoritmos
     */
    public AnalizadorRed(GrafoPonderado grafo) {
        this.grafo = grafo;
        this.grafo.setAnalizador(this);
    }

    // =====================================================================
    //  DIJKSTRA
    // =====================================================================

    /**
     * Ruta de menor tiempo entre dos estaciones (algoritmo de Dijkstra).
     * La estación con menor distancia se busca recorriendo un arreglo (sin cola de prioridad).
     * Complejidad O(V^2), suficiente para una red de metro.
     *
     * @param origen  ID o nombre de la estación de partida
     * @param destino ID o nombre de la estación de llegada
     * @return ListaEnlazada de Estacion desde el origen hasta el destino (ambos incluidos),
     *         o null si alguna estación no existe o no hay camino.
     *         El tiempo total acumulado queda disponible en {@link #getUltimoTiempoRuta()}.
     */
    public ListaEnlazada dijkstra(String origen, String destino) {
        Estacion estOrigen = buscarPorIdONombre(origen);
        Estacion estDestino = buscarPorIdONombre(destino);
        if (estOrigen == null || estDestino == null) {
            return null;
        }

        // Copiamos las estaciones a un arreglo para trabajar con posiciones enteras.
        int cantidad = grafo.numEstaciones();
        Estacion[] vertices = new Estacion[cantidad];
        int i = 0;
        for (Nodo nodo = grafo.getEstaciones().getCabeza(); nodo != null; nodo = nodo.getSiguiente()) {
            vertices[i++] = (Estacion) nodo.getDato();
        }
        int posOrigen = posicionDe(vertices, estOrigen);
        int posDestino = posicionDe(vertices, estDestino);

        double[] distancia = new double[cantidad];
        int[] previo = new int[cantidad];
        boolean[] visitado = new boolean[cantidad];
        for (int k = 0; k < cantidad; k++) {
            distancia[k] = Double.POSITIVE_INFINITY;
            previo[k] = -1;
        }
        distancia[posOrigen] = 0;

        for (int paso = 0; paso < cantidad; paso++) {
            int u = seleccionarMinimo(distancia, visitado);
            if (u == -1) break;              // lo que queda es inalcanzable
            visitado[u] = true;
            if (u == posDestino) break;      // la distancia al destino ya es definitiva

            // Relajar las aristas que salen de u
            ListaEnlazada adyacentes = grafo.obtenerAdyacentes(vertices[u]);
            for (Nodo nc = adyacentes.getCabeza(); nc != null; nc = nc.getSiguiente()) {
                Conexion c = (Conexion) nc.getDato();
                int v = posicionDe(vertices, c.getDestino());
                if (v == -1 || visitado[v]) continue;
                double candidata = distancia[u] + c.getTiempo();
                if (candidata < distancia[v]) {
                    distancia[v] = candidata;
                    previo[v] = u;
                }
            }
        }

        if (distancia[posDestino] == Double.POSITIVE_INFINITY) {
            return null;                     // no existe camino
        }
        ultimoTiempoRuta = distancia[posDestino];

        // Reconstruir desde el destino hacia el origen; insertarInicio deja el orden correcto.
        ListaEnlazada ruta = new ListaEnlazada();
        for (int actual = posDestino; actual != -1; actual = previo[actual]) {
            ruta.insertarInicio(vertices[actual]);
        }
        return ruta;
    }

    // =====================================================================
    //  SUBREDES AISLADAS (BFS)
    // =====================================================================

    /**
     * Componentes conexos de la red actual (cada componente es una subred).
     * @return ListaEnlazada de ListaEnlazada de Estacion (una lista por componente).
     */
    public ListaEnlazada componentesAislados() {
        return componentes(null, null);
    }

    /**
     * Simula la baja de UNA estación y calcula las subredes que quedarían.
     * No modifica el grafo: la estación simplemente se ignora durante el recorrido.
     *
     * @param idEstacion ID de la estación que se "desconecta"
     * @return ListaEnlazada de ListaEnlazada de Estacion (una lista por subred resultante)
     */
    public ListaEnlazada componentesSinEstacion(String idEstacion) {
        return componentes(idEstacion, null);
    }

    /**
     * Simula la baja de una LÍNEA completa y calcula las subredes que quedarían.
     * No modifica el grafo.
     *
     * @param linea nombre de la línea que se "desconecta"
     * @return ListaEnlazada de ListaEnlazada de Estacion (una lista por subred resultante)
     */
    public ListaEnlazada componentesSinLinea(String linea) {
        return componentes(null, linea);
    }

    /**
     * Recorrido BFS desde 'inicio' usando la Cola propia.
     * @param inicio estación de partida
     * @return estaciones alcanzadas, en orden de visita
     */
    public ListaEnlazada bfs(Estacion inicio) {
        return bfs(inicio, null, null);
    }

    /** Agrupa por BFS todas las estaciones no excluidas. */
    private ListaEnlazada componentes(String idExcluido, String lineaExcluida) {
        ListaEnlazada componentes = new ListaEnlazada();
        Estacion[] yaAsignadas = new Estacion[grafo.numEstaciones()];
        int cantAsignadas = 0;

        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion est = (Estacion) n.getDato();
            if (estaExcluida(est, idExcluido, lineaExcluida)) continue;

            if (!estaEn(yaAsignadas, cantAsignadas, est)) {
                ListaEnlazada subred = bfs(est, idExcluido, lineaExcluida);
                componentes.insertar(subred);
                for (Nodo m = subred.getCabeza(); m != null; m = m.getSiguiente()) {
                    yaAsignadas[cantAsignadas++] = (Estacion) m.getDato();
                }
            }
        }
        return componentes;
    }

    /** BFS que ignora la estación (por ID) o la línea excluidas; null significa "no excluir". */
    private ListaEnlazada bfs(Estacion inicio, String idExcluido, String lineaExcluida) {
        ListaEnlazada resultado = new ListaEnlazada();
        if (inicio == null || estaExcluida(inicio, idExcluido, lineaExcluida)) {
            return resultado;
        }

        Cola pendientes = new Cola();
        Estacion[] vistos = new Estacion[grafo.numEstaciones()];
        int cantVistos = 0;

        pendientes.enqueue(inicio);
        vistos[cantVistos++] = inicio;

        while (!pendientes.estaVacia()) {
            Estacion actual = (Estacion) pendientes.dequeue();
            resultado.insertar(actual);

            ListaEnlazada conexiones = grafo.obtenerAdyacentes(actual);
            for (Nodo n = conexiones.getCabeza(); n != null; n = n.getSiguiente()) {
                Estacion vecino = ((Conexion) n.getDato()).getDestino();
                if (estaExcluida(vecino, idExcluido, lineaExcluida)) continue;
                if (!estaEn(vistos, cantVistos, vecino)) {
                    vistos[cantVistos++] = vecino;
                    pendientes.enqueue(vecino);
                }
            }
        }
        return resultado;
    }

    /**
     * Recorrido DFS desde 'inicio' (recursivo o con pila propia).
     * @param inicio estación de partida
     * @return estaciones alcanzadas en orden de visita
     */
    public ListaEnlazada dfs(Estacion inicio) {
        // TODO: pendiente (no requerido en esta entrega)
        throw new UnsupportedOperationException("DFS pendiente de implementar");
    }

    // =====================================================================
    //  HUBS (CENTRALIDAD DE GRADO)
    // =====================================================================

    /**
     * Centralidad de grado: número de conexiones directas de la estación.
     * @param e estación a evaluar
     * @return cantidad de conexiones directas
     */
    public int centralidadGrado(Estacion e) {
        return e.getGrado();
    }

    /**
     * Las k estaciones con mayor centralidad de grado (hubs de transbordo),
     * ordenadas de mayor a menor grado. Usa selección parcial sobre un arreglo:
     * en cada vuelta se coloca en su sitio la estación con más conexiones.
     *
     * @param k cuántos hubs devolver (si hay menos estaciones, se devuelven todas)
     * @return ListaEnlazada de Estacion ordenada por grado descendente
     */
    public ListaEnlazada calcularHubs(int k) {
        int cantidad = grafo.numEstaciones();
        Estacion[] estaciones = new Estacion[cantidad];
        int i = 0;
        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            estaciones[i++] = (Estacion) n.getDato();
        }

        if (k > cantidad) k = cantidad;
        ListaEnlazada hubs = new ListaEnlazada();
        for (int pos = 0; pos < k; pos++) {
            int mejor = pos;
            for (int j = pos + 1; j < cantidad; j++) {
                if (centralidadGrado(estaciones[j]) > centralidadGrado(estaciones[mejor])) {
                    mejor = j;
                }
            }
            Estacion temporal = estaciones[pos];
            estaciones[pos] = estaciones[mejor];
            estaciones[mejor] = temporal;
            hubs.insertar(estaciones[pos]);
        }
        return hubs;
    }

    // =====================================================================
    //  MÉTODOS AUXILIARES
    // =====================================================================

    /** Vértice no visitado con menor distancia, o -1 si no quedan alcanzables. */
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

    /** Posición de la estación en el arreglo (comparando por ID), o -1. */
    private int posicionDe(Estacion[] vertices, Estacion e) {
        for (int k = 0; k < vertices.length; k++) {
            if (vertices[k].equals(e)) return k;
        }
        return -1;
    }

    /** Indica si la estación está entre las primeras 'cantidad' posiciones del arreglo. */
    private boolean estaEn(Estacion[] arreglo, int cantidad, Estacion e) {
        for (int k = 0; k < cantidad; k++) {
            if (arreglo[k].equals(e)) return true;
        }
        return false;
    }

    /** True si la estación es la excluida (por ID) o pertenece a la línea excluida. */
    private boolean estaExcluida(Estacion e, String idExcluido, String lineaExcluida) {
        if (idExcluido != null && e.getId().equals(idExcluido)) return true;
        return lineaExcluida != null && e.getLinea().equals(lineaExcluida);
    }

    /** Busca primero por ID (tabla hash) y, si no existe, por nombre sin distinguir mayúsculas. */
    private Estacion buscarPorIdONombre(String texto) {
        if (texto == null) return null;
        String clave = texto.trim();
        Estacion porId = grafo.buscarEstacion(clave);
        if (porId != null) return porId;
        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion e = (Estacion) n.getDato();
            if (e.getNombre() != null && e.getNombre().equalsIgnoreCase(clave)) return e;
        }
        return null;
    }

    public GrafoPonderado getGrafo() { return grafo; }
    public VisualizadorGrafo getVisualizador() { return visualizador; }
    public void setVisualizador(VisualizadorGrafo visualizador) { this.visualizador = visualizador; }

    /** @return tiempo total (minutos) de la última ruta calculada con {@link #dijkstra} */
    public double getUltimoTiempoRuta() { return ultimoTiempoRuta; }
}
