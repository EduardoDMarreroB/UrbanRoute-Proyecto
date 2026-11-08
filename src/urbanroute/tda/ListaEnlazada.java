package urbanroute.tda;

/**
 * Lista simplemente enlazada con referencia al primer y al ultimo nodo.
 * Los datos se comparan con equals().
 */
public class ListaEnlazada {
    private Nodo cabeza;
    private Nodo ultimo;
    private int tamano;

    public ListaEnlazada() {
        cabeza = null;
        ultimo = null;
        tamano = 0;
    }

    /** Agrega un dato al final de la lista. */
    public void insertar(Object dato) {
        Nodo nuevo = new Nodo(dato);
        if (cabeza == null) {
            cabeza = nuevo;
            ultimo = nuevo;
        } else {
            ultimo.setSiguiente(nuevo);
            ultimo = nuevo;
        }
        tamano++;
    }

    /** Agrega un dato al inicio de la lista. */
    public void insertarInicio(Object dato) {
        Nodo nuevo = new Nodo(dato);
        nuevo.setSiguiente(cabeza);
        cabeza = nuevo;
        if (ultimo == null) {
            ultimo = nuevo;
        }
        tamano++;
    }

    /** Devuelve el dato guardado que sea igual al recibido, o null si no esta. */
    public Object buscar(Object dato) {
        for (Nodo actual = cabeza; actual != null; actual = actual.getSiguiente()) {
            if (sonIguales(actual.getDato(), dato)) {
                return actual.getDato();
            }
        }
        return null;
    }

    /** Elimina la primera coincidencia. Devuelve true si elimino algo. */
    public boolean eliminar(Object dato) {
        Nodo previo = null;
        Nodo actual = cabeza;
        while (actual != null) {
            if (sonIguales(actual.getDato(), dato)) {
                if (previo == null) {
                    cabeza = actual.getSiguiente();
                } else {
                    previo.setSiguiente(actual.getSiguiente());
                }
                if (actual == ultimo) {
                    ultimo = previo;
                }
                tamano--;
                return true;
            }
            previo = actual;
            actual = actual.getSiguiente();
        }
        return false;
    }

    /** Devuelve el dato de la posicion indicada (la primera es la 0). */
    public Object obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Indice " + indice + ", tamano " + tamano);
        }
        Nodo actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    public int obtenerTamano() { return tamano; }

    public boolean esVacia() { return tamano == 0; }

    public void vaciar() {
        cabeza = null;
        ultimo = null;
        tamano = 0;
    }

    /** Primer nodo de la lista, para poder recorrerla. */
    public Nodo getCabeza() { return cabeza; }

    private boolean sonIguales(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
