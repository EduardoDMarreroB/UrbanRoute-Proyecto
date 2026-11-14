package urbanroute.modelo;

import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Implementación propia de una Tabla Hash diseñada para almacenar y buscar 
 * de manera eficiente las estaciones utilizando su ID como clave.
 * Esta estructura de datos optimiza las operaciones de búsqueda e inserción, 
 * basándose en una función de dispersión (hash) y un manejo adecuado de colisiones.
 * Referencia teórica: https://n9.cl/hash_table
 */
public class TablaHash {
    /**
     * Capacidad inicial de la tabla hash..
     * Se establece en 101, el cual es un número primo mayor a la cantidad 
     * prevista de estaciones iniciales (44). En la teoría de tablas hash, 
     * utilizar un número primo para el tamaño del arreglo resulta fundamental 
     * al aplicar la función módulo (hash % capacidad). Esto garantiza una 
     * distribución más uniforme de las claves a lo largo de la tabla, 
     * reduciendo significativamente la probabilidad y frecuencia de colisiones 
     * en comparación con el uso de números compuestos o pares, lo que a su vez 
     * retrasa el costoso proceso de redimensionar o agrandar (rehashing).
     * private' porque es una configuración 
     * estrictamente interna. 
     * "Private" para proteger la abstracción matemática de la tabla y evitar 
     * que otras partes del programa dependan de este valor o lo alteren.
     */
    private static final int capacidad_Inicial = 101; 
    
    /**
     * Factor de carga máximo permitido antes de redimensionar/agrandar
     * la tabla.
     * Un valor de 0.75 es el estándar óptimo que ofrece un buen equilibrio 
     * entre el costo de tiempo (búsquedas rápidas con pocas colisiones) 
     * y el costo de espacio (memoria utilizada). Cuando la cantidad de 
     * elementos supere el 75% de la capacidad, la tabla deberá agrandarse.
     * "Private" para garantizar que ninguna clase externa modifique este límite
     * y rompa la lógica del rehashing.
     */
    private static final double factor_Carga_Maximo = 0.75;

    /**
     * Guarda el par de datos: el ID (clave) y la estación.
     * "Private" porque al exponerla rompería la abstracción, obligando al 
     * usuario externo a conocer cómo se manejan internamente las colisiones.
    */
    private static class Entrada {
        private final String clave; // ID de la estacion
        private Estacion valor;     // La estacion guardada


    /**
     * Crea el registro con la clave y la estación correspondiente.
    */
        private Entrada(String clave, Estacion valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

    /**
     * Arreglo principal de listas para manejar colisiones.
     * Si dos IDs diferentes obtienen el mismo número de posición tras la 
     * operación matemática, no se pierden los datos, sino que mabas estaciones 
     * se guardan en esta lista.
     * "Private" para evitar que código externo acceda a la memoria 
     * directamente, borre nodos o modifique el arreglo saltándose los métodos 
     * seguros ya establecidos (insertar/eliminar).
    */
    private ListaEnlazada<Entrada>[] casillas;
    private int tamano; // Cantidad de estaciones guardadas en total (tamaño)

    /** 
     * Crea una tabla vacía empezando con 101 espacios.
     */
    public TablaHash() {
        casillas = crearCasillas(capacidad_Inicial);
        tamano = 0; // Inicializamos en cero
    }

    /**
     * Crea el arreglo principal de casillas con el tamaño que le pidamos. 
     * Como Java tiene ciertas reglas estrictas y no deja crear arreglos de 
     * tipos genéricos directamente, usamos un pequeño truco de casteo y 
     * silenciamos la advertencia del compilador usando @SuppressWarnings.
     * @param capacidad Cantidad de espacios (casillas) que tendrá el arreglo.
     * @return Un arreglo de listas enlazadas para guardar las entradas.
     */
    @SuppressWarnings("unchecked")
    private ListaEnlazada<Entrada>[] crearCasillas(int capacidad) {
        return (ListaEnlazada<Entrada>[]) new ListaEnlazada<?>[capacidad];
    }

/**
     * Función hash: decide en qué posición del arreglo va cada estación. 
     * Convierte el ID (texto) en un número, multiplicando por 
     * 31 en cada letra. Se utiliza el 31 porque es el estándar matemático en 
     * Java para mezclar bien los datos y evitar que IDs parecidos caigan en la
     * misma casilla. Al final, aplicamos el módulo (%) para asegurarnos de que
     * el número resultante no se salga del tamaño de nuestro arreglo.
     * Referencia teórica sobre el multiplicador 31: https://n9.cl/zx7rs
     * @param clave El ID de la estación.
     * @return El número de la casilla (índice) donde corresponde guardar o 
     * buscar.
     */    
    private int hash(String clave) {
        long resultado = 0;
        // Sumamos los valores de cada letra para sacar un número único
        for (int i = 0; i < clave.length(); i++) {
            resultado = (31L * resultado + clave.charAt(i)) 
                    % casillas.length;
        }
        return (int) resultado;
    }

/**
     * Revisa una casilla específica del arreglo para ver si el ID que buscamos 
     * ya está guardado ahí adentro. Como en una casilla puede haber varias 
     * estaciones juntas (por las colisiones), recorremos la lista una por una.
     * @param posicion El número de la casilla donde vamos a buscar.
     * @param clave El ID de la estación que queremos encontrar.
     * @return El registro completo (Entrada) si lo encuentra, o null 
     * si no está.
     */
    private Entrada buscarEntrada(int posicion, String clave) {
        if (casillas[posicion] == null) { // Si no hay ninguna lista ahi
            return null;
        }
        // Recorremos la lista de ese lugar para ver si está el ID
        for (Nodo<Entrada> actual = casillas[posicion].getCabeza();
             actual != null;
             actual = actual.getSiguiente()) {
            Entrada entrada = actual.getDato();
            if (entrada.clave.equals(clave)) { // Si lo encontramos
                return entrada;
            }
        }
        return null; // Si no estaba guardado ahí
    }

    /** 
     * Guarda una nueva estación en la tabla o actualiza una que ya existía.
     * Antes de guardar, se asegura de que el ID y la estación sean válidos 
     * y que ambos coincidan. Si resulta que el ID ya estaba guardado en la 
     * tabla, simplemente actualiza los datos para no tener duplicados.
     * Adicionalmente, justo antes de guardar calcula qué tan llena va a quedar
     * la tabla. Si al meter este nuevo dato supera el límite del 75% 
     * (factor de carga), se agranda la tabla automáticamente. Esto evita que 
     * las casillas se llenen de colisiones y asegura que las búsquedas sigan 
     * siendo rápidas.
     * @param clave El ID de la estación guardado.
     * @param valor El objeto Estacion con toda su información.
     * @throws IllegalArgumentException Si el ID está vacío, o si no coindice
     * con el de la estación, es nula (null).
     */
    public void insertar(String clave, Estacion valor) {
        if (clave == null || clave.trim().isEmpty()) { // Validamos el ID
            throw new IllegalArgumentException(
                    "El identificador no puede estar vacio."
            );
        }
        if (valor == null) { // Validamos la estación
            throw new IllegalArgumentException(
                    "La estación no puede ser nula."
            );
        }
        String claveLimpia = clave.trim();
        if (!claveLimpia.equals(valor.getId())) { // Tienen que coincidir
            throw new IllegalArgumentException(
                    "La clave debe ser igual al ID de la estación."
            );
        }
        int posicion = hash(claveLimpia);
        // Buscamos si ya estaba guardada para solo actualizarle los datos
        Entrada existente = buscarEntrada(posicion, claveLimpia);
        if (existente != null) {
            existente.valor = valor; // Actualizamos los datos
            return;
        }
        // Si se va a llenar mucho (más del 75%), la agrandamos primero
        if ((tamano + 1.0) / casillas.length > factor_Carga_Maximo) {
            agrandar();
            posicion = hash(claveLimpia); // Calculamos la posición otra vez
        }
        if (casillas[posicion] == null) { // Si no hay lista, la creamos
            casillas[posicion] = new ListaEnlazada<>();
        }
        // Guardamos el nuevo dato en la lista de esa posición
        casillas[posicion].insertar(new Entrada(claveLimpia, valor));
        tamano++; // Sumamos uno al contador total
    }

    /** 
     * Busca una estación utilizando su clave (ID).
     * Utiliza la función hash. Calcula la posición exacta de la estación, va a
     * la casilla correspondiente donde se encuentra y revisa si el ID se 
     * encuentra ahí.
     * @param clave El ID de la estación a buscar.
     * @return La estación con todos sus datos.
     */
    public Estacion buscar(String clave) {
        if (clave == null || clave.trim().isEmpty()) {
            return null;
        }
        String claveLimpia = clave.trim();
        int posicion = hash(claveLimpia);
        Entrada entrada = buscarEntrada(posicion, claveLimpia);
        // Si la encontró devuelve la estacion, si no devuelve null
        return entrada == null ? null : entrada.valor; 
    }

    /** 
     * Obtiene una estacion mediante su identificador (ID).
     * @param clave El ID de la estación a obtener.
     * @return La estación solicitada (null si no se encuentra guardada).
     */    
    public Estacion obtener(String clave) {
        return buscar(clave);
    }

    /** 
     * Borra una estacion usando su ID.
     * @param clave El ID de la estación a eliminar.
     * @return true si la estación se encontró y se borró con éxito, 
     * o false si el ID no era válido o la estación no estaba guardada.
     */
    public boolean eliminar(String clave) {
        if (clave == null || clave.trim().isEmpty()) {
            return false;
        }
        String claveLimpia = clave.trim();
        int posicion = hash(claveLimpia);
        if (casillas[posicion] == null) { // Si no hay nada en esa posición
            return false;
        }
        // Buscamos si el dato realmente existe ahí dentro
        Entrada existente = buscarEntrada(posicion, claveLimpia);
        if (existente == null) {
            return false; // No estaba guardada
        }
        // Le pedimos a la lista que borre ese dato exácto.
        boolean eliminada = casillas[posicion].eliminar(existente);
        if (eliminada) {
            tamano--; // Restamos uno del contador total
            if (casillas[posicion].esVacia()) { // Si la lista quedo vacía
                casillas[posicion] = null; // La borramos para hacer espacio
            }
        }
        return eliminada;
    }

    /**
     * Recopila todas las estaciones guardadas en la tabla hash y las devuelve 
     * juntas agrupadas en una sola lista enlazada.
     * @return lista enlazada
     */
    public ListaEnlazada<Estacion> obtenerTodas() {
        ListaEnlazada<Estacion> todas = new ListaEnlazada<>();
        for (int i = 0; i < casillas.length; i++) {
            if (casillas[i] == null) { // Saltamos los espacios en blanco
                continue;
            }
            // Recorremos cada elemento de la lista en esta casilla
            for (Nodo<Entrada> actual = casillas[i].getCabeza();
                 actual != null;
                 actual = actual.getSiguiente()) {
                todas.insertar(actual.getDato().valor); // La agregamos
            }
        }
        return todas;
    }

    /** 
     * Verifica si un ID ya se encuentra registrado en la tabla hash.
     * @param clave El ID de la estación a consultar.
     * @return true si la estación ya está guardada, o false si el ID no existe 
     * o el texto suminstrado está vacío.
     */
    public boolean contiene_ID(String clave) {
        if (clave == null || clave.trim().isEmpty()) {
            return false;
        }
        String claveLimpia = clave.trim();
        int posicion = hash(claveLimpia);
        // Retorna true si la encontró: false si dio null
        return buscarEntrada(posicion, claveLimpia) != null;
    }

    /** 
     * Retorna el número de estaciones (tamaño) que hay guardadas en total.
     */
    public int obtenerTamano() {
        return tamano;
    }

    /** 
     * Indica el tamaño actual del arreglo principal; es decir, cantidad de 
     * casillas/espacios disponibles en la tabla en ese momento.
     */
    public int obtenerCapacidad() {
        return casillas.length;
    }

    /** 
     * Calcula la relación matemática entre las estaciones guardadas y el tamaño
     * del arreglo (qué porcentaje de la tabla está ocupada).
     */
    public double obtenerFactorCarga() {
        return (double) tamano / casillas.length;
    }

    /**
     * Realiza el proceso de "Rehashing": agranda la tabla y reubica los datos.
     * Cuando la tabla alcanza su límite de capacidad, crea un nuevo arreglo con
     * el doble de tamaño (buscando siempre el siguiente número primo para 
     * evitar colisiones. Luego, pasa todas las estaciones de la tabla vieja 
     * a la nueva. Es obligatorio recalcular la posición de cada una, ya que 
     * al cambiar el tamaño total del arreglo, la operación módulo(%) de la
     * función hash genera nuevas posiciones.
     */
    private void agrandar() {
        ListaEnlazada<Entrada>[] anteriores = casillas;
        // Buscamos un tamaño nuevo que sea numero primo
        int nuevaCapacidad = siguientePrimo(anteriores.length * 2);
        casillas = crearCasillas(nuevaCapacidad); // Creamos la nueva tabla
        // Recorremos la tabla vieja para pasar todo a la nueva
        for (int i = 0; i < anteriores.length; i++) {
            if (anteriores[i] == null) {
                continue;
            }
            for (Nodo<Entrada> actual = anteriores[i].getCabeza();
                 actual != null;
                 actual = actual.getSiguiente()) {
                Entrada entrada = actual.getDato();
                int nuevaPos = hash(entrada.clave); // Calculamos nueva posición
                if (casillas[nuevaPos] == null) {
                    casillas[nuevaPos] = new ListaEnlazada<>();
                }
                casillas[nuevaPos].insertar(entrada); // Pasamos el dato
            }
        }
    }

     /**
     * Busca el siguiente número primo a partir del número que le pasemos. 
     * Cuando se duplica el tamaño de la tabla, este método se asegura de que
     * la nueva capacidad siga siendo un número primo. Si se le pasa un número
     * par, le suma uno y de ahí en adelante va saltando de dos en dos buscando
     * solo entre impares.
     * @param numero El número base (idealmente el doble de la capacidad actual).
     * @return El primer número primo que sea igual o mayor al número dado.
     */
    private int siguientePrimo(int numero) {
        int candidato = numero;
        if (candidato <= 2) {
            return 2;
        }
        if (candidato % 2 == 0) { //Si es par, le sumamos uno para hacerlo impar
            candidato++;
        }
        while (!esPrimo(candidato)) { //Mientras no sea primo, sigue buscando
            candidato += 2;
        }
        return candidato;
    }

    /** Verifica si un número entero dado es primo.
     * @param numero El número entero a verificar.
     * @return true si es primo, o false si se puede dividir por algún otro 
     * número.
     */    
    private boolean esPrimo(int numero) {
        if (numero < 2) {
            return false;
        }
        // Probamos hasta la mitad del numero para que la busqueda sea rapida
        for (int divisor = 2; divisor <= numero / divisor; divisor++) {
            if (numero % divisor == 0) { // Si se puede dividir por otro numero
                return false;
            }
        }
        return true;
    }

    /** 
     * Borra absolutamente todo y reinicia la tabla desde cero.
     */
    public void limpiar() {
        casillas = crearCasillas(capacidad_Inicial);
        tamano = 0;
    }
}
