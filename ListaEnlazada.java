package urbanroute.tda;

/**
 * Lista simplemente enlazada genérica con referencias al primer y al último nodo.
 * Los elementos se comparan mediante equals().
 *
 * Basado en la representación de listas enlazadas presentada en:
 * Cormen, Leiserson, Rivest y Stein, Introduction to Algorithms, 3.ª edición,
 * MIT Press, 2009, capítulo 10 (Elementary Data Structures).
 *
 * @param <T> Tipo de dato almacenado en la lista.
 */
public class ListaEnlazada<T> {

    private Nodo<T> cabeza;            // Primer nodo de la lista.
    private Nodo<T> ultimo;            // Último nodo para insertar al final en O(1).
    private int tamano;                // Cantidad de elementos almacenados.

    /** Construye una lista inicialmente vacía. */
    public ListaEnlazada() {
        cabeza = null;
        ultimo = null;
        tamano = 0;
    }

    /** Agrega un dato al final de la lista. */
    public void insertar(T dato) {
        Nodo<T> nuevoNodo = new Nodo<>(dato);

        if (cabeza == null) {
            cabeza = nuevoNodo;
            ultimo = nuevoNodo;
        } else {
            ultimo.setSiguiente(nuevoNodo);
            ultimo = nuevoNodo;
        }

        tamano++;
    }

    /** Agrega un dato al inicio de la lista. */
    public void insertarInicio(T dato) {
        Nodo<T> nuevoNodo = new Nodo<>(dato);
        nuevoNodo.setSiguiente(cabeza);
        cabeza = nuevoNodo;

        if (ultimo == null) {
            ultimo = nuevoNodo;
        }

        tamano++;
    }

    /**
     * Devuelve el primer dato igual al recibido, o null si no se encuentra.
     * La comparación admite también elementos null.
     */
    public T buscar(T dato) {
        for (Nodo<T> nodoActual = cabeza;
             nodoActual != null;
             nodoActual = nodoActual.getSiguiente()) {

            if (sonIguales(nodoActual.getDato(), dato)) {
                return nodoActual.getDato();
            }
        }

        return null;
    }

    /** Elimina la primera coincidencia. Devuelve true si eliminó un elemento. */
    public boolean eliminar(T dato) {
        Nodo<T> nodoAnterior = null;
        Nodo<T> nodoActual = cabeza;

        while (nodoActual != null) {
            if (sonIguales(nodoActual.getDato(), dato)) {
                desvincularNodo(nodoAnterior, nodoActual);
                return true;
            }

            nodoAnterior = nodoActual;
            nodoActual = nodoActual.getSiguiente();
        }

        return false;
    }

    /**
     * Elimina el elemento ubicado en la posición indicada (la primera es 0).
     *
     * @param posicionLista posición del elemento que se desea eliminar.
     * @throws IndexOutOfBoundsException si la posición no pertenece a la lista.
     */
    public void eliminarEn(int posicionLista) {
        validarPosicion(posicionLista);

        Nodo<T> nodoAnterior = null;
        Nodo<T> nodoActual = cabeza;

        for (int indiceLista = 0; indiceLista < posicionLista; indiceLista++) {
            nodoAnterior = nodoActual;
            nodoActual = nodoActual.getSiguiente();
        }

        desvincularNodo(nodoAnterior, nodoActual);
    }

    /** Devuelve el dato de la posición indicada (la primera es 0). */
    public T obtener(int posicionLista) {
        validarPosicion(posicionLista);

        Nodo<T> nodoActual = cabeza;
        for (int indiceLista = 0; indiceLista < posicionLista; indiceLista++) {
            nodoActual = nodoActual.getSiguiente();
        }

        return nodoActual.getDato();
    }

    /** Devuelve la cantidad de elementos almacenados. */
    public int obtenerTamano() {
        return tamano;
    }

    /** Indica si la lista no contiene elementos. */
    public boolean esVacia() {
        return tamano == 0;
    }

    /** Elimina todos los elementos de la lista. */
    public void vaciar() {
        cabeza = null;
        ultimo = null;
        tamano = 0;
    }

    /** Devuelve el primer nodo para permitir recorrer la lista. */
    public Nodo<T> getCabeza() {
        return cabeza;
    }

    /** Valida que una posición corresponda a un elemento existente. */
    private void validarPosicion(int posicionLista) {
        if (posicionLista < 0 || posicionLista >= tamano) {
            throw new IndexOutOfBoundsException(
                    "Posición " + posicionLista + ", tamaño " + tamano
            );
        }
    }

    /** Desvincula un nodo y mantiene correctamente las referencias de la lista. */
    private void desvincularNodo(Nodo<T> nodoAnterior, Nodo<T> nodoActual) {
        if (nodoAnterior == null) {
            cabeza = nodoActual.getSiguiente();
        } else {
            nodoAnterior.setSiguiente(nodoActual.getSiguiente());
        }

        if (nodoActual == ultimo) {
            ultimo = nodoAnterior;
        }

        tamano--;

        if (tamano == 0) {
            cabeza = null;
            ultimo = null;
        }
    }

    /** Compara dos datos contemplando el caso en que ambos sean null. */
    private boolean sonIguales(T datoA, T datoB) {
        return datoA == datoB || (datoA != null && datoA.equals(datoB));
    }
}
