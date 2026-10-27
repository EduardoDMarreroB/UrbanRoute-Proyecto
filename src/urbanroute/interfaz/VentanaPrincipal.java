package urbanroute.interfaz;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import urbanroute.control.AnalizadorRed;
import urbanroute.control.GestorArchivo;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.modelo.TablaHash;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Ventana principal (Swing). Es el punto de entrada (main) y el "dueño" de la
 * memoria del programa: TablaHash + GrafoPonderado. Se relaciona de forma
 * bidireccional con GestorArchivo y VisualizadorGrafo.
 */
public class VentanaPrincipal extends JFrame {
    private TablaHash tabla;
    private GrafoPonderado grafo;
    private GestorArchivo gestor;
    private AnalizadorRed analizador;
    private VisualizadorGrafo visualizador;

    private JTextArea areaLog;
    private File ultimaCarpeta;
    
    // Panel de división para mantener el Grafo y el Log organizados en la pantalla
    private JSplitPane panelDividido;

    public VentanaPrincipal() {
        super("UrbanRoute - Semana 6");
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
        setSize(950, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(boton("Cargar diccionario.csv", () -> accionCargarDiccionario()));
        botones.add(boton("Cargar grafo.csv", () -> accionCargarGrafo()));
        botones.add(boton("Guardar cambios", () -> accionGuardar()));
        
        // --- Nombre actualizado del botón ---
        botones.add(boton("Actualizar grafo", () -> accionMostrarGrafo()));
        
        botones.add(boton("Ruta mínima", () -> accionRutaMinima()));
        botones.add(boton("Componentes aislados", () -> accionComponentes()));
        botones.add(boton("Hubs", () -> accionHubs()));
        add(botones, BorderLayout.NORTH);

        areaLog = new JTextArea();
        areaLog.setEditable(false);
        JScrollPane scrollLog = new JScrollPane(areaLog);

        // Se utiliza JSplitPane para dividir la zona gráfica del log de texto
        panelDividido = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        panelDividido.setBottomComponent(scrollLog);
        panelDividido.setDividerLocation(420); // Tamaño inicial para el lienzo gráfico
        panelDividido.setResizeWeight(0.8);    // Prioridad de redimensión al área gráfica

        add(panelDividido, BorderLayout.CENTER);
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        return b;
    }

    // ---------- acciones ----------

    private void accionCargarDiccionario() {
        File f = elegirArchivo("Seleccionar diccionario.csv");
        if (f == null) return;
        if (tabla.tamano() > 0 && !confirmarSobrescritura(
                "Ya hay " + tabla.tamano() + " estaciones en memoria.\n"
              + "Cargar otro diccionario BORRARÁ las estaciones y conexiones actuales. ¿Continuar?")) {
            return;
        }
        try {
            grafo.limpiar();
            int n = gestor.cargarDiccionario(f.getAbsolutePath(), tabla, grafo);
            mostrarMensaje("Diccionario cargado: " + n + " estaciones (" + gestor.getLineasInvalidas() + " líneas omitidas).");
        } catch (IOException ex) {
            mostrarError("No se pudo leer el diccionario: " + ex.getMessage());
        }
    }

    private void accionCargarGrafo() {
        if (grafo.numEstaciones() == 0) {
            mostrarError("Primero cargue el diccionario de estaciones.");
            return;
        }
        File f = elegirArchivo("Seleccionar grafo.csv");
        if (f == null) return;
        if (grafo.numConexiones() > 0 && !confirmarSobrescritura(
                "Ya hay conexiones en memoria. Se reemplazarán por las del archivo. ¿Continuar?")) {
            return;
        }
        try {
            grafo.limpiarConexiones();
            int n = gestor.cargarGrafo(f.getAbsolutePath(), grafo);
            mostrarMensaje("Grafo cargado: " + n + " conexiones (" + gestor.getLineasInvalidas() + " líneas omitidas).");
            
            // Renderiza el grafo en pantalla automáticamente tras la carga
            accionMostrarGrafo();
        } catch (IOException ex) {
            mostrarError("No se pudo leer el grafo: " + ex.getMessage());
        }
    }

    private void accionGuardar() {
        JFileChooser fc = new JFileChooser(ultimaCarpeta);
        fc.setDialogTitle("Carpeta destino (se escribirán diccionario.csv y grafo.csv)");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File carpeta = fc.getSelectedFile();
        ultimaCarpeta = carpeta;
        if ((new File(carpeta, "diccionario.csv").exists() || new File(carpeta, "grafo.csv").exists())
                && !confirmarSobrescritura("Los archivos CSV existentes en esa carpeta serán SOBRESCRITOS. ¿Continuar?")) {
            return;
        }
        try {
            gestor.guardarCambiosCSV(carpeta.getAbsolutePath(), tabla, grafo);
            mostrarMensaje("Cambios guardados en " + carpeta.getAbsolutePath());
        } catch (IOException ex) {
            mostrarError("No se pudo guardar: " + ex.getMessage());
        }
    }

    private void accionMostrarGrafo() {
        if (grafo.numEstaciones() == 0) {
            mostrarError("No hay datos cargados.");
            return;
        }
        
        // Obtiene el componente de vista actualizado de GraphStream
        Component nuevoPanelGrafo = visualizador.obtenerComponenteVista();
        
        // Asigna el nuevo componente a la sección superior del JSplitPane
        panelDividido.setTopComponent(nuevoPanelGrafo);
        
        // Refresca los elementos gráficos en la ventana
        panelDividido.revalidate();
        panelDividido.repaint();
    }

    private void accionRutaMinima() {
        String origen = JOptionPane.showInputDialog(this, "ID de la estación origen:");
        if (origen == null) return;
        String destino = JOptionPane.showInputDialog(this, "ID de la estación destino:");
        if (destino == null) return;
        try {
            ListaEnlazada ruta = analizador.dijkstra(origen.trim(), destino.trim());
            if (ruta == null) {
                mostrarMensaje("No existe camino entre " + origen + " y " + destino + ".");
                return;
            }
            mostrarMensaje("Ruta mínima (" + analizador.getUltimoTiempoRuta() + "): " + listaATexto(ruta));
            visualizador.resaltarRuta(ruta);
        } catch (UnsupportedOperationException ex) {
            mostrarMensaje(ex.getMessage());
        }
    }

    private void accionComponentes() {
        try {
            ListaEnlazada comps = analizador.componentesAislados();
            mostrarMensaje("Componentes: " + comps.tamano());
            for (int i = 0; i < comps.tamano(); i++) {
                mostrarMensaje("  #" + (i + 1) + ": " + listaATexto((ListaEnlazada) comps.obtener(i)));
            }
        } catch (UnsupportedOperationException ex) {
            mostrarMensaje(ex.getMessage());
        }
    }

    private void accionHubs() {
        try {
            ListaEnlazada hubs = analizador.calcularHubs(5);
            mostrarMensaje("Hubs (top 5): " + listaATexto(hubs));
        } catch (UnsupportedOperationException ex) {
            mostrarMensaje(ex.getMessage());
        }
    }

    // ---------- utilidades ----------

    private File elegirArchivo(String titulo) {
        JFileChooser fc = new JFileChooser(ultimaCarpeta);
        fc.setDialogTitle(titulo);
        fc.setFileFilter(new FileNameExtensionFilter("Archivos CSV", "csv"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        ultimaCarpeta = f.getParentFile();
        return f;
    }

    private boolean confirmarSobrescritura(String mensaje) {
        int r = JOptionPane.showConfirmDialog(this, mensaje, "Advertencia",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return r == JOptionPane.YES_OPTION;
    }

    private String listaATexto(ListaEnlazada l) {
        StringBuilder sb = new StringBuilder();
        for (Nodo n = l.getCabeza(); n != null; n = n.getSiguiente()) {
            Object d = n.getDato();
            sb.append(d instanceof Estacion ? ((Estacion) d).getNombre() : String.valueOf(d));
            if (n.getSiguiente() != null) sb.append(" -> ");
        }
        return sb.toString();
    }

    /** Escribe en el log de la ventana (también lo usa GestorArchivo). */
    public void mostrarMensaje(String texto) {
        areaLog.append(texto + "\n");
    }

    public void mostrarError(String texto) {
        JOptionPane.showMessageDialog(this, texto, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public TablaHash getTabla() { return tabla; }
    public GrafoPonderado getGrafo() { return grafo; }
    public GestorArchivo getGestor() { return gestor; }
    public AnalizadorRed getAnalizador() { return analizador; }
    public VisualizadorGrafo getVisualizador() { return visualizador; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}