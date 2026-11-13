package urbanroute.tda;

/**
 * Cola con estructura FIFO hecha con nodos. Se usa para el recorrido BFS.
 */
public class Cola<T> {
    private Nodo<T> frente;
    private Nodo<T> fin;
    private int tamano;

     /**
     * Crea una cola sin elementos (vacía).
     */
    public Cola() {
        frente = null;
        fin = null;
        tamano = 0;
    }

    /** 
     * Agrega un dato al final de la cola. 
     */
    public void encolar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (fin == null) {
            frente = nuevo;
            fin = nuevo;
        } else {
            fin.setSiguiente(nuevo);
            fin = nuevo;
        }
        tamano++;
    }

    /** Saca y devuelve el dato del frente. 
     *  Si la cola está vacía, retorna null. 
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
     * Devuelve el dato del frente sin sacarlo.
     */
    public T verFrente() {
        return frente == null ? null : frente.getDato();
    }

    /**
     * Comprueba si la cola está vacía.
     */
    public boolean esVacia() { return frente == null; }

    /**
     * Obtiene la cantidad de elementos almacenados en la cola.
     */
    public int obtenerTamano() { return tamano; }
}
