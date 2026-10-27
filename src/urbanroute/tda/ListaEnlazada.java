package urbanroute.tda;

/**
 * Lista simplemente enlazada propia (sin Collections Framework).
 * Se usa para listas de adyacencia, cubetas de colisión de la TablaHash
 * y para devolver resultados de los algoritmos.
 * La comparación usa equals(), por lo que las entidades definen equals().
 */
public class ListaEnlazada {
    private Nodo cabeza;
    private Nodo ultimo;
    private int tamano;

    public ListaEnlazada() {
        this.cabeza = null;
        this.ultimo = null;
        this.tamano = 0;
    }

    /** Inserta al final en O(1). */
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

    /** Inserta al inicio en O(1). */
    public void insertarInicio(Object dato) {
        Nodo nuevo = new Nodo(dato);
        nuevo.setSiguiente(cabeza);
        cabeza = nuevo;
        if (ultimo == null) {
            ultimo = nuevo;
        }
        tamano++;
    }

    /** Devuelve el dato almacenado igual (equals) al buscado, o null si no existe. */
    public Object buscar(Object dato) {
        for (Nodo actual = cabeza; actual != null; actual = actual.getSiguiente()) {
            if (iguales(actual.getDato(), dato)) {
                return actual.getDato();
            }
        }
        return null;
    }

    /** Elimina la primera coincidencia. Devuelve true si eliminó algo. */
    public boolean eliminar(Object dato) {
        Nodo previo = null;
        Nodo actual = cabeza;
        while (actual != null) {
            if (iguales(actual.getDato(), dato)) {
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

    /** Obtiene el dato en la posición indicada (0-based). */
    public Object obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Índice " + indice + ", tamaño " + tamano);
        }
        Nodo actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    public int tamano() { return tamano; }
    public boolean estaVacia() { return tamano == 0; }

    public void vaciar() {
        cabeza = null;
        ultimo = null;
        tamano = 0;
    }

    /** Acceso al primer nodo para recorridos: for (Nodo n = l.getCabeza(); n != null; n = n.getSiguiente()). */
    public Nodo getCabeza() { return cabeza; }

    private static boolean iguales(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
