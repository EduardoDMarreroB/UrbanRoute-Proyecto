package urbanroute.tda;

/**
 * Nodo genérico de una lista simplemente enlazada.
 *
 * Cada nodo almacena un dato de tipo T y una referencia
 * al siguiente nodo de la lista.
 *
 * @param <T> Tipo de dato almacenado en el nodo.
 */
public class Nodo<T> {

    /** Dato almacenado en este nodo. */
    private T dato;

    /** Referencia al siguiente nodo de la lista. */
    private Nodo<T> siguiente;

    /**
     * Crea un nodo con el dato indicado y sin sucesor.
     *
     * @param dato Elemento que almacenará el nodo.
     */
    public Nodo(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    /**
     * Obtiene el dato almacenado en el nodo.
     *
     * @return Dato contenido en el nodo.
     */
    public T getDato() {
        return dato;
    }

    /**
     * Modifica el dato almacenado en el nodo.
     *
     * @param dato Nuevo elemento que se almacenará.
     */
    public void setDato(T dato) {
        this.dato = dato;
    }

    /**
     * Obtiene la referencia al siguiente nodo.
     *
     * @return Siguiente nodo, o null si no existe.
     */
    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    /**
     * Establece el siguiente nodo de la lista.
     *
     * @param siguiente Nodo que seguirá a este,
     *                  o null si será el último.
     */
    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }
}
