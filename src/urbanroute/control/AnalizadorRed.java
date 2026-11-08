package urbanroute.control;

import urbanroute.interfaz.VisualizadorGrafo;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.tda.Cola;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Algoritmos sobre el grafo: ruta minima (Dijkstra), subredes aisladas (BFS)
 * y estaciones con mas conexiones (centralidad de grado).
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
     * Calcula la ruta de menor tiempo entre dos estaciones (id o nombre).
     * Devuelve la lista de estaciones del origen al destino, o null si no hay camino.
     * El tiempo total queda en getUltimoTiempoRuta().
     * Complejidad: O(V^2 + E*V).
     */
    public ListaEnlazada dijkstra(String origen, String destino) {
        Estacion estOrigen = buscarEstacion(origen);
        Estacion estDestino = buscarEstacion(destino);
        if (estOrigen == null || estDestino == null) {
            return null;
        }

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
            int u = buscarMinimo(distancia, visitado);
            if (u == -1) break;
            visitado[u] = true;
            if (u == posDestino) break;

            ListaEnlazada vecinos = grafo.obtenerVecinos(vertices[u]);
            for (Nodo nc = vecinos.getCabeza(); nc != null; nc = nc.getSiguiente()) {
                Conexion c = (Conexion) nc.getDato();
                int v = posicionDe(vertices, c.getDestino());
                if (v == -1 || visitado[v]) continue;
                double nuevaDistancia = distancia[u] + c.getTiempo();
                if (nuevaDistancia < distancia[v]) {
                    distancia[v] = nuevaDistancia;
                    previo[v] = u;
                }
            }
        }

        if (distancia[posDestino] == Double.POSITIVE_INFINITY) {
            return null;
        }
        ultimoTiempoRuta = distancia[posDestino];

        ListaEnlazada ruta = new ListaEnlazada();
        for (int actual = posDestino; actual != -1; actual = previo[actual]) {
            ruta.insertarInicio(vertices[actual]);
        }
        return ruta;
    }

    /**
     * Agrupa las estaciones en subredes (componentes conexos) usando BFS.
     * Devuelve una lista donde cada elemento es la ListaEnlazada de estaciones de una subred.
     */
    public ListaEnlazada detectarSubredesBFS() {
        return agruparSubredes(null, null);
    }

    /** Igual que detectarSubredesBFS pero sin tomar en cuenta una estacion (simula su baja). */
    public ListaEnlazada detectarSubredesSinEstacion(String idEstacion) {
        return agruparSubredes(idEstacion, null);
    }

    /** Igual que detectarSubredesBFS pero sin tomar en cuenta una linea completa (simula su baja). */
    public ListaEnlazada detectarSubredesSinLinea(String linea) {
        return agruparSubredes(null, linea);
    }

    /** Recorrido en anchura desde la estacion de inicio. Devuelve las estaciones en orden de visita. */
    public ListaEnlazada bfs(Estacion inicio) {
        return recorrerBFS(inicio, null, null);
    }

    /** Recorrido en profundidad desde la estacion de inicio. Pendiente de implementar. */
    public ListaEnlazada dfs(Estacion inicio) {
        throw new UnsupportedOperationException("DFS pendiente de implementar");
    }

    /**
     * Devuelve las k estaciones con mas conexiones (hubs), de mayor a menor.
     * Ordena con seleccion sobre un arreglo.
     */
    public ListaEnlazada calcularCentralidadGrado(int k) {
        int cantidad = grafo.numEstaciones();
        Estacion[] estaciones = new Estacion[cantidad];
        int i = 0;
        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            estaciones[i++] = (Estacion) n.getDato();
        }

        if (k > cantidad) k = cantidad;
        ListaEnlazada hubs = new ListaEnlazada();
        for (int pos = 0; pos < k; pos++) {
            int mayor = pos;
            for (int j = pos + 1; j < cantidad; j++) {
                if (estaciones[j].getGrado() > estaciones[mayor].getGrado()) {
                    mayor = j;
                }
            }
            Estacion temporal = estaciones[pos];
            estaciones[pos] = estaciones[mayor];
            estaciones[mayor] = temporal;
            hubs.insertar(estaciones[pos]);
        }
        return hubs;
    }

    private ListaEnlazada agruparSubredes(String idAusente, String lineaAusente) {
        ListaEnlazada subredes = new ListaEnlazada();
        Estacion[] asignadas = new Estacion[grafo.numEstaciones()];
        int cantAsignadas = 0;

        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion est = (Estacion) n.getDato();
            if (estaAusente(est, idAusente, lineaAusente)) continue;

            if (!estaEn(asignadas, cantAsignadas, est)) {
                ListaEnlazada subred = recorrerBFS(est, idAusente, lineaAusente);
                subredes.insertar(subred);
                for (Nodo m = subred.getCabeza(); m != null; m = m.getSiguiente()) {
                    asignadas[cantAsignadas++] = (Estacion) m.getDato();
                }
            }
        }
        return subredes;
    }

    private ListaEnlazada recorrerBFS(Estacion inicio, String idAusente, String lineaAusente) {
        ListaEnlazada resultado = new ListaEnlazada();
        if (inicio == null || estaAusente(inicio, idAusente, lineaAusente)) {
            return resultado;
        }

        Cola pendientes = new Cola();
        Estacion[] vistos = new Estacion[grafo.numEstaciones()];
        int cantVistos = 0;

        pendientes.encolar(inicio);
        vistos[cantVistos++] = inicio;

        while (!pendientes.esVacia()) {
            Estacion actual = (Estacion) pendientes.desencolar();
            resultado.insertar(actual);

            ListaEnlazada vecinos = grafo.obtenerVecinos(actual);
            for (Nodo n = vecinos.getCabeza(); n != null; n = n.getSiguiente()) {
                Estacion vecino = ((Conexion) n.getDato()).getDestino();
                if (estaAusente(vecino, idAusente, lineaAusente)) continue;
                if (!estaEn(vistos, cantVistos, vecino)) {
                    vistos[cantVistos++] = vecino;
                    pendientes.encolar(vecino);
                }
            }
        }
        return resultado;
    }

    private int buscarMinimo(double[] distancia, boolean[] visitado) {
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

    private int posicionDe(Estacion[] vertices, Estacion e) {
        for (int k = 0; k < vertices.length; k++) {
            if (vertices[k].equals(e)) return k;
        }
        return -1;
    }

    private boolean estaEn(Estacion[] arreglo, int cantidad, Estacion e) {
        for (int k = 0; k < cantidad; k++) {
            if (arreglo[k].equals(e)) return true;
        }
        return false;
    }

    private boolean estaAusente(Estacion e, String idAusente, String lineaAusente) {
        if (idAusente != null && e.getId().equals(idAusente)) return true;
        return lineaAusente != null && e.getLinea().equals(lineaAusente);
    }

    private Estacion buscarEstacion(String texto) {
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
    public double getUltimoTiempoRuta() { return ultimoTiempoRuta; }
}
