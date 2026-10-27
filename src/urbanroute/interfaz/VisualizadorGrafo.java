package urbanroute.interfaz;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.camera.Camera;

import urbanroute.control.AnalizadorRed;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Renderiza el GrafoPonderado propio con GraphStream (solo como biblioteca de dibujo;
 * los datos y algoritmos siguen en las estructuras propias).
 *
 * - Cada línea (L1, L2, ...) tiene su propio color; las conexiones entre líneas
 *   distintas (transbordos) se dibujan punteadas en gris oscuro.
 * - Zoom: rueda del mouse o botones Zoom + / Zoom -. "Ajustar" restablece la vista.
 * - Mover la vista: clic derecho (o central) y arrastrar.
 * Requiere gs-core y gs-ui-swing (GraphStream 2.0).
 */
public class VisualizadorGrafo {
    private static final String[] PALETA = {
        "#4a90d9", "#2ca02c", "#ff7f0e", "#9467bd", "#17becf",
        "#d62728", "#8c564b", "#e377c2", "#bcbd22", "#7f7f7f"
    };
    private static final String COLOR_RESALTADO = "#e8453c";

    private GrafoPonderado grafo;
    private AnalizadorRed analizador;
    private VentanaPrincipal ventana;

    private Graph graphStream;
    private Viewer viewer;
    private View view;
    private JFrame frame;
    private Point ultimoPunto;

    public VisualizadorGrafo(GrafoPonderado grafo, VentanaPrincipal ventana) {
        this.grafo = grafo;
        this.ventana = ventana;
        System.setProperty("org.graphstream.ui", "swing");
    }

    // ---------- construcción del grafo visual ----------

    /** Reconstruye el grafo visual a partir del grafo en memoria. */
    public void construir() {
        graphStream = new SingleGraph("UrbanRoute");
        graphStream.setAttribute("ui.stylesheet", crearEstilos());
        graphStream.setAttribute("ui.quality");
        graphStream.setAttribute("ui.antialias");

        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion e = (Estacion) n.getDato();
            Node nodo = graphStream.addNode(e.getId());
            nodo.setAttribute("ui.label", e.getNombre());
            String clase = "linea" + indiceLinea(e.getLinea());
            nodo.setAttribute("clase.base", clase);
            nodo.setAttribute("ui.class", clase);
        }
        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion e = (Estacion) n.getDato();
            for (Nodo m = e.getConexiones().getCabeza(); m != null; m = m.getSiguiente()) {
                Conexion c = (Conexion) m.getDato();
                String a = c.getOrigen().getId();
                String b = c.getDestino().getId();
                if (buscarArista(a, b) == null) {
                    Edge arista = graphStream.addEdge(a + "--" + b, a, b, grafo.esDirigido());
                    arista.setAttribute("ui.label", String.valueOf(c.getTiempo()));
                    String clase = c.getOrigen().getLinea().equals(c.getDestino().getLinea())
                            ? "linea" + indiceLinea(c.getOrigen().getLinea())
                            : "transbordo";
                    arista.setAttribute("clase.base", clase);
                    arista.setAttribute("ui.class", clase);
                }
            }
        }
    }

    /** Hoja de estilos con una clase de color por cada línea distinta (sin colecciones de Java). */
    private String crearEstilos() {
        StringBuilder css = new StringBuilder();
        css.append("graph { padding: 50px; }");
        css.append("node { size: 16px; text-size: 13px; text-alignment: above; text-style: bold; }");
        css.append("edge { size: 2px; text-size: 10px; }");
        int i = 0;
        for (Nodo n = lineasDistintas().getCabeza(); n != null; n = n.getSiguiente()) {
            String color = PALETA[i % PALETA.length];
            css.append("node.linea").append(i).append(" { fill-color: ").append(color).append("; }");
            css.append("edge.linea").append(i).append(" { fill-color: ").append(color).append("; size: 3px; }");
            i++;
        }
        css.append("edge.transbordo { fill-color: #555555; stroke-mode: dashes; }");
        css.append("node.resaltado { fill-color: " + COLOR_RESALTADO + "; size: 24px; }");
        css.append("edge.ruta { fill-color: " + COLOR_RESALTADO + "; size: 5px; }");
        return css.toString();
    }

    /** Líneas distintas en orden de aparición (ListaEnlazada de String). */
    private ListaEnlazada lineasDistintas() {
        ListaEnlazada lineas = new ListaEnlazada();
        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            String linea = ((Estacion) n.getDato()).getLinea();
            if (lineas.buscar(linea) == null) {
                lineas.insertar(linea);
            }
        }
        return lineas;
    }

    private int indiceLinea(String linea) {
        int i = 0;
        for (Nodo n = lineasDistintas().getCabeza(); n != null; n = n.getSiguiente()) {
            if (linea.equals(n.getDato())) return i;
            i++;
        }
        return 0;
    }

    // ---------- ventana con zoom ----------

    /**
     * Construye el grafo visual y devuelve un panel listo para incrustar en cualquier
     * contenedor Swing (p. ej. el JSplitPane de VentanaPrincipal). Incluye la barra de
     * zoom, la vista de GraphStream y la leyenda de colores por línea.
     * Cada llamada reconstruye el grafo desde la memoria y cierra la vista anterior.
     */
    public Component obtenerComponenteVista() {
        cerrarViewer();
        construir();

        viewer = new SwingViewer(graphStream, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();
        view = viewer.addDefaultView(false);
        Component vista = (Component) view;
        instalarControlesMouse(vista);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barra.add(boton("Zoom +", () -> zoom(0.8)));
        barra.add(boton("Zoom -", () -> zoom(1.25)));
        barra.add(boton("Ajustar", () -> reiniciarVista()));
        barra.add(new JLabel("   Rueda: zoom  |  Clic derecho + arrastrar: mover"));

        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        int i = 0;
        for (Nodo n = lineasDistintas().getCabeza(); n != null; n = n.getSiguiente()) {
            JLabel l = new JLabel("\u25A0 " + n.getDato());
            l.setForeground(Color.decode(PALETA[i % PALETA.length]));
            leyenda.add(l);
            i++;
        }
        JLabel transb = new JLabel("- - transbordo");
        transb.setForeground(Color.DARK_GRAY);
        leyenda.add(transb);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(barra, BorderLayout.NORTH);
        panel.add(vista, BorderLayout.CENTER);
        panel.add(leyenda, BorderLayout.SOUTH);
        return panel;
    }

    /** Alternativa: muestra el grafo en una ventana independiente. */
    public void mostrar() {
        cerrarVentana();
        Component panel = obtenerComponenteVista();
        frame = new JFrame("UrbanRoute - Red de estaciones");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(panel);
        frame.setSize(950, 650);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void cerrarViewer() {
        if (viewer != null) {
            viewer.close();
            viewer = null;
        }
        view = null;
    }

    private void cerrarVentana() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        return b;
    }

    private void instalarControlesMouse(Component vista) {
        MouseAdapter adaptador = new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoom(e.getWheelRotation() < 0 ? 0.85 : 1.15);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                    ultimoPunto = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ultimoPunto = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (ultimoPunto == null || view == null) return;
                Camera cam = view.getCamera();
                double ratio = cam.getMetrics().ratioPx2Gu;
                if (ratio <= 0) return;
                Point3 c = cam.getViewCenter();
                cam.setViewCenter(
                        c.x - (e.getX() - ultimoPunto.x) / ratio,
                        c.y + (e.getY() - ultimoPunto.y) / ratio,
                        0);
                ultimoPunto = e.getPoint();
            }
        };
        vista.addMouseWheelListener(adaptador);
        vista.addMouseListener(adaptador);
        vista.addMouseMotionListener(adaptador);
    }

    /** factor < 1 acerca (zoom in); factor > 1 aleja (zoom out). */
    private void zoom(double factor) {
        if (view == null) return;
        Camera cam = view.getCamera();
        double p = cam.getViewPercent() * factor;
        if (p < 0.02) p = 0.02;
        if (p > 5.0) p = 5.0;
        cam.setViewPercent(p);
    }

    private void reiniciarVista() {
        if (view == null) return;
        view.getCamera().resetView();
    }

    // ---------- resaltado ----------

    public void resaltarNodo(String id) {
        if (graphStream == null) return;
        Node n = graphStream.getNode(id);
        if (n != null) {
            n.setAttribute("ui.class", "resaltado");
        }
    }

    /** Resalta nodos y aristas de una ruta (ListaEnlazada de Estacion en orden). */
    public void resaltarRuta(ListaEnlazada rutaEstaciones) {
        if (graphStream == null || rutaEstaciones == null) return;
        limpiarResaltado();
        Estacion previa = null;
        for (Nodo n = rutaEstaciones.getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion actual = (Estacion) n.getDato();
            resaltarNodo(actual.getId());
            if (previa != null) {
                Edge arista = buscarArista(previa.getId(), actual.getId());
                if (arista != null) {
                    arista.setAttribute("ui.class", "ruta");
                }
            }
            previa = actual;
        }
    }

    /** Devuelve nodos y aristas a su color de línea. */
    public void limpiarResaltado() {
        if (graphStream == null) return;
        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion e = (Estacion) n.getDato();
            Node nodo = graphStream.getNode(e.getId());
            if (nodo != null) nodo.setAttribute("ui.class", (String) nodo.getAttribute("clase.base"));
            for (Nodo m = e.getConexiones().getCabeza(); m != null; m = m.getSiguiente()) {
                Conexion c = (Conexion) m.getDato();
                Edge arista = buscarArista(c.getOrigen().getId(), c.getDestino().getId());
                if (arista != null) arista.setAttribute("ui.class", (String) arista.getAttribute("clase.base"));
            }
        }
    }

    private Edge buscarArista(String a, String b) {
        Edge e = graphStream.getEdge(a + "--" + b);
        return e != null ? e : graphStream.getEdge(b + "--" + a);
    }

    public GrafoPonderado getGrafo() { return grafo; }
    public AnalizadorRed getAnalizador() { return analizador; }
    public void setAnalizador(AnalizadorRed analizador) { this.analizador = analizador; }
    public VentanaPrincipal getVentana() { return ventana; }
}