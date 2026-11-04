package urbanroute.control;

import urbanroute.interfaz.VisualizadorGrafo;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.tda.ListaEnlazada;
import urbanroute.modelo.Conexion;
import urbanroute.tda.Nodo;
/**
 * Algoritmos de análisis sobre el GrafoPonderado (STUBS para la Semana 6).
 * Colabora con GrafoPonderado (lo recibe y se registra en él) y con VisualizadorGrafo
 * (para resaltar resultados): dependencia bidireccional.
 * Prohibido usar Collections: use ListaEnlazada, Cola y arreglos.
 */
public class AnalizadorRed {
    private GrafoPonderado grafo;
    private VisualizadorGrafo visualizador;
    private double ultimoTiempoRuta;

    public AnalizadorRed(GrafoPonderado grafo) {
        this.grafo = grafo;
        this.grafo.setAnalizador(this);
    }

    /**
     * Ruta mínima por tiempo (Dijkstra).
     * @return ListaEnlazada de Estacion desde origen hasta destino (en orden), o null si no hay camino.
     * Debe guardar el tiempo total en ultimoTiempoRuta.
     */
    /**
     * Calcula la ruta de menor tiempo entre dos estaciones con el algoritmo de Dijkstra.
     * <p>
     * Delega la búsqueda en {@link GrafoPonderado#rutaMenorTiempo(String, String)}, que usa
     * arreglos paralelos dist[]/previo[]/visitado[] indexados por la posición de cada estación.
     * Guarda en {@code ultimoTiempoRuta} la suma de los tiempos de las conexiones recorridas
     * (0.0 si no hay camino).
     * </p>
     *
     * @param idOrigen  ID de la estación de partida
     * @param idDestino ID de la estación de llegada
     * @return {@link ListaEnlazada} de {@link Estacion} desde el origen hasta el destino (en orden),
     *         o {@code null} si alguna estación no existe o no hay camino
     */
    public ListaEnlazada dijkstra(String idOrigen, String idDestino) {
        ListaEnlazada ruta = grafo.rutaMenorTiempo(idOrigen, idDestino);
        if (ruta.estaVacia()) {
            ultimoTiempoRuta = 0.0;
            return null;
        }

        double total = 0.0;
        for (Nodo nodo = ruta.getCabeza(); nodo != null && nodo.getSiguiente() != null;
                nodo = nodo.getSiguiente()) {
            Estacion a = (Estacion) nodo.getDato();
            Estacion b = (Estacion) nodo.getSiguiente().getDato();
            Conexion tramo = (Conexion) grafo.obtenerAdyacentes(a).buscar(new Conexion(a, b, 0));
            if (tramo != null) {
                total += tramo.getTiempo();
            }
        }
        ultimoTiempoRuta = total;

        return ruta;
    }

    /**
     * Componentes conexos (estaciones aisladas / subredes desconectadas) con BFS.
     * @return ListaEnlazada de ListaEnlazada de Estacion (una lista por componente).
     */
    public ListaEnlazada componentesAislados() {
        // TODO: recorrer estaciones; por cada no visitada lanzar bfs(...) usando Cola propia.
        throw new UnsupportedOperationException("Componentes aislados pendiente de implementar");
    }

    /** Recorrido BFS desde 'inicio'. Devuelve las estaciones alcanzadas en orden de visita. */
    public ListaEnlazada bfs(Estacion inicio) {
        // TODO: usar urbanroute.tda.Cola
        throw new UnsupportedOperationException("BFS pendiente de implementar");
    }

    /** Recorrido DFS desde 'inicio' (recursivo o con pila propia). */
    public ListaEnlazada dfs(Estacion inicio) {
        // TODO
        throw new UnsupportedOperationException("DFS pendiente de implementar");
    }

    /** Centralidad de grado: número de conexiones de la estación. */
    public int centralidadGrado(Estacion e) {
        return e.getGrado();
    }

    /**
     * Las k estaciones con mayor centralidad de grado (hubs).
     * @return ListaEnlazada de Estacion ordenada de mayor a menor grado.
     */
    public ListaEnlazada calcularHubs(int k) {
        // TODO: ordenar sin java.util (selección parcial o inserción ordenada en ListaEnlazada).
        throw new UnsupportedOperationException("Hubs pendiente de implementar");
    }

    public GrafoPonderado getGrafo() { return grafo; }
    public VisualizadorGrafo getVisualizador() { return visualizador; }
    public void setVisualizador(VisualizadorGrafo visualizador) { this.visualizador = visualizador; }
    public double getUltimoTiempoRuta() { return ultimoTiempoRuta; }
}
