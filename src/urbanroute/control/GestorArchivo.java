package urbanroute.control;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import urbanroute.interfaz.VentanaPrincipal;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.modelo.TablaHash;
import urbanroute.tda.Nodo;

/**
 * Lee y guarda los archivos CSV.
 * diccionario.csv: id,nombre,linea,capacidad. grafo.csv: origen,destino,tiempo.
 * Las lineas con errores se omiten y se avisan en la ventana.
 */
public class GestorArchivo {
    private VentanaPrincipal ventana;
    private int lineasInvalidas;

    public GestorArchivo(VentanaPrincipal ventana) {
        this.ventana = ventana;
    }

    /**
     * Lee diccionario.csv y agrega las estaciones al grafo y a la tabla hash.
     * Devuelve cuantas estaciones se cargaron.
     */
    public int cargarEstaciones(String ruta, TablaHash tabla, GrafoPonderado grafo) throws IOException {
        lineasInvalidas = 0;
        int cargadas = 0;
        try (BufferedReader br = abrirLectura(ruta)) {
            String linea;
            int numLinea = 0;
            while ((linea = br.readLine()) != null) {
                numLinea++;
                if (esIgnorable(linea, "id")) continue;
                String[] c = linea.split(",");
                if (c.length < 4) {
                    reportarInvalida(numLinea, linea);
                    continue;
                }
                try {
                    String id = c[0].trim();
                    int capacidad = Integer.parseInt(c[3].trim());
                    Estacion e = new Estacion(id, c[1].trim(), c[2].trim(), capacidad);
                    if (id.isEmpty() || !grafo.agregarEstacion(e)) {
                        reportarInvalida(numLinea, linea + "  (id vacio o repetido)");
                        continue;
                    }
                    tabla.insertar(id, e);
                    cargadas++;
                } catch (NumberFormatException ex) {
                    reportarInvalida(numLinea, linea);
                }
            }
        }
        return cargadas;
    }

    /**
     * Lee grafo.csv y agrega las conexiones. Las estaciones ya deben estar cargadas.
     * Devuelve cuantas conexiones se cargaron.
     */
    public int cargarConexiones(String ruta, GrafoPonderado grafo) throws IOException {
        lineasInvalidas = 0;
        int cargadas = 0;
        try (BufferedReader br = abrirLectura(ruta)) {
            String linea;
            int numLinea = 0;
            while ((linea = br.readLine()) != null) {
                numLinea++;
                if (esIgnorable(linea, "origen")) continue;
                String[] c = linea.split(",");
                if (c.length < 3) {
                    reportarInvalida(numLinea, linea);
                    continue;
                }
                try {
                    double tiempo = Double.parseDouble(c[2].trim());
                    if (grafo.agregarConexion(c[0].trim(), c[1].trim(), tiempo)) {
                        cargadas++;
                    } else {
                        reportarInvalida(numLinea, linea + "  (estacion inexistente o tiempo invalido)");
                    }
                } catch (NumberFormatException ex) {
                    reportarInvalida(numLinea, linea);
                }
            }
        }
        return cargadas;
    }

    /**
     * Guarda en la carpeta indicada diccionario.csv y grafo.csv con lo que hay en memoria.
     * Las estaciones se escriben en el orden del grafo y, si el grafo no es dirigido,
     * cada conexion se escribe una sola vez.
     */
    public void guardarCambios(String carpeta, TablaHash tabla, GrafoPonderado grafo) throws IOException {
        File destino = new File(carpeta);
        if (!destino.isDirectory()) {
            throw new IOException("La ruta no es una carpeta: " + carpeta);
        }
        try (PrintWriter pw = abrirEscritura(new File(destino, "diccionario.csv"))) {
            pw.println("id,nombre,linea,capacidad");
            for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
                Estacion enTabla = tabla.buscar(((Estacion) n.getDato()).getId());
                if (enTabla != null) {
                    pw.println(enTabla.toCSVString());
                }
            }
        }
        try (PrintWriter pw = abrirEscritura(new File(destino, "grafo.csv"))) {
            pw.println("origen,destino,tiempo");
            for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
                Estacion e = (Estacion) n.getDato();
                for (Nodo m = e.getConexiones().getCabeza(); m != null; m = m.getSiguiente()) {
                    Conexion c = (Conexion) m.getDato();
                    if (grafo.esDirigido() || c.getOrigen().comparar(c.getDestino()) < 0) {
                        pw.println(c.toCSVString());
                    }
                }
            }
        }
    }

    private BufferedReader abrirLectura(String ruta) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(ruta), "UTF-8"));
    }

    private PrintWriter abrirEscritura(File f) throws IOException {
        return new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
    }

    private boolean esIgnorable(String linea, String palabraCabecera) {
        String t = linea.trim();
        if (t.isEmpty() || t.startsWith("#")) return true;
        String primero = t.split(",")[0].trim();
        return primero.equalsIgnoreCase(palabraCabecera);
    }

    private void reportarInvalida(int numLinea, String linea) {
        lineasInvalidas++;
        if (ventana != null) {
            ventana.mostrarMensaje("Linea " + numLinea + " omitida: " + linea);
        }
    }

    public int getLineasInvalidas() { return lineasInvalidas; }
    public VentanaPrincipal getVentana() { return ventana; }
    public void setVentana(VentanaPrincipal ventana) { this.ventana = ventana; }
}
