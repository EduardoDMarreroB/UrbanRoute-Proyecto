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
 * Dibuja el grafo con GraphStream. Cada linea va en su propia fila y tiene su color.
 * Los datos y los algoritmos siguen siendo los de las estructuras propias.
 */
public class VisualizadorGrafo {
    private static final String[] PALETA = {
        "#4a90d9", "#2ca02c", "#ff7f0e", "#9467bd", "#17becf",
        "#d62728", "#8c564b", "#e377c2", "#bcbd22", "#7f7f7f"
    };
    private static final String COLOR_RESALTADO = "#e8453c";
    private static final double ESPACIO_X = 3;
    private static final double ESPACIO_Y = 4;

    private GrafoPonderado grafo;
    private AnalizadorRed analizador;
    private VentanaPrincipal ventana;

    private Graph graphStream;
    private Viewer viewer;
    private View view;
    private JFrame frame;
    private Point ultimoPunto;
    private JPanel leyenda;

    public VisualizadorGrafo(GrafoPonderado grafo, VentanaPrincipal ventana) {
        this.grafo = grafo;
        this.ventana = ventana;
        System.setProperty("org.graphstream.ui", "swing");
    }

    /** Arma el grafo de GraphStream desde cero: una fila por linea y una arista por conexion. */
    public void construir() {
        graphStream = new SingleGraph("UrbanRoute");
        graphStream.setAttribute("ui.stylesheet", crearEstilos());
        graphStream.setAttribute("ui.quality");
        graphStream.setAttribute("ui.antialias");

        int fila = 0;
        for (Nodo l = grafo.obtenerLineas().getCabeza(); l != null; l = l.getSiguiente()) {
            String linea = (String) l.getDato();
            int columna = 0;
            for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
                Estacion e = (Estacion) n.getDato();
                if (!e.getLinea().equals(linea)) continue;

                Node nodo = graphStream.addNode(e.getId());
                nodo.setAttribute("ui.label", e.getNombre());
                nodo.setAttribute("xy", columna * ESPACIO_X, -fila * ESPACIO_Y);
                if (columna % 2 == 1) {
                    nodo.setAttribute("ui.style", "text-alignment: under;");
                }
                String clase = "linea" + fila;
                nodo.setAttribute("clase.base", clase);
                nodo.setAttribute("ui.class", clase);
                columna++;
            }
            fila++;
        }
        agregarAristas();
    }

    private void agregarAristas() {
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

    private String crearEstilos() {
        StringBuilder css = new StringBuilder();
        css.append("graph { padding: 50px; }");
        css.append("node { size: 16px; text-size: 13px; text-alignment: above; text-style: bold; }");
        css.append("edge { size: 2px; text-size: 10px; }");
        int i = 0;
        for (Nodo n = grafo.obtenerLineas().getCabeza(); n != null; n = n.getSiguiente()) {
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

    private int indiceLinea(String linea) {
        int i = 0;
        for (Nodo n = grafo.obtenerLineas().getCabeza(); n != null; n = n.getSiguiente()) {
            if (linea.equals(n.getDato())) return i;
            i++;
        }
        return 0;
    }

    /**
     * Construye el grafo y devuelve un panel con los botones de zoom, el dibujo y la leyenda.
     * Cada llamada vuelve a dibujar todo desde cero.
     */
    public Component obtenerComponenteVista() {
        cerrarViewer();
        construir();

        viewer = new SwingViewer(graphStream, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        viewer.disableAutoLayout();
        view = viewer.addDefaultView(false);
        Component vista = (Component) view;
        instalarControlesMouse(vista);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barra.add(boton("Zoom +", () -> zoom(0.8)));
        barra.add(boton("Zoom -", () -> zoom(1.25)));
        barra.add(boton("Ajustar", () -> reiniciarVista()));
        barra.add(new JLabel("    Rueda: zoom  |  Clic derecho + arrastrar: mover"));

        leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        llenarLeyenda();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(barra, BorderLayout.NORTH);
        panel.add(vista, BorderLayout.CENTER);
        panel.add(leyenda, BorderLayout.SOUTH);
        return panel;
    }

    /** Muestra el grafo en una ventana aparte. */
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

    private void llenarLeyenda() {
        if (leyenda == null) return;
        leyenda.removeAll();
        int i = 0;
        for (Nodo n = grafo.obtenerLineas().getCabeza(); n != null; n = n.getSiguiente()) {
            JLabel etiqueta = new JLabel("\u25A0 " + n.getDato());
            etiqueta.setForeground(Color.decode(PALETA[i % PALETA.length]));
            leyenda.add(etiqueta);
            i++;
        }
        JLabel transbordo = new JLabel("- - transbordo");
        transbordo.setForeground(Color.DARK_GRAY);
        leyenda.add(transbordo);
        leyenda.revalidate();
        leyenda.repaint();
    }

    public boolean estaMostrandose() {
        return graphStream != null;
    }

    /**
     * Actualiza el dibujo despues de agregar o eliminar estaciones y conexiones.
     * Las estaciones que ya estaban dibujadas conservan su posicion.
     * Si el grafo todavia no se ha mostrado, no hace nada.
     */
    public void sincronizar() {
        if (graphStream == null) return;

        for (int i = graphStream.getNodeCount() - 1; i >= 0; i--) {
            Node nodo = graphStream.getNode(i);
            if (grafo.buscarEstacion(nodo.getId()) == null) {
                graphStream.removeNode(nodo);
            }
        }
        for (int i = graphStream.getEdgeCount() - 1; i >= 0; i--) {
            graphStream.removeEdge(graphStream.getEdge(i));
        }
        graphStream.setAttribute("ui.stylesheet", crearEstilos());

        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion e = (Estacion) n.getDato();
            int fila = indiceLinea(e.getLinea());
            Node nodo = graphStream.getNode(e.getId());
            if (nodo == null) {
                int columna = posicionEnLinea(e);
                nodo = graphStream.addNode(e.getId());
                nodo.setAttribute("ui.label", e.getNombre());
                nodo.setAttribute("xy", columna * ESPACIO_X, -fila * ESPACIO_Y);
                if (columna % 2 == 1) {
                    nodo.setAttribute("ui.style", "text-alignment: under;");
                }
            }
            String clase = "linea" + fila;
            nodo.setAttribute("clase.base", clase);
            nodo.setAttribute("ui.class", clase);
        }
        agregarAristas();
        llenarLeyenda();
    }

    private int posicionEnLinea(Estacion estacion) {
        int posicion = 0;
        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion e = (Estacion) n.getDato();
            if (e.equals(estacion)) return posicion;
            if (e.getLinea().equals(estacion.getLinea())) posicion++;
        }
        return posicion;
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

    public void resaltarNodo(String id) {
        if (graphStream == null) return;
        Node n = graphStream.getNode(id);
        if (n != null) {
            n.setAttribute("ui.class", "resaltado");
        }
    }

    /** Pinta en rojo las estaciones y conexiones de una ruta (lista de estaciones en orden). */
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

    /** Devuelve estaciones y conexiones a su color de linea. */
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
