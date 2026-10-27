package urbanroute.tda;

/** Cola FIFO propia basada en Nodo. Usada por el recorrido BFS. */
public class Cola {
    private Nodo frente;
    private Nodo fin;
    private int tamano;

    public Cola() {
        this.frente = null;
        this.fin = null;
        this.tamano = 0;
    }

    public void enqueue(Object dato) {
        Nodo nuevo = new Nodo(dato);
        if (fin == null) {
            frente = nuevo;
            fin = nuevo;
        } else {
            fin.setSiguiente(nuevo);
            fin = nuevo;
        }
        tamano++;
    }

    /** Extrae el elemento del frente; devuelve null si la cola está vacía. */
    public Object dequeue() {
        if (frente == null) {
            return null;
        }
        Object dato = frente.getDato();
        frente = frente.getSiguiente();
        if (frente == null) {
            fin = null;
        }
        tamano--;
        return dato;
    }

    public Object verFrente() { return frente == null ? null : frente.getDato(); }
    public boolean estaVacia() { return frente == null; }
    public int tamano() { return tamano; }
}
