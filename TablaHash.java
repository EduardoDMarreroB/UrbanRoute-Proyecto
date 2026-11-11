package urbanroute.modelo;

import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Implementa una tabla hash genérica para almacenar parejas clave-valor.
 *
 * Utiliza encadenamiento separado (Separate Chaining) para resolver
 * colisiones, almacenando las entradas de cada posición en una lista enlazada.
 *
 * @param <K> Tipo de dato de la clave.
 * @param <V> Tipo de dato del valor asociado a la clave.
 */
public class TablaHash<K, V> {

    // Capacidad inicial. Se utiliza 31 por ser un número primo que puede
    // favorecer una distribución equilibrada de los índices.
    private static final int CAPACIDAD_INICIAL = 31;

    // Umbral de ocupación que activa el redimensionamiento automático.
    private static final double FACTOR_MAXIMO = 0.75;


    /**
     * Representa una pareja clave-valor dentro de la tabla.
     */
    private static class Entrada<K, V> {

        private final K clave; // Identificador de la entrada.
        private V valor;       // Información asociada, que puede actualizarse.

        /**
         * Crea una entrada con su clave y valor.
         */
        Entrada(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }

        public K getClave() {
            return clave;
        }

        public V getValor() {
            return valor;
        }

        // Actualiza el valor sin modificar la clave.
        public void setValor(V valor) {
            this.valor = valor;
        }

        /**
         * Dos entradas son iguales cuando tienen la misma clave.
         * Esto permite buscarlas en la lista enlazada mediante una entrada temporal.
         */
        @Override
        public boolean equals(Object o) {

            if (this == o) return true;
            if (!(o instanceof Entrada<?, ?>)) return false;

            Entrada<?, ?> otra = (Entrada<?, ?>) o;

            return clave != null && clave.equals(otra.clave);
        }

        /**
         * Genera el código hash utilizando la clave, manteniendo
         * la coherencia con el criterio de igualdad de equals().
         */
        @Override
        public int hashCode() {
            return clave != null ? clave.hashCode() : 0;
        }
    }


    // Arreglo principal. Cada posición contiene una lista enlazada
    // para almacenar las entradas que produzcan el mismo índice.
    // Se utiliza sin parámetros porque nuestra ListaEnlazada no es genérica.
    private ListaEnlazada[] casillas;

    // Cantidad de entradas almacenadas actualmente.
    private int tamano;

    // Referencia al GrafoPonderado que utiliza esta tabla como índice.
    private GrafoPonderado propietario;

    /**
     * Constructor predeterminado. Utiliza 31 casillas iniciales.
     */
    public TablaHash() {
        this(CAPACIDAD_INICIAL);
    }

    /**
     * Constructor con capacidad personalizada.
     *
     * @param capacidad Número inicial de posiciones del arreglo.
     */
    public TablaHash(int capacidad) {

        // Se utiliza la capacidad predeterminada si se recibe un valor inválido.
        if (capacidad < 1) {
            capacidad = CAPACIDAD_INICIAL;
        }

        // Creamos el arreglo de listas enlazadas.
        casillas = new ListaEnlazada[capacidad];

        // La tabla comienza sin elementos almacenados.
        tamano = 0;
    }
  
    // FUNCIÓN HASH
  
    /**
     * Calcula el índice correspondiente a una clave dentro del arreglo.
     *
     * Utiliza hashCode() y Math.floorMod() para obtener una posición
     * válida, incluso cuando el código hash es negativo.
     *
     * @param clave Clave que se desea convertir en un índice.
     * @return Posición correspondiente dentro de casillas.
     */
    private int hash(K clave) {
        return calcularIndice(clave, casillas.length);
    }

    /**
     * Calcula el índice utilizando una capacidad específica.
     * Se utiliza durante el redimensionamiento para calcular las nuevas posiciones.
     */
    private int calcularIndice(K clave, int capacidad) {
        return Math.floorMod(clave.hashCode(), capacidad);
    }

    // BÚSQUEDA INTERNA DE ENTRADAS
  
     /**
     * Busca una entrada dentro de una casilla utilizando su clave.
     *
     * Se utiliza como método auxiliar para evitar repetir el mismo
     * procedimiento en insertar(), buscar() y contiene().
     *
     * @param pos Posición del arreglo donde se realizará la búsqueda.
     * @param clave Clave que se desea localizar.
     * @return Entrada encontrada o null si no existe.
     */
    @SuppressWarnings("unchecked")
    private Entrada<K, V> buscarEntrada(int pos, K clave) {

        // Si no existe una lista en esa posición, no hay entradas que buscar.
        if (casillas[pos] == null) {
            return null;
        }

        // Creamos una entrada temporal y buscamos por igualdad de claves.
        // La conversión es necesaria porque ListaEnlazada devuelve un objeto
        // sin tipo específico.
        return (Entrada<K, V>) casillas[pos].buscar(
                new Entrada<>(clave, null)
        );
    }
  
    // INSERCIÓN Y ACTUALIZACIÓN
  
    /**
     * Inserta una pareja clave-valor en la tabla.
     * Si la clave ya existe, actualiza su valor sin duplicar la entrada.
     *
     * @param clave Identificador del elemento.
     * @param valor Información que se desea almacenar.
     */
    public void insertar(K clave, V valor) {

        // Se rechazan claves null porque no pueden utilizarse como identificadores.
        if (clave == null) return;

        // Calculamos la posición correspondiente a la clave.
        int pos = hash(clave);

        // Si la casilla está vacía, creamos una lista para almacenar las entradas.
        if (casillas[pos] == null) {
            casillas[pos] = new ListaEnlazada();
        }

        // Comprobamos si la clave ya se encuentra almacenada.
        Entrada<K, V> existente = buscarEntrada(pos, clave);

        if (existente != null) {

            // Si existe, actualizamos el valor sin modificar el tamaño.
            existente.setValor(valor);
            return;
        }

        // Si la clave es nueva, insertamos su entrada e incrementamos el contador.
        casillas[pos].insertar(new Entrada<>(clave, valor));
        tamano++;

        // Si se supera el factor de carga, aumentamos la capacidad de la tabla.
        if ((double) tamano / casillas.length > FACTOR_MAXIMO) {
            redimensionar();
        }
    }
  
    // BÚSQUEDA DE ELEMENTOS
    /**
     * Busca una clave y devuelve su valor asociado.
     *
     * @param clave Identificador del elemento que se desea localizar.
     * @return Valor encontrado o null si no existe o si su valor es null.
     */
    public V buscar(K clave) {

        if (clave == null) return null;

        int pos = hash(clave);

        // Localizamos la entrada y recuperamos su valor.
        Entrada<K, V> encontrada = buscarEntrada(pos, clave);

        return encontrada != null ? encontrada.getValor() : null;
    }


    /**
     * Alias de buscar(), utilizado para mantener compatibilidad
     * con las llamadas que empleen el nombre obtener().
     *
     * @param clave Identificador del elemento.
     * @return Valor asociado a la clave.
     */
    public V obtener(K clave) {
        return buscar(clave);
    }

    // ELIMINACIÓN DE ELEMENTOS

    /**
     * Elimina una entrada utilizando su clave.
     *
     * @param clave Identificador del elemento que se desea eliminar.
     * @return true si se eliminó; false si la clave no existía.
     */
    public boolean eliminar(K clave) {

        if (clave == null) return false;

        int pos = hash(clave);

        // No es posible eliminar elementos de una casilla vacía.
        if (casillas[pos] == null) {
            return false;
        }

        // La lista enlazada localiza y elimina la entrada correspondiente.
        boolean eliminada = casillas[pos].eliminar(
                new Entrada<>(clave, null)
        );

        // El tamaño solamente disminuye cuando la eliminación es exitosa.
        if (eliminada) {
            tamano--;
        }

        return eliminada;
    }

    // REDIMENSIONAMIENTO

    /**
     * Aumenta la capacidad de la tabla cuando se supera el factor de carga.
     *
     * La nueva capacidad se calcula como capacidad actual * 2 + 1.
     * Posteriormente, se reorganizan las entradas mediante re-hashing.
     */
    @SuppressWarnings("unchecked")
    private void redimensionar() {

        // Utilizamos long para evitar desbordamientos durante el cálculo.
        long calculoNuevaCapacidad = (long) casillas.length * 2 + 1;

        if (calculoNuevaCapacidad > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "La tabla alcanzó el límite de capacidad."
            );
        }

        int nuevaCapacidad = (int) calculoNuevaCapacidad;

        // Creamos el nuevo arreglo con la capacidad ampliada.
        ListaEnlazada[] nuevas = new ListaEnlazada[nuevaCapacidad];

        // Recorremos todas las casillas del arreglo anterior.
        for (int i = 0; i < casillas.length; i++) {

            if (casillas[i] == null) continue;

            // Recorremos los nodos de cada lista enlazada.
            for (Nodo n = casillas[i].getCabeza();
                 n != null;
                 n = n.getSiguiente()) {

                // Recuperamos la entrada almacenada en el nodo.
                Entrada<K, V> e = (Entrada<K, V>) n.getDato();

                // Recalculamos su posición utilizando la nueva capacidad.
                int nuevaPos = calcularIndice(e.getClave(), nuevaCapacidad);

                // Creamos la lista si la nueva casilla está vacía.
                if (nuevas[nuevaPos] == null) {
                    nuevas[nuevaPos] = new ListaEnlazada();
                }

                // Reubicamos la entrada sin modificar el contador de elementos.
                nuevas[nuevaPos].insertar(e);
            }
        }

        // Sustituimos el arreglo anterior por el nuevo.
        casillas = nuevas;
    }

    // RECUPERACIÓN DE ELEMENTOS

    /**
     * Obtiene todos los valores almacenados en la tabla.
     *
     * @return Lista enlazada con los valores de todas las entradas.
     */
    @SuppressWarnings("unchecked")
    public ListaEnlazada obtenerTodas() {

        ListaEnlazada todas = new ListaEnlazada();

        // Recorremos todas las casillas para recuperar sus entradas.
        for (int i = 0; i < casillas.length; i++) {

            if (casillas[i] == null) continue;

            // Recorremos cada lista y agregamos los valores almacenados.
            for (Nodo n = casillas[i].getCabeza();
                 n != null;
                 n = n.getSiguiente()) {

                Entrada<K, V> entrada = (Entrada<K, V>) n.getDato();

                todas.insertar(entrada.getValor());
            }
        }

        return todas;
    }

    /**
     * Comprueba si una clave está almacenada en la tabla.
     *
     * A diferencia de buscar(), permite verificar la existencia de una clave
     * aunque el valor asociado sea null.
     *
     * @param clave Identificador que se desea comprobar.
     * @return true si la clave existe; false en caso contrario.
     */
    public boolean contiene(K clave) {

        if (clave == null) return false;

        int pos = hash(clave);

        return buscarEntrada(pos, clave) != null;
    }

    // INFORMACIÓN Y LIMPIEZA

    /**
     * Devuelve la cantidad de elementos almacenados.
     *
     * @return Número actual de entradas.
     */
    public int obtenerTamano() {
        return tamano;
    }

    /**
     * Devuelve la cantidad de casillas disponibles en el arreglo.
     *
     * @return Capacidad actual de la tabla hash.
     */
    public int obtenerCapacidad() {
        return casillas.length;
    }

    /**
     * Elimina todos los elementos y restaura la capacidad inicial.
     */
    public void limpiar() {

        // Creamos un arreglo nuevo para descartar las entradas anteriores.
        casillas = new ListaEnlazada[CAPACIDAD_INICIAL];

        tamano = 0;
    }

    // PROPIETARIO

    /**
     * Devuelve el grafo asociado a esta tabla hash.
     *
     * @return Referencia al GrafoPonderado propietario.
     */
    public GrafoPonderado getPropietario() {
        return propietario;
    }

    /**
     * Establece el grafo que utiliza esta tabla como índice.
     *
     * @param propietario Grafo al que pertenece esta tabla.
     */
    public void setPropietario(GrafoPonderado propietario) {
        this.propietario = propietario;
    }
}
