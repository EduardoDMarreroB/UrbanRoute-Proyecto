package urbanroute.modelo;

import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

// Tabla propia para guardar y buscar estaciones usando su ID
public class TablaHash {
    
    private static final int CAPACIDAD_INICIAL = 101; // Tamaño base 
    
    // Que tan llena puede estar la tabla antes de agrandarla 
    private static final double FACTOR_CARGA_MAXIMO = 0.75;


    // Guarda el par de datos: el ID y la estacion 
    private static class Entrada {
        private final String clave; // ID de la estacion
        private Estacion valor; // La estacion guardada


// Crea el registro con la clave y la estacion        
        private Entrada(String clave, Estacion valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

// Arreglo de listas para manejar colisiones: si dos IDs diferentes obtienen 
// el mismo numero de posicion, ambas estaciones se guardan en esta lista
    private ListaEnlazada<Entrada>[] casillas;

    private int tamano; // Cantidad de estaciones guardadas en total


// Crea una tabla vacia empezando con 101 espacios
    public TablaHash() {
        casillas = crearCasillas(CAPACIDAD_INICIAL);
        tamano = 0; // Empezamos en cero
    }


// Crea el arreglo de listas saltandose la restriccion de tipos de Java
    @SuppressWarnings("unchecked")
    private ListaEnlazada<Entrada>[] crearCasillas(int capacidad) {
        return (ListaEnlazada<Entrada>[]) new ListaEnlazada<?>[capacidad];
    }


// Calcula la posicion en el arreglo mediante una funcion hash    
    private int hash(String clave) {
        long resultado = 0;

        // Sumamos los valores de cada letra para sacar un numero unico
        for (int i = 0; i < clave.length(); i++) {
            resultado = (31L * resultado + clave.charAt(i)) 
                    % casillas.length;
        }

        return (int) resultado;
    }


// Busca si un ID ya esta guardado dentro de una posicion especifica
    private Entrada buscarEntrada(int posicion, String clave) {
        if (casillas[posicion] == null) { // Si no hay ninguna lista ahi
            return null;
        }

        // Recorremos la lista de ese lugar para ver si esta el ID
        for (Nodo<Entrada> actual = casillas[posicion].getCabeza();
             actual != null;
             actual = actual.getSiguiente()) {

            Entrada entrada = actual.getDato();

            if (entrada.clave.equals(clave)) { // Si lo encontramos
                return entrada;
            }
        }

        return null; // No estaba guardado ahi
    }


// Guarda una estacion por ID. Si la tabla esta muy llena, la hace mas grande
    public void insertar(String clave, Estacion valor) {
        if (clave == null || clave.trim().isEmpty()) { // Validamos el ID
            throw new IllegalArgumentException(
                    "El identificador no puede estar vacio."
            );
        }

        if (valor == null) { // Validamos la estacion
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

        // Si se va a llenar mucho (mas del 75%), la agrandamos primero
        if ((tamano + 1.0) / casillas.length > FACTOR_CARGA_MAXIMO) {
            redimensionar();
            posicion = hash(claveLimpia); // Calculamos la posicion otra vez
        }

        if (casillas[posicion] == null) { // Si no hay lista, la creamos
            casillas[posicion] = new ListaEnlazada<>();
        }

        // Guardamos el nuevo dato en la lista de esa posicion
        casillas[posicion].insertar(new Entrada(claveLimpia, valor));
        tamano++; // Sumamos uno al contador total
    }


// Busca una estacion usando su ID y te la devuelve
    public Estacion buscar(String clave) {
        if (clave == null || clave.trim().isEmpty()) {
            return null;
        }

        String claveLimpia = clave.trim();
        int posicion = hash(claveLimpia);
        
        Entrada entrada = buscarEntrada(posicion, claveLimpia);
        
        // Si la encontro devuelve la estacion, si no devuelve null
        return entrada == null ? null : entrada.valor; 
    }


// Obtiene una estacion mediante su identificador    
    public Estacion obtener(String clave) {
        return buscar(clave);
    }


// Borra una estacion usando su ID
    public boolean eliminar(String clave) {
        if (clave == null || clave.trim().isEmpty()) {
            return false;
        }

        String claveLimpia = clave.trim();
        int posicion = hash(claveLimpia);

        if (casillas[posicion] == null) { // Si no hay nada en esa posicion
            return false;
        }

        // Buscamos si el dato realmente existe ahi dentro
        Entrada existente = buscarEntrada(posicion, claveLimpia);

        if (existente == null) {
            return false; // No estaba guardada
        }

        // Le pedimos a la lista que borre ese dato exacto
        boolean eliminada = casillas[posicion].eliminar(existente);

        if (eliminada) {
            tamano--; // Restamos del contador total

            if (casillas[posicion].esVacia()) { // Si la lista quedo vacia
                casillas[posicion] = null; // Borramos la lista para hacer espacio
            }
        }

        return eliminada;
    }


// Recorre toda la tabla y te da una lista con todas las estaciones juntas
    public ListaEnlazada<Estacion> obtenerTodas() {
        ListaEnlazada<Estacion> todas = new ListaEnlazada<>();

        for (int i = 0; i < casillas.length; i++) {
            if (casillas[i] == null) { // Saltamos los espacios vacios
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


// Te dice si una estacion con ese ID ya esta guardada en la tabla
    public boolean contiene(String clave) {
        if (clave == null || clave.trim().isEmpty()) {
            return false;
        }

        String claveLimpia = clave.trim();
        int posicion = hash(claveLimpia);

        // Retorna true si la encontro, false si dio null
        return buscarEntrada(posicion, claveLimpia) != null;
    }


// Te dice cuantas estaciones hay guardadas en total
    public int obtenerTamano() {
        return tamano;
    }


// Te dice de que tamaño es el arreglo actualmente
    public int obtenerCapacidad() {
        return casillas.length;
    }


// Calcula que porcentaje de la tabla esta ocupada
    public double obtenerFactorCarga() {
        return (double) tamano / casillas.length;
    }


// Agranda la tabla al doble (buscando un numero primo) y reubica todo
    private void redimensionar() {
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
                int nuevaPos = hash(entrada.clave); // Calculamos nueva ubicacion

                if (casillas[nuevaPos] == null) {
                    casillas[nuevaPos] = new ListaEnlazada<>();
                }

                casillas[nuevaPos].insertar(entrada); // Pasamos el dato
            }
        }
    }


// Busca un numero primo que sea igual o mayor al numero que le pases
    private int siguientePrimo(int numero) {
        int candidato = numero;

        if (candidato <= 2) {
            return 2;
        }

        if (candidato % 2 == 0) { // Si es par, le sumamos uno para hacerlo impar
            candidato++;
        }

        while (!esPrimo(candidato)) { // Mientras no sea primo, sigue buscando
            candidato += 2;
        }

        return candidato;
    }


// Verifica si un numero entero dado es primo    
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


// Borra absolutamente todo y reinicia la tabla a como estaba al principio
    public void limpiar() {
        casillas = crearCasillas(CAPACIDAD_INICIAL);
        tamano = 0;
    }
}
