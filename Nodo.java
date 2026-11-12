package urbanroute.tda;

/**
 * Nodo genérico de una lista simplemente enlazada.
 *
 * @param <T> Tipo de dato almacenado en el nodo.
 */
public class Nodo<T> {

    private T dato;                    // Dato almacenado en este nodo.
    private Nodo<T> siguiente;         // Referencia al siguiente nodo.

    /** Crea un nodo con el dato indicado y sin sucesor. */
    public Nodo(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    public T getDato() {
        return dato;
    }

    public void setDato(T dato) {
        this.dato = dato;
    }

    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }
}
