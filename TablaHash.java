package urbanroute.modelo;

import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Implementa una tabla hash propia para almacenar e indexar estaciones
 * mediante su identificador único.
 *
 * Utiliza un arreglo de listas enlazadas para resolver las colisiones
 * mediante encadenamiento separado.
 *
 * Cada entrada contiene una clave y la estación correspondiente.
 * La posición se calcula mediante una función de dispersión propia.
 *
 * La capacidad aumenta cuando el factor de carga supera el 75 %,
 * para mantener controlado el número de colisiones.
 */
public class TablaHash {

    /** Capacidad inicial del arreglo. */
    private static final int CAPACIDAD_INICIAL = 101;

    /** Factor de carga máximo antes de ampliar la tabla. */
    private static final double FACTOR_CARGA_MAXIMO = 0.75;

    /**
     * Representa una entrada almacenada en la tabla.
     *
     * La clave es inmutable para evitar modificaciones que puedan
     * alterar la ubicación de la entrada dentro del arreglo.
     */
    private static class Entrada {

        /** Identificador de la estación. */
        private final String clave;

        /** Estación asociada al identificador. */
        private Estacion valor;

        /**
         * Crea una entrada con su clave y estación correspondiente.
         *
         * @param clave Identificador de la estación.
         * @param valor Estación que se almacenará.
         */
        private Entrada(String clave, Estacion valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

    /** Arreglo de listas enlazadas que almacena las entradas. */
    private ListaEnlazada<Entrada>[] casillas;

    /** Cantidad de estaciones almacenadas. */
    private int tamano;

    /**
     * Crea una tabla hash vacía con 101 casillas.
     */
    public TablaHash() {
        casillas = crearCasillas(CAPACIDAD_INICIAL);
        tamano = 0;
    }

    /**
     * Crea el arreglo de listas enlazadas.
     *
     * Se utiliza un arreglo nativo de Java porque no se pueden crear
     * directamente arreglos de tipos genéricos parametrizados.
     *
     * @param capacidad Cantidad de casillas del arreglo.
     * @return Arreglo preparado para almacenar listas de entradas.
     */
    @SuppressWarnings("unchecked")
    private ListaEnlazada<Entrada>[] crearCasillas(int capacidad) {
        return (ListaEnlazada<Entrada>[])
                new ListaEnlazada<?>[capacidad];
    }

    /**
     * Calcula la posición de una clave dentro del arreglo.
     *
     * Utiliza una función polinómica que procesa los caracteres
     * del identificador y aplica el módulo de la capacidad actual.
     *
     * El valor intermedio se mantiene reducido para evitar
     * desbordamientos durante el cálculo.
     *
     * @param clave Identificador de la estación.
     * @return Posición correspondiente dentro del arreglo.
     */
    private int hash(String clave) {

        long resultado = 0;

        for (int i = 0; i < clave.length(); i++) {
            resultado = (31L * resultado + clave.charAt(i))
                    % casillas.length;
        }

        return (int) resultado;
    }

    /**
     * Busca una entrada dentro de una casilla.
     *
     * Recorre la lista enlazada correspondiente y compara directamente
     * los identificadores almacenados.
     *
     * No utiliza equals() ni hashCode() en la clase Entrada.
     *
     * @param posicion Posición del arreglo donde se buscará.
     * @param clave Identificador que se desea localizar.
     * @return Entrada encontrada o null si no existe.
     */
    private Entrada buscarEntrada(int posicion, String clave) {

        if (casillas[posicion] == null) {
            return null;
        }

        for (Nodo<Entrada> nodoActual = casillas[posicion].getCabeza();
             nodoActual != null;
             nodoActual = nodoActual.getSiguiente()) {

            Entrada entrada = nodoActual.getDato();

            if (entrada.clave.equals(clave)) {
                return entrada;
            }
        }

        return null;
    }

    /**
     * Inserta una estación utilizando su identificador como clave.
     *
     * Si la clave ya existe, reemplaza la estación almacenada.
     * Si no existe, crea una nueva entrada y la agrega a la lista
     * enlazada correspondiente.
     *
     * Antes de insertar una nueva entrada, comprueba si el factor
     * de carga supera el límite establecido.
     *
     * @param clave Identificador de la estación.
     * @param valor Estación que se desea almacenar.
     * @throws IllegalArgumentException Si la clave es nula o está vacía,
     * si la estación es nula o si la clave no coincide con su ID.
     */
    public void insertar(String clave, Estacion valor) {

        if (clave == null || clave.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El identificador no puede ser nulo ni estar vacío."
            );
        }

        if (valor == null) {
            throw new IllegalArgumentException(
                    "La estación no puede ser nula."
            );
        }

        String claveNormalizada = clave.trim();

        if (!claveNormalizada.equals(valor.getId())) {
            throw new IllegalArgumentException(
                    "La clave debe coincidir con el ID de la estación."
            );
        }

        int posicion = hash(claveNormalizada);

        // Si la clave ya existe, se actualiza su valor.
        Entrada existente = buscarEntrada(posicion, claveNormalizada);

        if (existente != null) {
            existente.valor = valor;
            return;
        }

        // Se amplía la tabla si la nueva inserción supera el factor de carga.
        if ((tamano + 1.0) / casillas.length > FACTOR_CARGA_MAXIMO) {
            redimensionar();
            posicion = hash(claveNormalizada);
        }

        if (casillas[posicion] == null) {
            casillas[posicion] = new ListaEnlazada<>();
        }

        casillas[posicion].insertar(
                new Entrada(claveNormalizada, valor)
        );

        tamano++;
    }

    /**
     * Busca una estación mediante su identificador.
     *
     * Calcula la posición correspondiente y revisa únicamente
     * la lista enlazada de esa casilla.
     *
     * @param clave Identificador de la estación.
     * @return Estación encontrada o null si no existe.
     */
    public Estacion buscar(String clave) {

        if (clave == null || clave.trim().isEmpty()) {
            return null;
        }

        String claveNormalizada = clave.trim();

        int posicion = hash(claveNormalizada);

        Entrada entrada = buscarEntrada(posicion, claveNormalizada);

        return entrada == null ? null : entrada.valor;
    }

    /**
     * Obtiene una estación mediante su identificador.
     *
     * Conserva este método como alternativa de consulta para
     * mantener la compatibilidad con otras clases del proyecto.
     *
     * @param clave Identificador de la estación.
     * @return Estación encontrada o null si no existe.
     */
    public Estacion obtener(String clave) {
        return buscar(clave);
    }

    /**
     * Elimina una estación mediante su identificador.
     *
     * Primero localiza la entrada y después elimina de la lista
     * la misma referencia encontrada.
     *
     * Si la lista queda vacía, se libera la casilla correspondiente.
     *
     * @param clave Identificador de la estación que se desea eliminar.
     * @return true si se eliminó la estación; false si no existía.
     */
    public boolean eliminar(String clave) {

        if (clave == null || clave.trim().isEmpty()) {
            return false;
        }

        String claveNormalizada = clave.trim();

        int posicion = hash(claveNormalizada);

        if (casillas[posicion] == null) {
            return false;
        }

        Entrada existente = buscarEntrada(posicion, claveNormalizada);

        if (existente == null) {
            return false;
        }

        /*
         * Se elimina la misma entrada encontrada.
         * La ListaEnlazada reconoce que ambas referencias
         * corresponden al mismo objeto.
         */
        boolean eliminada = casillas[posicion].eliminar(existente);

        if (eliminada) {
            tamano--;

            if (casillas[posicion].esVacia()) {
                casillas[posicion] = null;
            }
        }

        return eliminada;
    }

    /**
     * Obtiene todas las estaciones almacenadas.
     *
     * Recorre las casillas del arreglo y sus respectivas listas
     * enlazadas, agregando las estaciones a una nueva lista.
     *
     * @return Lista enlazada con todas las estaciones registradas.
     */
    public ListaEnlazada<Estacion> obtenerTodas() {

        ListaEnlazada<Estacion> todas = new ListaEnlazada<>();

        for (int i = 0; i < casillas.length; i++) {

            if (casillas[i] == null) {
                continue;
            }

            for (Nodo<Entrada> nodoActual = casillas[i].getCabeza();
                 nodoActual != null;
                 nodoActual = nodoActual.getSiguiente()) {

                todas.insertar(nodoActual.getDato().valor);
            }
        }

        return todas;
    }

    /**
     * Comprueba si existe una estación con el identificador indicado.
     *
     * @param clave Identificador que se desea consultar.
     * @return true si existe una entrada con esa clave;
     * false en caso contrario.
     */
    public boolean contiene(String clave) {

        if (clave == null || clave.trim().isEmpty()) {
            return false;
        }

        String claveNormalizada = clave.trim();

        int posicion = hash(claveNormalizada);

        return buscarEntrada(posicion, claveNormalizada) != null;
    }

    /**
     * Obtiene la cantidad de estaciones almacenadas.
     *
     * @return Número total de estaciones.
     */
    public int obtenerTamano() {
        return tamano;
    }

    /**
     * Obtiene la capacidad actual del arreglo.
     *
     * @return Cantidad de casillas disponibles.
     */
    public int obtenerCapacidad() {
        return casillas.length;
    }

    /**
     * Calcula el factor de carga de la tabla.
     *
     * Representa la relación entre la cantidad de entradas
     * almacenadas y la capacidad disponible.
     *
     * @return Factor de carga actual.
     */
    public double obtenerFactorCarga() {
        return (double) tamano / casillas.length;
    }

    /**
     * Aumenta la capacidad del arreglo y redistribuye sus entradas.
     *
     * La nueva capacidad se calcula buscando un número primo
     * aproximadamente dos veces mayor que el anterior.
     *
     * Las estaciones y sus referencias se conservan.
     */
    private void redimensionar() {

        ListaEnlazada<Entrada>[] anteriores = casillas;

        int nuevaCapacidad = siguientePrimo(anteriores.length * 2);

        casillas = crearCasillas(nuevaCapacidad);

        for (int i = 0; i < anteriores.length; i++) {

            if (anteriores[i] == null) {
                continue;
            }

            for (Nodo<Entrada> nodoActual = anteriores[i].getCabeza();
                 nodoActual != null;
                 nodoActual = nodoActual.getSiguiente()) {

                Entrada entrada = nodoActual.getDato();

                int nuevaPosicion = hash(entrada.clave);

                if (casillas[nuevaPosicion] == null) {
                    casillas[nuevaPosicion] = new ListaEnlazada<>();
                }

                casillas[nuevaPosicion].insertar(entrada);
            }
        }
    }

    /**
     * Busca el siguiente número primo igual o mayor al recibido.
     *
     * @param numero Número a partir del cual se inicia la búsqueda.
     * @return Siguiente número primo.
     */
    private int siguientePrimo(int numero) {

        int candidato = numero;

        if (candidato <= 2) {
            return 2;
        }

        if (candidato % 2 == 0) {
            candidato++;
        }

        while (!esPrimo(candidato)) {
            candidato += 2;
        }

        return candidato;
    }

    /**
     * Comprueba si un número es primo.
     *
     * @param numero Número que se desea comprobar.
     * @return true si es primo; false en caso contrario.
     */
    private boolean esPrimo(int numero) {

        if (numero < 2) {
            return false;
        }

        for (int divisor = 2;
             divisor <= numero / divisor;
             divisor++) {

            if (numero % divisor == 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Elimina todas las entradas de la tabla.
     *
     * Restablece el arreglo a su capacidad inicial
     * y establece el tamaño en cero.
     */
    public void limpiar() {
        casillas = crearCasillas(CAPACIDAD_INICIAL);
        tamano = 0;
    }
}
