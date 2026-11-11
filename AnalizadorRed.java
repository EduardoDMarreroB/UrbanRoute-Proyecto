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
     */
    public ListaEnlazada dijkstra(String origen, String destino) {
        Estacion estOrigen = buscarEstacion(origen);
        Estacion estDestino = buscarEstacion(destino);
        if (estOrigen == null || estDestino == null) {
            return null;
        }

        int cantidad = grafo.numEstaciones();
        if (cantidad == 0) return null;

        Estacion[] vertices = new Estacion[cantidad];
        TablaHash mapaIndices = new TablaHash(); 
       
        /**llenamos el arreglo vertices e indexamos la tablahash(mapaindices)
       para poder consultar el numero de indice en cualquier estacion
        
        */
        int i = 0;
        for (Nodo nodo = grafo.getEstaciones().getCabeza(); nodo != null; nodo = nodo.getSiguiente()) {
            Estacion est = (Estacion) nodo.getDato();
            vertices[i] = est;
            mapaIndices.insertar(est.getId(), i); 
            i++;
        }

       /**convertimos las estaciones origen y destino a coordenadas numericas 
        * porque es mas facil comprar numeros y el programa correra mas rapido.
        */
        Integer idxOrigen = (Integer) mapaIndices.obtener(estOrigen.getId());
        Integer idxDestino = (Integer) mapaIndices.obtener(estDestino.getId());

        if (idxOrigen == null || idxDestino == null) {
            return null;
        }
        
        int posOrigen = idxOrigen;
        int posDestino = idxDestino;
  /**
   * almacenamos el acumulado del menor tiempo de viaje conocido
   * guardamos el indice de la estacion previa en la que llegamos
   * true o false para saber si la estacion fue procesada 
   */
        double[] distancia = new double[cantidad];
        int[] previo = new int[cantidad];
        boolean[] visitado = new boolean[cantidad];
/**
 * asumimos que al empezar no hay ruta hacia ningun nodo asi cualquier tiempo real 
 * encontrado sera menor que Infinito
 * indicamos que ningun nodo tiene antecesor definifo
 * el tiempo consumido para ir desde el origen hacia si mismo es 0
 */
        for (int k = 0; k < cantidad; k++) {
            distancia[k] = Double.POSITIVE_INFINITY;
            previo[k] = -1;
        }
        distancia[posOrigen] = 0;

     // haremos como maximo tantas iteraciones como estaciones   
        // pero no procesará todas las estaciones si no es necesario (early exits implementados abajo).
        for (int paso = 0; paso < cantidad; paso++) {
            //usamos el principio (greedy) de Dijkstra: 
            //explora siempre primero la estación más cercana disponible de las que no han sido visitadas.
            // u sera la "estación actual" en la que estamos parados para explorar sus salidas.
            
            int u = buscarMinimo(distancia, visitado);
           
            // Si retorna -1 significa que ya no quedan nodos alcanzables. 
            //Las estaciones restantes son "islas aisladas" 
            // en buscarMinimo se entendera mejor por que -1
            // por lo que detenemos el algoritmo por eficiencia.
            if (u == -1) {
                break;
            }
           // para garantizar que el nodo u no vuelva a ser procesado en iteraciones futuras.
            visitado[u] = true;
            
            // Si el nodo extraído es nuestro destino, ya encontramos 
            // la ruta más corta garantizada. Detenemos el cálculo del resto del grafo.
            if (u == posDestino) {
                break; 
            }
    //  Obtenemos la lista de conexiones de la estación vertices[u]
    //Examinamos todas las salidas directas disponibles desde la estación actual u
    
            ListaEnlazada vecinos = grafo.obtenerVecinos(vertices[u]);
            for (Nodo nc = vecinos.getCabeza(); nc != null; nc = nc.getSiguiente()) {
                // c sera  objeto conexion que es la que une la estacion actual con la vecina
                Conexion c = (Conexion) nc.getDato();
                
                /** usaremos v para guardar el indice numerico de la estacion vecina
                * verificaremos si fue procesada
                * registraremos la distancia y predecesores 
                * Si el camino hacia esa estación vecina es más rápido,
                * se actualiza su tiempo acumulado en distancia[v] y se guarda la estación de origen en previo[v].
                */
                Integer vInteger = (Integer) mapaIndices.obtener(c.getDestino().getId());
                if (vInteger == null) continue;
                
                int v = vInteger;
                if (visitado[v]) continue;
                // actualizamos la tabla cada vez que encontremos un atajo o camino mas rapido
                //hacia la estacion v
                double nuevaDistancia = distancia[u] + c.getTiempo();
                if (nuevaDistancia < distancia[v]) {
                    distancia[v] = nuevaDistancia;
                    previo[v] = u;
                }
            }
        }

        if (distancia[posDestino] == Double.POSITIVE_INFINITY) {
            return null; // No existe un camino entre el origen y el destino
        }
        
        ultimoTiempoRuta = distancia[posDestino];

        // Reconstruimos trazando hacia atrás mediante los 'predecesores', pero
        // usando 'insertarInicio' para asegurar que la lista mantenga el orden 
        // cronológico real y no quede invertida.
        
        ListaEnlazada ruta = new ListaEnlazada();
        int actual = posDestino;
        while (actual != -1) {
            ruta.insertarInicio(vertices[actual]);
            actual = previo[actual];
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
       /** 
        * para no crear arreglos de tamano 0 o relizar calculos que no sean
       necesasrios si la red de transporte no tiene estaciones cargadas
       * */
        int cantidad = grafo.numEstaciones();
        if (cantidad == 0) {
            return new ListaEnlazada();
        }

        /** 
         * 1. Pasamos la lista de estaciones a un arreglo plano
         * para poder entrar y ordenar elementos mediante pocisiones de un arreglo
         * usando: i,pos,j
         */
        Estacion[] estaciones = new Estacion[cantidad];
        int i = 0;
        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            estaciones[i++] = (Estacion) n.getDato();
        }

        // Ajustar k si es mayor al numero de estaciones disponibles
        if (k > cantidad) {
            k = cantidad;
        }
        if (k < 0) {
            k = 0;
        }

        ListaEnlazada hubs = new ListaEnlazada();

        // 2. Algoritmo de Ordenamiento por Seleccion (Top K)
        for (int pos = 0; pos < k; pos++) {
            
            // Validacion explicita de limites recomendada por el profesor
            if (pos >= 0 && pos < estaciones.length) {
                
                int mayor = pos; // Asumimos inicialmente que la estacion en 'pos' es la de mayor grado

                // Buscar el elemento con el grado estrictamente mayor en el resto del arreglo
                for (int j = pos + 1; j < estaciones.length; j++) {
                    // Validaciones de seguridad de limites para los indices j y mayor
                    if (j >= 0 && j < estaciones.length && mayor >= 0 && mayor < estaciones.length) {
                        // Se evalua si el elemento actual supera al candidato 'mayor'
                        if (estaciones[j].getGrado() > estaciones[mayor].getGrado()) {
                            mayor = j;
                        }
                    }
                }

                // 3. Logica de SWAP (Intercambio) transparente con variable auxiliar 'temp'
                // Solo realizamos el intercambio si encontramos un candidato con mayor grado
                if (mayor != pos && mayor >= 0 && mayor < estaciones.length) {
                    // PASO A: Guardar temporalmente el elemento actual en la variable auxiliar
                    Estacion temp = estaciones[pos];
                    
                    // PASO B: Mover el elemento de mayor grado a la posicion actual 'pos'
                    estaciones[pos] = estaciones[mayor];
                    
                    // PASO C: Colocar el elemento guardado en 'temp' en la posicion donde estaba el mayor
                    estaciones[mayor] = temp;
                }

                // Insertar el hub ya ordenado en la lista resultado
                hubs.insertar(estaciones[pos]);
            }
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