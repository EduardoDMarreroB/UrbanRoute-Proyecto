package urbanroute.tda;

/**
* Lista simplemente enlazada genérica implementada mediante nodos propios.
*/
public class ListaEnlazada<T> {
    /**
     * Referencia o apuntador al primer nodo de la lista.
     */
    private Nodo<T> cabeza;
    
    /**
     * Referencia o apuntador al último nodo de la lista para incorporaciones 
     * rápidas.
     */
    private Nodo<T> ultimo;  
    private int tamano;

    /**
     * Crea una lista simplemente enlazada vacía.
     */
    public ListaEnlazada() {
        cabeza = null;
        ultimo = null;
        tamano = 0; // Inicializamos el contador en cero
    }

    /**
     * Inserta un elemento al final de la lista en tiempo O(1).
     * @param dato El elemento que será guardado al final.
     */
    public void insertar(T dato) {
        Nodo<T> nuevoNodo = new Nodo<>(dato);
        if (cabeza == null) {  // Verifica si la lista está vacía
            cabeza = nuevoNodo;
            ultimo = nuevoNodo;
        } else { // Si ya hay elementos en la lista
            ultimo.setSiguiente(nuevoNodo); // Enlazamos el último nodo 
                                            // actual con el nuevo
            ultimo = nuevoNodo; // Actualizamos el puntero del ultimo nodo
        }
        tamano++; // Incrementamos la cantidad de elementos
    }
    
    /**
     * Inserta un nuevo elemento al inicio de la lista.
     * @param dato El elemento que será guardado al inicio.
     */
    public void insertarInicio(T dato) { // El nuevo nodo apunta a la 
                                         // antigua cabeza
        Nodo<T> nuevoNodo = new Nodo<>(dato); // la cabeza ahora es 
                                              // el nuevo nodo
        nuevoNodo.setSiguiente(cabeza); 
        cabeza = nuevoNodo;
        if (ultimo == null) { // Si es el primer elemento insertado
            ultimo = nuevoNodo;
        }
        tamano++;
    }

    /**
     * Busca la primera aparición de un elemento en la lista.
     * @param dato El elemento a encontrar de la lista.
     * @return El dato si lo encuentra, sino retorna null.
     */
    public T buscar(T dato) {
        // Recorremos los nodos desde la cabeza
        for (Nodo<T> nodoActual = cabeza; 
             nodoActual != null; 
             nodoActual = nodoActual.getSiguiente()) {
            if (sonIguales(nodoActual.getDato(), dato)) { // Elemento obtenido
                return nodoActual.getDato();
            }
        }
        return null; // Retornamos null si no existe
    }

    /**
     * Elimina la primera aparición de un elemento de la lista.
     * @param dato El elemento a elminar de la lista.
     * @return true si el borrado fue exitoso, o false si el dato no pudo ser
     * encontrado.
     */
    public boolean eliminar(T dato) {
        Nodo<T> nodoAnterior = null;
        Nodo<T> nodoActual = cabeza;
        while (nodoActual != null) {
            if (sonIguales(nodoActual.getDato(), dato)) { // Nodo Encontrado
                desvincularNodo(nodoAnterior, nodoActual); // Reenlazamos nodos
                return true;
            }
            // Avanzamos al siguiente nodo en la lista
            nodoAnterior = nodoActual;
            nodoActual = nodoActual.getSiguiente();
        }
        return false; // El elemento no fue encontrado
    }

    /**
     * Elimina el elemento ubicado en la posición indicada.
     * @param posicionLista Índice numérico (empezando en 0) del nodo a borrar.
     * @throws IndexOutOfBoundsException Si le pasamos una posición negativa o 
     * mayor al tamaño de la lista.
     */
    public void eliminarEn(int posicionLista) {
        validarPosicion(posicionLista); // Verificamos que el índice exista
        Nodo<T> nodoAnterior = null;
        Nodo<T> nodoActual = cabeza;
        // Iteramos hasta llegar a la posicion exacta
        for (int indiceLista = 0;
             indiceLista < posicionLista;
             indiceLista++) {
            nodoAnterior = nodoActual;
            nodoActual = nodoActual.getSiguiente();
        }
        desvincularNodo(nodoAnterior, nodoActual); // Procedemos a desconectarlo
    }

    /**
     * Obtiene el elemento almacenado en la posición indicada.
     * @param posicionLista La posición numérica a consultar en la lista.
     * @return El elemento guardado en esa posición.
     * @throws IndexOutOfBoundsException Si la posición solicitada no existe.
     */
    public T obtener(int posicionLista) {
        validarPosicion(posicionLista);
        Nodo<T> nodoActual = cabeza;
        // Avanzamos nodo por nodo hasta el indice deseado
        for (int indiceLista = 0; indiceLista < posicionLista; indiceLista++) {
            nodoActual = nodoActual.getSiguiente();
        }
        return nodoActual.getDato(); // Devolvemos el contenido
    }

    /**
     * Obtiene la cantidad de elementos almacenados en la lista.
     * @return La cantidad total de nodos en la lista.
     */
    public int obtenerTamano() {
        return tamano;
    }

    /**
     * Comprueba si la lista está vacía.
     * @return true si no hay ningún elemento, o false si tiene al menos uno.
     */
    public boolean esVacia() {
        return tamano == 0;
    }

    /**
     * Elimina todos los elementos de la lista.
     */
    public void vaciar() {
        cabeza = null; // Rompemos el enlace principal
        ultimo = null;
        tamano = 0; // Reiniciamos el contador
    }

    /**
     * Obtiene la referencia al primer nodo de la lista.
     * @return El nodo que actúa como cabeza de la lista.
     */
    public Nodo<T> getCabeza() {
        return cabeza;
    }

    /**
     * Comprueba que una posición se encuentre dentro de los límites (números 
     * enteros positivos y dentro del rango de elementos de la lista).
     * @param posicionLista El índice que queremos poner a prueba.
     * @throws IndexOutOfBoundsException Si el índice está fuera de los límites.
     */
    private void validarPosicion(int posicionLista) {
        if (posicionLista < 0 || posicionLista >= tamano) { // Validamos límites
            throw new IndexOutOfBoundsException(
                    "Posición " + posicionLista + ", tamaño " + tamano
            );
        }
    }

    /**
     * Desvincula un nodo de la lista y actualiza sus referencias.
     * @param nodoAnterior El nodo que está justo detrás del que vamos a borrar.
     * @param nodoActual El nodo a ser eliminado.
     */
    private void desvincularNodo(Nodo<T> nodoAnterior, Nodo<T> nodoActual) {
        if (nodoAnterior == null) { // Si estamos eliminando la cabeza
            cabeza = nodoActual.getSiguiente(); 
        } else { // Si es un nodo intermedio o final
                 // Conectamos el nodo anterior con el siguiente del eliminado
            nodoAnterior.setSiguiente(nodoActual.getSiguiente()); 
        }
        if (nodoActual == ultimo) { // Si eliminamos el ultimo nodo de la lista
            ultimo = nodoAnterior;  // El anterior pasa a ser el ultimo
        }
        tamano--; // Reducimos la cantidad de elementos
        if (tamano == 0) { // Si la lista quedo completamente vacía
            cabeza = null;
            ultimo = null;
        }
    }

    /**
     * Compara dos elementos de forma segura para evitar errores de tipo 
     * NullPointerException.
     * Verifica primero si ambas referencias apuntan al mismo espacio de memoria 
     * (o si ambas son nulas) usando "==". Si no es así, se asegura de que el 
     * primer bobjeto no sea nulo antes de invocar el método equals() para 
     * comparar sus valores.
     * @param datoA El primer elemento a comparar.
     * @param datoB El segundo elemento a comparar.
     * @return true si los elementos coinciden en valor o si ambos son null; 
     * o false en caso contrario.
     */
    private boolean sonIguales(T datoA, T datoB) {
        return datoA == datoB || (datoA != null && datoA.equals(datoB));
    }
}
