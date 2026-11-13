package urbanroute.tda;

    /**
     * Representa un nodo o eslabon de una lista simple.
     */
public class Nodo<T> {
    private T dato; // Dato guardado en este nodo
    
    // Enlace al proximo nodo de la lista (es null si es el último)
    private Nodo<T> siguiente; 

    /**
     * Crea un nodo guardando el dato y sin apuntar a nadie más todavía.
     */
    public Nodo(T dato) {
        this.dato = dato; // Guardamos el elemento recibido
        this.siguiente = null; // Por ahora asume que no hay siguiente nodo
    }

    /**
     * Obtiene el dato que esta guardado en el nodo.
     */
    public T getDato() {
        return dato;
    }

    /**
     * Cambia el dato guardado en el nodo por uno nuevo.
     */
    public void setDato(T dato) {
        this.dato = dato; // Actualizamos el dato
    }

    /**
     * Obtiene el nodo que le sigue a este (Siguiente Nodo).
     */
    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    /**
     * Conecta un nodo con el nodo que le va a seguir en la lista.
     */
    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente; // Hacemos la conexion
    }
}
