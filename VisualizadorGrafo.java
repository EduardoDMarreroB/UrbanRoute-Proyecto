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
import org.graphstream.ui.view.camera.Camera;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;

import urbanroute.control.AnalizadorRed;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Representa gráficamente la red de transporte.
 * <p>
 * Utiliza GraphStream exclusivamente para dibujar las estaciones y conexiones
 * que obtiene del {@link GrafoPonderado}. Cada línea de transporte se coloca
 * en una fila diferente y usa un color propio. Las conexiones entre estaciones
 * de distintas líneas se muestran como transbordos.
 * <p>
 * La clase no implementa algoritmos de grafos ni administra las estructuras
 * principales de la red: solo se encarga de la parte visual. Los datos siguen
 * viviendo en el modelo, y aquí se leen para construir el dibujo.
 */
public class VisualizadorGrafo {

   /** 
 * Colores asignados a las líneas de transporte. 
 * Si hay más líneas que colores, se vuelven a usar desde el principio.
 */
private static final String[] PALETA = {
    "#4a90d9", // Línea 1: azul
    "#2ca02c", // Línea 2: verde
    "#ff7f0e", // Línea 3: naranja
    "#9467bd", // Línea 4: púrpura
    "#17becf", // Línea 5: cian
    "#d62728", // Línea 6: rojo
    "#8c564b", // Línea 7: marrón
    "#e377c2", // Línea 8: rosa
    "#bcbd22", // Línea 9: oliva
    "#7f7f7f"  // Línea 10: gris
};

    /** Color utilizado para resaltar rutas y estaciones. */
    private static final String COLOR_RESALTADO = "#e8453c";

    /** Separación horizontal entre estaciones de una misma línea. */
    private static final double ESPACIO_X = 3.0;

    /** Separación vertical entre líneas de transporte. */
    private static final double ESPACIO_Y = 4.0;

    /** Grafo propio que contiene los datos de la red. */
    private GrafoPonderado grafo;

    /** Ventana principal de UrbanRoute. */
    private VentanaPrincipal ventana;

    /** Grafo de GraphStream utilizado únicamente para la representación gráfica. */
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
     * <p>
     * Se guarda la referencia al grafo y a la ventana principal, y se indica a
     * GraphStream que use el backend de Swing.
     *
     * @param grafo   grafo del modelo que se va a dibujar.
     * @param ventana ventana principal desde la que se abre el visualizador.
     * @throws IllegalArgumentException si el grafo es {@code null}.
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
     * <p>
     * Crea el grafo visual, aplica la hoja de estilos, genera los nodos con
     * sus posiciones según la línea de transporte y agrega las conexiones
     * correspondientes.
     * <p>
     * Este método debe ejecutarse desde el hilo de eventos de Swing cuando el
     * visualizador esté asociado a una interfaz gráfica.
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
     * <p>
     * Recorre las líneas y, para cada una, coloca sus estaciones en una fila.
     * Dentro de la fila, las estaciones se distribuyen horizontalmente según
     * el orden en que aparecen en el grafo. Si el nodo visual ya existe, se
     * reutiliza y solo se actualizan sus atributos.
     * <p>
     * Este método también se usa durante la sincronización, así que debe
     * encargarse tanto de crear nodos nuevos como de actualizar los que ya
     * están dibujados.
     */
    private void crearNodos() {
        ListaEnlazada<String> lineas = grafo.obtenerLineas();
        ListaEnlazada<Estacion> estaciones =
                grafo.getEstaciones();
        int fila = 0;

        // Cada línea ocupa una fila; dentro de la fila, las estaciones se
        // reparten en columnas según el orden en que aparecen en el grafo.
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

                    // Si el nodo visual ya existe (por ejemplo al sincronizar)
                    // se reutiliza; si no, se crea en este momento.
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
     * <p>
     * Actualiza la etiqueta, la posición, la clase de color y, si la columna
     * es impar, coloca el texto debajo del nodo para alternar la alineación
     * y evitar que las etiquetas se superpongan.
     *
     * @param nodoVisual nodo de GraphStream que representa la estación.
     * @param estacion   estación del modelo con los datos a mostrar.
     * @param columna    columna que ocupa la estación dentro de su línea.
     * @param fila       fila que ocupa la línea de la estación.
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

        // La fila se invierte en el eje Y para que la primera línea quede arriba.
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

        // Alternar la alineación del texto evita que las etiquetas de columnas
        // consecutivas se solapen.
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
     * <p>
     * En grafos no dirigidos, una misma conexión aparece en las listas de
     * adyacencia de ambas estaciones. Para no dibujarla dos veces se utiliza
     * un identificador canónico: los dos sentidos producen el mismo ID y solo
     * se crea una arista. En grafos dirigidos cada sentido tiene un ID
     * distinto, así que {@code A -> B} y {@code B -> A} se representan como
     * dos conexiones independientes.
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

                // Si la arista ya existe (típico en grafos no dirigidos, donde
                // la conexión aparece en los dos sentidos) no se vuelve a crear.
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
     * <p>
     * Cada línea recibe un color de la paleta. Los transbordos se muestran
     * con líneas discontinuas y las rutas seleccionadas usan el color de
     * resaltado.
     *
     * @return hoja de estilos CSS de GraphStream.
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

                // Se genera una clase de estilo por cada línea, usando la paleta
        // de forma cíclica para que no falten colores si hay más líneas.
        ListaEnlazada<String> lineas = grafo.obtenerLineas();
        Nodo<String> nodoLinea = lineas.getCabeza();
        int indice = 0;
        while (nodoLinea != null) {
            String color = PALETA[indice % PALETA.length];

            // Primero se arma el texto de la clase para el nodo y luego se agrega
            String estiloNodo = "node.linea" + indice + " { fill-color: " + color + "; }";
            css.append(estiloNodo);

            // Lo mismo para la conexión (arista)
            String estiloArista = "edge.linea" + indice + " { fill-color: " + color + "; size: 3px; }";
            css.append(estiloArista);

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
     * <p>
     * El índice determina el color y la fila que usará la línea en la
     * representación gráfica.
     *
     * @param linea nombre de la línea que se quiere buscar.
     * @return posición de la línea o {@code -1} si no existe.
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
     * <p>
     * Si la línea no existe en la red, se devuelve {@code "linea0"} como
     * valor seguro para no dejar nodos sin estilo.
     *
     * @param linea nombre de la línea.
     * @return clase CSS asignada a la línea.
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
     * <p>
     * Si las dos estaciones pertenecen a la misma línea, la conexión usa el
     * color de esa línea. Si son de líneas distintas, se marca como
     * transbordo.
     *
     * @param conexion conexión que se quiere clasificar.
     * @return clase CSS correspondiente a la conexión.
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
     * <p>
     * En un grafo dirigido se conserva el orden origen-destino. En un grafo
     * no dirigido se ordenan los dos IDs para que ambos sentidos de una misma
     * conexión produzcan el mismo identificador.
     * <p>
     * La longitud de cada ID forma parte de la clave para evitar
     * ambigüedades si los IDs contienen separadores.
     *
     * @param origen  ID de la estación de origen.
     * @param destino ID de la estación de destino.
     * @return identificador de la arista gráfica.
     */
    private String identificadorArista(
            String origen,
            String destino
    ) {
        String primero = origen;
        String segundo = destino;

        // En un grafo no dirigido da igual el orden, así que se ordenan los
        // IDs para que A->B y B->A generen la misma arista.
        if (!grafo.esDirigido()
                && primero.compareTo(segundo) > 0) {
            primero = destino;
            segundo = origen;
        }
        return "AR_"+ primero.length()+ "_"+ primero+ "_"+ segundo.length()+ "_"
                + segundo;
    }

    /**
     * Construye y devuelve el componente Swing que contiene la representación
     * visual del grafo.
     * <p>
     * La vista incluye botones de zoom, un botón para ajustar la cámara,
     * controles de ratón para desplazar la red y una leyenda de colores.
     *
     * @return panel Swing que contiene la representación gráfica.
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
     * <p>
     * Al cerrar la ventana se libera el {@link Viewer} y se actualiza el
     * estado interno del visualizador.
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

        // Al cerrar la ventana se libera el Viewer y el grafo visual, para
        // no dejar recursos colgando entre aperturas.
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
     * Cierra el {@link Viewer} de GraphStream y libera la vista actual.
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
     * <p>
     * También libera el {@link Viewer} asociado.
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
     * <p>
     * Cada línea se muestra con el color que usa en el grafo. Al final se
     * agrega una referencia visual para los transbordos.
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
     * <p>
     * No basta con que se haya construido el grafo de GraphStream: también
     * deben existir el {@link Viewer} y la vista.
     *
     * @return {@code true} si la vista gráfica está construida y activa.
     */
    public boolean estaMostrandose() {
        return graphStream != null
                && viewer != null
                && view != null;
    }

    /**
     * Sincroniza la representación visual con el grafo propio.
     * <p>
     * Elimina los nodos que ya no existen, reconstruye las conexiones y
     * actualiza etiquetas, posiciones, colores y leyenda. Si se invoca desde
     * otro hilo, programa la actualización en el hilo de eventos de Swing.
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

        // Primero se eliminan las aristas. Así no quedan referencias a nodos
        // que están a punto de desaparecer y las conexiones se vuelven a
        // dibujar desde cero con los datos actuales del modelo.
        for (int i = graphStream.getEdgeCount() - 1; i >= 0; i--) {
            Edge arista = graphStream.getEdge(i);
            graphStream.removeEdge(arista);
        }

        // Se eliminan los nodos cuyas estaciones ya no existen en el grafo
        // propio (por ejemplo tras eliminar una estación).
        for (int i = graphStream.getNodeCount() - 1; i >= 0; i--) {
            Node nodo = graphStream.getNode(i);
            if (grafo.buscarEstacion(nodo.getId()) == null) {
                graphStream.removeNode(nodo);
            }
        }

        // Se actualizan los estilos por si se agregaron o eliminaron líneas
        // de transporte.
        graphStream.setAttribute(
                "ui.stylesheet",
                crearEstilos()
        );

        // Se actualizan todos los nodos. También se recalculan sus posiciones
        // para reflejar los cambios de línea y las nuevas estaciones.
        crearNodos();

        // Se vuelven a dibujar las conexiones existentes y se refresca la leyenda.
        agregarAristas();
        llenarLeyenda();
    }

    /**
     * Crea un botón para ejecutar una acción gráfica.
     *
     * @param texto  texto que se mostrará en el botón.
     * @param accion acción que se ejecuta al pulsar el botón.
     * @return botón configurado.
     */
    private JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> accion.run());
        return boton;
    }

    /**
     * Instala los controles de ratón sobre la vista de GraphStream.
     * <p>
     * La rueda permite acercar y alejar el dibujo. El botón derecho o el
     * central permiten desplazar la cámara arrastrando.
     *
     * @param componenteVista componente gráfico que recibirá los eventos.
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
                // Solo se inicia el arrastre con el botón derecho o el central.
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

                // Se pasa de píxeles a unidades del grafo. Si la proporción no
                // es válida (por ejemplo antes de que la cámara esté lista),
                // no se hace nada para evitar movimientos raros.
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

                // Se invierte el eje Y porque la pantalla crece hacia abajo
                // y el grafo crece hacia arriba.
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
     * <p>
     * El porcentaje de vista se limita entre 0.02 y 5.0 para evitar que la
     * cámara se aleje o acerque demasiado.
     *
     * @param factor factor de ampliación o reducción.
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
     * @param id ID de la estación que se quiere resaltar.
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
            nodo.setAttribute("ui.class","resaltado");
        }
    }

    /**
     * Resalta las estaciones y conexiones de una ruta.
     * <p>
     * Recibe una lista enlazada de estaciones ordenadas según el recorrido
     * calculado por Dijkstra. En grafos dirigidos, las conexiones se buscan
     * respetando el sentido de cada tramo.
     *
     * @param rutaEstaciones lista de estaciones que forman la ruta.
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

            // Se va conectando cada estación con la anterior para resaltar
            // también las aristas del recorrido.
            if (anterior != null) {
                Edge arista = buscarArista(
                        anterior.getId(),
                        actual.getId()
                );
                if (arista != null) {
                    arista.setAttribute("ui.class","ruta");
                }
            }
            anterior = actual;
            nodo = nodo.getSiguiente();
        }
    }

    /**
     * Restablece los colores originales de las estaciones y conexiones.
     * <p>
     * Los colores se recalculan a partir de las líneas y de los extremos de
     * cada conexión del grafo propio.
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
            Node nodoVisual = graphStream.getNode(estacion.getId());
            if (nodoVisual != null) {
                nodoVisual.setAttribute("ui.class",
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
                    arista.setAttribute("ui.class",
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
     * <p>
     * En grafos dirigidos solo se busca el sentido solicitado. En grafos no
     * dirigidos se usa el identificador canónico, así que ambos sentidos
     * corresponden a la misma arista.
     *
     * @param origen  ID de la estación de origen.
     * @param destino ID de la estación de destino.
     * @return arista encontrada o {@code null} si no existe.
     */
    private Edge buscarArista(String origen, String destino) {
        if (graphStream == null) {
            return null;
        }
        String id = identificadorArista(origen, destino);
        return graphStream.getEdge(id);
    }

    /**
     * Obtiene el grafo propio utilizado como fuente de información.
     *
     * @return grafo del modelo asociado al visualizador.
     */
    public GrafoPonderado getGrafo() {
        return grafo;
    }

    /**
     * Obtiene la ventana principal asociada al visualizador.
     *
     * @return ventana principal de UrbanRoute.
     */
    public VentanaPrincipal getVentana() {
        return ventana;
    }
}
