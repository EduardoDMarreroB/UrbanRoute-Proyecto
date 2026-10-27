package urbanroute.control;

import urbanroute.interfaz.VisualizadorGrafo;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.tda.ListaEnlazada;

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
    public ListaEnlazada dijkstra(String idOrigen, String idDestino) {
        // TODO Semana 7: arreglos paralelos dist[]/previo[]/visitado[] indexados por posición
        //                de cada estación (sin PriorityQueue de java.util).
        throw new UnsupportedOperationException("Dijkstra pendiente de implementar");
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
