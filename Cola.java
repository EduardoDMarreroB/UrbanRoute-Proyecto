package urbanroute.tda;

// Cola genérica de estructura FIFO hecha con nodos.
public class Cola<T> {
    private Nodo<T> frente;
    private Nodo<T> fin;
    private int tamano;

    public Cola() {
        frente = null;
        fin = null;
        tamano = 0;
    }

    // Agrega un dato al final de la cola.
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

    // Saca y devuelve el dato del frente.
    // Devuelve null si la cola está vacía.
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

    // Devuelve el dato del frente sin sacarlo.
    public T verFrente() {
        return frente == null ? null : frente.getDato();
    }
    
    public boolean esVacia() {
        return frente == null;
    }
    
    public int obtenerTamano() {
        return tamano;
    }
}
