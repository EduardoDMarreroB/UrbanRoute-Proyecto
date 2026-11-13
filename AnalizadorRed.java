package urbanroute.control;

import urbanroute.interfaz.VisualizadorGrafo;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.tda.Cola;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Analiza la red: busca rutas rapidas, agrupa subredes y halla estaciones clave
 */
public class AnalizadorRed {

    private final GrafoPonderado grafo; // Mapa con estaciones y conexiones
    private VisualizadorGrafo visualizador; // Herramienta para dibujar la red
    private double ultimoTiempoRuta; // Minutos del ultimo viaje calculado

    /**
     * Diccionario interno para ubicar cada estacion en el arreglo rapido
     */
    private static class IndiceVertices {
        private final String[] claves; // IDs de las estaciones
        private final int[] posiciones; // Posicion en el arreglo principal
        private final boolean[] ocupadas; // Indica si el espacio ya tiene uso

        /**
         * Prepara los arreglos buscando un tamaño primo para evitar colisiones
         */
        private IndiceVertices(int cantidad) {
            int capacidad = siguientePrimo(
                    Math.max(3, cantidad * 2 + 1)
            );
            claves = new String[capacidad];
            posiciones = new int[capacidad];
            ocupadas = new boolean[capacidad];
        }

        /**
         * Convierte el ID de texto en un numero de posicion mediante hash
         */
        private int hash(String clave) {
            long resultado = 0;
            for (int i = 0; i < clave.length(); i++) {
                resultado = (31L * resultado + clave.charAt(i))
                        % claves.length;
            }
            return (int) resultado;
        }

        /**
         * Guarda el ID y su posicion. Si hay colision (dos IDs tienen el
         * mismo indice), avanza buscando la siguiente casilla libre
         */
        private void insertar(String clave, int posicion) {
            int indice = hash(clave);

            while (ocupadas[indice]) {
                if (claves[indice].equals(clave)) { // Si existe, actualiza
                    posiciones[indice] = posicion;
                    return;
                }
                indice = (indice + 1) % claves.length; // Avanza al siguiente
            }

            claves[indice] = clave;
            posiciones[indice] = posicion;
            ocupadas[indice] = true;
        }

        /**
         * Busca en que posicion del arreglo esta una estacion usando su ID
         */
        private int buscar(String clave) {
            if (clave == null) {
                return -1;
            }

            int indice = hash(clave);
            int inicio = indice;

            while (ocupadas[indice]) {
                if (claves[indice].equals(clave)) {
                    return posiciones[indice]; // Estacion encontrada
                }
                indice = (indice + 1) % claves.length;

                if (indice == inicio) { // Dio la vuelta completa sin exito
                    return -1;
                }
            }
            return -1; // Espacio vacio
        }

        /**
         * Busca un numero primo igual o mayor al valor indicado
         */
        private static int siguientePrimo(int numero) {
            int candidato = numero;

            if (candidato <= 2) {
                return 2;
            }
            if (candidato % 2 == 0) { // Si es par, lo vuelve impar
                candidato++;
            }
            while (!esPrimo(candidato)) { // Busca de 2 en 2 hasta hallar primo
                candidato += 2;
            }
            return candidato;
        }

        /**
         * Verifica si un numero entero dado es primo
         */
        private static boolean esPrimo(int numero) {
            if (numero < 2) {
                return false;
            }
            for (int divisor = 2;
                 divisor <= numero / divisor;
                 divisor++) {
                if (numero % divisor == 0) {
                    return false; // Es divisible, no es primo
                }
            }
            return true;
        }
    }

    /**
     * Constructor: prepara el analizador validando que el grafo no sea nulo
     */
    public AnalizadorRed(GrafoPonderado grafo) {
        if (grafo == null) {
            throw new IllegalArgumentException(
                    "El grafo no puede ser nulo."
            );
        }
        this.grafo = grafo;
        this.ultimoTiempoRuta = Double.POSITIVE_INFINITY; // Sin ruta inicial
        this.visualizador = null;
    }

    /**
     * Busca una estacion en el mapa limpiando espacios extra en el ID
     */
    public Estacion buscarEstacion(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return grafo.buscarEstacion(id.trim());
    }

    /**
     * Calcula la ruta mas rapida entre dos estaciones usando Dijkstra
     */
    public ListaEnlazada<Estacion> dijkstra(
            String origen, String destino) {

        ultimoTiempoRuta = Double.POSITIVE_INFINITY; // Reinicia el tiempo

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

        // Crea el indice auxiliar para ubicar cada estacion en el arreglo
        IndiceVertices indiceVertices = construirIndice(vertices);

        int posOrigen = indiceVertices.buscar(estOrigen.getId());
        int posDestino = indiceVertices.buscar(estDestino.getId());

        if (posOrigen == -1 || posDestino == -1) {
            return null;
        }

        // Arreglos de control: tiempo acumulado, nodo previo y visitados
        double[] distancia = new double[cantidad];
        int[] previo = new int[cantidad];
        boolean[] visitado = new boolean[cantidad];

        // Inicializa distancias en infinito y anteriores en -1
        for (int i = 0; i < cantidad; i++) {
            distancia[i] = Double.POSITIVE_INFINITY;
            previo[i] = -1;
        }
        distancia[posOrigen] = 0; // El origen esta a 0 minutos de si mismo

        // Selecciona en cada paso la estacion no visitada mas cercana
        for (int paso = 0; paso < cantidad; paso++) {
            int u = buscarMinimo(distancia, visitado);

            // Si retorna -1 ya no hay estaciones alcanzables, corta el ciclo
            if (u == -1) {
                break;
            }

            // Si la distancia sigue en infinito, el resto es inalcanzable
            if (!Double.isFinite(distancia[u])) {
                break;
            }

            visitado[u] = true; // Marca la estacion como procesada

            if (u == posDestino) { // Si llegamos al destino, termina
                break;
            }

            // Revisa las conexiones que salen de la estacion actual
            ListaEnlazada<Conexion> vecinos =
                    grafo.obtenerVecinos(vertices[u]);

            for (Nodo<Conexion> nodo = vecinos.getCabeza();
                 nodo != null;
                 nodo = nodo.getSiguiente()) {

                Conexion conexion = nodo.getDato();
                int v = indiceVertices.buscar(
                        conexion.getDestino().getId()
                );

                if (v == -1 || visitado[v]) { // Ignora si ya fue visitada
                    continue;
                }

                double tiempo = conexion.getTiempo();

                // Ignora conexiones con tiempos negativos o infinitos
                if (!Double.isFinite(tiempo) || tiempo < 0) {
                    continue;
                }

                // Si este camino es mas corto que el anterior, lo actualiza
                double nuevaDistancia = distancia[u] + tiempo;

                if (nuevaDistancia < distancia[v]) {
                    distancia[v] = nuevaDistancia;
                    previo[v] = u;
                }
            }
        }

        // Si el destino quedo en infinito, no existe camino posible
        if (!Double.isFinite(distancia[posDestino])) {
            return null;
        }

        ultimoTiempoRuta = distancia[posDestino]; // Guarda tiempo final

        // Reconstruye la ruta retrocediendo desde el destino al origen
        ListaEnlazada<Estacion> ruta = new ListaEnlazada<>();
        int actual = posDestino;

        while (actual != -1) {
            ruta.insertarInicio(vertices[actual]);
            actual = previo[actual];
        }

        return ruta;
    }

    /**
     * Detecta todas las subredes conectadas en el mapa completo
     */
    public ListaEnlazada<ListaEnlazada<Estacion>>
            detectarSubredesBFS() {
        return agruparSubredes(null, null);
    }

    /**
     * Simula el cierre de una estacion y muestra como quedan las subredes
     */
    public ListaEnlazada<ListaEnlazada<Estacion>>
            detectarSubredesSinEstacion(String idEstacion) {
        return agruparSubredes(idEstacion, null);
    }

    /**
     * Simula el cierre de una linea completa y muestra las subredes
     */
    public ListaEnlazada<ListaEnlazada<Estacion>>
            detectarSubredesSinLinea(String linea) {
        return agruparSubredes(null, linea);
    }

    /**
     * Recorre el grafo a lo ancho (BFS) desde una estacion inicial
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

            // Encola todos los vecinos no visitados
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
     * Recorre el grafo en profundidad (DFS) usando una pila manual
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
        int[] pila = new int[vertices.length]; // Arreglo usado como pila
        int tope = 0;

        visitados[posInicio] = true;
        pila[tope++] = posInicio;

        while (tope > 0) {
            int actual = pila[--tope]; // Extrae el ultimo elemento apilado
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
                    visitados[posicionVecino] = true; // Evita duplicados
                    pila[tope++] = posicionVecino;
                }
            }
        }
        return resultado;
    }

    /**
     * Obtiene las k estaciones con mayor cantidad de conexiones (hubs)
     */
    public ListaEnlazada<Estacion> calcularCentralidadGrado(int k) {
        Estacion[] estaciones = obtenerVertices();
        int cantidad = estaciones.length;
        ListaEnlazada<Estacion> hubs = new ListaEnlazada<>();

        if (cantidad == 0 || k <= 0) {
            return hubs;
        }

        if (k > cantidad) { // Ajusta k si supera el total de estaciones
            k = cantidad;
        }

        // Ordenamiento por seleccion parcial para sacar las k mayores
        for (int pos = 0; pos < k; pos++) {
            int mayor = pos;

            for (int j = pos + 1; j < cantidad; j++) {
                if (estaciones[j].getGrado()
                        > estaciones[mayor].getGrado()) {
                    mayor = j;
                }
            }

            if (mayor != pos) { // Intercambia la mayor hacia adelante
                Estacion temporal = estaciones[pos];
                estaciones[pos] = estaciones[mayor];
                estaciones[mayor] = temporal;
            }
            hubs.insertar(estaciones[pos]);
        }
        return hubs;
    }

    /**
     * Agrupa las estaciones en subredes ignorando elementos excluidos
     */
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

        // Si el grafo es dirigido, mapea tambien las conexiones entrantes
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
                                i,
                                vertices,
                                indiceVertices,
                                asignadas,
                                idAusente,
                                lineaAusente,
                                entrantes
                        );

                if (!subred.esVacia()) {
                    subredes.insertar(subred);
                }
            }
        }

        return subredes;
    }

    /**
     * Construye listas de conexiones entrantes para grafos dirigidos
     */
    @SuppressWarnings("unchecked")
    private ListaEnlazada<Integer>[] construirConexionesEntrantes(
            Estacion[] vertices, IndiceVertices indiceVertices) {

        ListaEnlazada<Integer>[] entrantes =
                (ListaEnlazada<Integer>[])
                        new ListaEnlazada<?>[vertices.length];

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

    /**
     * Recorre un componente conexo usando BFS respetando exclusiones
     */
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

            // Revisa las conexiones salientes
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

            // En grafos dirigidos revisa tambien las conexiones entrantes
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

    /**
     * Convierte la lista de estaciones del grafo en un arreglo simple
     */
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

    /**
     * Construye el indice auxiliar con las posiciones de cada estacion
     */
    private IndiceVertices construirIndice(Estacion[] vertices) {
        IndiceVertices indice = new IndiceVertices(vertices.length);
        for (int i = 0; i < vertices.length; i++) {
            indice.insertar(vertices[i].getId(), i);
        }
        return indice;
    }

    /**
     * Busca el indice de la estacion no visitada con menor distancia
     */
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
        return mejor; // Retorna -1 si no quedan estaciones alcanzables
    }

    /**
     * Verifica que la estacion exista y sea la misma instancia del grafo
     */
    private boolean perteneceAlGrafo(Estacion estacion) {
        if (estacion == null) {
            return false;
        }
        return grafo.buscarEstacion(estacion.getId()) == estacion;
    }

    /**
     * Comprueba si una estacion debe excluirse por ID o por linea
     */
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
     * Devuelve el grafo asociado al analizador
     */
    public GrafoPonderado getGrafo() {
        return grafo;
    }

    /**
     * Devuelve el visualizador asociado, si existe
     */
    public VisualizadorGrafo getVisualizador() {
        return visualizador;
    }

    /**
     * Asocia un visualizador grafico al analizador
     */
    public void setVisualizador(VisualizadorGrafo visualizador) {
        this.visualizador = visualizador;
    }

    /**
     * Devuelve el tiempo total en minutos de la ultima ruta calculada
     */
    public double getUltimoTiempoRuta() {
        return ultimoTiempoRuta;
    }
}
