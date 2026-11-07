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
 * Prohibido usar Collections: usa ListaEnlazada, Cola y arreglos.
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
     * Permite buscar tanto por ID como por Nombre de estación (case-insensitive).
     * @return ListaEnlazada de Estacion desde origen hasta destino (en orden).
     */
    public ListaEnlazada dijkstra(String origenIngresado, String destinoIngresado) {
        if (origenIngresado == null || destinoIngresado == null) return null;

        int cant = grafo.numEstaciones();
        ListaEnlazada nodos = grafo.getEstaciones();

        Estacion[] estaciones = new Estacion[cant];
        double[] distancias = new double[cant];
        int[] previos = new int[cant];
        boolean[] visitados = new boolean[cant];

        int idxOrigen = -1, idxDestino = -1;

        // 1. Mapear estaciones permitiendo búsqueda por ID o por Nombre
        int i = 0;
        for (Nodo n = nodos.getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion e = (Estacion) n.getDato();
            estaciones[i] = e;
            distancias[i] = Double.MAX_VALUE; // Representa infinito
            previos[i] = -1;

            // Comprueba coincidencia por ID o Nombre
            boolean esOrigen = (e.getId() != null && e.getId().equalsIgnoreCase(origenIngresado))
                    || (e.getNombre() != null && e.getNombre().equalsIgnoreCase(origenIngresado));
            
            boolean esDestino = (e.getId() != null && e.getId().equalsIgnoreCase(destinoIngresado))
                    || (e.getNombre() != null && e.getNombre().equalsIgnoreCase(destinoIngresado));

            if (esOrigen) idxOrigen = i;
            if (esDestino) idxDestino = i;
            i++;
        }

        // Si origen o destino no existen en el grafo
        if (idxOrigen == -1 || idxDestino == -1) return null; 

        distancias[idxOrigen] = 0;

        // 2. Algoritmo principal de Dijkstra
        for (int count = 0; count < cant - 1; count++) {
            double min = Double.MAX_VALUE;
            int u = -1;

            // Seleccionar el nodo no visitado de menor distancia
            for (int v = 0; v < cant; v++) {
                if (!visitados[v] && distancias[v] <= min) {
                    min = distancias[v];
                    u = v;
                }
            }

            if (u == -1) break; // Quedan solo nodos inalcanzables
            visitados[u] = true;

            // Relajar aristas adyacentes
            ListaEnlazada adyacentes = grafo.obtenerAdyacentes(estaciones[u]);
            if (adyacentes != null) {
                for (Nodo n = adyacentes.getCabeza(); n != null; n = n.getSiguiente()) {
                    Conexion c = (Conexion) n.getDato();
                    
                    int v = -1;
                    for (int j = 0; j < cant; j++) {
                        if (estaciones[j].getId().equalsIgnoreCase(c.getDestino().getId())) {
                            v = j;
                            break;
                        }
                    }

                    if (v != -1 && !visitados[v] && distancias[u] != Double.MAX_VALUE 
                            && distancias[u] + c.getTiempo() < distancias[v]) {
                        distancias[v] = distancias[u] + c.getTiempo();
                        previos[v] = u;
                    }
                }
            }
        }

        // No hay camino si la distancia al destino sigue siendo infinita
        if (distancias[idxDestino] == Double.MAX_VALUE) return null; 

        // Guardar tiempo total calculado
        this.ultimoTiempoRuta = distancias[idxDestino]; 

        // 3. Reconstrucción de la ruta desde destino hacia origen
        int[] caminoIndices = new int[cant];
        int pasos = 0;
        int actual = idxDestino;
        
        while (actual != -1) {
            caminoIndices[pasos++] = actual;
            actual = previos[actual];
        }
        
        // Volcar a ListaEnlazada propia en orden (Origen -> Destino)
        ListaEnlazada rutaFinal = new ListaEnlazada();
        for (int j = pasos - 1; j >= 0; j--) {
            rutaFinal.insertar(estaciones[caminoIndices[j]]);
        }
        
        return rutaFinal;
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

            // Revisamos si esta estación ya pertenece a una subred explorada
            boolean yaVisitada = false;
            for (int i = 0; i < cantVisitados; i++) {
                if (visitadosGlobal[i] != null && visitadosGlobal[i].equals(est)) {
                    yaVisitada = true;
                    break;
                }
            }

            // Si es nueva, exploramos su subred con BFS
            if (!yaVisitada) {
                ListaEnlazada subred = bfs(est);
                componentes.insertar(subred);

                // Marcamos todas sus estaciones como visitadas
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
