package urbanroute.control;

import urbanroute.interfaz.VisualizadorGrafo;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.modelo.TablaHash;
import urbanroute.tda.Cola;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Analiza la red de transporte mediante algoritmos de grafos.
 *
 * <p>Incluye el cálculo de rutas mínimas con Dijkstra, la identificación de
 * componentes conexos mediante búsqueda en anchura (BFS) y en profundidad (DFS),
 * y la obtención de estaciones con mayor grado de conexión.</p>
 *
 * <p>Los algoritmos siguen los modelos clásicos descritos en Cormen et al.,
 * <i>Introduction to Algorithms</i>, 3.ª edición, MIT Press, 2009.</p>
 */
public class AnalizadorRed {

    private GrafoPonderado grafo;              // Grafo sobre el que se ejecutan los algoritmos.
    private VisualizadorGrafo visualizador;    // Componente gráfico asociado al analizador.
    private double ultimoTiempoRuta;            // Tiempo total de la última ruta calculada correctamente.

    /**
     * Crea un analizador para el grafo indicado y registra la relación entre ambos.
     *
     * @param grafo grafo de transporte que será analizado.
     */
    public AnalizadorRed(GrafoPonderado grafo) {
        if (grafo == null) {
            throw new IllegalArgumentException("El grafo no puede ser null.");
        }
        this.grafo = grafo;
        this.grafo.setAnalizador(this);
    }

    /**
     * Calcula la ruta de menor tiempo entre dos estaciones identificadas por su
     * código o nombre, mediante el algoritmo de Dijkstra.
     *
     * @param origen identificador o nombre de la estación de origen.
     * @param destino identificador o nombre de la estación de destino.
     * @return lista de estaciones desde el origen hasta el destino, o {@code null}
     *         si alguna estación no existe o no hay una ruta disponible.
     * @see #getUltimoTiempoRuta()
     */
    public ListaEnlazada<Estacion> dijkstra(String origen, String destino) {
        Estacion estacionOrigen = buscarEstacion(origen);
        Estacion estacionDestino = buscarEstacion(destino);
        if (estacionOrigen == null || estacionDestino == null) {
            return null;
        }

        int cantidadEstaciones = grafo.numEstaciones();
        if (cantidadEstaciones == 0) {
            return null;
        }

        // Se utiliza un arreglo para trabajar con posiciones numéricas durante Dijkstra.
        Estacion[] vertices = new Estacion[cantidadEstaciones];
        // La tabla relaciona el ID de cada estación con su posición en el arreglo.
        TablaHash<String, Integer> mapaIndices = new TablaHash<>();

        int cantidadCopiada = 0;
        for (Nodo<Estacion> nodoEstacion = grafo.getEstaciones().getCabeza();
             nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {
            Estacion estacionActual = nodoEstacion.getDato();
            vertices[cantidadCopiada] = estacionActual;
            mapaIndices.insertar(estacionActual.getId(), cantidadCopiada);
            cantidadCopiada++;
        }

        // Se traducen los identificadores de origen y destino a posiciones del arreglo.
        Integer indiceOrigen = mapaIndices.obtener(estacionOrigen.getId());
        Integer indiceDestino = mapaIndices.obtener(estacionDestino.getId());
        if (indiceOrigen == null || indiceDestino == null) {
            return null;
        }

        int posicionOrigen = indiceOrigen;
        int posicionDestino = indiceDestino;

        // distancia almacena el menor tiempo conocido; previo guarda el antecesor.
        double[] distancia = new double[cantidadEstaciones];
        int[] previo = new int[cantidadEstaciones];
        boolean[] visitado = new boolean[cantidadEstaciones];

        for (int posicionTabla = 0; posicionTabla < cantidadEstaciones; posicionTabla++) {
            distancia[posicionTabla] = Double.POSITIVE_INFINITY;
            previo[posicionTabla] = -1;
        }
        distancia[posicionOrigen] = 0.0;

        // Dijkstra procesa primero la estación no visitada con menor distancia acumulada.
        for (int paso = 0; paso < cantidadEstaciones; paso++) {
            int posicionActual = buscarMinimo(distancia, visitado);

            // Si no existe una posición alcanzable, las estaciones restantes están desconectadas.
            if (posicionActual == -1) {
                break;
            }

            visitado[posicionActual] = true;

            // Al llegar al destino, su distancia mínima ya está determinada.
            if (posicionActual == posicionDestino) {
                break;
            }

            // Se examinan las conexiones que salen de la estación actual.
            ListaEnlazada<Conexion> conexiones = grafo.obtenerVecinos(vertices[posicionActual]);
            for (Nodo<Conexion> nodoConexion = conexiones.getCabeza(); nodoConexion != null;
                 nodoConexion = nodoConexion.getSiguiente()) {
                Conexion conexionActual = nodoConexion.getDato();

                // Se obtiene la posición de la estación destino de esta conexión.
                Integer indiceVecino = mapaIndices.obtener(conexionActual.getDestino().getId());
                if (indiceVecino == null) {
                    continue;
                }

                int posicionVecino = indiceVecino;
                if (visitado[posicionVecino]) {
                    continue;
                }

                // Se actualiza la ruta si pasar por la conexión produce un tiempo menor.
                double nuevaDistancia = distancia[posicionActual] + conexionActual.getTiempo();
                if (nuevaDistancia < distancia[posicionVecino]) {
                    distancia[posicionVecino] = nuevaDistancia;
                    previo[posicionVecino] = posicionActual;
                }
            }
        }

        if (distancia[posicionDestino] == Double.POSITIVE_INFINITY) {
            return null;
        }

        ultimoTiempoRuta = distancia[posicionDestino];

        // Se reconstruye la ruta desde el destino hasta el origen y se inserta al inicio.
        ListaEnlazada<Estacion> ruta = new ListaEnlazada<>();
        int posicionRuta = posicionDestino;
        while (posicionRuta != -1) {
            ruta.insertarInicio(vertices[posicionRuta]);
            posicionRuta = previo[posicionRuta];
        }

        return ruta;
    }

    /**
     * Agrupa todas las estaciones en componentes conexos mediante un recorrido BFS.
     *
     * @return lista de subredes; cada subred contiene sus estaciones en orden de visita.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>> detectarSubredesBFS() {
        return agruparSubredes(null, null, false);
    }

    /**
     * Agrupa todas las estaciones en componentes conexos mediante un recorrido DFS.
     *
     * @return lista de subredes; cada subred contiene sus estaciones en orden de visita.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>> detectarSubredesDFS() {
        return agruparSubredes(null, null, true);
    }

    /**
     * Calcula las subredes que quedarían al excluir una estación de la red.
     *
     * @param idEstacion identificador de la estación que se excluirá.
     * @return lista de componentes conexos sin la estación indicada.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>> detectarSubredesSinEstacion(String idEstacion) {
        return agruparSubredes(idEstacion, null, false);
    }

    /**
     * Calcula las subredes mediante DFS al excluir una estación de la red.
     *
     * @param idEstacion identificador de la estación que se excluirá.
     * @return lista de componentes conexos sin la estación indicada.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>> detectarSubredesSinEstacionDFS(String idEstacion) {
        return agruparSubredes(idEstacion, null, true);
    }

    /**
     * Calcula las subredes que quedarían al excluir todas las conexiones de una línea.
     *
     * @param linea nombre o identificador de la línea que se excluirá.
     * @return lista de componentes conexos sin la línea indicada.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>> detectarSubredesSinLinea(String linea) {
        return agruparSubredes(null, linea, false);
    }

    /**
     * Calcula las subredes mediante DFS al excluir todas las conexiones de una línea.
     *
     * @param linea nombre o identificador de la línea que se excluirá.
     * @return lista de componentes conexos sin la línea indicada.
     */
    public ListaEnlazada<ListaEnlazada<Estacion>> detectarSubredesSinLineaDFS(String linea) {
        return agruparSubredes(null, linea, true);
    }

    /**
     * Realiza un recorrido en anchura (BFS) desde la estación indicada.
     *
     * @param inicio estación desde la que comienza el recorrido.
     * @return estaciones alcanzables en el orden en que son visitadas.
     */
    public ListaEnlazada<Estacion> bfs(Estacion inicio) {
        return recorrerBFS(inicio, null, null);
    }

    /**
     * Realiza un recorrido en profundidad (DFS) desde la estación indicada.
     * Utiliza un arreglo como pila para evitar depender de Java Collections Framework.
     *
     * @param inicio estación desde la que comienza el recorrido.
     * @return estaciones alcanzables en el orden en que son visitadas.
     */
    public ListaEnlazada<Estacion> dfs(Estacion inicio) {
        return recorrerDFS(inicio, null, null);
    }

    /**
     * Obtiene las {@code k} estaciones con mayor grado de conexión, ordenadas de mayor a menor.
     * Utiliza el algoritmo de ordenamiento por selección.
     *
     * @param k cantidad máxima de estaciones que se desea obtener.
     * @return lista con hasta {@code k} estaciones de mayor grado; si {@code k} es negativo,
     *         devuelve una lista vacía.
     */
    public ListaEnlazada<Estacion> calcularCentralidadGrado(int k) {
        int cantidadEstaciones = grafo.numEstaciones();
        ListaEnlazada<Estacion> hubs = new ListaEnlazada<>();

        if (cantidadEstaciones == 0 || k <= 0) {
            return hubs;
        }

        // Se copia la lista a un arreglo para aplicar ordenamiento por selección.
        Estacion[] estacionesOrdenadas = new Estacion[cantidadEstaciones];
        int cantidadCopiada = 0;
        for (Nodo<Estacion> nodoEstacion = grafo.getEstaciones().getCabeza();
             nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {
            estacionesOrdenadas[cantidadCopiada++] = nodoEstacion.getDato();
        }

        // No se pueden devolver más estaciones de las que existen en el grafo.
        int limiteResultados = Math.min(k, cantidadEstaciones);

        // En cada pasada se coloca en la posición actual la estación con mayor grado restante.
        for (int posicionActual = 0; posicionActual < limiteResultados; posicionActual++) {
            int posicionMayor = posicionActual;

            for (int posicionCandidata = posicionActual + 1;
                 posicionCandidata < cantidadEstaciones;
                 posicionCandidata++) {
                if (estacionesOrdenadas[posicionCandidata].getGrado()
                        > estacionesOrdenadas[posicionMayor].getGrado()) {
                    posicionMayor = posicionCandidata;
                }
            }

            // Se intercambian las estaciones si se encontró una de mayor grado.
            if (posicionMayor != posicionActual) {
                Estacion estacionTemporal = estacionesOrdenadas[posicionActual];
                estacionesOrdenadas[posicionActual] = estacionesOrdenadas[posicionMayor];
                estacionesOrdenadas[posicionMayor] = estacionTemporal;
            }

            hubs.insertar(estacionesOrdenadas[posicionActual]);
        }

        return hubs;
    }

    /**
     * Agrupa las estaciones en componentes conexos, omitiendo opcionalmente una estación
     * o una línea.
     *
     * @param idAusente identificador de la estación que se omitirá, o {@code null}.
     * @param lineaAusente línea que se omitirá, o {@code null}.
     * @return lista de componentes conexos resultantes.
     */
    private ListaEnlazada<ListaEnlazada<Estacion>> agruparSubredes(
            String idAusente, String lineaAusente, boolean utilizarDFS) {
        ListaEnlazada<ListaEnlazada<Estacion>> subredes = new ListaEnlazada<>();
        Estacion[] estacionesAsignadas = new Estacion[grafo.numEstaciones()];
        int cantidadAsignadas = 0;

        for (Nodo<Estacion> nodoEstacion = grafo.getEstaciones().getCabeza();
             nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {
            Estacion estacionActual = nodoEstacion.getDato();
            if (estaAusente(estacionActual, idAusente, lineaAusente)) {
                continue;
            }

            if (!estaEn(estacionesAsignadas, cantidadAsignadas, estacionActual)) {
                // Se aplica el algoritmo elegido para obtener el componente conexo actual.
                ListaEnlazada<Estacion> subredActual = utilizarDFS
                        ? recorrerDFS(estacionActual, idAusente, lineaAusente)
                        : recorrerBFS(estacionActual, idAusente, lineaAusente);
                subredes.insertar(subredActual);

                // Se registran las estaciones ya agrupadas para no repetir componentes.
                for (Nodo<Estacion> nodoSubred = subredActual.getCabeza(); nodoSubred != null;
                     nodoSubred = nodoSubred.getSiguiente()) {
                    estacionesAsignadas[cantidadAsignadas++] = nodoSubred.getDato();
                }
            }
        }

        return subredes;
    }

    /**
     * Ejecuta BFS desde una estación, respetando las exclusiones indicadas.
     *
     * @param inicio estación inicial del recorrido.
     * @param idAusente identificador de la estación que se omitirá, o {@code null}.
     * @param lineaAusente línea que se omitirá, o {@code null}.
     * @return estaciones visitadas durante el recorrido.
     */
    private ListaEnlazada<Estacion> recorrerBFS(
            Estacion inicio, String idAusente, String lineaAusente) {
        ListaEnlazada<Estacion> resultado = new ListaEnlazada<>();
        if (inicio == null || estaAusente(inicio, idAusente, lineaAusente)) {
            return resultado;
        }

        Cola<Estacion> pendientes = new Cola<>();
        Estacion[] estacionesVisitadas = new Estacion[grafo.numEstaciones()];
        int cantidadVisitadas = 0;

        pendientes.encolar(inicio);
        estacionesVisitadas[cantidadVisitadas++] = inicio;

        // BFS visita primero las estaciones más cercanas al punto de inicio.
        while (!pendientes.esVacia()) {
            Estacion estacionActual = pendientes.desencolar();
            resultado.insertar(estacionActual);

            ListaEnlazada<Conexion> conexiones = grafo.obtenerVecinos(estacionActual);
            for (Nodo<Conexion> nodoConexion = conexiones.getCabeza(); nodoConexion != null;
                 nodoConexion = nodoConexion.getSiguiente()) {
                Estacion estacionVecina = nodoConexion.getDato().getDestino();
                if (estaAusente(estacionVecina, idAusente, lineaAusente)) {
                    continue;
                }

                if (!estaEn(estacionesVisitadas, cantidadVisitadas, estacionVecina)) {
                    estacionesVisitadas[cantidadVisitadas++] = estacionVecina;
                    pendientes.encolar(estacionVecina);
                }
            }
        }

        return resultado;
    }

    /**
     * Ejecuta DFS desde una estación, respetando las exclusiones indicadas.
     * La pila se implementa con un arreglo cuyo tamaño máximo es el número de estaciones.
     *
     * @param inicio estación inicial del recorrido.
     * @param idAusente identificador de la estación que se omitirá, o {@code null}.
     * @param lineaAusente línea que se omitirá, o {@code null}.
     * @return estaciones visitadas durante el recorrido en profundidad.
     */
    private ListaEnlazada<Estacion> recorrerDFS(
            Estacion inicio, String idAusente, String lineaAusente) {
        ListaEnlazada<Estacion> resultado = new ListaEnlazada<>();
        if (inicio == null || estaAusente(inicio, idAusente, lineaAusente)
                || grafo.buscarEstacion(inicio.getId()) == null) {
            return resultado;
        }

        // Cada conexión puede generar una inserción en la pila; por eso se reserva
        // espacio para todas las conexiones y la estación inicial.
        Estacion[] pila = new Estacion[grafo.numConexiones() + 1];
        int cima = -1;

        // Este arreglo registra las estaciones que ya fueron procesadas.
        Estacion[] estacionesVisitadas = new Estacion[grafo.numEstaciones()];
        int cantidadVisitadas = 0;

        pila[++cima] = inicio;

        while (cima >= 0) {
            Estacion estacionActual = pila[cima--];

            // Una estación puede estar varias veces pendiente por conexiones distintas.
            // Se procesa solo la primera vez que se extrae de la pila.
            if (estaEn(estacionesVisitadas, cantidadVisitadas, estacionActual)) {
                continue;
            }

            estacionesVisitadas[cantidadVisitadas++] = estacionActual;
            resultado.insertar(estacionActual);

            ListaEnlazada<Conexion> conexiones = grafo.obtenerVecinos(estacionActual);
            for (Nodo<Conexion> nodoConexion = conexiones.getCabeza(); nodoConexion != null;
                 nodoConexion = nodoConexion.getSiguiente()) {
                Estacion estacionVecina = nodoConexion.getDato().getDestino();
                if (estaAusente(estacionVecina, idAusente, lineaAusente)
                        || estaEn(estacionesVisitadas, cantidadVisitadas, estacionVecina)) {
                    continue;
                }

                // La pila es LIFO: la última conexión insertada se explora primero.
                pila[++cima] = estacionVecina;
            }
        }

        return resultado;
    }

    /**
     * Busca la posición de menor distancia que todavía no haya sido visitada.
     *
     * @param distancias arreglo de distancias acumuladas.
     * @param visitadas indica qué posiciones ya fueron procesadas.
     * @return posición con menor distancia disponible, o {@code -1} si no queda ninguna.
     */
    private int buscarMinimo(double[] distancias, boolean[] visitadas) {
        int mejorPosicion = -1;
        double menorDistancia = Double.POSITIVE_INFINITY;

        for (int posicionTabla = 0; posicionTabla < distancias.length; posicionTabla++) {
            if (!visitadas[posicionTabla] && distancias[posicionTabla] < menorDistancia) {
                menorDistancia = distancias[posicionTabla];
                mejorPosicion = posicionTabla;
            }
        }

        return mejorPosicion;
    }

    /**
     * Comprueba si una estación ya aparece en las primeras posiciones ocupadas del arreglo.
     *
     * @param estaciones arreglo de estaciones.
     * @param cantidad cantidad de posiciones ocupadas que deben revisarse.
     * @param estacionBuscada estación que se desea localizar.
     * @return {@code true} si la estación ya está en el arreglo; de lo contrario, {@code false}.
     */
    private boolean estaEn(Estacion[] estaciones, int cantidad, Estacion estacionBuscada) {
        for (int posicionTabla = 0; posicionTabla < cantidad; posicionTabla++) {
            if (estaciones[posicionTabla].equals(estacionBuscada)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determina si una estación debe excluirse del análisis actual.
     *
     * @param estacion estación que se evaluará.
     * @param idAusente identificador de estación excluida, o {@code null}.
     * @param lineaAusente línea excluida, o {@code null}.
     * @return {@code true} si coincide con alguna exclusión.
     */
    private boolean estaAusente(Estacion estacion, String idAusente, String lineaAusente) {
        if (idAusente != null && estacion.getId().equals(idAusente)) {
            return true;
        }
        return lineaAusente != null && estacion.getLinea().equals(lineaAusente);
    }

    /**
     * Busca una estación primero por identificador y después por nombre.
     * La comparación por nombre no distingue entre mayúsculas y minúsculas.
     *
     * @param texto identificador o nombre que se desea buscar.
     * @return estación encontrada, o {@code null} si no existe.
     */
    private Estacion buscarEstacion(String texto) {
        if (texto == null) {
            return null;
        }

        String clave = texto.trim();
        Estacion estacionPorId = grafo.buscarEstacion(clave);
        if (estacionPorId != null) {
            return estacionPorId;
        }

        for (Nodo<Estacion> nodoEstacion = grafo.getEstaciones().getCabeza();
             nodoEstacion != null;
             nodoEstacion = nodoEstacion.getSiguiente()) {
            Estacion estacionActual = nodoEstacion.getDato();
            if (estacionActual.getNombre() != null
                    && estacionActual.getNombre().equalsIgnoreCase(clave)) {
                return estacionActual;
            }
        }

        return null;
    }

    /**
     * Devuelve el grafo asociado a este analizador.
     *
     * @return grafo que analiza esta instancia.
     */
    public GrafoPonderado getGrafo() {
        return grafo;
    }

    /**
     * Devuelve el visualizador gráfico asociado al analizador.
     *
     * @return visualizador asociado, o {@code null} si todavía no se ha establecido.
     */
    public VisualizadorGrafo getVisualizador() {
        return visualizador;
    }

    /**
     * Establece el visualizador gráfico asociado al analizador.
     *
     * @param visualizador componente que representa gráficamente la red.
     */
    public void setVisualizador(VisualizadorGrafo visualizador) {
        this.visualizador = visualizador;
    }

    /**
     * Devuelve el tiempo total de la última ruta calculada correctamente con Dijkstra.
     *
     * @return tiempo acumulado de la última ruta exitosa.
     */
    public double getUltimoTiempoRuta() {
        return ultimoTiempoRuta;
    }
}
