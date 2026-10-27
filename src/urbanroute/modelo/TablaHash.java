package urbanroute.modelo;

import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Tabla hash propia: arreglo de cubetas (ListaEnlazada) con encadenamiento.
 * Capacidad dinámica: crece cuando el factor de carga supera 0.75.
 * Guarda una referencia al GrafoPonderado propietario (dependencia bidireccional:
 * el grafo posee el índice y el índice conoce a su grafo).
 */
public class TablaHash {
    private static final int CAPACIDAD_INICIAL = 31;
    private static final double FACTOR_CARGA_MAX = 0.75;

    /** Par clave-valor almacenado en cada cubeta. Igualdad por clave. */
    private static class Entrada {
        final String clave;
        Estacion valor;

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

    private ListaEnlazada[] cubetas;
    private int capacidad;
    private int tamano;
    private GrafoPonderado propietario;

    public TablaHash() {
        this(CAPACIDAD_INICIAL);
    }

    public TablaHash(int capacidadInicial) {
        this.capacidad = capacidadInicial < 1 ? CAPACIDAD_INICIAL : capacidadInicial;
        this.cubetas = new ListaEnlazada[this.capacidad];
        this.tamano = 0;
    }

    /** Función hash propia (djb2 con módulo) sobre la clave. */
    private int hash(String clave, int cap) {
        long h = 5381;
        for (int i = 0; i < clave.length(); i++) {
            h = (h * 33 + clave.charAt(i)) % 2147483647L;
        }
        return (int) (h % cap);
    }

    /** Inserta o reemplaza el valor asociado a la clave. */
    public void insertar(String key, Estacion estacion) {
        int pos = hash(key, capacidad);
        if (cubetas[pos] == null) {
            cubetas[pos] = new ListaEnlazada();
        }
        Entrada existente = (Entrada) cubetas[pos].buscar(new Entrada(key, null));
        if (existente != null) {
            existente.valor = estacion;
            return;
        }
        cubetas[pos].insertar(new Entrada(key, estacion));
        tamano++;
        if ((double) tamano / capacidad > FACTOR_CARGA_MAX) {
            redimensionar();
        }
    }

    /** Devuelve la Estacion asociada o null. */
    public Estacion buscar(String key) {
        int pos = hash(key, capacidad);
        if (cubetas[pos] == null) {
            return null;
        }
        Entrada e = (Entrada) cubetas[pos].buscar(new Entrada(key, null));
        return e == null ? null : e.valor;
    }

    /** Elimina la clave. Devuelve true si existía. */
    public boolean eliminar(String key) {
        int pos = hash(key, capacidad);
        if (cubetas[pos] == null) {
            return false;
        }
        boolean eliminado = cubetas[pos].eliminar(new Entrada(key, null));
        if (eliminado) {
            tamano--;
        }
        return eliminado;
    }

    private void redimensionar() {
        int nuevaCap = capacidad * 2 + 1;
        ListaEnlazada[] nuevas = new ListaEnlazada[nuevaCap];
        for (int i = 0; i < capacidad; i++) {
            if (cubetas[i] == null) continue;
            for (Nodo n = cubetas[i].getCabeza(); n != null; n = n.getSiguiente()) {
                Entrada e = (Entrada) n.getDato();
                int pos = hash(e.clave, nuevaCap);
                if (nuevas[pos] == null) {
                    nuevas[pos] = new ListaEnlazada();
                }
                nuevas[pos].insertar(e);
            }
        }
        cubetas = nuevas;
        capacidad = nuevaCap;
    }

    /** Devuelve todas las estaciones en una ListaEnlazada (para guardar a CSV). */
    public ListaEnlazada obtenerTodas() {
        ListaEnlazada todas = new ListaEnlazada();
        for (int i = 0; i < capacidad; i++) {
            if (cubetas[i] == null) continue;
            for (Nodo n = cubetas[i].getCabeza(); n != null; n = n.getSiguiente()) {
                todas.insertar(((Entrada) n.getDato()).valor);
            }
        }
        return todas;
    }

    public boolean contiene(String key) { return buscar(key) != null; }
    public int tamano() { return tamano; }
    public int getCapacidad() { return capacidad; }

    public void limpiar() {
        cubetas = new ListaEnlazada[capacidad];
        tamano = 0;
    }

    public GrafoPonderado getPropietario() { return propietario; }
    public void setPropietario(GrafoPonderado propietario) { this.propietario = propietario; }
}
