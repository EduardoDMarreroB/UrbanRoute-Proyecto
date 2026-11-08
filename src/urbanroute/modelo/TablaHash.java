package urbanroute.modelo;

import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Tabla hash que guarda las estaciones por su id.
 * Las colisiones se resuelven con encadenamiento: cada casilla es una ListaEnlazada.
 * Cuando se llena mas del 75% la tabla crece.
 */
public class TablaHash {
    private static final int CAPACIDAD_INICIAL = 31;
    private static final double FACTOR_MAXIMO = 0.75;

    private static class Entrada {
        private String clave;
        private Estacion valor;

        Entrada(String clave, Estacion valor) {
            this.clave = clave;
            this.valor = valor;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Entrada && clave.equals(((Entrada) o).clave);
        }

        @Override
        public int hashCode() { return clave.hashCode(); }
    }

    private ListaEnlazada[] casillas;
    private int tamano;
    private GrafoPonderado propietario;

    public TablaHash() {
        this(CAPACIDAD_INICIAL);
    }

    public TablaHash(int capacidad) {
        if (capacidad < 1) {
            capacidad = CAPACIDAD_INICIAL;
        }
        casillas = new ListaEnlazada[capacidad];
        tamano = 0;
    }

    /** Funcion hash (tipo djb2). Devuelve un numero positivo; la posicion se saca con el modulo. */
    private int hash(String clave) {
        long h = 5381;
        for (int i = 0; i < clave.length(); i++) {
            h = (h * 33 + clave.charAt(i)) % 2147483647L;
        }
        return (int) h;
    }

    /** Guarda la estacion con esa clave. Si la clave ya existe, reemplaza el valor. */
    public void insertar(String clave, Estacion estacion) {
        int pos = hash(clave) % casillas.length;
        if (casillas[pos] == null) {
            casillas[pos] = new ListaEnlazada();
        }
        Entrada existente = (Entrada) casillas[pos].buscar(new Entrada(clave, null));
        if (existente != null) {
            existente.valor = estacion;
            return;
        }
        casillas[pos].insertar(new Entrada(clave, estacion));
        tamano++;
        if ((double) tamano / casillas.length > FACTOR_MAXIMO) {
            redimensionar();
        }
    }

    /** Devuelve la estacion con esa clave, o null si no existe. */
    public Estacion buscar(String clave) {
        int pos = hash(clave) % casillas.length;
        if (casillas[pos] == null) {
            return null;
        }
        Entrada e = (Entrada) casillas[pos].buscar(new Entrada(clave, null));
        return e == null ? null : e.valor;
    }

    /** Elimina la clave. Devuelve true si existia. */
    public boolean eliminar(String clave) {
        int pos = hash(clave) % casillas.length;
        if (casillas[pos] == null) {
            return false;
        }
        boolean eliminada = casillas[pos].eliminar(new Entrada(clave, null));
        if (eliminada) {
            tamano--;
        }
        return eliminada;
    }

    /** Duplica la capacidad y vuelve a calcular la posicion de cada entrada. */
    private void redimensionar() {
        ListaEnlazada[] nuevas = new ListaEnlazada[casillas.length * 2 + 1];
        for (int i = 0; i < casillas.length; i++) {
            if (casillas[i] == null) continue;
            for (Nodo n = casillas[i].getCabeza(); n != null; n = n.getSiguiente()) {
                Entrada e = (Entrada) n.getDato();
                int pos = hash(e.clave) % nuevas.length;
                if (nuevas[pos] == null) {
                    nuevas[pos] = new ListaEnlazada();
                }
                nuevas[pos].insertar(e);
            }
        }
        casillas = nuevas;
    }

    /** Devuelve todas las estaciones guardadas en una ListaEnlazada. */
    public ListaEnlazada obtenerTodas() {
        ListaEnlazada todas = new ListaEnlazada();
        for (int i = 0; i < casillas.length; i++) {
            if (casillas[i] == null) continue;
            for (Nodo n = casillas[i].getCabeza(); n != null; n = n.getSiguiente()) {
                todas.insertar(((Entrada) n.getDato()).valor);
            }
        }
        return todas;
    }

    public boolean contiene(String clave) { return buscar(clave) != null; }

    public int obtenerTamano() { return tamano; }

    public int obtenerCapacidad() { return casillas.length; }

    public void limpiar() {
        casillas = new ListaEnlazada[casillas.length];
        tamano = 0;
    }

    public GrafoPonderado getPropietario() { return propietario; }
    public void setPropietario(GrafoPonderado propietario) { this.propietario = propietario; }
}
