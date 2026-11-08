package urbanroute.tda;

/**
 * Cola FIFO hecha con nodos. Se usa en el recorrido BFS.
 */
public class Cola {
    private Nodo frente;
    private Nodo fin;
    private int tamano;

    public Cola() {
        frente = null;
        fin = null;
        tamano = 0;
    }

    /** Agrega un dato al final de la cola. */
    public void encolar(Object dato) {
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

    /** Saca y devuelve el dato del frente. Devuelve null si la cola esta vacia. */
    public Object desencolar() {
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

    /** Devuelve el dato del frente sin sacarlo. */
    public Object verFrente() {
        return frente == null ? null : frente.getDato();
    }

    public boolean esVacia() { return frente == null; }

    public int obtenerTamano() { return tamano; }
}
