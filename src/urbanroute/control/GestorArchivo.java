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
 * Lectura y escritura de CSV.
 *  diccionario.csv -> id,nombre,linea,capacidad
 *  grafo.csv       -> idOrigen,idDestino,tiempo
 * Se ignoran líneas vacías, comentarios (#) y la cabecera (primer campo "id"/"origen").
 * Los campos no pueden contener comas. Colabora con VentanaPrincipal para reportar
 * líneas inválidas (dependencia bidireccional).
 */
public class GestorArchivo {
    private VentanaPrincipal ventana;
    private int lineasInvalidas;

    public GestorArchivo(VentanaPrincipal ventana) {
        this.ventana = ventana;
    }

    /**
     * Lee diccionario.csv y crea las estaciones, registrándolas en el GrafoPonderado
     * y en la TablaHash. Las líneas inválidas se omiten y se informan en la ventana.
     * @param ruta  ruta del archivo diccionario.csv
     * @param tabla TablaHash que se puebla
     * @param grafo grafo al que se agregan los vértices
     * @return cantidad de estaciones cargadas
     * @throws IOException si el archivo no se puede leer
     */
    public int cargarDiccionario(String ruta, TablaHash tabla, GrafoPonderado grafo) throws IOException {
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
                        reportarInvalida(numLinea, linea + "  (ID vacío o duplicado)");
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
     * Lee grafo.csv y crea las conexiones entre estaciones ya cargadas.
     * Requiere haber cargado antes el diccionario.
     * @param ruta  ruta del archivo grafo.csv
     * @param grafo grafo al que se agregan las aristas
     * @return cantidad de conexiones cargadas
     * @throws IOException si el archivo no se puede leer
     */
    public int cargarGrafo(String ruta, GrafoPonderado grafo) throws IOException {
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
                        reportarInvalida(numLinea, linea + "  (estación inexistente o tiempo inválido)");
                    }
                } catch (NumberFormatException ex) {
                    reportarInvalida(numLinea, linea);
                }
            }
        }
        return cargadas;
    }

    /**
     * Reescribe diccionario.csv y grafo.csv con el estado ACTUAL de la memoria
     * (incluye altas y bajas hechas desde la interfaz). Sobrescribe si existen.
     * Las estaciones se guardan en el orden del grafo, para que al volver a cargar
     * cada línea conserve su orden. En un grafo no dirigido cada vía se escribe una vez.
     * @param ruta  CARPETA de destino
     * @param tabla TablaHash con las estaciones
     * @param grafo grafo con las estaciones y sus conexiones
     * @throws IOException si la ruta no es una carpeta o no se puede escribir
     */
    public void guardarCambiosCSV(String ruta, TablaHash tabla, GrafoPonderado grafo) throws IOException {
        File carpeta = new File(ruta);
        if (!carpeta.isDirectory()) {
            throw new IOException("La ruta no es una carpeta: " + ruta);
        }
        try (PrintWriter pw = abrirEscritura(new File(carpeta, "diccionario.csv"))) {
            pw.println("id,nombre,linea,capacidad");
            for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
                Estacion enTabla = tabla.buscar(((Estacion) n.getDato()).getId());
                if (enTabla != null) {
                    pw.println(enTabla.toCSVString());
                }
            }
        }
        try (PrintWriter pw = abrirEscritura(new File(carpeta, "grafo.csv"))) {
            pw.println("origen,destino,tiempo");
            for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
                Estacion e = (Estacion) n.getDato();
                for (Nodo m = e.getConexiones().getCabeza(); m != null; m = m.getSiguiente()) {
                    Conexion c = (Conexion) m.getDato();
                    // En un grafo no dirigido la vía se escribe una sola vez.
                    if (grafo.esDirigido() || c.getOrigen().comparar(c.getDestino()) < 0) {
                        pw.println(c.toCSVString());
                    }
                }
            }
        }
    }

    // ---------- utilidades privadas ----------

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
            ventana.mostrarMensaje("Línea " + numLinea + " omitida: " + linea);
        }
    }

    public int getLineasInvalidas() { return lineasInvalidas; }
    public VentanaPrincipal getVentana() { return ventana; }
    public void setVentana(VentanaPrincipal ventana) { this.ventana = ventana; }
}
