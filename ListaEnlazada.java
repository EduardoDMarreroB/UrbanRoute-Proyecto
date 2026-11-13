package urbanroute.tda;

/**
 * Lista simplemente enlazada genérica implementada mediante nodos propios.
 *
 * Mantiene referencias al primer y al último nodo para permitir
 * inserciones al inicio y al final sin recorrer toda la lista.
 * Los elementos se comparan mediante equals().
 *
 * @param <T> Tipo de dato almacenado en la lista.
 */
public class ListaEnlazada<T> {

    /** Referencia al primer nodo de la lista. */
    private Nodo<T> cabeza;

    /** Referencia al último nodo de la lista. */
    private Nodo<T> ultimo;

    /** Cantidad de elementos almacenados en la lista. */
    private int tamano;

    /**
     * Crea una lista simplemente enlazada vacía.
     *
     * La cabeza y el último nodo comienzan en null,
     * y el tamaño inicial es cero.
     */
    public ListaEnlazada() {
        cabeza = null;
        ultimo = null;
        tamano = 0;
    }

    /**
     * Inserta un elemento al final de la lista.
     *
     * La operación se realiza en tiempo O(1) porque se mantiene
     * una referencia directa al último nodo.
     *
     * @param dato Elemento que se desea insertar.
     */
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

    /**
     * Inserta un elemento al inicio de la lista.
     *
     * El nuevo nodo pasa a ser la cabeza y queda enlazado
     * con el nodo que anteriormente ocupaba esa posición.
     *
     * La operación se realiza en tiempo O(1).
     *
     * @param dato Elemento que se desea insertar.
     */
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
     * Busca la primera aparición de un elemento en la lista.
     *
     * Recorre los nodos desde la cabeza hasta encontrar un elemento
     * igual al recibido, utilizando equals().
     *
     * También permite buscar null.
     *
     * La operación tiene complejidad O(n), donde n es el tamaño
     * de la lista.
     *
     * @param dato Elemento que se desea buscar.
     * @return Elemento encontrado o null si no existe.
     *
     * @apiNote Si se almacena null en la lista, el valor de retorno
     * no permite distinguir entre encontrar ese elemento y no encontrarlo.
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

    /**
     * Elimina la primera aparición de un elemento de la lista.
     *
     * Actualiza los enlaces correspondientes y mantiene correctamente
     * las referencias a la cabeza y al último nodo.
     *
     * La operación tiene complejidad O(n) debido a la búsqueda
     * del elemento.
     *
     * @param dato Elemento que se desea eliminar.
     * @return true si el elemento fue eliminado; false si no se encontró.
     */
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
     * Elimina el elemento ubicado en la posición indicada.
     *
     * Las posiciones comienzan en cero. Después de eliminar,
     * actualiza las referencias de la lista y su tamaño.
     *
     * La operación tiene complejidad O(n) en el peor caso.
     *
     * @param posicionLista Posición del elemento que se desea eliminar.
     * @throws IndexOutOfBoundsException Si la posición es negativa
     * o mayor o igual al tamaño de la lista.
     */
    public void eliminarEn(int posicionLista) {
        validarPosicion(posicionLista);

        Nodo<T> nodoAnterior = null;
        Nodo<T> nodoActual = cabeza;

        for (int indiceLista = 0;
             indiceLista < posicionLista;
             indiceLista++) {

            nodoAnterior = nodoActual;
            nodoActual = nodoActual.getSiguiente();
        }

        desvincularNodo(nodoAnterior, nodoActual);
    }

    /**
     * Obtiene el elemento almacenado en la posición indicada.
     *
     * Las posiciones comienzan en cero.
     *
     * La operación tiene complejidad O(n) en el peor caso.
     *
     * @param posicionLista Posición del elemento que se desea obtener.
     * @return Elemento almacenado en la posición indicada.
     * @throws IndexOutOfBoundsException Si la posición es negativa
     * o mayor o igual al tamaño de la lista.
     */
    public T obtener(int posicionLista) {
        validarPosicion(posicionLista);

        Nodo<T> nodoActual = cabeza;

        for (int indiceLista = 0;
             indiceLista < posicionLista;
             indiceLista++) {

            nodoActual = nodoActual.getSiguiente();
        }

        return nodoActual.getDato();
    }

    /**
     * Obtiene la cantidad de elementos almacenados en la lista.
     *
     * La operación se realiza en tiempo O(1).
     *
     * @return Número de elementos de la lista.
     */
    public int obtenerTamano() {
        return tamano;
    }

    /**
     * Comprueba si la lista está vacía.
     *
     * La operación se realiza en tiempo O(1).
     *
     * @return true si la lista no contiene elementos;
     * false en caso contrario.
     */
    public boolean esVacia() {
        return tamano == 0;
    }

    /**
     * Elimina todos los elementos de la lista.
     *
     * Restablece las referencias a la cabeza y al último nodo,
     * y establece el tamaño en cero.
     *
     * La operación se realiza en tiempo O(1).
     */
    public void vaciar() {
        cabeza = null;
        ultimo = null;
        tamano = 0;
    }

    /**
     * Obtiene la referencia al primer nodo de la lista.
     *
     * Permite recorrer los elementos utilizando getSiguiente().
     * La referencia devuelta pertenece a la estructura interna,
     * por lo que no deben modificarse sus enlaces desde fuera
     * de la clase que administra la lista.
     *
     * La operación se realiza en tiempo O(1).
     *
     * @return Primer nodo de la lista o null si está vacía.
     */
    public Nodo<T> getCabeza() {
        return cabeza;
    }

    /**
     * Comprueba que una posición se encuentre dentro de los límites
     * de la lista.
     *
     * @param posicionLista Posición que se desea validar.
     * @throws IndexOutOfBoundsException Si la posición es negativa
     * o mayor o igual al tamaño de la lista.
     */
    private void validarPosicion(int posicionLista) {
        if (posicionLista < 0 || posicionLista >= tamano) {
            throw new IndexOutOfBoundsException(
                    "Posición " + posicionLista + ", tamaño " + tamano
            );
        }
    }

    /**
     * Desvincula un nodo de la lista y actualiza sus referencias.
     *
     * Contempla la eliminación de la cabeza, de un nodo intermedio
     * o del último nodo. También restablece las referencias cuando
     * la lista queda vacía.
     *
     * @param nodoAnterior Nodo anterior al que se eliminará,
     *                     o null si se eliminará la cabeza.
     * @param nodoActual Nodo que se desea desvincular.
     */
    private void desvincularNodo(
            Nodo<T> nodoAnterior, Nodo<T> nodoActual) {

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

    /**
     * Compara dos elementos, contemplando también las referencias null.
     *
     * Dos elementos son iguales si hacen referencia al mismo objeto
     * o si el primero no es null y su método equals() considera
     * iguales ambos elementos.
     *
     * @param datoA Primer elemento.
     * @param datoB Segundo elemento.
     * @return true si ambos elementos son iguales; false en caso contrario.
     */
    private boolean sonIguales(T datoA, T datoB) {
        return datoA == datoB
                || (datoA != null && datoA.equals(datoB));
    }
}
