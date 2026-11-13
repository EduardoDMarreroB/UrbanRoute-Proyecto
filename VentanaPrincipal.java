package urbanroute.interfaz;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import urbanroute.control.AnalizadorRed;
import urbanroute.control.GestorArchivo;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.modelo.TablaHash;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;
/**
 * Ventana principal de UrbanRoute.
 *
 * Coordina las operaciones realizadas por el usuario y conecta
 * la interfaz gráfica con las estructuras de datos y los módulos
 * encargados de la persistencia, el análisis y la visualización.
 *
 * La administración de estaciones y conexiones se delega en
 * GrafoPonderado, que debe mantener sincronizada la tabla hash.
 *
 * La clase no implementa directamente los algoritmos del grafo
 * ni las operaciones internas de las estructuras de datos.
 *
 * @author Equipo UrbanRoute
 */
public class VentanaPrincipal extends JFrame {

    /** Tabla hash utilizada para indexar las estaciones. */
    private TablaHash tabla;

    /** Grafo ponderado que representa la red de transporte. */
    private GrafoPonderado grafo;

    /** Encargado de cargar y guardar los archivos CSV. */
    private GestorArchivo gestor;

    /** Encargado de ejecutar los algoritmos de análisis de la red. */
    private AnalizadorRed analizador;

    /** Encargado de representar gráficamente la red. */
    private VisualizadorGrafo visualizador;

    /** Área donde se muestran los mensajes de la aplicación. */
    private JTextArea areaLog;

    /** Carpeta utilizada como referencia para los selectores de archivos. */
    private File ultimaCarpeta;

    /** Archivo CSV de estaciones cargado o guardado actualmente. */
    private File archivoDiccionario;

    /** Archivo CSV de conexiones cargado o guardado actualmente. */
    private File archivoGrafo;

    /** Panel que contiene la visualización y el registro de mensajes. */
    private JSplitPane panelDividido;

    /** Indica si existen modificaciones que todavía no se han guardado. */
    private boolean cambiosPendientes;

    /**
     * Construye la ventana principal de UrbanRoute.
     *
     * Inicializa la tabla hash, el grafo ponderado, el gestor de
     * archivos, el analizador y el visualizador.
     *
     * También construye la interfaz e intenta cargar los archivos
     * predeterminados de la carpeta datos.
     */
    public VentanaPrincipal() {
        super("UrbanRoute");

        tabla = new TablaHash();
        grafo = new GrafoPonderado(tabla, false);
        gestor = new GestorArchivo(this);

        cambiosPendientes = false;

        reconstruirDependencias();
        construirUI();

        cargarDatosPorDefecto();
    }

    /**
     * Construye los componentes gráficos y organiza la ventana.
     *
     * La interfaz se divide en tres grupos de operaciones:
     * archivos, modificaciones de la red y análisis.
     */
    private void construirUI() {

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        agregarControlDeCierre();

        JPanel grupoArchivos = grupo(
                "Archivos",

                boton("Cargar diccionario", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        accionCargarEstaciones();
                    }
                }),

                boton("Cargar grafo", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        accionCargarConexiones();
                    }
                }),

                boton("Guardar cambios", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        accionGuardar();
                    }
                })
        );

        JPanel grupoRed = grupo(
                "Red",

                boton("Agregar estación", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        accionAgregarEstacion();
                    }
                }),

                boton("Eliminar estación", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        accionEliminarEstacion();
                    }
                }),

                boton("Agregar conexión", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        accionAgregarConexion();
                    }
                }),

                boton("Eliminar conexión", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        accionEliminarConexion();
                    }
                })
        );

        JPanel grupoAnalisis = grupo(
                "Análisis",

                boton("Ruta mínima", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        accionRutaMinima();
                    }
                }),

                boton("Subredes aisladas", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        accionSubredes();
                    }
                }),

                boton("Hubs", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        accionHubs();
                    }
                }),

                boton("Actualizar grafo", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        accionMostrarGrafo();
                    }
                })
        );

        JPanel barra = new JPanel(new GridLayout(1, 3, 8, 0));
        barra.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        barra.add(grupoArchivos);
        barra.add(grupoRed);
        barra.add(grupoAnalisis);

        add(barra, BorderLayout.NORTH);

        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);

        panelDividido = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        panelDividido.setTopComponent(
                new JLabel(
                        "Cargue el diccionario y el archivo de conexiones para visualizar la red.",
                        SwingConstants.CENTER
                )
        );

        panelDividido.setBottomComponent(new JScrollPane(areaLog));
        panelDividido.setDividerLocation(430);
        panelDividido.setResizeWeight(0.8);

        add(panelDividido, BorderLayout.CENTER);
    }

    /**
     * Configura el comportamiento de la ventana cuando el usuario
     * intenta cerrarla.
     *
     * Si existen modificaciones pendientes, solicita que el usuario
     * decida si desea guardarlas, descartarlas o cancelar el cierre.
     */
    private void agregarControlDeCierre() {

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarAplicacion();
            }
        });
    }

    /**
     * Controla el cierre de la aplicación.
     *
     * @return true si la aplicación puede cerrarse; false en caso contrario
     */
    private void cerrarAplicacion() {

        if (!cambiosPendientes) {
            dispose();
            return;
        }

        String[] opciones = {
            "Guardar y salir",
            "Salir sin guardar",
            "Cancelar"
        };

        int seleccion = JOptionPane.showOptionDialog(
                this,
                "Existen modificaciones que todavía no se han guardado.",
                "Cambios pendientes",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                opciones,
                opciones[0]
        );

        if (seleccion == 0) {

            if (guardarCambiosEnCarpeta()) {
                dispose();
            }

        } else if (seleccion == 1) {
            dispose();
        }
    }

    /**
     * Inicializa nuevamente los módulos que dependen del grafo.
     *
     * Debe ejecutarse cuando se sustituye la tabla hash o el grafo,
     * para evitar que el analizador y el visualizador conserven
     * referencias a estructuras anteriores.
     */
    private void reconstruirDependencias() {

        analizador = new AnalizadorRed(grafo);
        visualizador = new VisualizadorGrafo(grafo, this);

        analizador.setVisualizador(visualizador);
        visualizador.setAnalizador(analizador);

        if (panelDividido != null) {
            actualizarVisualizacion();
        }
    }

    /**
     * Crea un botón y registra la acción que debe ejecutar.
     *
     * @param texto texto que aparecerá en el botón
     * @param accion acción asociada al botón
     * @return botón configurado
     */
    private JButton boton(String texto, ActionListener accion) {

        JButton boton = new JButton(texto);
        boton.addActionListener(accion);

        return boton;
    }

    /**
     * Construye un panel con título y hasta cuatro botones.
     *
     * @param titulo título del grupo
     * @param botones botones que se incluirán
     * @return panel organizado en una cuadrícula
     */
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

    /**
     * Intenta cargar automáticamente los archivos predeterminados.
     *
     * Utiliza estructuras temporales para evitar que una carga
     * fallida deje el modelo principal en un estado inconsistente.
     */
    private void cargarDatosPorDefecto() {

        File diccionario = new File("datos/diccionario.csv");
        File archivoConexiones = new File("datos/grafo.csv");

        TablaHash tablaTemporal = new TablaHash();
        GrafoPonderado grafoTemporal =
                new GrafoPonderado(tablaTemporal, false);

        boolean diccionarioCargado = false;

        if (diccionario.exists()) {

            try {

                int cantidad = gestor.cargarEstaciones(
                        diccionario.getAbsolutePath(),
                        tablaTemporal,
                        grafoTemporal
                );

                mostrarMensaje(
                        "Carga automática: " + cantidad
                        + " estaciones desde datos/diccionario.csv."
                );

                archivoDiccionario = diccionario;
                diccionarioCargado = true;

                ultimaCarpeta = diccionario.getParentFile();

            } catch (IOException | RuntimeException ex) {

                mostrarMensaje(
                        "Aviso: no se pudo cargar el diccionario: "
                        + ex.getMessage()
                );
            }

        } else {

            mostrarMensaje("Aviso: no se encontró datos/diccionario.csv.");
        }

        if (diccionarioCargado
                && archivoConexiones.exists()
                && grafoTemporal.numEstaciones() > 0) {

            try {

                int cantidad = gestor.cargarConexiones(
                        archivoConexiones.getAbsolutePath(),
                        grafoTemporal
                );

                mostrarMensaje(
                        "Carga automática: " + cantidad
                        + " conexiones desde datos/grafo.csv."
                );

                archivoGrafo = archivoConexiones;

            } catch (IOException | RuntimeException ex) {

                mostrarMensaje(
                        "Aviso: no se pudieron cargar las conexiones: "
                        + ex.getMessage()
                );
            }

        } else if (archivoConexiones.exists()
                && grafoTemporal.numEstaciones() == 0) {

            mostrarMensaje(
                    "Aviso: no se cargó grafo.csv porque no hay estaciones cargadas."
            );

        } else if (!archivoConexiones.exists()) {

            mostrarMensaje("Aviso: no se encontró datos/grafo.csv.");
        }

        tabla = tablaTemporal;
        grafo = grafoTemporal;

        cambiosPendientes = false;

        reconstruirDependencias();
    }

    /**
     * Comprueba si el usuario desea sustituir los datos actuales.
     *
     * Advierte expresamente cuando existen modificaciones pendientes
     * de guardar.
     *
     * @param operacion descripción de la sustitución que se realizará
     * @return true si el usuario acepta; false si cancela
     */
    private boolean confirmarSustitucion(String operacion) {

        if (grafo.numEstaciones() == 0 && !cambiosPendientes) {
            return true;
        }

        StringBuilder mensaje = new StringBuilder();

        if (cambiosPendientes) {
            mensaje.append(
                    "Hay modificaciones pendientes de guardar.\n"
            );
        }

        mensaje.append(operacion);
        mensaje.append("\n¿Desea continuar?");

        return confirmarAdvertencia(mensaje.toString());
    }

    /**
     * Carga un nuevo diccionario de estaciones.
     *
     * La carga se realiza primero en una tabla hash y un grafo
     * temporales. Las estructuras actuales se sustituyen únicamente
     * si el gestor completa la operación.
     */
    private void accionCargarEstaciones() {

        File archivo = elegirArchivo("Seleccionar diccionario.csv");

        if (archivo == null) {
            return;
        }

        if (!confirmarSustitucion(
                "Se reemplazarán las estaciones y conexiones actuales."
        )) {
            return;
        }

        TablaHash tablaTemporal = new TablaHash();

        GrafoPonderado grafoTemporal =
                new GrafoPonderado(tablaTemporal, false);

        try {

            int cantidad = gestor.cargarEstaciones(
                    archivo.getAbsolutePath(),
                    tablaTemporal,
                    grafoTemporal
            );

            tabla = tablaTemporal;
            grafo = grafoTemporal;

            archivoDiccionario = archivo;
            archivoGrafo = null;

            ultimaCarpeta = archivo.getParentFile();

            cambiosPendientes = false;

            reconstruirDependencias();

            mostrarMensaje(
                    "Diccionario cargado correctamente: "
                    + cantidad + " estaciones."
            );

        } catch (IOException | RuntimeException ex) {

            mostrarError(
                    "No se pudo cargar el diccionario.\n"
                    + "La red anterior se ha conservado.\n\n"
                    + ex.getMessage()
            );
        }
    }

    /**
     * Construye un nuevo grafo con copias de las estaciones actuales.
     *
     * Cada estación conserva sus datos descriptivos, pero las
     * conexiones se reconstruyen desde el archivo seleccionado.
     *
     * Esto evita reutilizar una tabla hash que ya contiene las
     * estaciones del grafo anterior.
     *
     * @return nueva estructura de grafo sin conexiones
     */
    private GrafoPonderado crearGrafoSinConexiones() {

        TablaHash tablaNueva = new TablaHash();

        GrafoPonderado grafoNuevo =
                new GrafoPonderado(tablaNueva, false);

        Estacion[] estaciones = todasLasEstaciones();

        for (int i = 0; i < estaciones.length; i++) {

            Estacion original = estaciones[i];

            Estacion copia = new Estacion(
                    original.getId(),
                    original.getNombre(),
                    original.getLinea(),
                    original.getZona(),
                    original.getCapacidad()
            );

            grafoNuevo.agregarEstacion(copia);
        }

        return grafoNuevo;
    }

    /**
     * Carga las conexiones de un archivo CSV.
     *
     * La operación se realiza sobre un grafo temporal que contiene
     * copias de las estaciones actuales. De esta manera, si la lectura
     * falla, el grafo que estaba utilizando el usuario permanece intacto.
     */
    private void accionCargarConexiones() {

        if (grafo.numEstaciones() == 0) {
            mostrarError("Primero cargue el diccionario de estaciones.");
            return;
        }

        File archivo = elegirArchivo("Seleccionar grafo.csv");

        if (archivo == null) {
            return;
        }

        if (!confirmarSustitucion(
                "Se reemplazarán las conexiones actuales. "
                + "Se conservarán las estaciones existentes."
        )) {
            return;
        }

        boolean habiaCambiosPendientes = cambiosPendientes;

        try {

            GrafoPonderado grafoTemporal = crearGrafoSinConexiones();

            int cantidad = gestor.cargarConexiones(
                    archivo.getAbsolutePath(),
                    grafoTemporal
            );

            tabla = obtenerTablaDelGrafoTemporal(grafoTemporal);
            grafo = grafoTemporal;

            archivoGrafo = archivo;
            ultimaCarpeta = archivo.getParentFile();

            /*
             * Se conserva el estado pendiente anterior porque
             * las estaciones podrían contener modificaciones
             * que todavía no se han guardado.
             */
            cambiosPendientes = habiaCambiosPendientes;

            reconstruirDependencias();

            mostrarMensaje(
                    "Grafo cargado correctamente: "
                    + cantidad + " conexiones."
            );

        } catch (IOException | RuntimeException ex) {

            mostrarError(
                    "No se pudo cargar el archivo de conexiones.\n"
                    + "La red anterior se ha conservado.\n\n"
                    + ex.getMessage()
            );
        }
    }

    /**
     * Obtiene la tabla hash asociada al grafo temporal.
     *
     * La tabla que se conserva debe ser exactamente la utilizada
     * internamente por el grafo, para mantener sincronizadas ambas
     * estructuras.
     *
     * @param grafoTemporal grafo del que se obtiene la tabla
     * @return tabla hash asociada al grafo
     */
    private TablaHash obtenerTablaDelGrafoTemporal(
            GrafoPonderado grafoTemporal
    ) {
        /*
         * Se crea la tabla antes de construir el grafo temporal.
         * Este método se mantiene como punto de integración.
         *
         * La implementación del proyecto debe proporcionar el acceso
         * a la tabla asociada al grafo si se necesita recuperar
         * esa misma instancia.
         */
        return grafoTemporal.getIndice();
    }

    /**
     * Abre el selector de carpetas y guarda los cambios de la red.
     */
    private void accionGuardar() {
        guardarCambiosEnCarpeta();
    }

    /**
     * Guarda el diccionario y las conexiones en una carpeta seleccionada.
     *
     * Cuando la operación termina correctamente, se actualizan las
     * referencias a los archivos guardados y se elimina el indicador
     * de modificaciones pendientes.
     *
     * @return true si el guardado terminó correctamente
     */
    private boolean guardarCambiosEnCarpeta() {

        if (grafo.numEstaciones() == 0) {
            mostrarError("No hay estaciones para guardar.");
            return false;
        }

        JFileChooser selector = new JFileChooser(obtenerCarpetaInicial());

        selector.setDialogTitle(
                "Carpeta destino para diccionario.csv y grafo.csv"
        );

        selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (selector.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return false;
        }

        File carpeta = selector.getSelectedFile();

        File nuevoDiccionario = new File(carpeta, "diccionario.csv");
        File nuevoGrafo = new File(carpeta, "grafo.csv");

        if ((nuevoDiccionario.exists() || nuevoGrafo.exists())
                && !confirmarAdvertencia(
                        "Los archivos CSV existentes en esa carpeta "
                        + "serán sobrescritos. ¿Continuar?"
                )) {
            return false;
        }

        try {

            gestor.guardarCambios(
                    carpeta.getAbsolutePath(),
                    tabla,
                    grafo
            );

            archivoDiccionario = nuevoDiccionario;
            archivoGrafo = nuevoGrafo;

            ultimaCarpeta = carpeta;

            cambiosPendientes = false;

            mostrarMensaje(
                    "Cambios guardados correctamente en:\n"
                    + carpeta.getAbsolutePath()
            );

            return true;

        } catch (IOException | RuntimeException ex) {

            mostrarError(
                    "No se pudieron guardar los cambios:\n"
                    + ex.getMessage()
            );

            return false;
        }
    }

    /**
     * Determina la carpeta inicial que utilizará el selector de guardado.
     *
     * Prioriza la última carpeta utilizada. Si todavía no existe,
     * utiliza la ubicación de alguno de los archivos cargados.
     *
     * @return carpeta inicial para el selector
     */
    private File obtenerCarpetaInicial() {

        if (ultimaCarpeta != null && ultimaCarpeta.exists()) {
            return ultimaCarpeta;
        }

        if (archivoDiccionario != null) {
            return archivoDiccionario.getParentFile();
        }

        if (archivoGrafo != null) {
            return archivoGrafo.getParentFile();
        }

        return new File("datos");
    }

    /**
     * Muestra la representación visual actual del grafo.
     */
    private void accionMostrarGrafo() {

        if (grafo.numEstaciones() == 0) {
            mostrarError("No hay datos cargados.");
            return;
        }

        actualizarVisualizacion();
    }

    /**
     * Sincroniza el visualizador con el grafo actual y actualiza
     * el componente mostrado en la ventana.
     */
    private void actualizarVisualizacion() {

        if (panelDividido == null || visualizador == null) {
            return;
        }

        int posicionDivisor = panelDividido.getDividerLocation();

        visualizador.sincronizar();

        if (grafo.numEstaciones() > 0) {

            Component panelGrafo =
                    visualizador.obtenerComponenteVista();

            panelDividido.setTopComponent(panelGrafo);

        } else {

            panelDividido.setTopComponent(
                    new JLabel(
                            "No hay estaciones cargadas.",
                            SwingConstants.CENTER
                    )
            );
        }

        panelDividido.setDividerLocation(posicionDivisor);
        panelDividido.revalidate();
        panelDividido.repaint();
    }

    /**
     * Agrega una estación y, opcionalmente, una conexión con una
     * estación que ya exista en la red.
     *
     * Los datos se validan antes de modificar el grafo.
     */
    private void accionAgregarEstacion() {

        JTextField campoId = new JTextField(12);
        JTextField campoNombre = new JTextField(12);
        JTextField campoLinea = new JTextField(12);
        JTextField campoZona = new JTextField(12);
        JTextField campoCapacidad = new JTextField("0", 12);
        JTextField campoTiempo = new JTextField(12);

        Estacion[] existentes = todasLasEstaciones();

        String[] opcionesVecino = new String[existentes.length + 1];

        opcionesVecino[0] = "(sin conexión)";

        for (int i = 0; i < existentes.length; i++) {

            opcionesVecino[i + 1] =
                    existentes[i].getNombre()
                    + " [" + existentes[i].getId() + "]";
        }

        JComboBox<String> comboVecino =
                new JComboBox<String>(opcionesVecino);

        JLabel etiquetaLineas = new JLabel(
                "Líneas actuales: " + textoLineasExistentes()
        );

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));

        panel.add(new JLabel("ID:"));
        panel.add(campoId);

        panel.add(new JLabel("Nombre:"));
        panel.add(campoNombre);

        panel.add(new JLabel("Línea:"));
        panel.add(campoLinea);

        panel.add(etiquetaLineas);
        panel.add(new JLabel(""));

        panel.add(new JLabel("Zona:"));
        panel.add(campoZona);

        panel.add(new JLabel("Capacidad:"));
        panel.add(campoCapacidad);

        panel.add(new JLabel("Conectar con:"));
        panel.add(comboVecino);

        panel.add(new JLabel("Tiempo de viaje (min):"));
        panel.add(campoTiempo);

        while (true) {

            int resultado = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Agregar estación",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (resultado != JOptionPane.OK_OPTION) {
                return;
            }

            String id = campoId.getText().trim();
            String nombre = campoNombre.getText().trim();
            String linea = campoLinea.getText().trim();
            String zona = campoZona.getText().trim();

            String error = null;

            int capacidad = 0;
            double tiempo = -1;

            Estacion vecino = null;

            if (!textoValido(id)
                    || !textoValido(nombre)
                    || !textoValido(linea)
                    || !textoValido(zona)) {

                error = "ID, nombre, línea y zona son obligatorios. "
                        + "No se permiten comas ni saltos de línea.";

            } else if (id.startsWith("#")
                    || id.equalsIgnoreCase("id")
                    || id.equalsIgnoreCase("origen")) {

                error = "Ese ID está reservado. Utilice otro.";

            } else if (grafo.buscarEstacion(id) != null) {

                error = "Ya existe una estación con el ID " + id + ".";

            } else {

                try {

                    capacidad = Integer.parseInt(
                            campoCapacidad.getText().trim()
                    );

                    if (capacidad < 0) {
                        error = "La capacidad no puede ser negativa.";
                    }

                } catch (NumberFormatException ex) {

                    error = "La capacidad debe ser un número entero.";
                }
            }

            int indiceVecino = comboVecino.getSelectedIndex();

            if (error == null && indiceVecino > 0) {

                vecino = existentes[indiceVecino - 1];

                tiempo = leerTiempo(campoTiempo.getText());

                if (tiempo <= 0) {
                    error = "Ingrese un tiempo de viaje positivo y finito.";
                }
            }

            if (error != null) {
                mostrarError(error);
                continue;
            }

            Estacion nueva = new Estacion(
                    id,
                    nombre,
                    linea,
                    zona,
                    capacidad
            );

            try {

                grafo.agregarEstacion(nueva);

                if (grafo.buscarEstacion(id) != nueva) {
                    throw new IllegalStateException(
                            "No se pudo registrar la estación en el grafo."
                    );
                }

                if (vecino != null) {

                    grafo.agregarConexion(
                            nueva.getId(),
                            vecino.getId(),
                            tiempo
                    );
                }

            } catch (RuntimeException ex) {

                /*
                 * Si la inserción de la conexión falla después
                 * de insertar la estación, se intenta revertir
                 * la inserción para evitar un estado parcial.
                 */
                try {

                    if (grafo.buscarEstacion(id) == nueva) {
                        grafo.eliminarEstacion(id);
                    }

                } catch (RuntimeException errorReversion) {

                    mostrarError(
                            "No se pudo completar la operación ni "
                            + "revertirla automáticamente. Revise la red."
                    );

                    return;
                }

                mostrarError(
                        "No se pudo agregar la estación:\n"
                        + ex.getMessage()
                );

                return;
            }

            marcarCambiosPendientes();
            actualizarVisualizacion();

            mostrarMensaje(
                    "Estación agregada: " + nueva
                    + " (línea " + linea
                    + ", zona " + zona + ")"
                    + (vecino != null
                            ? ", conectada con " + vecino.getNombre()
                            + " (" + tiempo + " min)."
                            : ", sin conexiones todavía.")
            );

            return;
        }
    }

    /**
     * Elimina una estación y las conexiones asociadas.
     *
     * @return no devuelve ningún valor
     */
    private void accionEliminarEstacion() {

        Estacion estacion = elegirEstacion(
                "Eliminar estación",
                "Seleccione la estación que desea eliminar:",
                todasLasEstaciones()
        );

        if (estacion == null) {
            return;
        }

        int conexiones = estacion.getGrado();

        if (!confirmarAdvertencia(
                "Se eliminará " + estacion
                + " y sus " + conexiones
                + " conexiones. ¿Continuar?"
        )) {
            return;
        }

        try {

            grafo.eliminarEstacion(estacion.getId());

            if (grafo.buscarEstacion(estacion.getId()) != null) {
                throw new IllegalStateException(
                        "La estación continúa registrada en el grafo."
                );
            }

            marcarCambiosPendientes();
            actualizarVisualizacion();

            mostrarMensaje(
                    "Estación eliminada: " + estacion
                    + ". Conexiones asociadas: " + conexiones + "."
            );

        } catch (RuntimeException ex) {

            mostrarError(
                    "No se pudo eliminar la estación:\n"
                    + ex.getMessage()
            );
        }
    }

    /**
     * Agrega o actualiza una conexión entre dos estaciones.
     */
    private void accionAgregarConexion() {

        Estacion origen = elegirEstacion(
                "Agregar conexión",
                "Seleccione la estación de origen:",
                todasLasEstaciones()
        );

        if (origen == null) {
            return;
        }

        Estacion destino = elegirEstacion(
                "Agregar conexión",
                "Seleccione la estación de destino:",
                todasExcepto(origen)
        );

        if (destino == null) {
            return;
        }

        Double tiempo = pedirTiempo(
                "Tiempo de viaje entre "
                + origen.getNombre() + " y "
                + destino.getNombre() + " (minutos):"
        );

        if (tiempo == null) {
            return;
        }

        try {

            grafo.agregarConexion(
                    origen.getId(),
                    destino.getId(),
                    tiempo.doubleValue()
            );

            marcarCambiosPendientes();
            actualizarVisualizacion();

            mostrarMensaje(
                    "Conexión agregada o actualizada: "
                    + origen.getNombre() + " <-> "
                    + destino.getNombre()
                    + " (" + tiempo + " min)."
            );

        } catch (RuntimeException ex) {

            mostrarError(
                    "No se pudo agregar la conexión:\n"
                    + ex.getMessage()
            );
        }
    }

    /**
     * Elimina una conexión existente entre dos estaciones.
     */
    private void accionEliminarConexion() {

        Estacion origen = elegirEstacion(
                "Eliminar conexión",
                "Seleccione la estación de origen:",
                todasLasEstaciones()
        );

        if (origen == null) {
            return;
        }

        Estacion destino = elegirEstacion(
                "Eliminar conexión",
                "Seleccione la estación de destino:",
                todasExcepto(origen)
        );

        if (destino == null) {
            return;
        }

        try {

            boolean eliminada = grafo.eliminarConexion(origen, destino);

            if (eliminada) {

                marcarCambiosPendientes();
                actualizarVisualizacion();

                mostrarMensaje(
                        "Conexión eliminada entre "
                        + origen.getNombre() + " y "
                        + destino.getNombre() + "."
                );

            } else {

                mostrarError(
                        "No existe una conexión directa entre "
                        + origen.getNombre() + " y "
                        + destino.getNombre() + "."
                );
            }

        } catch (RuntimeException ex) {

            mostrarError(
                    "No se pudo eliminar la conexión:\n"
                    + ex.getMessage()
            );
        }
    }

    /**
     * Solicita una ruta óptima entre dos estaciones.
     *
     * Delega el cálculo en AnalizadorRed y presenta el resultado
     * en el registro y en la visualización gráfica.
     */
    private void accionRutaMinima() {

        Estacion origen = elegirEstacion(
                "Ruta mínima",
                "Estación de origen:",
                todasLasEstaciones()
        );

        if (origen == null) {
            return;
        }

        Estacion destino = elegirEstacion(
                "Ruta mínima",
                "Estación de destino:",
                todasLasEstaciones()
        );

        if (destino == null) {
            return;
        }

        try {

            ListaEnlazada<Estacion> ruta = analizador.dijkstra(
                    origen.getId(),
                    destino.getId()
            );

            if (ruta == null) {

                mostrarMensaje(
                        "No existe camino entre "
                        + origen.getNombre() + " y "
                        + destino.getNombre() + "."
                );

                return;
            }

            mostrarMensaje(
                    "Ruta mínima ("
                    + analizador.getUltimoTiempoRuta()
                    + " min, "
                    + ruta.obtenerTamano()
                    + " estaciones): "
                    + listaATexto(ruta, " -> ")
            );

            if (!visualizador.estaMostrandose()) {
                accionMostrarGrafo();
            }

            visualizador.resaltarRuta(ruta);

        } catch (RuntimeException ex) {

            mostrarError(
                    "No se pudo calcular la ruta mínima:\n"
                    + ex.getMessage()
            );
        }
    }

    /**
     * Permite analizar las subredes de la red actual o simular
     * la interrupción de una estación o línea.
     */
    private void accionSubredes() {

        if (grafo.numEstaciones() == 0) {
            mostrarError("No hay datos cargados.");
            return;
        }

        String[] opciones = {
            "Red actual",
            "Sin una estación",
            "Sin una línea"
        };

        int opcion = JOptionPane.showOptionDialog(
                this,
                "¿Qué desea analizar?",
                "Subredes aisladas",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );

        try {

            if (opcion == 0) {

                mostrarSubredes(
                        "red actual",
                        analizador.detectarSubredesBFS()
                );

            } else if (opcion == 1) {

                Estacion estacion = elegirEstacion(
                        "Simular baja de estación",
                        "Estación que se desconecta:",
                        todasLasEstaciones()
                );

                if (estacion == null) {
                    return;
                }

                mostrarSubredes(
                        "sin la estación " + estacion.getNombre(),
                        analizador.detectarSubredesSinEstacion(
                                estacion.getId()
                        )
                );

            } else if (opcion == 2) {

                String[] lineas = lineasExistentes();

                if (lineas.length == 0) {
                    mostrarError("No hay líneas disponibles.");
                    return;
                }

                String linea = elegirLinea(
                        "Simular baja de línea",
                        "Seleccione la línea que se desconecta:",
                        lineas
                );

                if (linea == null) {
                    return;
                }

                mostrarSubredes(
                        "sin la línea " + linea,
                        analizador.detectarSubredesSinLinea(linea)
                );
            }

        } catch (RuntimeException ex) {

            mostrarError(
                    "No se pudieron analizar las subredes:\n"
                    + ex.getMessage()
            );
        }
    }

    /**
     * Presenta los componentes conexos encontrados por el analizador.
     *
     * @param descripcion escenario utilizado para el análisis
     * @param subredes componentes encontrados
     */
    private void mostrarSubredes(
            String descripcion,
            ListaEnlazada<ListaEnlazada<Estacion>> subredes
    ) {

        mostrarMensaje(
                "Subredes (" + descripcion + "): "
                + subredes.obtenerTamano()
        );

        Nodo<ListaEnlazada<Estacion>> nodo = subredes.getCabeza();

        int indice = 1;

        while (nodo != null) {

            ListaEnlazada<Estacion> subred = nodo.getDato();

            mostrarMensaje(
                    "  #" + indice
                    + (subred.obtenerTamano() == 1
                            ? " [AISLADA]"
                            : "")
                    + " (" + subred.obtenerTamano()
                    + " estaciones): "
                    + listaATexto(subred, ", ")
            );

            indice++;

            nodo = nodo.getSiguiente();
        }
    }

    /**
     * Calcula y muestra las estaciones con mayor centralidad de grado.
     */
    private void accionHubs() {

        if (grafo.numEstaciones() == 0) {
            mostrarError("No hay datos cargados.");
            return;
        }

        String texto = JOptionPane.showInputDialog(
                this,
                "¿Cuántos hubs desea mostrar?",
                "5"
        );

        if (texto == null) {
            return;
        }

        int cantidad;

        try {

            cantidad = Integer.parseInt(texto.trim());

        } catch (NumberFormatException ex) {

            mostrarError("Ingrese un número entero.");
            return;
        }

        if (cantidad < 1) {
            mostrarError("Ingrese un número mayor que 0.");
            return;
        }

        try {

            ListaEnlazada<Estacion> hubs =
                    analizador.calcularCentralidadGrado(cantidad);

            mostrarMensaje(
                    "Hubs (top " + hubs.obtenerTamano()
                    + " por conexiones directas):"
            );

            int puesto = 1;

            Nodo<Estacion> nodo = hubs.getCabeza();

            while (nodo != null) {

                Estacion estacion = nodo.getDato();

                mostrarMensaje(
                        "  " + puesto + ". " + estacion
                        + " - línea " + estacion.getLinea()
                        + " - " + estacion.getGrado()
                        + " conexiones"
                );

                puesto++;

                nodo = nodo.getSiguiente();
            }

        } catch (RuntimeException ex) {

            mostrarError(
                    "No se pudo calcular la centralidad:\n"
                    + ex.getMessage()
            );
        }
    }

    /**
     * Abre un selector de archivos CSV.
     *
     * @param titulo título que aparecerá en el selector
     * @return archivo elegido o null si el usuario cancela
     */
    private File elegirArchivo(String titulo) {

        JFileChooser selector = new JFileChooser(ultimaCarpeta);

        selector.setDialogTitle(titulo);

        selector.setFileFilter(
                new FileNameExtensionFilter(
                        "Archivos CSV",
                        "csv"
                )
        );

        if (selector.showOpenDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File archivo = selector.getSelectedFile();

        if (archivo == null || !archivo.isFile()) {
            mostrarError("Seleccione un archivo válido.");
            return null;
        }

        if (!archivo.getName().toLowerCase().endsWith(".csv")) {
            mostrarError("El archivo seleccionado debe tener extensión .csv.");
            return null;
        }

        ultimaCarpeta = archivo.getParentFile();

        return archivo;
    }

    /**
     * Solicita confirmación antes de realizar una operación destructiva.
     *
     * @param mensaje advertencia que verá el usuario
     * @return true si el usuario confirma
     */
    private boolean confirmarAdvertencia(String mensaje) {

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                mensaje,
                "Advertencia",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        return respuesta == JOptionPane.YES_OPTION;
    }

    /**
     * Permite seleccionar una estación mediante una lista tipada.
     *
     * @param titulo título del diálogo
     * @param mensaje instrucción para el usuario
     * @param opciones estaciones disponibles
     * @return estación seleccionada o null si se cancela
     */
    private Estacion elegirEstacion(
            String titulo,
            String mensaje,
            Estacion[] opciones
    ) {

        if (opciones == null || opciones.length == 0) {
            mostrarError("No hay estaciones disponibles.");
            return null;
        }

        JList<Estacion> lista = new JList<Estacion>(opciones);

        lista.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        lista.setVisibleRowCount(10);
        lista.setSelectedIndex(0);

        JScrollPane desplazamiento = new JScrollPane(lista);

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                desplazamiento,
                mensaje + "\n" + titulo,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (respuesta != JOptionPane.OK_OPTION) {
            return null;
        }

        return lista.getSelectedValue();
    }

    /**
     * Permite seleccionar una línea mediante una lista tipada.
     *
     * @param titulo título del diálogo
     * @param mensaje instrucción para el usuario
     * @param opciones líneas disponibles
     * @return línea seleccionada o null si se cancela
     */
    private String elegirLinea(
            String titulo,
            String mensaje,
            String[] opciones
    ) {

        if (opciones == null || opciones.length == 0) {
            return null;
        }

        JList<String> lista = new JList<String>(opciones);

        lista.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        lista.setVisibleRowCount(8);
        lista.setSelectedIndex(0);

        JScrollPane desplazamiento = new JScrollPane(lista);

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                desplazamiento,
                mensaje + "\n" + titulo,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (respuesta != JOptionPane.OK_OPTION) {
            return null;
        }

        return lista.getSelectedValue();
    }

    /**
     * Obtiene todas las estaciones registradas en el grafo.
     *
     * @return arreglo de estaciones actuales
     */
    private Estacion[] todasLasEstaciones() {

        Estacion[] estaciones =
                new Estacion[grafo.numEstaciones()];

        int indice = 0;

        Nodo<Estacion> nodo =
                grafo.getEstaciones().getCabeza();

        while (nodo != null && indice < estaciones.length) {

            estaciones[indice] = nodo.getDato();

            indice++;

            nodo = nodo.getSiguiente();
        }

        return estaciones;
    }

    /**
     * Obtiene las estaciones de la red, excluyendo una estación.
     *
     * @param excluida estación que no debe aparecer
     * @return arreglo con las estaciones restantes
     */
    private Estacion[] todasExcepto(Estacion excluida) {

        Estacion[] todas = todasLasEstaciones();

        int cantidad = 0;

        for (int i = 0; i < todas.length; i++) {

            if (!todas[i].getId().equals(excluida.getId())) {
                cantidad++;
            }
        }

        Estacion[] restantes = new Estacion[cantidad];

        int indice = 0;

        for (int i = 0; i < todas.length; i++) {

            if (!todas[i].getId().equals(excluida.getId())) {

                restantes[indice] = todas[i];

                indice++;
            }
        }

        return restantes;
    }

    /**
     * Obtiene las líneas diferentes que existen en la red.
     *
     * Utiliza arreglos y comparaciones directas, sin recurrir a
     * estructuras del Java Collections Framework.
     *
     * @return arreglo de líneas sin repetición
     */
    private String[] lineasExistentes() {

        ListaEnlazada<Estacion> estaciones =
                grafo.getEstaciones();

        String[] temporales =
                new String[grafo.numEstaciones()];

        int cantidad = 0;

        Nodo<Estacion> nodo = estaciones.getCabeza();

        while (nodo != null) {

            Estacion estacion = nodo.getDato();

            String linea = estacion.getLinea();

            boolean repetida = false;

            for (int i = 0; i < cantidad; i++) {

                if (temporales[i].equals(linea)) {
                    repetida = true;
                    break;
                }
            }

            if (!repetida) {

                temporales[cantidad] = linea;

                cantidad++;
            }

            nodo = nodo.getSiguiente();
        }

        String[] resultado = new String[cantidad];

        for (int i = 0; i < cantidad; i++) {
            resultado[i] = temporales[i];
        }

        return resultado;
    }

    /**
     * Construye una cadena con las líneas disponibles.
     *
     * @return texto con las líneas existentes
     */
    private String textoLineasExistentes() {

        String[] lineas = lineasExistentes();

        if (lineas.length == 0) {
            return "ninguna";
        }

        StringBuilder texto = new StringBuilder();

        for (int i = 0; i < lineas.length; i++) {

            texto.append(lineas[i]);

            if (i < lineas.length - 1) {
                texto.append(", ");
            }
        }

        return texto.toString();
    }

    /**
     * Comprueba que un texto obligatorio sea válido.
     *
     * Se rechazan valores vacíos, comas y saltos de línea para
     * evitar problemas con el formato CSV utilizado por el proyecto.
     *
     * @param texto texto que se desea validar
     * @return true si el texto es válido
     */
    private boolean textoValido(String texto) {

        return texto != null
                && !texto.trim().isEmpty()
                && texto.indexOf(',') < 0
                && texto.indexOf('\n') < 0
                && texto.indexOf('\r') < 0;
    }

    /**
     * Convierte el texto recibido en un tiempo de viaje válido.
     *
     * Solo acepta valores estrictamente positivos y finitos.
     *
     * @param texto representación textual del tiempo
     * @return tiempo válido o -1 si la entrada es incorrecta
     */
    private double leerTiempo(String texto) {

        try {

            double tiempo = Double.parseDouble(
                    texto.trim().replace(',', '.')
            );

            if (Double.isNaN(tiempo)
                    || Double.isInfinite(tiempo)) {
                return -1;
            }

            return tiempo > 0 ? tiempo : -1;

        } catch (NumberFormatException ex) {

            return -1;
        }
    }

    /**
     * Solicita al usuario un tiempo de viaje válido.
     *
     * @param mensaje instrucción que aparecerá en el diálogo
     * @return tiempo seleccionado o null si el usuario cancela
     */
    private Double pedirTiempo(String mensaje) {

        while (true) {

            String texto = JOptionPane.showInputDialog(
                    this,
                    mensaje,
                    "Tiempo de viaje",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (texto == null) {
                return null;
            }

            double tiempo = leerTiempo(texto);

            if (tiempo > 0) {
                return Double.valueOf(tiempo);
            }

            mostrarError(
                    "Ingrese un número mayor que 0 y finito "
                    + "(por ejemplo 3.5)."
            );
        }
    }

    /**
     * Convierte una lista de estaciones en una cadena de texto.
     *
     * @param lista estaciones que se mostrarán
     * @param separador texto utilizado entre estaciones
     * @return representación textual de la lista
     */
    private String listaATexto(
            ListaEnlazada<Estacion> lista,
            String separador
    ) {

        StringBuilder texto = new StringBuilder();

        Nodo<Estacion> nodo = lista.getCabeza();

        while (nodo != null) {

            Estacion estacion = nodo.getDato();

            texto.append(estacion.getNombre());

            if (nodo.getSiguiente() != null) {
                texto.append(separador);
            }

            nodo = nodo.getSiguiente();
        }

        return texto.toString();
    }

    /**
     * Marca la red como modificada.
     *
     * Debe invocarse después de que una operación de modificación
     * haya terminado satisfactoriamente.
     */
    private void marcarCambiosPendientes() {
        cambiosPendientes = true;
    }

    /**
     * Escribe una línea en el registro de la ventana.
     *
     * @param texto mensaje que se desea mostrar
     */
    public void mostrarMensaje(String texto) {

        if (areaLog == null) {
            return;
        }

        areaLog.append(texto + "\n");

        areaLog.setCaretPosition(
                areaLog.getDocument().getLength()
        );
    }

    /**
     * Muestra un cuadro de diálogo de error.
     *
     * @param texto descripción del error
     */
    public void mostrarError(String texto) {

        JOptionPane.showMessageDialog(
                this,
                texto,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Obtiene la tabla hash utilizada por el sistema.
     *
     * @return tabla hash actual
     */
    public TablaHash getTabla() {
        return tabla;
    }

    /**
     * Obtiene el grafo ponderado actual.
     *
     * @return grafo utilizado por la aplicación
     */
    public GrafoPonderado getGrafo() {
        return grafo;
    }

    /**
     * Obtiene el gestor de archivos.
     *
     * @return gestor encargado de la persistencia
     */
    public GestorArchivo getGestor() {
        return gestor;
    }

    /**
     * Obtiene el analizador de la red.
     *
     * @return analizador de algoritmos del grafo
     */
    public AnalizadorRed getAnalizador() {
        return analizador;
    }

    /**
     * Obtiene el visualizador gráfico de la red.
     *
     * @return visualizador actual
     */
    public VisualizadorGrafo getVisualizador() {
        return visualizador;
    }
}
