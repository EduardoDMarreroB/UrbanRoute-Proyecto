// Archivo: urbanroute/tda/Nodo.java
package urbanroute.tda;

/**
 * Nodo de una estructura enlazada: guarda un dato de tipo genérico y la referencia al nodo siguiente.
 * @param <T> Tipo de dato que almacenará el nodo.
 */
public class Nodo<T> {
    private T dato;
    private Nodo<T> siguiente;

    public Nodo(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    public T getDato() { return dato; }
    public void setDato(T dato) { this.dato = dato; }
    public Nodo<T> getSiguiente() { return siguiente; }
    public void setSiguiente(Nodo<T> siguiente) { this.siguiente = siguiente; }
}
