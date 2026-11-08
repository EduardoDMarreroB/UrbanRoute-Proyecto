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
 * Renderiza el GrafoPonderado propio con GraphStream.
 */
public class VisualizadorGrafo {
    private static final String[] PALETA = {
        "#4a90d9", "#2ca02c", "#ff7f0e", "#9467bd", "#17becf",
        "#d62728", "#8c564b", "#e377c2", "#bcbd22", "#7f7f7f"
    };
    private static final String COLOR_RESALTADO = "#e8453c";
    private static final double ESPACIO_X = 3;   // separación entre estaciones de una línea
    private static final double ESPACIO_Y = 4;   // separación entre líneas

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

    // ---------- Construcción del grafo visual ----------

    /**
     * Construye el grafo de GraphStream desde cero a partir de la memoria:
     * una fila por línea, un nodo por estación y una arista por conexión.
     */
    public void construir() {
        graphStream = new SingleGraph("UrbanRoute");
        graphStream.setAttribute("ui.stylesheet", crearEstilos());
        graphStream.setAttribute("ui.quality");
        graphStream.setAttribute("ui.antialias");

        // Cada línea ocupa su propia fila; sus estaciones van de izquierda a derecha
        // en el orden en que aparecen en diccionario.csv. Luego se pueden mover a mano.
        int fila = 0;
        for (Nodo l = lineasDistintas().getCabeza(); l != null; l = l.getSiguiente()) {
            String linea = (String) l.getDato();
            int columna = 0;
            for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; n = n.getSiguiente()) {
                Estacion e = (Estacion) n.getDato();
                if (!e.getLinea().equals(linea)) continue;

                Node nodo = graphStream.addNode(e.getId());
                nodo.setAttribute("ui.label", e.getNombre());
                nodo.setAttribute("xy", columna * ESPACIO_X, -fila * ESPACIO_Y);
                if (columna % 2 == 1) {
                    // Etiquetas alternas (arriba/abajo) para que no se pisen
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

    /** Crea en el dibujo una arista por cada conexión del modelo (sin duplicar las de ida y vuelta). */
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

    private ListaEnlazada lineasDistintas() {
        return grafo.obtenerLineas();
    }

    private int indiceLinea(String linea) {
        int i = 0;
        for (Nodo n = lineasDistintas().getCabeza(); n != null; n = n.getSiguiente()) {
            if (linea.equals(n.getDato())) return i;
            i++;
        }
        return 0;
    }

    // ---------- Componentes de Swing ----------

    /**
     * Construye el grafo y devuelve un panel Swing (barra de zoom + dibujo + leyenda)
     * listo para incrustar en la ventana principal. Cada llamada reconstruye el dibujo
     * (las posiciones movidas a mano se pierden; para altas y bajas use {@link #sincronizar()}).
     * @return panel con el grafo
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

    /** Rellena la leyenda con un cuadro de color por línea y el aviso de transbordo. */
    private void llenarLeyenda() {
        if (leyenda == null) return;
        leyenda.removeAll();
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
        leyenda.revalidate();
        leyenda.repaint();
    }

    /** @return true si el grafo ya se construyó en pantalla */
    public boolean estaMostrandose() {
        return graphStream != null;
    }

    /**
     * Pone el dibujo al día con la memoria después de altas o bajas, SIN reconstruirlo:
     * las estaciones que ya estaban conservan la posición que el usuario les dio.
     * Si el grafo todavía no se ha mostrado, no hace nada.
     */
    public void sincronizar() {
        if (graphStream == null) return;

        // 1) Quitar las estaciones que ya no existen (GraphStream borra también sus aristas)
        for (int i = graphStream.getNodeCount() - 1; i >= 0; i--) {
            Node nodo = graphStream.getNode(i);
            if (grafo.buscarEstacion(nodo.getId()) == null) {
                graphStream.removeNode(nodo);
            }
        }
        // 2) Quitar todas las aristas; se vuelven a crear desde el modelo
        for (int i = graphStream.getEdgeCount() - 1; i >= 0; i--) {
            graphStream.removeEdge(graphStream.getEdge(i));
        }
        // 3) Estilos al día (puede haber aparecido o desaparecido una línea)
        graphStream.setAttribute("ui.stylesheet", crearEstilos());

        // 4) Agregar las estaciones nuevas y refrescar el color de todas
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

        // 5) Aristas y leyenda
        agregarAristas();
        llenarLeyenda();
    }

    /** Lugar (0, 1, 2...) que ocupa la estación entre las de su misma línea. */
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

    // Controles de zoom y navegación con mouse
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

    // ---------- Resaltado de rutas ----------

    public void resaltarNodo(String id) {
        if (graphStream == null) return;
        Node n = graphStream.getNode(id);
        if (n != null) {
            n.setAttribute("ui.class", "resaltado");
        }
    }

    /**
     * Pinta en rojo los nodos y aristas de una ruta.
     * @param rutaEstaciones ListaEnlazada de Estacion en orden de recorrido
     */
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
