package urbanroute.interfaz;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import urbanroute.control.AnalizadorRed;
import urbanroute.control.GestorArchivo;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.modelo.TablaHash;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Ventana principal (Swing). Es el "dueño" de la
 * memoria del programa: TablaHash + GrafoPonderado. Se relaciona de forma
 * bidireccional con GestorArchivo, AnalizadorRed y VisualizadorGrafo.
 * Cada alta o baja actualiza la memoria y luego refresca el dibujo.
 */
public class VentanaPrincipal extends JFrame {
    private TablaHash tabla;
    private GrafoPonderado grafo;
    private GestorArchivo gestor;
    private AnalizadorRed analizador;
    private VisualizadorGrafo visualizador;

    private JTextArea areaLog;
    private File ultimaCarpeta;

    // Panel de división: arriba el grafo, abajo el registro de mensajes
    private JSplitPane panelDividido;

    /** Crea la memoria (TablaHash + GrafoPonderado), los colaboradores y la interfaz. */
    public VentanaPrincipal() {
        super("UrbanRoute");
        tabla = new TablaHash();
        grafo = new GrafoPonderado(tabla);
        gestor = new GestorArchivo(this);
        analizador = new AnalizadorRed(grafo);
        visualizador = new VisualizadorGrafo(grafo, this);
        analizador.setVisualizador(visualizador);
        visualizador.setAnalizador(analizador);
        construirUI();
    }

    private void construirUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // Tres grupos con título, uno al lado del otro; los botones de cada grupo
        // forman una cuadrícula de 2x2 y todos miden lo mismo.
        JPanel grupoArchivos = grupo("Archivos",
                boton("Cargar diccionario", () -> accionCargarDiccionario()),
                boton("Cargar grafo", () -> accionCargarGrafo()),
                boton("Guardar cambios", () -> accionGuardar()));

        JPanel grupoRed = grupo("Red",
                boton("Agregar estación", () -> accionAgregarEstacion()),
                boton("Eliminar estación", () -> accionEliminarEstacion()),
                boton("Agregar conexión", () -> accionAgregarConexion()),
                boton("Eliminar conexión", () -> accionEliminarConexion()));

        JPanel grupoAnalisis = grupo("Análisis",
                boton("Ruta mínima", () -> accionRutaMinima()),
                boton("Subredes aisladas", () -> accionSubredes()),
                boton("Hubs", () -> accionHubs()),
                boton("Actualizar grafo", () -> accionMostrarGrafo()));

        JPanel barra = new JPanel(new GridLayout(1, 3, 8, 0));
        barra.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        barra.add(grupoArchivos);
        barra.add(grupoRed);
        barra.add(grupoAnalisis);
        add(barra, BorderLayout.NORTH);

        areaLog = new JTextArea();
        areaLog.setEditable(false);
        JScrollPane scrollLog = new JScrollPane(areaLog);

        panelDividido = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        panelDividido.setTopComponent(new JLabel(
                "Cargue diccionario.csv y grafo.csv para ver la red", SwingConstants.CENTER));
        panelDividido.setBottomComponent(scrollLog);
        panelDividido.setDividerLocation(430);
        panelDividido.setResizeWeight(0.8);
        add(panelDividido, BorderLayout.CENTER);
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        return b;
    }

    /** Panel con título y cuadrícula de 2x2; las celdas que sobran quedan vacías. */
    private JPanel grupo(String titulo, JButton... botones) {
        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        for (int i = 0; i < botones.length; i++) {
            panel.add(botones[i]);
        }
        for (int i = botones.length; i < 4; i++) {
            panel.add(new JLabel());
        }
        return panel;
    }

    // =====================================================================
    //  CARGA, GUARDADO Y DIBUJO
    // =====================================================================

    /** Carga diccionario.csv (con aviso si ya hay datos en memoria) y refresca el dibujo. */
    private void accionCargarDiccionario() {
        File f = elegirArchivo("Seleccionar diccionario.csv");
        if (f == null) return;
        if (tabla.tamano() > 0 && !confirmarAdvertencia(
                "Ya hay " + tabla.tamano() + " estaciones en memoria.\n"
              + "Cargar otro diccionario BORRARÁ las estaciones y conexiones actuales. ¿Continuar?")) {
            return;
        }
        try {
            grafo.limpiar();
            int n = gestor.cargarDiccionario(f.getAbsolutePath(), tabla, grafo);
            mostrarMensaje("Diccionario cargado: " + n + " estaciones (" + gestor.getLineasInvalidas() + " líneas omitidas).");
            visualizador.sincronizar();
        } catch (IOException ex) {
            mostrarError("No se pudo leer el diccionario: " + ex.getMessage());
        }
    }

    /** Carga grafo.csv (con aviso si ya hay conexiones) y dibuja la red. */
    private void accionCargarGrafo() {
        if (grafo.numEstaciones() == 0) {
            mostrarError("Primero cargue el diccionario de estaciones.");
            return;
        }
        File f = elegirArchivo("Seleccionar grafo.csv");
        if (f == null) return;
        if (grafo.numConexiones() > 0 && !confirmarAdvertencia(
                "Ya hay conexiones en memoria. Se reemplazarán por las del archivo. ¿Continuar?")) {
            return;
        }
        try {
            grafo.limpiarConexiones();
            int n = gestor.cargarGrafo(f.getAbsolutePath(), grafo);
            mostrarMensaje("Grafo cargado: " + n + " conexiones (" + gestor.getLineasInvalidas() + " líneas omitidas).");
            accionMostrarGrafo();
        } catch (IOException ex) {
            mostrarError("No se pudo leer el grafo: " + ex.getMessage());
        }
    }

    /** Guarda el estado actual de la memoria en diccionario.csv y grafo.csv (carpeta elegida). */
    private void accionGuardar() {
        JFileChooser fc = new JFileChooser(ultimaCarpeta);
        fc.setDialogTitle("Carpeta destino (se escribirán diccionario.csv y grafo.csv)");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File carpeta = fc.getSelectedFile();
        ultimaCarpeta = carpeta;
        if ((new File(carpeta, "diccionario.csv").exists() || new File(carpeta, "grafo.csv").exists())
                && !confirmarAdvertencia("Los archivos CSV existentes en esa carpeta serán SOBRESCRITOS. ¿Continuar?")) {
            return;
        }
        try {
            gestor.guardarCambiosCSV(carpeta.getAbsolutePath(), tabla, grafo);
            mostrarMensaje("Cambios guardados en " + carpeta.getAbsolutePath());
        } catch (IOException ex) {
            mostrarError("No se pudo guardar: " + ex.getMessage());
        }
    }

    /** Reconstruye el grafo desde cero y lo coloca en la parte superior de la ventana. */
    private void accionMostrarGrafo() {
        if (grafo.numEstaciones() == 0) {
            mostrarError("No hay datos cargados.");
            return;
        }
        Component nuevoPanelGrafo = visualizador.obtenerComponenteVista();
        int divisor = panelDividido.getDividerLocation();
        panelDividido.setTopComponent(nuevoPanelGrafo);
        panelDividido.setDividerLocation(divisor);
        panelDividido.revalidate();
        panelDividido.repaint();
    }

    // =====================================================================
    //  ALTAS Y BAJAS
    // =====================================================================

    /**
     * Formulario para agregar una estación. Opcionalmente la conecta con una estación
     * existente indicando el tiempo de viaje. Actualiza TablaHash, GrafoPonderado y dibujo.
     */
    private void accionAgregarEstacion() {
        JTextField campoId = new JTextField(12);
        JTextField campoNombre = new JTextField(12);
        JTextField campoCapacidad = new JTextField("0", 12);
        JTextField campoTiempo = new JTextField(12);

        JComboBox<String> comboLinea = new JComboBox<String>(lineasExistentes());
        comboLinea.setEditable(true);   // se puede elegir una línea o escribir una nueva

        Estacion[] existentes = todasLasEstaciones();
        Object[] opcionesVecino = new Object[existentes.length + 1];
        opcionesVecino[0] = "(sin conexión)";
        for (int i = 0; i < existentes.length; i++) {
            opcionesVecino[i + 1] = existentes[i];
        }
        JComboBox<Object> comboVecino = new JComboBox<Object>(opcionesVecino);

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("ID:"));                         panel.add(campoId);
        panel.add(new JLabel("Nombre:"));                     panel.add(campoNombre);
        panel.add(new JLabel("Línea (elegir o escribir):"));  panel.add(comboLinea);
        panel.add(new JLabel("Capacidad:"));                  panel.add(campoCapacidad);
        panel.add(new JLabel("Conectar con:"));               panel.add(comboVecino);
        panel.add(new JLabel("Tiempo de viaje (min):"));      panel.add(campoTiempo);

        while (true) {
            int r = JOptionPane.showConfirmDialog(this, panel, "Agregar estación",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return;

            String id = campoId.getText().trim();
            String nombre = campoNombre.getText().trim();
            Object editor = comboLinea.getEditor().getItem();
            String linea = editor == null ? "" : editor.toString().trim();

            String error = null;
            int capacidad = 0;
            double tiempo = 0;
            Estacion vecino = null;

            if (!textoValido(id) || !textoValido(nombre) || !textoValido(linea)) {
                error = "ID, nombre y línea son obligatorios y no pueden contener comas.";
            } else if (id.startsWith("#") || id.equalsIgnoreCase("id") || id.equalsIgnoreCase("origen")) {
                error = "Ese ID está reservado por el formato CSV. Use otro.";
            } else if (grafo.buscarEstacion(id) != null) {
                error = "Ya existe una estación con el ID " + id + ".";
            } else {
                try {
                    capacidad = Integer.parseInt(campoCapacidad.getText().trim());
                    if (capacidad < 0) error = "La capacidad no puede ser negativa.";
                } catch (NumberFormatException ex) {
                    error = "La capacidad debe ser un número entero.";
                }
            }
            if (error == null && comboVecino.getSelectedItem() instanceof Estacion) {
                vecino = (Estacion) comboVecino.getSelectedItem();
                tiempo = parsearTiempo(campoTiempo.getText());
                if (tiempo < 0) error = "Indique un tiempo de viaje mayor que 0 (por ejemplo 3.5).";
            }
            if (error != null) {
                mostrarError(error);
                continue;   // vuelve a mostrar el formulario con lo escrito
            }

            Estacion nueva = new Estacion(id, nombre, linea, capacidad);
            grafo.agregarEstacion(nueva);          // lista de vértices + TablaHash (índice)
            if (vecino != null) {
                grafo.agregarConexion(nueva, vecino, tiempo);
            }
            visualizador.sincronizar();
            mostrarMensaje("Estación agregada: " + nueva + " (línea " + linea + ")"
                    + (vecino != null ? ", conectada con " + vecino.getNombre() + " (" + tiempo + " min)."
                                      : ", sin conexiones todavía."));
            return;
        }
    }

    /**
     * Elimina una estación: la quita de la TablaHash y de la lista de vértices y borra
     * todas las aristas que la tocan. Pide confirmación antes de actuar.
     */
    private void accionEliminarEstacion() {
        Estacion e = elegirEstacion("Eliminar estación", "Seleccione la estación a eliminar:", todasLasEstaciones());
        if (e == null) return;
        int conexiones = e.getGrado();
        if (!confirmarAdvertencia("Se eliminará " + e + " y sus " + conexiones + " conexiones. ¿Continuar?")) {
            return;
        }
        grafo.eliminarEstacion(e.getId());
        visualizador.sincronizar();
        mostrarMensaje("Estación eliminada: " + e + " (se eliminaron " + conexiones + " conexiones).");
    }

    /** Agrega una conexión directa entre dos estaciones con su tiempo de viaje. */
    private void accionAgregarConexion() {
        Estacion origen = elegirEstacion("Agregar conexión", "Estación de origen:", todasLasEstaciones());
        if (origen == null) return;
        Estacion destino = elegirEstacion("Agregar conexión", "Estación de destino:", todasExcepto(origen));
        if (destino == null) return;
        Double tiempo = pedirTiempo("Tiempo de viaje entre " + origen.getNombre() + " y " + destino.getNombre() + " (minutos):");
        if (tiempo == null) return;

        boolean yaExistia = grafo.buscarConexion(origen, destino) != null;
        grafo.agregarConexion(origen, destino, tiempo);
        visualizador.sincronizar();
        mostrarMensaje((yaExistia ? "Conexión actualizada: " : "Conexión agregada: ")
                + origen.getNombre() + " <-> " + destino.getNombre() + " (" + tiempo + " min).");
    }

    /** Elimina una conexión directa; el destino se elige entre los vecinos de la estación de origen. */
    private void accionEliminarConexion() {
        Estacion origen = elegirEstacion("Eliminar conexión", "Estación de origen:", todasLasEstaciones());
        if (origen == null) return;
        Estacion[] vecinos = vecinosDe(origen);
        if (vecinos.length == 0) {
            mostrarError(origen.getNombre() + " no tiene conexiones.");
            return;
        }
        Estacion destino = elegirEstacion("Eliminar conexión", "Conexión con:", vecinos);
        if (destino == null) return;

        grafo.eliminarConexion(origen, destino);
        visualizador.sincronizar();
        mostrarMensaje("Conexión eliminada: " + origen.getNombre() + " <-> " + destino.getNombre() + ".");
    }

    // =====================================================================
    //  ALGORITMOS
    // =====================================================================

    /** Calcula la ruta de menor tiempo entre dos estaciones y la resalta en el grafo. */
    private void accionRutaMinima() {
        Estacion origen = elegirEstacion("Ruta mínima", "Estación de origen:", todasLasEstaciones());
        if (origen == null) return;
        Estacion destino = elegirEstacion("Ruta mínima", "Estación de destino:", todasLasEstaciones());
        if (destino == null) return;

        ListaEnlazada ruta = analizador.dijkstra(origen.getId(), destino.getId());
        if (ruta == null) {
            mostrarMensaje("No existe camino entre " + origen.getNombre() + " y " + destino.getNombre() + ".");
            return;
        }
        mostrarMensaje("Ruta mínima (" + analizador.getUltimoTiempoRuta() + " min, "
                + ruta.tamano() + " estaciones): " + listaATexto(ruta, " -> "));
        if (!visualizador.estaMostrandose()) {
            accionMostrarGrafo();
        }
        visualizador.resaltarRuta(ruta);
    }

    /**
     * Subredes aisladas con BFS. Permite ver la red actual o simular la baja de una
     * estación o de una línea completa (sin modificar los datos).
     */
    private void accionSubredes() {
        if (grafo.numEstaciones() == 0) {
            mostrarError("No hay datos cargados.");
            return;
        }
        String[] opciones = {"Red actual", "Sin una estación", "Sin una línea"};
        int op = JOptionPane.showOptionDialog(this, "¿Qué desea analizar?", "Subredes aisladas",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (op < 0) return;

        if (op == 0) {
            mostrarSubredes("red actual", analizador.componentesAislados());
        } else if (op == 1) {
            Estacion e = elegirEstacion("Simular baja de estación", "Estación que se desconecta:", todasLasEstaciones());
            if (e == null) return;
            mostrarSubredes("sin la estación " + e.getNombre(), analizador.componentesSinEstacion(e.getId()));
        } else {
            String[] lineas = lineasExistentes();
            Object sel = JOptionPane.showInputDialog(this, "Línea que se desconecta:", "Simular baja de línea",
                    JOptionPane.QUESTION_MESSAGE, null, lineas, lineas[0]);
            if (sel == null) return;
            mostrarSubredes("sin la línea " + sel, analizador.componentesSinLinea((String) sel));
        }
    }

    /** Muestra en el registro cada subred; las de una sola estación se marcan como aisladas. */
    private void mostrarSubredes(String descripcion, ListaEnlazada componentes) {
        mostrarMensaje("Subredes (" + descripcion + "): " + componentes.tamano());
        for (int i = 0; i < componentes.tamano(); i++) {
            ListaEnlazada subred = (ListaEnlazada) componentes.obtener(i);
            mostrarMensaje("  #" + (i + 1) + (subred.tamano() == 1 ? " [AISLADA]" : "")
                    + " (" + subred.tamano() + " estaciones): " + listaATexto(subred, ", "));
        }
    }

    /** Muestra las k estaciones con más conexiones directas (hubs de transbordo). */
    private void accionHubs() {
        if (grafo.numEstaciones() == 0) {
            mostrarError("No hay datos cargados.");
            return;
        }
        String texto = JOptionPane.showInputDialog(this, "¿Cuántos hubs mostrar?", "5");
        if (texto == null) return;
        int k;
        try {
            k = Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            mostrarError("Ingrese un número entero.");
            return;
        }
        if (k < 1) {
            mostrarError("Ingrese un número mayor que 0.");
            return;
        }
        ListaEnlazada hubs = analizador.calcularHubs(k);
        mostrarMensaje("Hubs (top " + hubs.tamano() + " por conexiones directas):");
        int puesto = 1;
        for (Nodo n = hubs.getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion e = (Estacion) n.getDato();
            mostrarMensaje("  " + puesto++ + ". " + e + " - línea " + e.getLinea()
                    + " - " + analizador.centralidadGrado(e) + " conexiones");
        }
    }

    // =====================================================================
    //  UTILIDADES
    // =====================================================================

    private File elegirArchivo(String titulo) {
        JFileChooser fc = new JFileChooser(ultimaCarpeta);
        fc.setDialogTitle(titulo);
        fc.setFileFilter(new FileNameExtensionFilter("Archivos CSV", "csv"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        ultimaCarpeta = f.getParentFile();
        return f;
    }

    private boolean confirmarAdvertencia(String mensaje) {
        int r = JOptionPane.showConfirmDialog(this, mensaje, "Advertencia",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return r == JOptionPane.YES_OPTION;
    }

    /** Lista desplegable de estaciones; devuelve la elegida o null si se cancela. */
    private Estacion elegirEstacion(String titulo, String mensaje, Estacion[] opciones) {
        if (opciones.length == 0) {
            mostrarError("No hay estaciones disponibles.");
            return null;
        }
        Object sel = JOptionPane.showInputDialog(this, mensaje, titulo,
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        return (Estacion) sel;
    }

    private Estacion[] todasLasEstaciones() {
        Estacion[] arreglo = new Estacion[grafo.numEstaciones()];
        int i = 0;
        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            arreglo[i++] = (Estacion) n.getDato();
        }
        return arreglo;
    }

    private Estacion[] todasExcepto(Estacion excluida) {
        Estacion[] todas = todasLasEstaciones();
        Estacion[] resto = new Estacion[todas.length - 1];
        int j = 0;
        for (int i = 0; i < todas.length; i++) {
            if (!todas[i].equals(excluida)) {
                resto[j++] = todas[i];
            }
        }
        return resto;
    }

    private Estacion[] vecinosDe(Estacion origen) {
        ListaEnlazada adyacentes = grafo.obtenerAdyacentes(origen);
        Estacion[] vecinos = new Estacion[adyacentes.tamano()];
        int i = 0;
        for (Nodo n = adyacentes.getCabeza(); n != null; n = n.getSiguiente()) {
            vecinos[i++] = ((Conexion) n.getDato()).getDestino();
        }
        return vecinos;
    }

    private String[] lineasExistentes() {
        ListaEnlazada lineas = grafo.obtenerLineas();
        String[] arreglo = new String[lineas.tamano()];
        int i = 0;
        for (Nodo n = lineas.getCabeza(); n != null; n = n.getSiguiente()) {
            arreglo[i++] = (String) n.getDato();
        }
        return arreglo;
    }

    /** Texto no vacío y sin comas ni saltos de línea (romperían el CSV). */
    private boolean textoValido(String texto) {
        return texto.length() > 0 && texto.indexOf(',') < 0 && texto.indexOf('\n') < 0;
    }

    /** Convierte texto a minutos (acepta coma decimal). Devuelve -1 si no es un número mayor que 0. */
    private double parsearTiempo(String texto) {
        try {
            double t = Double.parseDouble(texto.trim().replace(',', '.'));
            return t > 0 ? t : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /** Pide un tiempo de viaje hasta que sea válido; devuelve null si se cancela. */
    private Double pedirTiempo(String mensaje) {
        while (true) {
            String texto = JOptionPane.showInputDialog(this, mensaje, "Tiempo de viaje", JOptionPane.QUESTION_MESSAGE);
            if (texto == null) return null;
            double t = parsearTiempo(texto);
            if (t > 0) return Double.valueOf(t);
            mostrarError("Ingrese un número mayor que 0 (por ejemplo 3.5).");
        }
    }

    /** Une los elementos de la lista con el separador dado (" -> " para rutas, ", " para grupos). */
    private String listaATexto(ListaEnlazada l, String separador) {
        StringBuilder sb = new StringBuilder();
        for (Nodo n = l.getCabeza(); n != null; n = n.getSiguiente()) {
            Object d = n.getDato();
            sb.append(d instanceof Estacion ? ((Estacion) d).getNombre() : String.valueOf(d));
            if (n.getSiguiente() != null) sb.append(separador);
        }
        return sb.toString();
    }

    /**
     * Escribe una línea en el registro de la ventana (también lo usa GestorArchivo).
     * @param texto mensaje a mostrar
     */
    public void mostrarMensaje(String texto) {
        areaLog.append(texto + "\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    /**
     * Muestra un cuadro de error.
     * @param texto mensaje de error
     */
    public void mostrarError(String texto) {
        JOptionPane.showMessageDialog(this, texto, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public TablaHash getTabla() { return tabla; }
    public GrafoPonderado getGrafo() { return grafo; }
    public GestorArchivo getGestor() { return gestor; }
    public AnalizadorRed getAnalizador() { return analizador; }
    public VisualizadorGrafo getVisualizador() { return visualizador; }
}
