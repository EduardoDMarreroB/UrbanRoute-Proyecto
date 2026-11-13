package urbanroute.tda;

/**
 * Cola genérica FIFO (First In, First Out) implementada mediante
 * nodos propios de una lista simplemente enlazada.
 *
 * Los elementos se insertan al final de la cola y se extraen
 * desde el frente, respetando el orden de llegada.
 *
 * Mantiene referencias al primer y al último nodo para realizar
 * las operaciones de inserción y extracción en tiempo O(1).
 *
 * No permite almacenar elementos null.
 *
 * @param <T> Tipo de dato que almacenará la cola.
 */
public class Cola<T> {

    /** Referencia al primer nodo de la cola. */
    private Nodo<T> frente;

    /** Referencia al último nodo de la cola. */
    private Nodo<T> fin;

    /** Cantidad de elementos almacenados en la cola. */
    private int tamano;

    /**
     * Crea una cola vacía.
     *
     * Inicializa las referencias al frente y al final en null,
     * y establece el tamaño inicial en cero.
     */
    public Cola() {
        frente = null;
        fin = null;
        tamano = 0;
    }

    /**
     * Inserta un elemento al final de la cola.
     *
     * Si la cola está vacía, el nuevo nodo será tanto el frente
     * como el final. En caso contrario, se enlaza después del
     * último nodo existente.
     *
     * La operación se realiza en tiempo O(1).
     *
     * @param dato Elemento que se desea insertar.
     * @throws IllegalArgumentException Si el elemento es null.
     */
    public void encolar(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException(
                    "No se permiten elementos nulos en la cola."
            );
        }

        Nodo<T> nuevoNodo = new Nodo<>(dato);

        if (fin == null) {
            frente = nuevoNodo;
            fin = nuevoNodo;
        } else {
            fin.setSiguiente(nuevoNodo);
            fin = nuevoNodo;
        }

        tamano++;
    }

    /**
     * Extrae y devuelve el elemento ubicado al frente de la cola.
     *
     * Después de extraer el elemento, actualiza la referencia
     * al frente. Si la cola queda vacía, también establece
     * la referencia al final en null.
     *
     * La operación se realiza en tiempo O(1).
     *
     * @return Elemento que se encontraba al frente,
     *         o null si la cola está vacía.
     */
    public T desencolar() {
        if (frente == null) {
            return null;
        }

        T dato = frente.getDato();

        frente = frente.getSiguiente();

        if (frente == null) {
            fin = null;
        }

        tamano--;

        return dato;
    }

    /**
     * Consulta el elemento ubicado al frente de la cola
     * sin extraerlo.
     *
     * La operación se realiza en tiempo O(1).
     *
     * @return Elemento ubicado al frente,
     *         o null si la cola está vacía.
     */
    public T verFrente() {
        return frente == null ? null : frente.getDato();
    }

    /**
     * Comprueba si la cola no contiene elementos.
     *
     * La operación se realiza en tiempo O(1).
     *
     * @return true si la cola está vacía;
     *         false en caso contrario.
     */
    public boolean esVacia() {
        return frente == null;
    }

    /**
     * Obtiene la cantidad de elementos almacenados actualmente.
     *
     * La operación se realiza en tiempo O(1).
     *
     * @return Número de elementos que contiene la cola.
     */
    public int obtenerTamano() {
        return tamano;
    }
}
