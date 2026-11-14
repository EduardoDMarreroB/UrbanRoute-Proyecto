package urbanroute.control;

import urbanroute.interfaz.VisualizadorGrafo;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.tda.Cola;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Analiza la red buscando rutas rápidas, subredes y hubs.
 */
public class AnalizadorRed {

    private final GrafoPonderado grafo;
    private VisualizadorGrafo visualizador;
    private double ultimoTiempoRuta;

    // Diccionario interno para ubicar estaciones por ID rápidamente
    private static class IndiceVertices {
        private final String[] claves;
        private final int[] posiciones;
        private final boolean[] ocupadas;

        // Prepara los espacios buscando un tamaño primo para evitar choques
        private IndiceVertices(int cantidad) {
            int capacidad = siguientePrimo(
                    Math.max(3, cantidad * 2 + 1)
            );
            claves = new String[capacidad];
            posiciones = new int[capacidad];
            ocupadas = new boolean[capacidad];
        }

        // Convierte el texto del ID en un número para ubicarlo rápido
        private int hash(String clave) {
            long resultado = 0;
            for (int i = 0; i < clave.length(); i++) {
                resultado = (31L * resultado + clave.charAt(i))
                        % claves.length;
            }
            return (int) resultado;
        }

        // Guarda la clave y posición; si hay choque, avanza a la siguiente libre
        private void insertar(String clave, int posicion) {
            int indice = hash(clave);
            while (ocupadas[indice]) {
                if (claves[indice].equals(clave)) {
                    posiciones[indice] = posicion;
                    return;
                }
                indice = (indice + 1) % claves.length;
            }
            claves[indice] = clave;
            posiciones[indice] = posicion;
            ocupadas[indice] = true;
        }

        // Busca la posición del arreglo usando el ID de la estación
        private int buscar(String clave) {
            if (clave == null) {
                return -1;
            }
            int indice = hash(clave);
            int inicio = indice;
            while (ocupadas[indice]) {
                if (claves[indice].equals(clave)) {
                    return posiciones[indice];
                }
                indice = (indice + 1) % claves.length;
                if (indice == inicio) {
                    return -1;
                }
            }
            return -1;
        }

        // Busca el siguiente número primo para el tamaño del diccionario
        private static int siguientePrimo(int numero) {
            int candidato = numero;
            if (candidato <= 2) {
                return 2;
            }
            if (candidato % 2 == 0) {
                candidato++;
            }
            while (!esPrimo(candidato)) {
                candidato += 2;
            }
            return candidato;
        }

        // Revisa si un número entero es primo probando divisiones
        private static boolean esPrimo(int numero) {
            if (numero < 2) {
                return false;
            }
            for (int divisor = 2;
                 divisor <= numero / divisor;
                 divisor++) {
                if (numero % divisor == 0) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Constructor del analizador de red.
     */
    public AnalizadorRed(GrafoPonderado grafo) {
        if (grafo == null) {
            throw new IllegalArgumentException(
                    "El grafo no puede ser nulo."
            );
        }
        this.grafo = grafo;
        this.ultimoTiempoRuta = Double.POSITIVE_INFINITY;
        this.visualizador = null;
    }

    /**
     * Busca una estación limpiando espacios en blanco en el ID.
     */
    public Estacion buscarEstacion(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return grafo.buscarEstacion(id.trim());
    }

    /**
     * Calcula la ruta más rápida usando Dijkstra.
     */
    public ListaEnlazada<Estacion> dijkstra(
            String origen, String destino) {
        ultimoTiempoRuta = Double.POSITIVE_INFINITY;
        Estacion estOrigen = buscarEstacion(origen);
        Estacion estDestino = buscarEstacion(destino);
        if (estOrigen == null || estDestino == null) {
            return null;
        }
        Estacion[] vertices = obtenerVertices();
        int cantidad = vertices.length;
        if (cantidad == 0) {
            return null;
        }
        IndiceVertices indiceVertices = construirIndice(vertices);
        int posOrigen = indiceVertices.buscar(estOrigen.getId());
        int posDestino = indiceVertices.buscar(estDestino.getId());
        if (posOrigen == -1 || posDestino == -1) {
            return null;
        }
        double[] distancia = new double[cantidad];
        int[] previo = new int[cantidad];
        boolean[] visitado = new boolean[cantidad];
        for (int i = 0; i < cantidad; i++) {
            distancia[i] = Double.POSITIVE_INFINITY;
            previo[i] = -1;
        }
        distancia[posOrigen] = 0;

        for (int paso = 0; paso < cantidad; paso++) {
            int u = buscarMinimo(distancia, visitado);
            if (u == -1 || !Double.isFinite(distancia[u])) {
                break;
            }
            visitado[u] = true;
            if (u == posDestino) {
                break;
            }
            ListaEnlazada<Conexion> vecinos =
                    grafo.obtenerVecinos(vertices[u]);
            for (Nodo<Conexion> nodo = vecinos.getCabeza();
                  nodo != null;
                  nodo = nodo.getSiguiente()) {
                Conexion conexion = nodo.getDato();
                int v = indiceVertices.buscar(
                        conexion.getDestino().getId()
                );
                if (v == -1 || visitado[v]) {
                    continue;
                }
                double tiempo = conexion.getTiempo();
                if (!Double.isFinite(tiempo) || tiempo < 0) {
                    continue;
                }
                double nuevaDistancia = distancia[u] + tiempo;
                if (nuevaDistancia < distancia[v]) {
                    distancia[v] = nuevaDistancia;
                    previo[v] = u;
                }
            }
        }

        if (!Double.isFinite(distancia[posDestino])) {
            return null;
        }
        ultimoTiempoRuta = distancia[posDestino];
        ListaEnlazada<Estacion> ruta = new ListaEnlazada<>();
        int actual = posDestino;
        while (actual != -1) {
            ruta.insertarInicio(vertices[actual]);
            actual = previo[actual];
        }
        return ruta;
    }

    /**
     * Detecta todas las subredes de la red.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>>
            detectarSubredesBFS() {
        return agruparSubredes(null, null);
    }

    /**
     * Detecta subredes simulando el cierre de una estación.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>>
            detectarSubredesSinEstacion(String idEstacion) {
        return agruparSubredes(idEstacion, null);
    }

    /**
     * Detecta subredes simulando el cierre de una línea.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>>
            detectarSubredesSinLinea(String linea) {
        return agruparSubredes(null, linea);
    }

    /**
     * Recorre el grafo mediante BFS estándar.
     */
    public ListaEnlazada<Estacion> bfs(Estacion inicio) {
        ListaEnlazada<Estacion> resultado = new ListaEnlazada<>();
        if (!perteneceAlGrafo(inicio)) {
            return resultado;
        }
        Estacion[] vertices = obtenerVertices();
        IndiceVertices indiceVertices = construirIndice(vertices);
        int posInicio = indiceVertices.buscar(inicio.getId());
        if (posInicio == -1) {
            return resultado;
        }
        boolean[] visitados = new boolean[vertices.length];
        Cola<Integer> pendientes = new Cola<>();
        visitados[posInicio] = true;
        pendientes.encolar(posInicio);
        while (!pendientes.esVacia()) {
            int actual = pendientes.desencolar();
            resultado.insertar(vertices[actual]);
            ListaEnlazada<Conexion> vecinos =
                    grafo.obtenerVecinos(vertices[actual]);
            for (Nodo<Conexion> nodo = vecinos.getCabeza();
                  nodo != null;
                  nodo = nodo.getSiguiente()) {
                Conexion conexion = nodo.getDato();
                int posicionVecino = indiceVertices.buscar(
                        conexion.getDestino().getId()
                );
                if (posicionVecino != -1
                        && !visitados[posicionVecino]) {
                    visitados[posicionVecino] = true;
                    pendientes.encolar(posicionVecino);
                }
            }
        }
        return resultado;
    }

    /**
     * Recorre el grafo en profundidad (DFS) usando una pila manual.
     */
    public ListaEnlazada<Estacion> dfs(Estacion inicio) {
        ListaEnlazada<Estacion> resultado = new ListaEnlazada<>();
        if (!perteneceAlGrafo(inicio)) {
            return resultado;
        }
        Estacion[] vertices = obtenerVertices();
        IndiceVertices indiceVertices = construirIndice(vertices);
        int posInicio = indiceVertices.buscar(inicio.getId());
        if (posInicio == -1) {
            return resultado;
        }
        boolean[] visitados = new boolean[vertices.length];
        int[] pila = new int[vertices.length];
        int tope = 0;
        visitados[posInicio] = true;
        pila[tope++] = posInicio;
        while (tope > 0) {
            int actual = pila[--tope];
            resultado.insertar(vertices[actual]);
            ListaEnlazada<Conexion> vecinos =
                    grafo.obtenerVecinos(vertices[actual]);
            for (Nodo<Conexion> nodo = vecinos.getCabeza();
                  nodo != null;
                  nodo = nodo.getSiguiente()) {
                Conexion conexion = nodo.getDato();
                int posicionVecino = indiceVertices.buscar(
                        conexion.getDestino().getId()
                );
                if (posicionVecino != -1
                        && !visitados[posicionVecino]) {
                    visitados[posicionVecino] = true;
                    pila[tope++] = posicionVecino;
                }
            }
        }
        return resultado;
    }

    /**
     * Obtiene las k estaciones con mayor centralidad de grado (hubs).
     */
    public ListaEnlazada<Estacion> calcularCentralidadGrado(int k) {
        Estacion[] estaciones = obtenerVertices();
        int cantidad = estaciones.length;
        ListaEnlazada<Estacion> hubs = new ListaEnlazada<>();
        if (cantidad == 0 || k <= 0) {
            return hubs;
        }
        if (k > cantidad) {
            k = cantidad;
        }
        for (int pos = 0; pos < k; pos++) {
            int mayor = pos;
            for (int j = pos + 1; j < cantidad; j++) {
                if (estaciones[j].getGrado()
                        > estaciones[mayor].getGrado()) {
                    mayor = j;
                }
            }
            if (mayor != pos) {
                Estacion temporal = estaciones[pos];
                estaciones[pos] = estaciones[mayor];
                estaciones[mayor] = temporal;
            }
            hubs.insertar(estaciones[pos]);
        }
        return hubs;
    }

    // Agrupa las estaciones en subredes saltando las que estén excluidas
    private ListaEnlazada<ListaEnlazada<Estacion>>
            agruparSubredes(String idAusente, String lineaAusente) {
        ListaEnlazada<ListaEnlazada<Estacion>> subredes =
                new ListaEnlazada<>();
        Estacion[] vertices = obtenerVertices();
        int cantidad = vertices.length;
        if (cantidad == 0) {
            return subredes;
        }
        IndiceVertices indiceVertices = construirIndice(vertices);
        boolean[] asignadas = new boolean[cantidad];
        ListaEnlazada<Integer>[] entrantes = null;
        if (grafo.esDirigido()) {
            entrantes = construirConexionesEntrantes(
                    vertices, indiceVertices
            );
        }
        for (int i = 0; i < cantidad; i++) {
            Estacion estacion = vertices[i];
            if (estaAusente(estacion, idAusente, lineaAusente)) {
                continue;
            }
            if (!asignadas[i]) {
                ListaEnlazada<Estacion> subred =
                        recorrerComponente(
                                i, vertices, indiceVertices,
                                asignadas, idAusente,
                                lineaAusente, entrantes
                        );
                if (!subred.esVacia()) {
                    subredes.insertar(subred);
                }
            }
        }
        return subredes;
    }

    // Crea listas con las conexiones que llegan a cada vértice (para dirigidos)
    private ListaEnlazada<Integer>[] construirConexionesEntrantes(
            Estacion[] vertices, IndiceVertices indiceVertices) {
        ListaEnlazada<Integer>[] entrantes = crearArregloListas(vertices.length);
        for (int i = 0; i < vertices.length; i++) {
            entrantes[i] = new ListaEnlazada<>();
        }
        for (int i = 0; i < vertices.length; i++) {
            ListaEnlazada<Conexion> vecinos =
                    grafo.obtenerVecinos(vertices[i]);
            for (Nodo<Conexion> nodo = vecinos.getCabeza();
                  nodo != null;
                  nodo = nodo.getSiguiente()) {
                Conexion conexion = nodo.getDato();
                int destino = indiceVertices.buscar(
                        conexion.getDestino().getId()
                );
                if (destino != -1) {
                    entrantes[destino].insertar(i);
                }
            }
        }
        return entrantes;
    }

    // Método auxiliar para armar arreglos de listas de forma limpia
    private ListaEnlazada<Integer>[] crearArregloListas(int tamano) {
        Object[] arreglo = new Object[tamano];
        for (int i = 0; i < tamano; i++) {
            arreglo[i] = new ListaEnlazada<Integer>();
        }
        return (ListaEnlazada<Integer>[]) arreglo;
    }

    // Recorre una subred usando BFS tanto por salidas como por entradas
    private ListaEnlazada<Estacion> recorrerComponente(
            int inicio,
            Estacion[] vertices,
            IndiceVertices indiceVertices,
            boolean[] asignadas,
            String idAusente,
            String lineaAusente,
            ListaEnlazada<Integer>[] entrantes) {
        ListaEnlazada<Estacion> resultado = new ListaEnlazada<>();
        Cola<Integer> pendientes = new Cola<>();
        asignadas[inicio] = true;
        pendientes.encolar(inicio);
        
        while (!pendientes.esVacia()) {
            int actual = pendientes.desencolar();
            resultado.insertar(vertices[actual]);
            ListaEnlazada<Conexion> vecinos =
                    grafo.obtenerVecinos(vertices[actual]);
            
            // Explora las conexiones salientes (hacia adelante)
            for (Nodo<Conexion> nodo = vecinos.getCabeza();
                  nodo != null;
                  nodo = nodo.getSiguiente()) {
                Conexion conexion = nodo.getDato();
                int posicionVecino = indiceVertices.buscar(
                        conexion.getDestino().getId()
                );
                if (posicionVecino == -1 || asignadas[posicionVecino]) {
                    continue;
                }
                Estacion vecino = vertices[posicionVecino];
                if (estaAusente(vecino, idAusente, lineaAusente)) {
                    continue;
                }
                asignadas[posicionVecino] = true;
                pendientes.encolar(posicionVecino);
            }
            
            // Explora las conexiones entrantes (hacia atrás).
            // NOTA: Reutiliza la misma estructura de validación y encolado
            // del bloque anterior pero en sentido inverso, permitiendo
            // que el BFS detecte la subred de forma bidireccional.
            if (entrantes != null) {
                for (Nodo<Integer> nodo = entrantes[actual].getCabeza();
                      nodo != null;
                      nodo = nodo.getSiguiente()) {
                    int posicionVecino = nodo.getDato();
                    if (asignadas[posicionVecino]) {
                        continue;
                    }
                    Estacion vecino = vertices[posicionVecino];
                    if (estaAusente(vecino, idAusente, lineaAusente)) {
                        continue;
                    }
                    asignadas[posicionVecino] = true;
                    pendientes.encolar(posicionVecino);
                }
            }
        }
        return resultado;
    }

    // Pasa las estaciones de la lista a un arreglo plano para trabajar rápido
    private Estacion[] obtenerVertices() {
        ListaEnlazada<Estacion> lista = grafo.getEstaciones();
        Estacion[] vertices = new Estacion[lista.obtenerTamano()];
        int i = 0;
        for (Nodo<Estacion> nodo = lista.getCabeza();
              nodo != null;
              nodo = nodo.getSiguiente()) {
            vertices[i++] = nodo.getDato();
        }
        return vertices;
    }

    // Llena el diccionario rápido con los IDs y posiciones de las estaciones
    private IndiceVertices construirIndice(Estacion[] vertices) {
        IndiceVertices indice = new IndiceVertices(vertices.length);
        for (int i = 0; i < vertices.length; i++) {
            indice.insertar(vertices[i].getId(), i);
        }
        return indice;
    }

    // Busca la estación no visitada con menor distancia para Dijkstra
    private int buscarMinimo(
            double[] distancia, boolean[] visitado) {
        int mejor = -1;
        double menor = Double.POSITIVE_INFINITY;
        for (int i = 0; i < distancia.length; i++) {
            if (!visitado[i] && distancia[i] < menor) {
                menor = distancia[i];
                mejor = i;
            }
        }
        return mejor;
    }

    // Revisa si la estación existe y pertenece al grafo actual
    private boolean perteneceAlGrafo(Estacion estacion) {
        if (estacion == null) {
            return false;
        }
        return grafo.buscarEstacion(estacion.getId()) == estacion;
    }

    // Revisa si una estación está excluida por ID o por línea
    private boolean estaAusente(
            Estacion estacion,
            String idAusente,
            String lineaAusente) {
        if (estacion == null) {
            return true;
        }
        if (idAusente != null
                && estacion.getId().equals(idAusente.trim())) {
            return true;
        }
        return lineaAusente != null
                && estacion.getLinea().equals(lineaAusente.trim());
    }

    /**
     * Devuelve el grafo asociado.
     */
    public GrafoPonderado getGrafo() {
        return grafo;
    }

    /**
     * Devuelve el visualizador asociado.
     */
    public VisualizadorGrafo getVisualizador() {
        return visualizador;
    }

    /**
     * Asocia un visualizador gráfico.
     */
    public void setVisualizador(VisualizadorGrafo visualizador) {
        this.visualizador = visualizador;
    }

    /**
     * Devuelve el tiempo de la última ruta calculada.
     */
    public double getUltimoTiempoRuta() {
        return ultimoTiempoRuta;
    }
}
