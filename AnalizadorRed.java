package urbanroute.control;

import urbanroute.interfaz.VisualizadorGrafo;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.tda.Cola;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Algoritmos de análisis sobre el GrafoPonderado.
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
     * @return ListaEnlazada de Estacion desde origen hasta destino (en orden).
     */
    public ListaEnlazada dijkstra(String idOrigen, String idDestino) {
        throw new UnsupportedOperationException("Dijkstra pendiente de implementar");
    }

    /**
     * Componentes conexos (estaciones aisladas / subredes desconectadas) con BFS.
     * @return ListaEnlazada de ListaEnlazada de Estacion (una lista por componente).
     */
    public ListaEnlazada componentesAislados() {
        ListaEnlazada componentes = new ListaEnlazada();
        ListaEnlazada todasLasEstaciones = grafo.getEstaciones();

        Estacion[] visitadosGlobal = new Estacion[grafo.numEstaciones()];
        int cantVisitados = 0;

        for (Nodo n = todasLasEstaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion est = (Estacion) n.getDato();

            // Revisamos si esta estación ya pertenece a una subred que exploramos
            boolean yaVisitada = false;
            for (int i = 0; i < cantVisitados; i++) {
                if (visitadosGlobal[i] != null && visitadosGlobal[i].equals(est)) {
                    yaVisitada = true;
                    break;
                }
            }

            // Si es nueva, encontramos una subred desconectada nueva
            if (!yaVisitada) {
                // El BFS saca todos los panas conectados a esta estación
                ListaEnlazada subred = bfs(est);
                componentes.insertar(subred);

                // Marcamos todas las estaciones de esta nueva subred como visitadas
                for (Nodo nodoSubred = subred.getCabeza(); nodoSubred != null; nodoSubred = nodoSubred.getSiguiente()) {
                    visitadosGlobal[cantVisitados++] = (Estacion) nodoSubred.getDato();
                }
            }
        }

        return componentes;
    }

    /** Recorrido BFS desde 'inicio'. Devuelve las estaciones alcanzadas en orden de visita. */
    public ListaEnlazada bfs(Estacion inicio) {
        if (inicio == null) return new ListaEnlazada();

        ListaEnlazada resultado = new ListaEnlazada();
        Cola pendientes = new Cola();

        Estacion[] vistos = new Estacion[grafo.numEstaciones()];
        int cantVistos = 0;

        pendientes.enqueue(inicio);
        vistos[cantVistos++] = inicio;

        while (!pendientes.estaVacia()) {
            Estacion actual = (Estacion) pendientes.dequeue();
            
            resultado.insertar(actual);

            ListaEnlazada conexiones = grafo.obtenerAdyacentes(actual);

            if (conexiones != null) {
                for (Nodo n = conexiones.getCabeza(); n != null; n = n.getSiguiente()) {
                    Conexion con = (Conexion) n.getDato();
                    Estacion pana = con.getDestino();

                    boolean yaLoVi = false;
                    for (int j = 0; j < cantVistos; j++) {
                        if (vistos[j] != null && vistos[j].equals(pana)) {
                            yaLoVi = true;
                            break;
                        }
                    }

                    if (!yaLoVi) {
                        vistos[cantVistos++] = pana;
                        pendientes.enqueue(pana);
                    }
                }
            }
        }

        return resultado;
    }

    /** Centralidad de grado: número de conexiones de la estación. */
    public int centralidadGrado(Estacion e) {
        return e.getGrado();
    }

    /** Las k estaciones con mayor centralidad de grado (hubs). */
    public ListaEnlazada calcularHubs(int k) {
        throw new UnsupportedOperationException("Hubs pendiente de implementar");
    }

    public GrafoPonderado getGrafo() { return grafo; }
    public VisualizadorGrafo getVisualizador() { return visualizador; }
    public void setVisualizador(VisualizadorGrafo visualizador) { this.visualizador = visualizador; }
    public double getUltimoTiempoRuta() { return ultimoTiempoRuta; }
}
