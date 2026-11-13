package urbanroute.control;
import urbanroute.interfaz.VisualizadorGrafo;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.tda.Cola;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;
/**
 * Analiza la red de transporte mediante algoritmos de grafos.
 *
 * Implementa Dijkstra para calcular rutas de menor tiempo,
 * BFS y DFS para recorrer estaciones, detección de subredes
 * y centralidad de grado para identificar estaciones principales.
 *
 * Utiliza arreglos y estructuras de datos propias del proyecto.
 * No depende de Java Collections Framework.
 */
public class AnalizadorRed {
    /** Grafo sobre el que se realizan los análisis. */
    private final GrafoPonderado grafo;
    /** Visualizador asociado, si existe. */
    private VisualizadorGrafo visualizador;
    /** Tiempo total de la última ruta válida calculada. */
    private double ultimoTiempoRuta;
    /**
     * Índice auxiliar que relaciona los identificadores de las estaciones
     * con sus posiciones dentro del arreglo utilizado por los algoritmos.
     *
     * Utiliza direccionamiento abierto con exploración lineal.
     * Es una estructura auxiliar temporal y no sustituye a TablaHash,
     * que continúa siendo el índice principal del grafo.
     */
    private static class IndiceVertices {
        private final String[] claves;
        private final int[] posiciones;
        private final boolean[] ocupadas;
        /**
         * Crea un índice con capacidad suficiente para almacenar
         * las estaciones del grafo.
         *
         * @param cantidad Cantidad de estaciones que se indexarán.
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
         * Calcula la posición inicial de una clave.
         *
         * @param clave Identificador de una estación.
         * @return Posición calculada dentro del arreglo.
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
         * Registra el identificador de una estación y su posición.
         *
         * Si la clave ya existe, actualiza su posición.
         *
         * @param clave Identificador de la estación.
         * @param posicion Posición de la estación en el arreglo de vértices.
         */
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
        /**
         * Busca la posición de una estación mediante su identificador.
         *
         * Utiliza la misma secuencia de exploración que la inserción.
         *
         * @param clave Identificador que se desea localizar.
         * @return Posición de la estación o -1 si no existe.
         */
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
        /**
         * Busca un número primo igual o mayor al recibido.
         *
         * @param numero Número inicial.
         * @return Siguiente número primo.
         */
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
        /**
         * Comprueba si un número es primo.
         *
         * @param numero Número que se desea comprobar.
         * @return true si es primo; false en caso contrario.
         */
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
     * Crea un analizador asociado a un grafo.
     *
     * @param grafo Grafo ponderado que se desea analizar.
     * @throws IllegalArgumentException Si el grafo es nulo.
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
     * Busca una estación mediante su identificador.
     *
     * Delega la búsqueda en el índice hash del grafo.
     *
     * @param id Identificador de la estación.
     * @return Estación encontrada o null si no existe.
     */
    public Estacion buscarEstacion(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return grafo.buscarEstacion(id.trim());
    }
    /**
     * Calcula la ruta de menor tiempo entre dos estaciones
     * utilizando el algoritmo de Dijkstra.
     *
     * Los tiempos de las conexiones deben ser finitos y no negativos.
     *
     * @param origen Identificador de la estación de origen.
     * @param destino Identificador de la estación de destino.
     * @return Lista de estaciones de la ruta óptima, desde el origen
     * hasta el destino, o null si alguna estación no existe
     * o no hay un camino disponible.
     *
     * @apiNote El tiempo total se obtiene mediante
     * getUltimoTiempoRuta().
     */
    public ListaEnlazada<Estacion> dijkstra(
            String origen, String destino) {
        // Evita conservar el tiempo de una consulta anterior.
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
        // Construye el índice auxiliar de identificadores y posiciones.
        IndiceVertices indiceVertices = construirIndice(vertices);
        int posOrigen = indiceVertices.buscar(estOrigen.getId());
        int posDestino = indiceVertices.buscar(estDestino.getId());
        if (posOrigen == -1 || posDestino == -1) {
            return null;
        }
        double[] distancia = new double[cantidad];
        int[] previo = new int[cantidad];
        boolean[] visitado = new boolean[cantidad];
        // Inicializa las distancias y los predecesores.
        for (int i = 0; i < cantidad; i++) {
            distancia[i] = Double.POSITIVE_INFINITY;
            previo[i] = -1;
        }
        distancia[posOrigen] = 0;
        /*
         * En cada iteración se selecciona la estación no visitada
         * con la menor distancia conocida.
         *Si buscarMinimo devuelve -1 y el código no se detiene con el break, ese -1 se usaría como índice para acceder a los arreglos 
         (por ejemplo, distancia[u] o visitado[u]), lo que provocaría inmediatamente un error fatal de ejecución de tipo 
         ArrayIndexOutOfBoundsException, ya que no existe una posición -1 en un arreglo de Java. 
         En resumen: El -1 es la forma en que buscarMinimo le avisa al bucle principal: 
         "Ya terminé de recorrer todo lo que se podía conectar; no pierdas el tiempo buscando más, salgamos del ciclo"
         */
        for (int paso = 0; paso < cantidad; paso++) {
            int u = buscarMinimo(distancia, visitado);
            if (u == -1) { 
                break;
            }
            // Si la menor distancia es infinita, no quedan
            // estaciones alcanzables desde el origen.
            if (!Double.isFinite(distancia[u])) {
                break;
            }
            visitado[u] = true;
            if (u == posDestino) {
                break;
            }
            // Examina las conexiones salientes de la estación actual.
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
                // Protege el algoritmo ante pesos inválidos.
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
        // El destino no es alcanzable desde el origen.
        if (!Double.isFinite(distancia[posDestino])) {
            return null;
        }
        ultimoTiempoRuta = distancia[posDestino];
        // Reconstruye la ruta desde el destino hasta el origen.
        ListaEnlazada<Estacion> ruta = new ListaEnlazada<>();
        int actual = posDestino;
        while (actual != -1) {
            ruta.insertarInicio(vertices[actual]);
            actual = previo[actual];
        }
        return ruta;
    }
    /**
     * Detecta las subredes del grafo utilizando búsqueda en anchura.
     *
     * En grafos no dirigidos identifica componentes conexos.
     * En grafos dirigidos identifica componentes débilmente conexos,
     * considerando conexiones entrantes y salientes.
     *
     * @return Lista de subredes, donde cada subred contiene
     * una lista de estaciones.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>>
            detectarSubredesBFS() {
        return agruparSubredes(null, null);
    }
    /**
     * Detecta las subredes simulando la eliminación de una estación.
     *
     * La estación indicada se excluye del recorrido sin modificar
     * el grafo original.
     *
     * @param idEstacion Identificador de la estación que se excluirá.
     * @return Lista de subredes resultantes.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>>
            detectarSubredesSinEstacion(String idEstacion) {
        return agruparSubredes(idEstacion, null);
    }
    /**
     * Detecta las subredes simulando el cierre de una línea.
     *
     * Se excluyen las estaciones cuyo atributo de línea coincida
     * con la línea indicada.
     *
     * @param linea Nombre de la línea que se excluirá.
     * @return Lista de subredes resultantes.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>>
            detectarSubredesSinLinea(String linea) {
        return agruparSubredes(null, linea);
    }
    /**
     * Realiza un recorrido en anchura desde una estación.
     *
     * Sigue las conexiones salientes cuando el grafo es dirigido.
     *
     * @param inicio Estación desde la que comienza el recorrido.
     * @return Lista de estaciones en orden de visita.
     * Si la estación no pertenece al grafo, devuelve una lista vacía.
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
     * Realiza un recorrido en profundidad desde una estación.
     *
     * Utiliza un arreglo como pila auxiliar, evitando la recursividad
     * y el riesgo de desbordamiento de la pila de llamadas.
     *
     * @param inicio Estación desde la que comienza el recorrido.
     * @return Lista de estaciones en orden de visita.
     * Si la estación no pertenece al grafo, devuelve una lista vacía.
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
        // Arreglo utilizado como pila.
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
                    // Se marca al insertar para evitar duplicados.
                    visitados[posicionVecino] = true;
                    pila[tope++] = posicionVecino;
                }
            }
        }
        return resultado;
    }
    /**
     * Calcula las k estaciones con mayor grado.
     *
     * Utiliza ordenamiento por selección parcial sobre un arreglo,
     * desde la estación con más conexiones hasta la de menor grado.
     *
     * En grafos dirigidos, el grado considerado es el de salida,
     * según la cantidad de conexiones almacenadas en cada estación.
     *
     * @param k Cantidad máxima de estaciones que se desean obtener.
     * @return Lista de estaciones ordenadas por grado descendente.
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
        /*
         * Selección parcial: busca la estación con mayor grado
         * en las posiciones que todavía no han sido ordenadas.
         */
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
    /**
     * Agrupa las estaciones en subredes mediante BFS.
     *
     * Utiliza un arreglo compartido de estaciones visitadas para
     * evitar que una estación aparezca en más de una subred.
     *
     * En grafos dirigidos construye adicionalmente las relaciones
     * entrantes para identificar componentes débilmente conexos.
     *
     * @param idAusente Identificador de una estación que se excluirá.
     * @param lineaAusente Línea que se excluirá.
     * @return Lista de subredes resultantes.
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
        /*
         * En un grafo dirigido, se necesitan conocer las conexiones
         * entrantes para calcular componentes débilmente conexos.
         */
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
     * Construye las relaciones entrantes de las estaciones.
     *
     * Cada posición representa una estación y contiene los índices
     * de las estaciones que tienen conexiones dirigidas hacia ella.
     *
     * @param vertices Arreglo de estaciones.
     * @param indiceVertices Índice auxiliar de identificadores.
     * @return Arreglo de listas con las conexiones entrantes.
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
     * Realiza BFS para obtener un componente de la red.
     *
     * En grafos no dirigidos recorre las conexiones almacenadas
     * en las listas de adyacencia.
     *
     * En grafos dirigidos también considera las conexiones entrantes,
     * permitiendo obtener componentes débilmente conexos.
     *
     * @param inicio Posición de la estación inicial.
     * @param vertices Arreglo de estaciones.
     * @param indiceVertices Índice auxiliar de posiciones.
     * @param asignadas Arreglo compartido de estaciones visitadas.
     * @param idAusente Identificador de la estación excluida.
     * @param lineaAusente Línea excluida.
     * @param entrantes Listas de conexiones entrantes o null
     *                  si el grafo no es dirigido.
     * @return Lista de estaciones pertenecientes al componente.
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
            // Examina las conexiones salientes.
            ListaEnlazada<Conexion> vecinos =
                    grafo.obtenerVecinos(vertices[actual]);
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
            /*
             * Para grafos dirigidos también se recorren las conexiones
             * que llegan a la estación actual.
             */
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
     * Obtiene las estaciones del grafo en un arreglo.
     *
     * Conserva el orden de incorporación de los vértices.
     *
     * @return Arreglo con las estaciones del grafo.
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
     * Construye un índice auxiliar para localizar las estaciones
     * dentro del arreglo de vértices.
     *
     * @param vertices Arreglo de estaciones.
     * @return Índice de identificadores y posiciones.
     */
    private IndiceVertices construirIndice(Estacion[] vertices) {
        IndiceVertices indice = new IndiceVertices(vertices.length);
        for (int i = 0; i < vertices.length; i++) {
            indice.insertar(vertices[i].getId(), i);
        }
        return indice;
    }
    /**
     * Busca la posición no visitada con menor distancia conocida.
     *
     * @param distancia Arreglo de distancias desde el origen.
     * @param visitado Indica qué estaciones ya fueron procesadas.
     * @return Posición de la estación con menor distancia
     * o -1 si no queda ninguna.
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
        return mejor;
    }
    /**
     * Comprueba si una estación pertenece al grafo.
     *
     * La referencia debe coincidir con la instancia registrada
     * mediante el identificador correspondiente.
     *
     * @param estacion Estación que se desea comprobar.
     * @return true si pertenece al grafo; false en caso contrario.
     */
    private boolean perteneceAlGrafo(Estacion estacion) {
        if (estacion == null) {
            return false;
        }
        return grafo.buscarEstacion(estacion.getId()) == estacion;
    }
    /**
     * Comprueba si una estación debe excluirse de un análisis.
     *
     * Permite simular el cierre de una estación o de una línea
     * sin modificar físicamente el grafo.
     *
     * @param estacion Estación que se desea comprobar.
     * @param idAusente Identificador excluido.
     * @param lineaAusente Nombre de la línea excluida.
     * @return true si la estación debe excluirse.
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
     * Obtiene el grafo analizado.
     *
     * @return Grafo ponderado asociado.
     */
    public GrafoPonderado getGrafo() {
        return grafo;
    }
    /**
     * Obtiene el visualizador asociado.
     *
     * @return Visualizador o null si no se ha establecido.
     */
    public VisualizadorGrafo getVisualizador() {
        return visualizador;
    }
    /**
     * Asocia un visualizador a esta clase.
     *
     * @param visualizador Visualizador que se desea asociar.
     */
    public void setVisualizador(VisualizadorGrafo visualizador) {
        this.visualizador = visualizador;
    }
    /**
     * Obtiene el tiempo total de la última ruta válida calculada.
     *
     * Si la última búsqueda no encontró un camino, devuelve
     * Double.POSITIVE_INFINITY.
     *
     * @return Tiempo mínimo de recorrido en minutos.
     */
    public double getUltimoTiempoRuta() {
        return ultimoTiempoRuta;
    }
}
