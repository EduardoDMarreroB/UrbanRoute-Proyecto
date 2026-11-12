package urbanroute.modelo;

import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Tabla hash genérica para almacenar parejas clave-valor.
 * Resuelve las colisiones mediante encadenamiento separado (Separate Chaining).
 *
 * Fundamento teórico: el modelo de tablas hash con encadenamiento separado se
 * presenta en Cormen, Leiserson, Rivest y Stein, Introduction to Algorithms,
 * 3.ª edición, MIT Press, 2009, capítulo 11; y en Weiss, Data Structures and
 * Algorithm Analysis in Java, 3.ª edición, Pearson, 2012, capítulo 5.
 *
 * @param <K> tipo de dato de la clave.
 * @param <V> tipo de dato del valor asociado a la clave.
 */
public class TablaHash<K, V> {

    /*
     * 31 es un número primo impar. Usar una capacidad prima en la reducción
     * modular ayuda a evitar patrones regulares de agrupamiento para ciertos
     * conjuntos de claves; no garantiza por sí solo que no existan colisiones.
     */
    private static final int CAPACIDAD_INICIAL = 31;

    private static final double FACTOR_MAXIMO = 0.75; // Umbral de carga antes de redimensionar.

    /** Representa una pareja clave-valor almacenada en una casilla. */
    private static class Entrada<K, V> {
        private final K clave;         // Clave única de la entrada.
        private V valor;               // Valor asociado, que puede actualizarse.

        private Entrada(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }

        private K getClave() {
            return clave;
        }

        private V getValor() {
            return valor;
        }

        private void setValor(V valor) {
            this.valor = valor;
        }
    }

    /*
     * Java no permite crear directamente arreglos de tipos parametrizados
     * (por ejemplo, ListaEnlazada<Entrada<K,V>>[]) sin una conversión no
     * comprobada o una supresión de advertencias. Para evitar ambas cosas,
     * casillas se representa como una lista fija de listas: cada lista interna
     * es una cadena de colisiones y la lista externa mantiene las posiciones.
     * No se utiliza Java Collections Framework.
     */
    private ListaEnlazada<ListaEnlazada<Entrada<K, V>>> casillas;

    private int capacidad;             // Cantidad de casillas de la tabla.
    private int tamano;                // Cantidad total de entradas almacenadas.
    private GrafoPonderado propietario; // Grafo que utiliza esta tabla como índice.

    /** Constructor predeterminado: crea 31 casillas. */
    public TablaHash() {
        this(CAPACIDAD_INICIAL);
    }

    /**
     * Construye la tabla con una capacidad inicial personalizada.
     * Si la capacidad recibida es menor que 1, utiliza 31.
     */
    public TablaHash(int capacidadInicial) {
        if (capacidadInicial < 1) {
            capacidadInicial = CAPACIDAD_INICIAL;
        }

        capacidad = capacidadInicial;
        tamano = 0;
        casillas = crearCasillas(capacidad);
    }

    /**
     * Calcula el índice de una clave mediante DJB2.
     *
     * Se utiliza la recurrencia h(i + 1) = 33 * h(i) + c(i), con semilla 5381.
     * La multiplicación por 33 se implementa como desplazamiento y suma.
     * Referencia: Daniel J. Bernstein, algoritmo DJB2, documentado en
     * "Hash Functions", University of York:
     * https://www.cse.yorku.ca/~oz/hash.html
     *
     * Para claves genéricas se utiliza su representación String. Por ello,
     * cada tipo K debe proporcionar un toString() estable y coherente con
     * equals(): dos claves iguales deben producir la misma representación.
     */
    private int calcularIndice(K clave, int capacidadTabla) {
        String representacionClave = clave.toString();
        int valorHash = 5381;         // Semilla inicial del algoritmo DJB2.

        for (int indiceCaracter = 0;
             indiceCaracter < representacionClave.length();
             indiceCaracter++) {

            char caracterActual = representacionClave.charAt(indiceCaracter);
            valorHash = ((valorHash << 5) + valorHash) + caracterActual; // hash * 33 + carácter.
        }

        return Math.floorMod(valorHash, capacidadTabla);
    }

    /** Calcula el índice de una clave usando la capacidad actual. */
    private int hash(K clave) {
        return calcularIndice(clave, capacidad);
    }

    /** Crea la estructura de casillas y una lista de colisiones por posición. */
    private ListaEnlazada<ListaEnlazada<Entrada<K, V>>> crearCasillas(int cantidadCasillas) {
        ListaEnlazada<ListaEnlazada<Entrada<K, V>>> nuevasCasillas = new ListaEnlazada<>();

        for (int posicionTabla = 0; posicionTabla < cantidadCasillas; posicionTabla++) {
            nuevasCasillas.insertar(new ListaEnlazada<>());
        }

        return nuevasCasillas;
    }

    /** Devuelve la lista de colisiones ubicada en una posición de la tabla. */
    private ListaEnlazada<Entrada<K, V>> obtenerCasilla(int posicionTabla) {
        return casillas.obtener(posicionTabla);
    }

    /** Busca una entrada por su clave dentro de una casilla concreta. */
    private Entrada<K, V> buscarEntrada(int posicionTabla, K clave) {
        ListaEnlazada<Entrada<K, V>> casillaActual = obtenerCasilla(posicionTabla);

        for (Nodo<Entrada<K, V>> nodoActual = casillaActual.getCabeza();
             nodoActual != null;
             nodoActual = nodoActual.getSiguiente()) {

            Entrada<K, V> entradaActual = nodoActual.getDato();
            if (entradaActual.getClave().equals(clave)) {
                return entradaActual;
            }
        }

        return null;
    }

    /** Devuelve el índice de una entrada dentro de su lista, o -1 si no existe. */
    private int buscarIndiceEntrada(int posicionTabla, K clave) {
        ListaEnlazada<Entrada<K, V>> casillaActual = obtenerCasilla(posicionTabla);
        int indiceEnCasilla = 0;

        for (Nodo<Entrada<K, V>> nodoActual = casillaActual.getCabeza();
             nodoActual != null;
             nodoActual = nodoActual.getSiguiente()) {

            if (nodoActual.getDato().getClave().equals(clave)) {
                return indiceEnCasilla;
            }

            indiceEnCasilla++;
        }

        return -1;
    }

    /** Inserta una pareja clave-valor o actualiza el valor si la clave existe. */
    public void insertar(K clave, V valor) {
        if (clave == null) {
            return; // Las claves null no se admiten.
        }

        int posicionTabla = hash(clave);
        Entrada<K, V> entradaExistente = buscarEntrada(posicionTabla, clave);

        if (entradaExistente != null) {
            entradaExistente.setValor(valor);
            return;
        }

        obtenerCasilla(posicionTabla).insertar(new Entrada<>(clave, valor));
        tamano++;

        if ((double) tamano / capacidad > FACTOR_MAXIMO) {
            redimensionar();
        }
    }

    /** Busca una clave y devuelve su valor asociado, o null si no existe. */
    public V buscar(K clave) {
        if (clave == null) {
            return null;
        }

        int posicionTabla = hash(clave);
        Entrada<K, V> entradaEncontrada = buscarEntrada(posicionTabla, clave);
        return entradaEncontrada == null ? null : entradaEncontrada.getValor();
    }

    /** Alias de buscar(), conservado por compatibilidad con el código del proyecto. */
    public V obtener(K clave) {
        return buscar(clave);
    }

    /** Elimina la entrada asociada a la clave indicada. */
    public boolean eliminar(K clave) {
        if (clave == null) {
            return false;
        }

        int posicionTabla = hash(clave);
        int indiceEnCasilla = buscarIndiceEntrada(posicionTabla, clave);

        if (indiceEnCasilla < 0) {
            return false;
        }

        obtenerCasilla(posicionTabla).eliminarEn(indiceEnCasilla);
        tamano--;
        return true;
    }

    /**
     * Duplica aproximadamente la capacidad y redistribuye las entradas.
     * El contador tamano no cambia porque solo se trasladan entradas existentes.
     */
    private void redimensionar() {
        long calculoNuevaCapacidad = (long) capacidad * 2 + 1;

        if (calculoNuevaCapacidad > Integer.MAX_VALUE) {
            throw new IllegalStateException("La tabla alcanzó el límite de capacidad.");
        }

        int nuevaCapacidad = (int) calculoNuevaCapacidad;
        ListaEnlazada<ListaEnlazada<Entrada<K, V>>> nuevasCasillas = crearCasillas(nuevaCapacidad);

        for (int posicionTabla = 0; posicionTabla < capacidad; posicionTabla++) {
            ListaEnlazada<Entrada<K, V>> casillaActual = obtenerCasilla(posicionTabla);

            for (Nodo<Entrada<K, V>> nodoActual = casillaActual.getCabeza();
                 nodoActual != null;
                 nodoActual = nodoActual.getSiguiente()) {

                Entrada<K, V> entradaActual = nodoActual.getDato();
                int nuevoIndice = calcularIndice(entradaActual.getClave(), nuevaCapacidad);
                nuevasCasillas.obtener(nuevoIndice).insertar(entradaActual);
            }
        }

        casillas = nuevasCasillas;
        capacidad = nuevaCapacidad;
    }

    /** Devuelve una lista enlazada con todos los valores almacenados. */
    public ListaEnlazada<V> obtenerTodas() {
        ListaEnlazada<V> todosLosValores = new ListaEnlazada<>();

        for (int posicionTabla = 0; posicionTabla < capacidad; posicionTabla++) {
            ListaEnlazada<Entrada<K, V>> casillaActual = obtenerCasilla(posicionTabla);

            for (Nodo<Entrada<K, V>> nodoActual = casillaActual.getCabeza();
                 nodoActual != null;
                 nodoActual = nodoActual.getSiguiente()) {

                todosLosValores.insertar(nodoActual.getDato().getValor());
            }
        }

        return todosLosValores;
    }

    /** Comprueba si una clave existe, incluso cuando su valor asociado es null. */
    public boolean contiene(K clave) {
        if (clave == null) {
            return false;
        }

        int posicionTabla = hash(clave);
        return buscarEntrada(posicionTabla, clave) != null;
    }

    /** Devuelve la cantidad de entradas almacenadas. */
    public int obtenerTamano() {
        return tamano;
    }

    /** Devuelve la capacidad actual de la tabla. */
    public int obtenerCapacidad() {
        return capacidad;
    }

    /** Elimina las entradas y restaura la capacidad inicial. */
    public void limpiar() {
        capacidad = CAPACIDAD_INICIAL;
        casillas = crearCasillas(capacidad);
        tamano = 0;
    }

    /** Devuelve el grafo asociado a esta tabla hash. */
    public GrafoPonderado getPropietario() {
        return propietario;
    }

    /** Establece el grafo que utiliza esta tabla como índice. */
    public void setPropietario(GrafoPonderado propietario) {
        this.propietario = propietario;
    }
}
