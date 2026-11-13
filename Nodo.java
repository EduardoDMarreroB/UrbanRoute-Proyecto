package urbanroute.tda;

// Nodo genérico de una lista simplemente enlazada
public class Nodo<T> {

    private T dato; // Dato almacenado en este nodo
    private Nodo<T> siguiente; // Referencia al siguiente nodo de la lista, null si es el último

    // Crea un nodo con el dato indicado y sin sucesor
    public Nodo(T dato) {
        this.dato = dato; // guardamos el elemento
        this.siguiente = null; // Se asume que no hay siguiente
    }

    public T getDato() {
        return dato; // Retorna el contenido actual del nodo
    }

    public void setDato(T dato) {
        this.dato = dato; // actualizamos el dato
    }

    public Nodo<T> getSiguiente() {
        return siguiente; // Retorna el enlace al próximo nodo
    }

    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente; // Conectamos este nodo con el sucesor
    }
}
