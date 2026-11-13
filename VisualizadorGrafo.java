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
 */
public class VisualizadorGrafo {

    // Paleta de colores predefinida para asignar a las líneas de transporte
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

    private static final String COLOR_RESALTADO = "#e8453c"; // Color para destacar rutas
    private static final double ESPACIO_X = 3.0; // Separación horizontal entre estaciones
    private static final double ESPACIO_Y = 4.0; // Separación vertical entre líneas

    private GrafoPonderado grafo; // Estructura propia con los datos de la red
    private AnalizadorRed analizador; // Analizador asociado a la aplicación
    private VentanaPrincipal ventana; // Ventana principal de la interfaz
    private Graph graphStream; // Grafo visual utilizado por GraphStream
    private Viewer viewer; // Administrador de la vista gráfica
    private View view; // Componente visual principal
    private JFrame frame; // Ventana independiente para mostrar el grafo
    private Point ultimoPunto; // Último punto para desplazar la vista con el mouse
    private JPanel leyenda; // Panel contenedor de la leyenda de colores

    /**
     * Constructor: inicializa el visualizador validando que el grafo exista.
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

        // Configuramos la propiedad de UI para GraphStream con Swing
        System.setProperty("org.graphstream.ui", "swing");
    }

    /**
     * Construye desde cero la representación gráfica de la red.
     */
    public void construir() {

        // Cierra cualquier vista anterior activa
        cerrarViewer();

        // Inicializa un nuevo grafo múltiple en GraphStream
        graphStream = new MultiGraph("UrbanRoute");

        // Asigna la hoja de estilos CSS generada
        graphStream.setAttribute(
                "ui.stylesheet",
                crearEstilos()
        );

        // Activa mejoras visuales de calidad y suavizado
        graphStream.setAttribute("ui.quality");
        graphStream.setAttribute("ui.antialias");

        // Crea los nodos y las aristas correspondientes
        crearNodos();
        agregarAristas();
    }

    /**
     * Crea o actualiza los nodos gráficos de todas las estaciones.
     */
    private void crearNodos() {

        ListaEnlazada<String> lineas = grafo.obtenerLineas();
        ListaEnlazada<Estacion> estaciones = grafo.getEstaciones();

        int fila = 0;
        Nodo<String> nodoLinea = lineas.getCabeza();

        // Recorre cada línea de transporte disponible
        while (nodoLinea != null) {

            String linea = nodoLinea.getDato();
            int columna = 0;
            Nodo<Estacion> nodoEstacion = estaciones.getCabeza();

            // Recorre todas las estaciones buscando las que pertenezcan a esta línea
            while (nodoEstacion != null) {

                Estacion estacion = nodoEstacion.getDato();

                if (estacion.getLinea().equals(linea)) {

                    Node nodoVisual = graphStream.getNode(estacion.getId());

                    // Si el nodo visual no existe, lo añade al grafo
                    if (nodoVisual == null) {
                        nodoVisual = graphStream.addNode(estacion.getId());
                    }

                    // Configura posición, etiqueta y estilo del nodo
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
     * Configura los atributos visuales, posición y estilo de una estación.
     */
    private void configurarNodo(
            Node nodoVisual,
            Estacion estacion,
            int columna,
            int fila
    ) {

        // Asigna el nombre de la estación como etiqueta visible
        nodoVisual.setAttribute(
                "ui.label",
                estacion.getNombre()
        );

        // Define las coordenadas espaciales en la cuadrícula
        nodoVisual.setAttribute(
                "xy",
                columna * ESPACIO_X,
                -fila * ESPACIO_Y
        );

        // Obtiene la clase de color según la línea
        String clase = obtenerClaseLinea(estacion.getLinea());

        nodoVisual.setAttribute(
                "ui.class",
                clase
        );

        // Alterna la posición del texto para evitar superposiciones
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
     */
    private void agregarAristas() {

        ListaEnlazada<Estacion> estaciones = grafo.getEstaciones();
        Nodo<Estacion> nodoEstacion = estaciones.getCabeza();

        // Recorre cada estación y sus conexiones asociadas
        while (nodoEstacion != null) {

            Estacion estacion = nodoEstacion.getDato();
            ListaEnlazada<Conexion> conexiones = estacion.getConexiones();
            Nodo<Conexion> nodoConexion = conexiones.getCabeza();

            while (nodoConexion != null) {

                Conexion conexion = nodoConexion.getDato();

                String origen = conexion.getOrigen().getId();
                String destino = conexion.getDestino().getId();

                // Genera el identificador único para la arista
                String idArista = identificadorArista(origen, destino);

                // Si la arista no ha sido dibujada, se añade
                if (graphStream.getEdge(idArista) == null) {

                    Edge arista = graphStream.addEdge(
                            idArista,
                            origen,
                            destino,
                            grafo.esDirigido()
                    );

                    // Muestra el tiempo de viaje como etiqueta de la arista
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
     * Construye la hoja de estilos CSS utilizada por GraphStream.
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

        // Asigna colores dinámicos a cada línea de transporte en el CSS
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

        // Estilos para transbordos y elementos resaltados
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
     * Obtiene el índice numérico que corresponde a una línea de transporte.
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
     * Obtiene la clase visual CSS correspondiente a una línea.
     */
    private String obtenerClaseLinea(String linea) {

        int indice = indiceLinea(linea);

        if (indice < 0) {
            return "linea0";
        }

        return "linea" + indice;
    }

    /**
     * Determina la clase visual de una conexión (línea o transbordo).
     */
    private String obtenerClaseConexion(Conexion conexion) {

        String lineaOrigen = conexion.getOrigen().getLinea();
        String lineaDestino = conexion.getDestino().getLinea();

        // Si ambas estaciones son de la misma línea, usa su color
        if (lineaOrigen.equals(lineaDestino)) {
            return obtenerClaseLinea(lineaOrigen);
        }

        return "transbordo";
    }

    /**
     * Genera un identificador único y canónico para una conexión.
     */
    private String identificadorArista(
            String origen,
            String destino
    ) {

        String primero = origen;
        String segundo = destino;

        // Ordena alfabéticamente si no es dirigido para evitar duplicados
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
     * Construye y devuelve el componente Swing con la vista gráfica.
     */
    public Component obtenerComponenteVista() {

        cerrarVentana();
        construir();

        // Crea el visor gráfico ejecutándose en el hilo de la GUI
        viewer = new SwingViewer(
                graphStream,
                Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD
        );

        viewer.disableAutoLayout();

        view = viewer.addDefaultView(false);

        Component componenteVista = (Component) view;

        // Instala los controles de zoom y movimiento con el ratón
        instalarControlesMouse(componenteVista);

        JPanel barra = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        // Agrega botones de control de zoom y ajuste a la barra superior
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
     * Muestra el grafo en una ventana independiente de Swing.
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

        // Controla eventos de cierre para liberar recursos correctamente
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
     * Cierra el Viewer de GraphStream y libera los recursos de la vista.
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
     * Cierra la ventana independiente del visualizador y libera el Viewer.
     */
    private void cerrarVentana() {

        if (frame != null) {

            JFrame ventanaAnterior = frame;

            frame = null;

            ventanaAnterior.dispose(); // Cierra y destruye la ventana
        }

        cerrarViewer();
    }

    /**
     * Construye la leyenda visual con los colores de cada línea y los transbordos.
     */
    private void llenarLeyenda() {

        if (leyenda == null) {
            return;
        }

        leyenda.removeAll(); // Limpia elementos anteriores de la leyenda

        ListaEnlazada<String> lineas = grafo.obtenerLineas();
        Nodo<String> nodo = lineas.getCabeza();
        int indice = 0;

        // Añade una etiqueta de color por cada línea disponible
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

        // Agrega la referencia visual para los transbordos
        JLabel transbordo = new JLabel("- - Transbordo");

        transbordo.setForeground(Color.DARK_GRAY);

        leyenda.add(transbordo);

        leyenda.revalidate();
        leyenda.repaint();
    }

    /**
     * Comprueba si la vista gráfica se encuentra activa y disponible.
     */
    public boolean estaMostrandose() {

        return graphStream != null
                && viewer != null
                && view != null;
    }

    /**
     * Sincroniza la representación visual con los cambios del grafo.
     */
    public void sincronizar() {

        // Asegura que la sincronización corra en el hilo de Swing
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
         * Se eliminan primero las aristas para evitar referencias
         * obsoletas antes de actualizar el modelo visual.
         */
        for (int i = graphStream.getEdgeCount() - 1; i >= 0; i--) {

            Edge arista = graphStream.getEdge(i);

            graphStream.removeEdge(arista);
        }

        /*
         * Se eliminan los nodos cuyas estaciones ya no existen
         * en el grafo propio del sistema.
         */
        for (int i = graphStream.getNodeCount() - 1; i >= 0; i--) {

            Node nodo = graphStream.getNode(i);

            if (grafo.buscarEstacion(nodo.getId()) == null) {
                graphStream.removeNode(nodo);
            }
        }

        /*
         * Actualiza los estilos por si hubo modificaciones en las líneas.
         */
        graphStream.setAttribute(
                "ui.stylesheet",
                crearEstilos()
        );

        // Reconstruye nodos y recalcula posiciones actuales
        crearNodos();

        // Vuelve a dibujar las conexiones vigentes
        agregarAristas();

        llenarLeyenda();
    }

    /**
     * Crea un botón de interfaz para ejecutar acciones gráficas.
     */
    private JButton boton(String texto, Runnable accion) {

        JButton boton = new JButton(texto);

        // Ejecuta la acción asignada al presionar el botón
        boton.addActionListener(e -> accion.run());

        return boton;
    }

    /**
     * Configura los eventos del ratón para permitir zoom y desplazamiento.
     */
    private void instalarControlesMouse(Component componenteVista) {

        MouseAdapter adaptador = new MouseAdapter() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {

                // Controla el acercamiento o alejamiento con la rueda del ratón
                if (e.getWheelRotation() < 0) {
                    zoom(0.85);
                } else {
                    zoom(1.15);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

                // Captura el punto inicial al arrastrar con botón derecho o central
                if (SwingUtilities.isRightMouseButton(e)
                        || SwingUtilities.isMiddleMouseButton(e)) {

                    ultimoPunto = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ultimoPunto = null; // Libera el punto al soltar el botón
            }

            @Override
            public void mouseDragged(MouseEvent e) {

                if (ultimoPunto == null || view == null) {
                    return;
                }

                Camera camara = view.getCamera();

                double proporcion =
                        camara.getMetrics().ratioPx2Gu();

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

                // Mueve el centro de la cámara según el arrastre del ratón
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
     * Ajusta el factor de acercamiento o alejamiento de la cámara.
     */
    private void zoom(double factor) {

        if (view == null) {
            return;
        }

        Camera camara = view.getCamera();

        double porcentaje =
                camara.getViewPercent() * factor;

        // Limita el zoom mínimo y máximo permitido
        if (porcentaje < 0.02) {
            porcentaje = 0.02;
        }

        if (porcentaje > 5.0) {
            porcentaje = 5.0;
        }

        camara.setViewPercent(porcentaje);
    }

    /**
     * Restablece la cámara gráfica a su posición y vista inicial.
     */
    private void reiniciarVista() {

        if (view == null) {
            return;
        }

        view.getCamera().resetView();
    }

    /**
     * Destaca visualmente una estación específica por su identificador.
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
            // Aplica la clase CSS de resaltado al nodo gráfico
            nodo.setAttribute(
                    "ui.class",
                    "resaltado"
            );
        }
    }

    /**
     * Resalta las estaciones y conexiones que componen una ruta calculada.
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

        limpiarResaltado(); // Limpia destaques previos

        Estacion anterior = null;
        Nodo<Estacion> nodo = rutaEstaciones.getCabeza();

        // Recorre la ruta paso a paso resaltando nodos y tramos
        while (nodo != null) {

            Estacion actual = nodo.getDato();

            resaltarNodo(actual.getId());

            if (anterior != null) {

                Edge arista = buscarArista(
                        anterior.getId(),
                        actual.getId()
                );

                if (arista != null) {
                    // Aplica la clase CSS de ruta a la arista correspondiente
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
     * Restablece los colores originales de todas las estaciones y conexiones.
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

        ListaEnlazada<Estacion> estaciones = grafo.getEstaciones();
        Nodo<Estacion> nodoEstacion = estaciones.getCabeza();

        // Restaura el estilo original de cada nodo y arista
        while (nodoEstacion != null) {

            Estacion estacion = nodoEstacion.getDato();
            Node nodoVisual = graphStream.getNode(estacion.getId());

            if (nodoVisual != null) {
                nodoVisual.setAttribute(
                        "ui.class",
                        obtenerClaseLinea(estacion.getLinea())
                );
            }

            ListaEnlazada<Conexion> conexiones = estacion.getConexiones();
            Nodo<Conexion> nodoConexion = conexiones.getCabeza();

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
     * Busca la arista gráfica correspondiente a una conexión de estaciones.
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
     * Obtiene el grafo ponderado asociado.
     */
    public GrafoPonderado getGrafo() {
        return grafo;
    }

    /**
     * Obtiene el analizador asociado a la aplicación.
     */
    public AnalizadorRed getAnalizador() {
        return analizador;
    }

    /**
     * Asocia un analizador de red al visualizador.
     */
    public void setAnalizador(AnalizadorRed analizador) {
        this.analizador = analizador;
    }

    /**
     * Obtiene la ventana principal asociada.
     */
    public VentanaPrincipal getVentana() {
        return ventana;
    }
}
