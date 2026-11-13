package urbanroute.interfaz;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.view.Camera;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;

import urbanroute.control.AnalizadorRed;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Representa gráficamente la red de transporte UrbanRoute.
 *
 * Utiliza GraphStream exclusivamente para dibujar las estaciones
 * y conexiones que obtiene del GrafoPonderado del proyecto.
 *
 * Cada línea de transporte se representa en una fila diferente
 * y utiliza un color propio. Las conexiones entre estaciones
 * de distintas líneas se identifican como transbordos.
 *
 * La clase no implementa algoritmos de grafos ni administra
 * las estructuras principales de la red.
 *
 * @author Equipo UrbanRoute
 */
public class VisualizadorGrafo {

    /** Colores asignados a las líneas de transporte. */
    private static final String[] PALETA = {
        "#4a90d9",
        "#2ca02c",
        "#ff7f0e",
        "#9467bd",
        "#17becf",
        "#d62728",
        "#8c564b",
        "#e377c2",
        "#bcbd22",
        "#7f7f7f"
    };

    /** Color utilizado para resaltar rutas y estaciones. */
    private static final String COLOR_RESALTADO = "#e8453c";

    /** Separación horizontal entre estaciones. */
    private static final double ESPACIO_X = 3.0;

    /** Separación vertical entre líneas de transporte. */
    private static final double ESPACIO_Y = 4.0;

    /** Grafo propio que contiene los datos de la red. */
    private GrafoPonderado grafo;

    /**
     * Analizador asociado a la aplicación.
     *
     * Se conserva para mantener la integración con VentanaPrincipal.
     * Los cálculos de los algoritmos no se realizan en esta clase.
     */
    private AnalizadorRed analizador;

    /** Ventana principal de UrbanRoute. */
    private VentanaPrincipal ventana;

    /** Grafo utilizado únicamente para la representación gráfica. */
    private Graph graphStream;

    /** Administrador de la vista gráfica de GraphStream. */
    private Viewer viewer;

    /** Componente gráfico que muestra la red. */
    private View view;

    /** Ventana independiente que puede mostrar el grafo. */
    private JFrame frame;

    /** Último punto utilizado para desplazar la representación gráfica. */
    private Point ultimoPunto;

    /** Panel que contiene la leyenda de las líneas de transporte. */
    private JPanel leyenda;

    /**
     * Inicializa el visualizador asociado al grafo de transporte.
     *
     * @param grafo estructura propia que contiene estaciones y conexiones
     * @param ventana ventana principal de la aplicación
     */
    public VisualizadorGrafo(
            GrafoPonderado grafo,
            VentanaPrincipal ventana
    ) {

        if (grafo == null) {
            throw new IllegalArgumentException(
                    "El grafo no puede ser nulo."
            );
        }

        this.grafo = grafo;
        this.ventana = ventana;

        System.setProperty("org.graphstream.ui", "swing");
    }

    /**
     * Construye desde cero la representación gráfica de la red.
     *
     * Crea los nodos visuales, asigna sus posiciones según la línea
     * de transporte y agrega las conexiones correspondientes.
     *
     * Este método debe ejecutarse desde el hilo de eventos de Swing
     * cuando el visualizador esté asociado a una interfaz gráfica.
     */
    public void construir() {

        cerrarViewer();

        graphStream = new MultiGraph("UrbanRoute");

        graphStream.setAttribute(
                "ui.stylesheet",
                crearEstilos()
        );

        graphStream.setAttribute("ui.quality");
        graphStream.setAttribute("ui.antialias");

        crearNodos();

        agregarAristas();
    }

    /**
     * Crea o actualiza los nodos gráficos de todas las estaciones.
     *
     * Cada línea ocupa una fila y las estaciones de una misma línea
     * se distribuyen horizontalmente.
     *
     * Este método también se utiliza durante la sincronización,
     * por lo que actualiza las etiquetas y posiciones de los nodos
     * que ya existen.
     */
    private void crearNodos() {

        ListaEnlazada<String> lineas = grafo.obtenerLineas();

        ListaEnlazada<Estacion> estaciones =
                grafo.getEstaciones();

        int fila = 0;

        Nodo<String> nodoLinea = lineas.getCabeza();

        while (nodoLinea != null) {

            String linea = nodoLinea.getDato();

            int columna = 0;

            Nodo<Estacion> nodoEstacion =
                    estaciones.getCabeza();

            while (nodoEstacion != null) {

                Estacion estacion = nodoEstacion.getDato();

                if (estacion.getLinea().equals(linea)) {

                    Node nodoVisual =
                            graphStream.getNode(estacion.getId());

                    if (nodoVisual == null) {

                        nodoVisual =
                                graphStream.addNode(estacion.getId());
                    }

                    configurarNodo(
                            nodoVisual,
                            estacion,
                            columna,
                            fila
                    );

                    columna++;
                }

                nodoEstacion = nodoEstacion.getSiguiente();
            }

            fila++;

            nodoLinea = nodoLinea.getSiguiente();
        }
    }

    /**
     * Configura los atributos visuales de una estación.
     *
     * Actualiza la etiqueta, la posición y la clase de color.
     * También ajusta la ubicación del texto para alternar su
     * alineación y evitar que las etiquetas se superpongan.
     *
     * @param nodoVisual nodo de GraphStream que se configurará
     * @param estacion estación del modelo propio
     * @param columna posición horizontal dentro de su línea
     * @param fila posición vertical de la línea
     */
    private void configurarNodo(
            Node nodoVisual,
            Estacion estacion,
            int columna,
            int fila
    ) {

        nodoVisual.setAttribute(
                "ui.label",
                estacion.getNombre()
        );

        nodoVisual.setAttribute(
                "xy",
                columna * ESPACIO_X,
                -fila * ESPACIO_Y
        );

        String clase = obtenerClaseLinea(estacion.getLinea());

        nodoVisual.setAttribute(
                "ui.class",
                clase
        );

        if (columna % 2 == 1) {

            nodoVisual.setAttribute(
                    "ui.style",
                    "text-alignment: under;"
            );

        } else {

            nodoVisual.removeAttribute("ui.style");
        }
    }

    /**
     * Agrega las conexiones del grafo propio a GraphStream.
     *
     * En grafos no dirigidos, una misma conexión puede aparecer
     * en las listas de adyacencia de ambas estaciones. Para evitar
     * dibujarla dos veces se utiliza un identificador canónico.
     *
     * En grafos dirigidos, cada sentido tiene un identificador
     * diferente, por lo que A -> B y B -> A se representan
     * como conexiones independientes.
     */
    private void agregarAristas() {

        ListaEnlazada<Estacion> estaciones =
                grafo.getEstaciones();

        Nodo<Estacion> nodoEstacion =
                estaciones.getCabeza();

        while (nodoEstacion != null) {

            Estacion estacion = nodoEstacion.getDato();

            ListaEnlazada<Conexion> conexiones =
                    estacion.getConexiones();

            Nodo<Conexion> nodoConexion =
                    conexiones.getCabeza();

            while (nodoConexion != null) {

                Conexion conexion = nodoConexion.getDato();

                String origen = conexion.getOrigen().getId();
                String destino = conexion.getDestino().getId();

                String idArista =
                        identificadorArista(origen, destino);

                if (graphStream.getEdge(idArista) == null) {

                    Edge arista = graphStream.addEdge(
                            idArista,
                            origen,
                            destino,
                            grafo.esDirigido()
                    );

                    arista.setAttribute(
                            "ui.label",
                            String.valueOf(conexion.getTiempo())
                    );

                    arista.setAttribute(
                            "ui.class",
                            obtenerClaseConexion(conexion)
                    );
                }

                nodoConexion = nodoConexion.getSiguiente();
            }

            nodoEstacion = nodoEstacion.getSiguiente();
        }
    }

    /**
     * Construye la hoja de estilos utilizada por GraphStream.
     *
     * Cada línea recibe un color de la paleta. Los transbordos
     * se muestran mediante líneas discontinuas y las rutas
     * seleccionadas utilizan un color de resaltado.
     *
     * @return hoja de estilos CSS de GraphStream
     */
    private String crearEstilos() {

        StringBuilder css = new StringBuilder();

        css.append("graph { padding: 50px; }");

        css.append(
                "node { size: 16px;"
                + " text-size: 13px;"
                + " text-alignment: above;"
                + " text-style: bold; }"
        );

        css.append(
                "edge { size: 2px;"
                + " text-size: 10px; }"
        );

        ListaEnlazada<String> lineas = grafo.obtenerLineas();

        Nodo<String> nodoLinea = lineas.getCabeza();

        int indice = 0;

        while (nodoLinea != null) {

            String color = PALETA[indice % PALETA.length];

            css.append("node.linea")
                    .append(indice)
                    .append(" { fill-color: ")
                    .append(color)
                    .append("; }");

            css.append("edge.linea")
                    .append(indice)
                    .append(" { fill-color: ")
                    .append(color)
                    .append("; size: 3px; }");

            indice++;

            nodoLinea = nodoLinea.getSiguiente();
        }

        css.append(
                "edge.transbordo {"
                + " fill-color: #555555;"
                + " stroke-mode: dashes; }"
        );

        css.append(
                "node.resaltado {"
                + " fill-color: "
                + COLOR_RESALTADO
                + "; size: 24px; }"
        );

        css.append(
                "edge.ruta {"
                + " fill-color: "
                + COLOR_RESALTADO
                + "; size: 5px; }"
        );

        return css.toString();
    }

    /**
     * Obtiene el índice que corresponde a una línea de transporte.
     *
     * El índice determina el color y la fila que utilizará la línea
     * en la representación gráfica.
     *
     * @param linea nombre de la línea que se desea localizar
     * @return posición de la línea o -1 si no existe
     */
    private int indiceLinea(String linea) {

        ListaEnlazada<String> lineas = grafo.obtenerLineas();

        Nodo<String> nodo = lineas.getCabeza();

        int indice = 0;

        while (nodo != null) {

            if (linea.equals(nodo.getDato())) {
                return indice;
            }

            indice++;

            nodo = nodo.getSiguiente();
        }

        return -1;
    }

    /**
     * Obtiene la clase visual correspondiente a una línea.
     *
     * @param linea nombre de la línea
     * @return clase CSS asignada a la línea
     */
    private String obtenerClaseLinea(String linea) {

        int indice = indiceLinea(linea);

        if (indice < 0) {
            return "linea0";
        }

        return "linea" + indice;
    }

    /**
     * Determina la clase visual de una conexión.
     *
     * Las conexiones entre estaciones de una misma línea utilizan
     * el color de dicha línea. Las conexiones entre líneas distintas
     * se identifican como transbordos.
     *
     * @param conexion conexión que se desea representar
     * @return clase CSS correspondiente a la conexión
     */
    private String obtenerClaseConexion(Conexion conexion) {

        String lineaOrigen =
                conexion.getOrigen().getLinea();

        String lineaDestino =
                conexion.getDestino().getLinea();

        if (lineaOrigen.equals(lineaDestino)) {
            return obtenerClaseLinea(lineaOrigen);
        }

        return "transbordo";
    }

    /**
     * Genera un identificador único para una conexión.
     *
     * En un grafo dirigido, conserva el orden de origen y destino.
     * En un grafo no dirigido, ordena los identificadores para que
     * ambos sentidos de una misma conexión produzcan el mismo ID.
     *
     * La longitud de cada identificador forma parte de la clave
     * para evitar ambigüedades por separadores presentes en los IDs.
     *
     * @param origen identificador de la estación de origen
     * @param destino identificador de la estación de destino
     * @return identificador de la arista gráfica
     */
    private String identificadorArista(
            String origen,
            String destino
    ) {

        String primero = origen;
        String segundo = destino;

        if (!grafo.esDirigido()
                && primero.compareTo(segundo) > 0) {

            primero = destino;
            segundo = origen;
        }

        return "AR_"
                + primero.length()
                + "_"
                + primero
                + "_"
                + segundo.length()
                + "_"
                + segundo;
    }

    /**
     * Construye y devuelve el componente Swing que contiene
     * la representación visual del grafo.
     *
     * La vista incluye botones de zoom, ajuste de cámara,
     * controles para desplazar la red y una leyenda de colores.
     *
     * @return panel Swing que contiene la representación gráfica
     */
    public Component obtenerComponenteVista() {

        cerrarVentana();

        construir();

        viewer = new SwingViewer(
                graphStream,
                Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD
        );

        viewer.disableAutoLayout();

        view = viewer.addDefaultView(false);

        Component componenteVista = (Component) view;

        instalarControlesMouse(componenteVista);

        JPanel barra = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        barra.add(
                boton(
                        "Zoom +",
                        () -> zoom(0.8)
                )
        );

        barra.add(
                boton(
                        "Zoom -",
                        () -> zoom(1.25)
                )
        );

        barra.add(
                boton(
                        "Ajustar",
                        () -> reiniciarVista()
                )
        );

        barra.add(
                new JLabel(
                        "    Rueda: zoom | "
                        + "Clic derecho o central + arrastrar: mover"
                )
        );

        leyenda = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        llenarLeyenda();

        JPanel panel = new JPanel(new BorderLayout());

        panel.add(barra, BorderLayout.NORTH);

        panel.add(componenteVista, BorderLayout.CENTER);

        panel.add(new JScrollPane(leyenda), BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Muestra el grafo en una ventana independiente.
     *
     * Al cerrar la ventana se libera el Viewer y se actualiza
     * el estado interno del visualizador.
     */
    public void mostrar() {

        cerrarVentana();

        Component panel = obtenerComponenteVista();

        final JFrame nuevaVentana =
                new JFrame("UrbanRoute - Red de estaciones");

        frame = nuevaVentana;

        nuevaVentana.setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );

        nuevaVentana.add(panel);

        nuevaVentana.setSize(950, 650);

        nuevaVentana.setLocationRelativeTo(null);

        nuevaVentana.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {

                if (frame == nuevaVentana) {
                    frame = null;
                }

                cerrarViewer();

                graphStream = null;
            }
        });

        nuevaVentana.setVisible(true);
    }

    /**
     * Cierra el Viewer de GraphStream y libera la vista actual.
     */
    private void cerrarViewer() {

        if (viewer != null) {

            viewer.close();

            viewer = null;
        }

        view = null;

        ultimoPunto = null;
    }

    /**
     * Cierra la ventana independiente del visualizador, si existe.
     *
     * También libera el Viewer asociado.
     */
    private void cerrarVentana() {

        if (frame != null) {

            JFrame ventanaAnterior = frame;

            frame = null;

            ventanaAnterior.dispose();
        }

        cerrarViewer();
    }

    /**
     * Construye la leyenda de las líneas de transporte.
     *
     * Cada línea se muestra con el color que utiliza en el grafo.
     * También se agrega una referencia visual para los transbordos.
     */
    private void llenarLeyenda() {

        if (leyenda == null) {
            return;
        }

        leyenda.removeAll();

        ListaEnlazada<String> lineas = grafo.obtenerLineas();

        Nodo<String> nodo = lineas.getCabeza();

        int indice = 0;

        while (nodo != null) {

            JLabel etiqueta = new JLabel(
                    "\u25A0 " + nodo.getDato()
            );

            etiqueta.setForeground(
                    Color.decode(
                            PALETA[indice % PALETA.length]
                    )
            );

            leyenda.add(etiqueta);

            indice++;

            nodo = nodo.getSiguiente();
        }

        JLabel transbordo = new JLabel("- - Transbordo");

        transbordo.setForeground(Color.DARK_GRAY);

        leyenda.add(transbordo);

        leyenda.revalidate();

        leyenda.repaint();
    }

    /**
     * Indica si existe una vista gráfica activa.
     *
     * No se limita a comprobar si se construyó el grafo de GraphStream:
     * verifica que el Viewer y la vista estén disponibles.
     *
     * @return true si la vista gráfica está construida y activa
     */
    public boolean estaMostrandose() {

        return graphStream != null
                && viewer != null
                && view != null;
    }

    /**
     * Sincroniza la representación visual con el grafo propio.
     *
     * Elimina los nodos que ya no existen, reconstruye las conexiones
     * y actualiza las etiquetas, posiciones, colores y leyenda.
     *
     * Si se invoca desde otro hilo, programa la actualización
     * en el hilo de eventos de Swing.
     */
    public void sincronizar() {

        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(
                    () -> sincronizar()
            );

            return;
        }

        if (graphStream == null) {
            return;
        }

        /*
         * Se eliminan primero las aristas.
         * Esto permite actualizar las conexiones sin conservar
         * referencias gráficas que ya no corresponden al modelo.
         */
        for (int i = graphStream.getEdgeCount() - 1; i >= 0; i--) {

            Edge arista = graphStream.getEdge(i);

            graphStream.removeEdge(arista);
        }

        /*
         * Se eliminan los nodos cuyas estaciones ya no existen
         * en el grafo propio.
         */
        for (int i = graphStream.getNodeCount() - 1; i >= 0; i--) {

            Node nodo = graphStream.getNode(i);

            if (grafo.buscarEstacion(nodo.getId()) == null) {

                graphStream.removeNode(nodo);
            }
        }

        /*
         * Se actualizan los estilos por si se agregaron o eliminaron
         * líneas de transporte.
         */
        graphStream.setAttribute(
                "ui.stylesheet",
                crearEstilos()
        );

        /*
         * Se actualizan todos los nodos.
         * También se recalculan sus posiciones para reflejar
         * los cambios de línea y las nuevas estaciones.
         */
        crearNodos();

        /*
         * Se vuelven a dibujar las conexiones existentes.
         */
        agregarAristas();

        llenarLeyenda();
    }

    /**
     * Crea un botón para ejecutar una acción gráfica.
     *
     * @param texto texto que mostrará el botón
     * @param accion acción que se ejecutará al pulsarlo
     * @return botón configurado
     */
    private JButton boton(String texto, Runnable accion) {

        JButton boton = new JButton(texto);

        boton.addActionListener(e -> accion.run());

        return boton;
    }

    /**
     * Instala los controles de ratón sobre la vista de GraphStream.
     *
     * La rueda permite acercar y alejar el dibujo. El botón derecho
     * o central permite desplazar la cámara mediante arrastre.
     *
     * @param componenteVista componente gráfico que recibirá los eventos
     */
    private void instalarControlesMouse(Component componenteVista) {

        MouseAdapter adaptador = new MouseAdapter() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {

                if (e.getWheelRotation() < 0) {
                    zoom(0.85);
                } else {
                    zoom(1.15);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

                if (SwingUtilities.isRightMouseButton(e)
                        || SwingUtilities.isMiddleMouseButton(e)) {

                    ultimoPunto = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {

                ultimoPunto = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {

                if (ultimoPunto == null || view == null) {
                    return;
                }

                Camera camara = view.getCamera();

                double proporcion =
                        camara.getMetrics().ratioPx2Gu;

                if (proporcion <= 0
                        || Double.isNaN(proporcion)
                        || Double.isInfinite(proporcion)) {

                    return;
                }

                Point3 centro = camara.getViewCenter();

                double desplazamientoX =
                        e.getX() - ultimoPunto.x;

                double desplazamientoY =
                        e.getY() - ultimoPunto.y;

                camara.setViewCenter(
                        centro.x - desplazamientoX / proporcion,
                        centro.y + desplazamientoY / proporcion,
                        0
                );

                ultimoPunto = e.getPoint();
            }
        };

        componenteVista.addMouseWheelListener(adaptador);

        componenteVista.addMouseListener(adaptador);

        componenteVista.addMouseMotionListener(adaptador);
    }

    /**
     * Acerca o aleja la representación gráfica.
     *
     * @param factor factor de ampliación o reducción
     */
    private void zoom(double factor) {

        if (view == null) {
            return;
        }

        Camera camara = view.getCamera();

        double porcentaje =
                camara.getViewPercent() * factor;

        if (porcentaje < 0.02) {
            porcentaje = 0.02;
        }

        if (porcentaje > 5.0) {
            porcentaje = 5.0;
        }

        camara.setViewPercent(porcentaje);
    }

    /**
     * Restablece la cámara a su posición y ampliación inicial.
     */
    private void reiniciarVista() {

        if (view == null) {
            return;
        }

        view.getCamera().resetView();
    }

    /**
     * Resalta visualmente una estación por su identificador.
     *
     * @param id identificador de la estación
     */
    public void resaltarNodo(String id) {

        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(
                    () -> resaltarNodo(id)
            );

            return;
        }

        if (graphStream == null || id == null) {
            return;
        }

        Node nodo = graphStream.getNode(id);

        if (nodo != null) {

            nodo.setAttribute(
                    "ui.class",
                    "resaltado"
            );
        }
    }

    /**
     * Resalta las estaciones y conexiones de una ruta.
     *
     * Recibe una lista enlazada de estaciones ordenadas según
     * el recorrido calculado por el algoritmo de Dijkstra.
     *
     * Para grafos dirigidos, las conexiones se buscan respetando
     * el sentido de desplazamiento de cada tramo.
     *
     * @param rutaEstaciones estaciones que forman la ruta
     */
    public void resaltarRuta(
            ListaEnlazada<Estacion> rutaEstaciones
    ) {

        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(
                    () -> resaltarRuta(rutaEstaciones)
            );

            return;
        }

        if (graphStream == null || rutaEstaciones == null) {
            return;
        }

        limpiarResaltado();

        Estacion anterior = null;

        Nodo<Estacion> nodo =
                rutaEstaciones.getCabeza();

        while (nodo != null) {

            Estacion actual = nodo.getDato();

            resaltarNodo(actual.getId());

            if (anterior != null) {

                Edge arista = buscarArista(
                        anterior.getId(),
                        actual.getId()
                );

                if (arista != null) {

                    arista.setAttribute(
                            "ui.class",
                            "ruta"
                    );
                }
            }

            anterior = actual;

            nodo = nodo.getSiguiente();
        }
    }

    /**
     * Restablece los colores originales de las estaciones
     * y conexiones.
     *
     * Los colores se recalculan a partir de las líneas y los
     * extremos de cada conexión del grafo propio.
     */
    public void limpiarResaltado() {

        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(
                    () -> limpiarResaltado()
            );

            return;
        }

        if (graphStream == null) {
            return;
        }

        ListaEnlazada<Estacion> estaciones =
                grafo.getEstaciones();

        Nodo<Estacion> nodoEstacion =
                estaciones.getCabeza();

        while (nodoEstacion != null) {

            Estacion estacion = nodoEstacion.getDato();

            Node nodoVisual =
                    graphStream.getNode(estacion.getId());

            if (nodoVisual != null) {

                nodoVisual.setAttribute(
                        "ui.class",
                        obtenerClaseLinea(estacion.getLinea())
                );
            }

            ListaEnlazada<Conexion> conexiones =
                    estacion.getConexiones();

            Nodo<Conexion> nodoConexion =
                    conexiones.getCabeza();

            while (nodoConexion != null) {

                Conexion conexion = nodoConexion.getDato();

                Edge arista = buscarArista(
                        conexion.getOrigen().getId(),
                        conexion.getDestino().getId()
                );

                if (arista != null) {

                    arista.setAttribute(
                            "ui.class",
                            obtenerClaseConexion(conexion)
                    );
                }

                nodoConexion = nodoConexion.getSiguiente();
            }

            nodoEstacion = nodoEstacion.getSiguiente();
        }
    }

    /**
     * Busca la representación gráfica de una conexión.
     *
     * En grafos dirigidos solamente busca el sentido solicitado.
     * En grafos no dirigidos utiliza el identificador canónico,
     * por lo que ambos sentidos corresponden a la misma arista.
     *
     * @param origen identificador de la estación inicial
     * @param destino identificador de la estación final
     * @return arista encontrada o null si no existe
     */
    private Edge buscarArista(
            String origen,
            String destino
    ) {

        if (graphStream == null) {
            return null;
        }

        String id = identificadorArista(origen, destino);

        return graphStream.getEdge(id);
    }

    /**
     * Obtiene el grafo propio utilizado como fuente de información.
     *
     * @return grafo ponderado del sistema
     */
    public GrafoPonderado getGrafo() {
        return grafo;
    }

    /**
     * Obtiene el analizador asociado a la aplicación.
     *
     * @return analizador de la red
     */
    public AnalizadorRed getAnalizador() {
        return analizador;
    }

    /**
     * Establece el analizador asociado a la aplicación.
     *
     * Este método permite conservar la compatibilidad con
     * VentanaPrincipal. El visualizador no ejecuta los algoritmos.
     *
     * @param analizador analizador que se asociará a la aplicación
     */
    public void setAnalizador(AnalizadorRed analizador) {
        this.analizador = analizador;
    }

    /**
     * Obtiene la ventana principal asociada al visualizador.
     *
     * @return ventana principal de UrbanRoute
     */
    public VentanaPrincipal getVentana() {
        return ventana;
    }
}
