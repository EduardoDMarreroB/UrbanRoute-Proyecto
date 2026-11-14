package urbanroute.modelo;

import urbanroute.control.AnalizadorRed;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Grafo ponderado con listas de adyacencia.
 * 
 * Guarda todas las estaciones en una lista enlazada y usa una tabla hash
 * para encontrarlas rapidamente por su ID. Cada estacion tiene su propia
 * lista de conexiones, asi que el grafo se representa como una lista
 * de adyacencia.
 * 
 * Por defecto el grafo no es dirigido: cuando se agrega una conexion, se
 * guarda tambien en el sentido contrario para poder recorrerlo en ambos
 * sentidos.
 */
public class GrafoPonderado {

    /** Lista con todas las estaciones registradas en el grafo. */
    private ListaEnlazada<Estacion> estaciones;

    /** Tabla hash que permite buscar estaciones por su ID. */
    private TablaHash indice;

    /** Indica si el grafo es dirigido o no. */
    private boolean dirigido;

    /**
     * Crea un grafo ponderado indicando si es dirigido o no. Requiere una 
     * tabla hash inicializada. Lanza un error si la tabla hash es nula.
     */
    public GrafoPonderado(TablaHash indice, boolean dirigido) {
        if (indice == null) {
            throw new IllegalArgumentException(
                    "La tabla hash no puede ser nula."
            );
        }
        this.estaciones = new ListaEnlazada<>();
        this.indice = indice;
        this.dirigido = dirigido;
    }

    /**
     * Construye un grafo ponderado no dirigido por defecto, usando el indice 
     * proporcionado para buscar las estaciones.
     */
    public GrafoPonderado(TablaHash indice) {
        this(indice, false);
    }

    /**
     * Agrega una estacion al grafo y a la tabla hash. No se permite agregar 
     * una estacion nula ni una cuyo ID ya exista. Devuelve verdadero si la 
     * operacion fue exitosa, o falso si fue rechazada.
     */
    public boolean agregarEstacion(Estacion e) {
        if (e == null || estaciones.buscar(e) != null) {
            return false;
        }
        estaciones.insertar(e);
        indice.insertar(e.getId(), e);
        return true;
    }

    /**
     * Elimina una estacion del grafo y de la tabla hash. Antes de eliminarla 
     * recorre las demas para quitar conexiones que apuntaban a ella, evitando 
     * dejar rutas colgadas. Devuelve verdadero si se borro correctamente.
     */
    public boolean eliminarEstacion(String id) {
        Estacion e = indice.buscar(id);
        if (e == null) {
            return false;
        }

        // Se usa tiempo 0: al comparar conexiones solo importan origen y destino
        for (Nodo<Estacion> n = estaciones.getCabeza(); 
             n != null; 
             n = n.getSiguiente()) {
            
            Estacion otra = n.getDato();
            otra.getConexiones().eliminar(new Conexion(otra, e, 0));
        }
        
        e.getConexiones().vaciar();
        estaciones.eliminar(e);
        indice.eliminar(id);
        return true;
    }

    /**
     * Agrega una conexion entre dos estaciones con un tiempo determinado.
     * Si la conexion ya existia, se actualiza su tiempo. Si el grafo no es
     * dirigido, tambien se registra en sentido inverso. Retorna verdadero 
     * si se guardo con exito.
     */
    public boolean agregarConexion(
            Estacion origen, Estacion destino, double tiempo) {
        
        if (origen == null || destino == null || origen.equals(destino) ||
                tiempo < 0) {
            return false;
        }

        registrarConexion(origen, destino, tiempo);

        // Si es no dirigido, la conexion se recorre en ambos sentidos.
        if (!dirigido) {
            registrarConexion(destino, origen, tiempo);
        }
        return true;
    }

    /**
     * Version alternativa para agregar una conexion buscando las estaciones 
     * directamente por sus textos de ID. Devuelve verdadero si se registro.
     */
    public boolean agregarConexion(
            String idOrigen, String idDestino, double tiempo) {
        return agregarConexion(
                indice.buscar(idOrigen), indice.buscar(idDestino), tiempo
        );
    }

    /**
     * Metodo interno que guarda el tiempo entre dos estaciones. Si la 
     * conexion ya existia, solo se actualiza su duracion.
     */
    private void registrarConexion(Estacion a, Estacion b, double tiempo) {
        Conexion existente = (Conexion)
                a.getConexiones().buscar(new Conexion(a, b, 0));
        
        if (existente != null) {
            existente.setTiempo(tiempo);
        } else {
            a.getConexiones().insertar(new Conexion(a, b, tiempo));
        }
    }

    /**
     * Elimina la conexion entre dos estaciones. Si el grafo no es dirigido, 
     * tambien elimina la conexion inversa para mantener la red consistente.
     * Retorna verdadero si se logro eliminar algo.
     */
    public boolean eliminarConexion(Estacion origen, Estacion destino) {
        if (origen == null || destino == null) {
            return false;
        }
        boolean eliminada = origen.getConexiones().
                eliminar(new Conexion(origen, destino, 0));
        
        if (!dirigido) {
            eliminada = destino.getConexiones().
                    eliminar(new Conexion(destino, origen, 0)) || eliminada;
        }
        return eliminada;
    }

    /**
     * Devuelve la lista de conexiones que salen de una estacion especifica.
     * Si la estacion entregada es nula, devuelve una lista vacia.
     */
    public ListaEnlazada<Conexion> obtenerVecinos(Estacion e) {
        if (e == null) {
            return new ListaEnlazada<>();
        }
        return e.getConexiones();
    }

    /**
     * Busca una estacion por su ID usando la tabla hash y la devuelve.
     */
    public Estacion buscarEstacion(String id) {
        return indice.buscar(id);
    }

    /**
     * Devuelve la conexion exacta entre dos estaciones, o nulo si no existe.
     */
    public Conexion buscarConexion(Estacion origen, Estacion destino) {
        if (origen == null || destino == null) {
            return null;
        }
        return (Conexion) origen.getConexiones().
                buscar(new Conexion(origen, destino, 0));
    }

    /**
     * Devuelve las lineas distintas que aparecen en la red. Recorre la lista 
     * de estaciones y guarda los nombres de las lineas sin repetir, 
     * manteniendo el orden original.
     */
    public ListaEnlazada<String> obtenerLineas() {
        ListaEnlazada<String> lineas = new ListaEnlazada<>();
        
        for (Nodo<Estacion> n = estaciones.getCabeza(); 
             n != null; 
             n = n.getSiguiente()) {
            
            String linea = n.getDato().getLinea();

            if (lineas.buscar(linea) == null) {
                lineas.insertar(linea);
            }
        }
        return lineas;
    }

    /**
     * Devuelve el numero total de estaciones registradas en el grafo.
     */
    public int numEstaciones() { 
        return estaciones.obtenerTamano(); 
    }

    /**
     * Cuenta las conexiones guardadas en total. En un grafo no dirigido cada 
     * viaje cuenta doble porque se guarda internamente en ambos sentidos.
     */
    public int numConexiones() {
        int total = 0;
        for (Nodo<Estacion> n = estaciones.getCabeza(); 
             n != null; 
             n = n.getSiguiente()) {
            
            total += n.getDato().getConexiones().obtenerTamano();
        }
        return total;
    }

    /**
     * Vacia todas las rutas y conexiones, pero mantiene las estaciones intactas.
     */
    public void limpiarConexiones() {
        for (Nodo<Estacion> n = estaciones.getCabeza(); 
             n != null; 
             n = n.getSiguiente()) {
            
            n.getDato().getConexiones().vaciar();
        }
    }

    /**
     * Borra absolutamente todo: deja vacios el grafo y la tabla hash.
     */
    public void limpiar() {
        limpiarConexiones();
        estaciones.vaciar();
        indice.limpiar();
    }

    /**
     * Devuelve la estructura principal que contiene a todas las estaciones.
     */
    public ListaEnlazada<Estacion> getEstaciones() { 
        return estaciones; 
    }

    /**
     * Devuelve la tabla hash utilizada para encontrar estaciones.
     */
    public TablaHash getIndice() { 
        return indice; 
    }

    /**
     * Indica mediante un valor booleano si este grafo es dirigido.
     */
    public boolean esDirigido() { 
        return dirigido; 
    }
}
