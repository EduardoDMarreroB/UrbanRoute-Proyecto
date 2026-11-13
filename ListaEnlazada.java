package urbanroute.tda;

// Lista simplemente enlazada genérica implementada mediante nodos propios
public class ListaEnlazada<T> {
    private Nodo<T> cabeza; // Referencia al primer nodo de la lista
    private Nodo<T> ultimo; // Referencia al último nodo de la lista para incorporaciones rápidas
    private int tamano; // Cantidad de elementos almacenados en la lista

    
// Crea una lista simplemente enlazada vacía
    public ListaEnlazada() {
        cabeza = null;
        ultimo = null;
        tamano = 0; // Empezamos el contador en cero
    }

    
// Inserta un elemento al final de la lista en tiempo O(1)
    public void insertar(T dato) {
        Nodo<T> nuevoNodo = new Nodo<>(dato);

        if (cabeza == null) {  // Si la lista esta vacia
            cabeza = nuevoNodo;
            ultimo = nuevoNodo;
        } else { // Si ya hay elementos en la lista
            ultimo.setSiguiente(nuevoNodo); // Enlazamos el ultimo nodo actual con el nuevo
            ultimo = nuevoNodo; // Actualizamos el puntero del ultimo nodo
        }
        tamano++; // Incrementamos la cantidad de elementos
    }
    }


  // Inserta un elemento al inicio de la lista
    public void insertarInicio(T dato) { // El nuevo nodo apunta a la antigua cabeza
        Nodo<T> nuevoNodo = new Nodo<>(dato); // la cabeza ahora es el nuevo nodo
        nuevoNodo.setSiguiente(cabeza); 
        cabeza = nuevoNodo;

        if (ultimo == null) { // Si es el primer elemento insertado
            ultimo = nuevoNodo;
        }
        tamano++;
    }


 // Busca la primera aparición de un elemento en la lista
    public T buscar(T dato) {
        // Recorremos los nodos desde la cabeza
        for (Nodo<T> nodoActual = cabeza; 
             nodoActual != null; 
             nodoActual = nodoActual.getSiguiente()) {
            if (sonIguales(nodoActual.getDato(), dato)) { // Encontramos el elemento
                return nodoActual.getDato();
            }
        }
        return null; // Retornamos null si no existe
    }


// Elimina la primera aparición de un elemento de la lista
    public boolean eliminar(T dato) {
        Nodo<T> nodoAnterior = null;
        Nodo<T> nodoActual = cabeza;

        while (nodoActual != null) {
            if (sonIguales(nodoActual.getDato(), dato)) { // Encontramos el nodo a eliminar
                desvincularNodo(nodoAnterior, nodoActual); // Reenlazamos los nodos
                return true;
            }
            // Avanzamos al siguiente nodo en la lista
            nodoAnterior = nodoActual;
            nodoActual = nodoActual.getSiguiente();
        }

        return false; // El elemento no fue encontrado
    }


// Elimina el elemento ubicado en la posición indicada
    public void eliminarEn(int posicionLista) {
        validarPosicion(posicionLista); // Verificamos que el indice exista
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


    // Obtiene el elemento almacenado en la posición indicada
    public T obtener(int posicionLista) {
        validarPosicion(posicionLista);

        Nodo<T> nodoActual = cabeza;

        // avanzamos nodo por nodo hasta el indice deseado
        for (int indiceLista = 0; indiceLista < posicionLista; indiceLista++) {
            nodoActual = nodoActual.getSiguiente();
        }
        return nodoActual.getDato(); // devolvemos el contenido
    }


// Obtiene la cantidad de elementos almacenados en la lista
    public int obtenerTamano() {
        return tamano;
    }

// Comprueba si la lista está vacía
    public boolean esVacia() {
        return tamano == 0;
    }

// Elimina todos los elementos de la lista
    public void vaciar() {
        cabeza = null; // Rompemos el enlace principal
        ultimo = null;
        tamano = 0; // Reiniciamos el contador
    }

// Obtiene la referencia al primer nodo de la lista
    public Nodo<T> getCabeza() {
        return cabeza;
    }

// Comprueba que una posición se encuentre dentro de los límites
    private void validarPosicion(int posicionLista) {
        if (posicionLista < 0 || posicionLista >= tamano) { // Validacion de limites
            throw new IndexOutOfBoundsException(
                    "Posición " + posicionLista + ", tamaño " + tamano
            );
        }
    }

// Desvincula un nodo de la lista y actualiza sus referencias
    private void desvincularNodo(Nodo<T> nodoAnterior, Nodo<T> nodoActual) {
        if (nodoAnterior == null) { // Si estamos eliminando la cabeza
            cabeza = nodoActual.getSiguiente(); 
        } else { // Si es un nodo intermedio o final
            // Conectamos el nodo anterior con el siguiente del eliminado
            nodoAnterior.setSiguiente(nodoActual.getSiguiente()); 
        }
        if (nodoActual == ultimo) { // Si eliminamos el ultimo nodo de la lista
            ultimo = nodoAnterior; // El anterior pasa a ser el ultimo
        }
        tamano--; // Reducimos la cantidad de elementos

        if (tamano == 0) { // Si la lista quedo completamente vacia
            cabeza = null;
            ultimo = null;
        }
    }


// Compara dos elementos, contemplando también las referencias null
    private boolean sonIguales(T datoA, T datoB) {
        // es el mismo objeto o ambos equals coinciden
        return datoA == datoB || (datoA != null && datoA.equals(datoB));
    }
}
