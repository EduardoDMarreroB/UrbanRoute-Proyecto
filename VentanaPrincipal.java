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
 * Ventana principal de UrbanRoute.
 *
 * Se encarga de crear el modelo ({@link TablaHash} y {@link GrafoPonderado}),
 * los controladores ({@link GestorArchivo} y {@link AnalizadorRed}) y el
 * visualizador del grafo. También construye la interfaz Swing y centraliza
 * las acciones que se ejecutan desde los botones.
 */
public class VentanaPrincipal extends JFrame {

    /** Tabla hash donde se guardan las estaciones usando su ID como clave. */
    private TablaHash tabla;

    /** Grafo ponderado que representa la red de estaciones y conexiones. */
    private GrafoPonderado grafo;

    /** Gestor encargado de leer y escribir los archivos CSV. */
    private GestorArchivo gestor;

    /** Analizador que ejecuta los algoritmos sobre el grafo. */
    private AnalizadorRed analizador;

    /** Visualizador que dibuja el grafo en la ventana. */
    private VisualizadorGrafo visualizador;

    /** Área de texto donde se van mostrando los mensajes de la aplicación. */
    private JTextArea areaLog;

    /** Última carpeta usada en los diálogos de archivos, para no empezar 
     * siempre desde cero. */
    private File ultimaCarpeta;

    /** Panel que divide la zona del grafo y el registro de mensajes. */
    private JSplitPane panelDividido;
    private String zona;

    /**
     * Crea la ventana principal y todos los objetos que necesita la aplicación.
     *
     * Primero se crean el modelo y los controladores. Después se conectan el
     * analizador y el visualizador para que puedan comunicarse entre sí.
     */
    public VentanaPrincipal() {
        super("UrbanRoute");
        tabla = new TablaHash();
        grafo = new GrafoPonderado(tabla);
        gestor = new GestorArchivo(this);
        analizador = new AnalizadorRed(grafo);
        visualizador = new VisualizadorGrafo(grafo, this);
        analizador.setVisualizador(visualizador);
        construirUI();
    }

    /**
     * Construye y organiza los componentes de la interfaz gráfica.
     *
     * La barra superior agrupa las acciones por tipo: archivos, red y análisis.
     * En el centro se coloca el panel dividido que muestra el grafo y el 
     * registro.
     */
    private void construirUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel grupoArchivos = grupo("Archivos",
        boton("Cargar diccionario", () -> accionCargarEstaciones()),
        boton("Cargar grafo", () -> accionCargarConexiones()),
        boton("Guardar cambios", () -> accionGuardar()),
        boton("Ver estación en detalle", () -> accionVerDetalleEstacion()));

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

        panelDividido = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        panelDividido.setTopComponent(new JLabel(
                "Cargue diccionario.csv y grafo.csv para ver la red", 
                SwingConstants.CENTER));
        panelDividido.setBottomComponent(new JScrollPane(areaLog));
        panelDividido.setDividerLocation(430);
        panelDividido.setResizeWeight(0.8);
        add(panelDividido, BorderLayout.CENTER);
    }

    /**
     * Crea un botón y le asocia la acción que debe ejecutar.
     *
     * @param texto  texto que se mostrará en el botón.
     * @param accion acción que se ejecuta al pulsar el botón.
     * @return el botón ya configurado.
     */
    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        return b;
    }

    /**
     * Crea un panel con título y una cuadrícula de botones.
     * 
     * Si se pasan menos de cuatro botones, se rellenan los huecos con etiquetas
     * vacías para que la cuadrícula no cambie de tamaño.
     *
     * @param titulo  título del panel.
     * @param botones botones que se quieren colocar dentro.
     * @return panel configurado con los botones indicados.
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
     * Carga el diccionario de estaciones desde un archivo CSV.
     *
     * Si ya hay estaciones en memoria, se pide confirmación porque cargar otro
     * diccionario borra las estaciones y conexiones actuales.
     */
    private void accionCargarEstaciones() {
        File f = elegirArchivo("Seleccionar diccionario.csv");
        if (f == null) return;

        // Cargar otro diccionario reinicia el grafo, así que se avisa antes 
        // de borrar nada.
        if (tabla.obtenerTamano() > 0 && !confirmarAdvertencia(
                "Ya hay " + tabla.obtenerTamano() + " estaciones en memoria.\n"
              + "Cargar otro diccionario BORRARÁ las estaciones y conexiones "
                        + "actuales. ¿Continuar?")) {
            return;
        }
        try {
            grafo.limpiar();
            int n = gestor.cargarEstaciones(f.getAbsolutePath(), tabla, grafo);
            mostrarMensaje("Diccionario cargado: " + n + " estaciones "
                    + "(" + gestor.getLineasInvalidas() + " líneas omitidas).");
            visualizador.sincronizar();
        } catch (IOException ex) {
            mostrarError("No se pudo leer el diccionario: " + ex.getMessage());
        }
    }

    /**
     * Carga las conexiones del grafo desde un archivo CSV.
     * 
     * Antes de cargar conexiones se comprueba que existan estaciones, porque
     * una conexión siempre debe unir dos estaciones ya registradas.
     */
    private void accionCargarConexiones() {
        if (grafo.numEstaciones() == 0) {
            mostrarError("Primero cargue el diccionario de estaciones.");
            return;
        }
        File f = elegirArchivo("Seleccionar grafo.csv");
        if (f == null) return;

        // Si ya hay conexiones, se reemplazan por las del archivo.
        if (grafo.numConexiones() > 0 && !confirmarAdvertencia(
                "Ya hay conexiones en memoria. Se reemplazarán por las del"
                        + "archivo. ¿Continuar?")) {
            return;
        }
        try {
            grafo.limpiarConexiones();
            int n = gestor.cargarConexiones(f.getAbsolutePath(), grafo);
            mostrarMensaje("Grafo cargado: " + n + " conexiones "
                    + "(" + gestor.getLineasInvalidas() + " líneas omitidas).");
            accionMostrarGrafo();
        } catch (IOException ex) {
            mostrarError("No se pudo leer el grafo: " + ex.getMessage());
        }
    }

    /**
     * Guarda el estado actual de la red en dos archivos CSV dentro de una 
     * carpeta.
     *
     * El usuario elige la carpeta destino y se avisa si ya existen
     * {@code diccionario.csv} o {@code grafo.csv}, porque serán sobrescritos.
     */
    private void accionGuardar() {
        JFileChooser fc = new JFileChooser(ultimaCarpeta);
        fc.setDialogTitle("Carpeta destino (se escribirán diccionario.csv "
                + "y grafo.csv)");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File carpeta = fc.getSelectedFile();
        ultimaCarpeta = carpeta;

        if ((new File(carpeta, "diccionario.csv").exists() || 
                new File(carpeta, "grafo.csv").exists())
                && !confirmarAdvertencia("Los archivos CSV existentes en esa "
                        + "carpeta serán SOBRESCRITOS. ¿Continuar?")) {
            return;
        }
        try {
            gestor.guardarCambios(carpeta.getAbsolutePath(), tabla, grafo);
            mostrarMensaje("Cambios guardados en " + carpeta.getAbsolutePath());
        } catch (IOException ex) {
            mostrarError("No se pudo guardar: " + ex.getMessage());
        }
    }

    /**
     * Muestra el grafo en el panel superior de la ventana.
     * 
     * Se conserva la posición del divisor para que el cambio de panel no mueva
     * la vista que el usuario ya tenía.
     */
    private void accionMostrarGrafo() {
        if (grafo.numEstaciones() == 0) {
            mostrarError("No hay datos cargados.");
            return;
        }
        Component panelGrafo = visualizador.obtenerComponenteVista();
        int divisor = panelDividido.getDividerLocation();
        panelDividido.setTopComponent(panelGrafo);
        panelDividido.setDividerLocation(divisor);
        panelDividido.revalidate();
        panelDividido.repaint();
    }
    
    /**
    * Permite seleccionar una estación y consultar todas sus propiedades.
     */
    private void accionVerDetalleEstacion() {
        if (grafo.numEstaciones() == 0) {
            mostrarError("No hay estaciones cargadas para consultar.");
            return;
        }
        Estacion estacion = elegirEstacion(
            "Ver estación en detalle",
            "Seleccione la estación que desea consultar:",
            todasLasEstaciones()
        );
         if (estacion == null) return;
         String detalles =
            "INFORMACIÓN DE LA ESTACIÓN"
            + "\n\nID: " + estacion.getId()
            + "\nNombre: " + estacion.getNombre()
            + "\nLínea: " + estacion.getLinea()
            + "\nZona: " + estacion.getZona()
            + "\nCapacidad: " + estacion.getCapacidad()
            + " personas"
            + "\nConexiones directas: " + estacion.getGrado();
        JOptionPane.showMessageDialog(
            this,
            detalles,
            "Detalle de la estación",
            JOptionPane.INFORMATION_MESSAGE
    );
}

    /**
     * Pide los datos de una nueva estación y la agrega al grafo.
     * 
     * El formulario se repite mientras haya errores de validación, de modo que
     * el usuario pueda corregir los datos sin perder lo que ya escribió.
     */
    private void accionAgregarEstacion() {

    JTextField campoId = new JTextField(12);
    JTextField campoNombre = new JTextField(12);
    JTextField campoZona = new JTextField(12);
    JTextField campoCapacidad = new JTextField("100", 12);
    JTextField campoTiempo = new JTextField(12);
    JComboBox<String> comboLinea = new JComboBox<String>(lineasExistentes());
    comboLinea.setEditable(true);
    comboLinea.setSelectedItem("Línea 1");
    Estacion[] existentes = todasLasEstaciones();
    Object[] opcionesVecino = new Object[existentes.length + 1];
    opcionesVecino[0] = "(sin conexión)";
    for (int i = 0; i < existentes.length; i++) {
        opcionesVecino[i + 1] = existentes[i];
    }
    JComboBox<Object> comboVecino =
            new JComboBox<Object>(opcionesVecino);
    // El tiempo permanece bloqueado hasta seleccionar una estación.
    JLabel etiquetaTiempo = new JLabel("Tiempo de viaje (min):");
    campoTiempo.setEnabled(false);
    etiquetaTiempo.setEnabled(false);
    comboVecino.addActionListener(e -> {
        boolean hayVecino =
                comboVecino.getSelectedItem() instanceof Estacion;
        campoTiempo.setEnabled(hayVecino);
        etiquetaTiempo.setEnabled(hayVecino);
        if (!hayVecino) {
            campoTiempo.setText("");
        }
    });
    JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
    panel.add(new JLabel("ID:"));
    panel.add(campoId);
    panel.add(new JLabel("Nombre:"));
    panel.add(campoNombre);
    panel.add(new JLabel("Línea (elegir o escribir):"));
    panel.add(comboLinea);
    panel.add(new JLabel("Zona:"));
    panel.add(campoZona);
    panel.add(new JLabel("Capacidad:"));
    panel.add(campoCapacidad);
    panel.add(new JLabel("Conectar con:"));
    panel.add(comboVecino);
    panel.add(etiquetaTiempo);
    panel.add(campoTiempo);
    while (true) {
        int r = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Agregar estación",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (r != JOptionPane.OK_OPTION) {
            return;
        }
        // Obtenemos los datos y normalizamos el texto.
        String id = campoId.getText().trim().toUpperCase(
                java.util.Locale.ROOT
        );
        // El ID se convierte a mayúsculas dentro del programa.
        campoId.setText(id);
        String nombre = campoNombre.getText().trim();
        String zona = campoZona.getText().trim();
        Object editor = comboLinea.getEditor().getItem();
        String linea = editor == null ? "": editor.toString().trim();
        // También se normaliza la línea a mayúsculas.
        comboLinea.getEditor().setItem(linea);
        String error = null;
        int capacidad = 0;
        double tiempo = 0;
        Estacion vecino = null;
        
        // Validamos el nombre.
        if (!textoSoloPalabrasYNumeros(nombre)) {
            error = "El nombre de la estación es obligatorio.\n"
                    + "Solo se permiten letras, números y espacios.";
        }
        
        // Validamos la zona.
        else if (!textoSoloPalabrasYNumeros(zona)) {
            error = "La zona es obligatoria.\n"
                    + "Solo se permiten letras, números y espacios.";
        }

        //Validamos el ID.
        else {
            String letrasNombre = nombre.replaceAll(
                    "[^\\p{L}]",
                    ""
            ).toUpperCase(java.util.Locale.ROOT);
            if (!id.matches("\\p{Lu}{3}")) {
                error = "El ID debe contener exactamente tres letras "
                        + "mayúsculas.";
            } else if (letrasNombre.length() < 3) {
                error = "El nombre de la estación debe contener al "
                        + "menos tres letras para generar su ID.";
            } else if (!id.equals(letrasNombre.substring(0, 3))) {
                error = "El ID debe corresponder a las primeras tres "
                        + "letras del nombre de la estación.\n"
                        + "ID esperado: "
                        + letrasNombre.substring(0, 3);
            } else if (grafo.buscarEstacion(id) != null) {
                error = "Ya existe una estación con el ID "
                        + id + ".";
            }
        }

       // Validamos la línea.
        if (error == null) {
            if (!linea.matches("(?iu)^l[ií]nea\\s+[0-9]+$")) {
        error = "La línea debe tener el formato Línea n.\n"
                + "Ejemplos válidos: Línea 1, Línea 2, Línea 10.";
            } else {
            // Extraer el número de línea.
            String numeroLinea = linea.replaceFirst(
                    "(?iu)^l[ií]nea\\s+","");
            // Normalizar para almacenarlo sin tilde.
            linea = "Linea " + numeroLinea;
        }
    }

        // Validamos la capacidad.
        if (error == null) {
            String textoCapacidad =
                    campoCapacidad.getText().trim();
            try {
                if (!textoCapacidad.matches("[0-9]+")) {
                    error = "La capacidad debe contener solamente "
                            + "números enteros positivos.";
                } else {
                    capacidad = Integer.parseInt(textoCapacidad);
                    if (capacidad < 100) {
                        error = "La capacidad mínima permitida "
                                + "es de 100 personas.";
                    }
                }

            } catch (NumberFormatException ex) {
                error = "La capacidad debe ser un número entero "
                        + "dentro del rango permitido.";
            }
        }

        // Validamos conexión y tiempo de viaje.
        if (error == null
                && comboVecino.getSelectedItem() instanceof Estacion) {
            vecino = (Estacion) comboVecino.getSelectedItem();
            String textoTiempo =
                    campoTiempo.getText().trim();
            try {
                // Se permiten enteros y decimales.
                // También se acepta coma como separador decimal.
                if (!textoTiempo.matches(
                        "[0-9]+([.,][0-9]+)?"
                )) {error = "El tiempo de viaje debe ser un número "
                            + "positivo.\nEjemplos: 3, 3.5 o 10.";
                } else {
                    tiempo = Double.parseDouble(
                            textoTiempo.replace(',', '.')
                    );
                    if (tiempo <= 0
                            || Double.isNaN(tiempo)
                            || Double.isInfinite(tiempo)) {
                        error = "El tiempo de viaje debe ser "
                                + "mayor que cero.";
                    }
                }
            } catch (NumberFormatException ex) {
                error = "El tiempo de viaje debe ser un número "
                        + "válido.";
            }
        }

        // Mostramos los errores sin cerrar el formulario.
        if (error != null) {
            mostrarError(error);
            continue;
        }

        // Creamos y agregamos la estación
        Estacion nueva = new Estacion(
                id,
                nombre,
                linea,
                zona,
                capacidad
        );
        grafo.agregarEstacion(nueva);
        if (vecino != null) {
            grafo.agregarConexion(
                    nueva,
                    vecino,
                    tiempo
            );
        }
        visualizador.sincronizar();
        mostrarMensaje(
                "Estación agregada: " + nueva
                + "\nID: " + id
                + "\nLínea: " + linea
                + "\nZona: " + zona
                + "\nCapacidad: " + capacidad
                + (vecino != null
                        ? "\nConectada con: " + vecino.getNombre()
                          + "\nTiempo: " + tiempo + " min."
                        : "\nSin conexiones todavía.")
        );
        return;
    }
}

    /**
     * Elimina una estación del grafo después de pedir confirmación.
     * 
     * Antes de eliminarla se guarda su grado para poder informar cuántas
     * conexiones se van a perder junto con la estación.
     */
    private void accionEliminarEstacion() {
        Estacion e = elegirEstacion("Eliminar estación", "Seleccione la "
                + "estación a eliminar:", todasLasEstaciones());
        if (e == null) return;

        int conexiones = e.getGrado();
        if (!confirmarAdvertencia("Se eliminará " + e + " y sus " + conexiones 
                + " conexiones. ¿Continuar?")) {
            return;
        }
        grafo.eliminarEstacion(e.getId());
        visualizador.sincronizar();
        mostrarMensaje("Estación eliminada: " + e + " (se eliminaron " 
                + conexiones + " conexiones).");
    }

    /**
     * Agrega o actualiza una conexión entre dos estaciones.
     * 
     * Se eligen origen y destino, y luego se pide el tiempo de viaje. Si la
     * conexión ya existía, el grafo la actualiza y solo cambia el mensaje.
     */
    private void accionAgregarConexion() {
        Estacion origen = elegirEstacion("Agregar conexión", 
                "Estación de origen:", todasLasEstaciones());
        if (origen == null) return;

        Estacion destino = elegirEstacion("Agregar conexión", 
                "Estación de destino:", todasExcepto(origen));
        if (destino == null) return;

        Double tiempo = pedirTiempo("Tiempo de viaje entre " 
                + origen.getNombre() + " y " + destino.getNombre() 
                + " (minutos):");
        if (tiempo == null) return;

        boolean yaExistia = grafo.buscarConexion(origen, destino) != null;
        grafo.agregarConexion(origen, destino, tiempo);
        visualizador.sincronizar();
        mostrarMensaje((yaExistia ? "Conexión actualizada: " : 
                "Conexión agregada: ")
                + origen.getNombre() + " <-> " + destino.getNombre() 
                + " (" + tiempo + " min).");
    }

    /**
     * Elimina la conexión entre dos estaciones.
     * 
     * Primero se elige la estación de origen y después solo se ofrecen sus
     * vecinos directos, porque no tiene sentido borrar una conexión que no existe.
     */
    private void accionEliminarConexion() {
        Estacion origen = elegirEstacion("Eliminar conexión", 
                "Estación de origen:", todasLasEstaciones());
        if (origen == null) return;

        Estacion[] vecinos = vecinosDe(origen);
        if (vecinos.length == 0) {
            mostrarError(origen.getNombre() + " no tiene conexiones.");
            return;
        }

        Estacion destino = elegirEstacion("Eliminar conexión", 
                "Conexión con:", vecinos);
        if (destino == null) return;

        grafo.eliminarConexion(origen, destino);
        visualizador.sincronizar();
        mostrarMensaje("Conexión eliminada: " + origen.getNombre() 
                + " <-> " + destino.getNombre() + ".");
    }

    /**
     * Calcula y muestra la ruta mínima entre dos estaciones.
     * 
     * Usa el algoritmo de Dijkstra del analizador. Si no hay camino, se avisa
     * al usuario. Si hay ruta, se muestra en el registro y se resalta en el 
     * grafo.
     */
    private void accionRutaMinima() {
        Estacion origen = elegirEstacion("Ruta mínima", "Estación de origen:",
                todasLasEstaciones());
        if (origen == null) return;

        Estacion destino = elegirEstacion("Ruta mínima", "Estación de destino:",
                todasLasEstaciones());
        if (destino == null) return;

        ListaEnlazada ruta = analizador.dijkstra(origen.getId(), 
                destino.getId());
        if (ruta == null) {
            mostrarMensaje("No existe camino entre " + origen.getNombre() 
                    + " y " + destino.getNombre() + ".");
            return;
        }
        mostrarMensaje("Ruta mínima (" + analizador.getUltimoTiempoRuta() 
                + " min, "
                + ruta.obtenerTamano() + " estaciones): " 
                + listaATexto(ruta, " -> "));

        // Si el grafo no se está viendo, se muestra antes de resaltar la ruta.
        if (!visualizador.estaMostrandose()) {
            accionMostrarGrafo();
        }
        visualizador.resaltarRuta(ruta);
    }

    /**
     * Analiza las subredes aisladas de la red.
     * 
     * Permite analizar la red actual, simular la baja de una estación o simular
     * la baja de una línea completa.
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
    int op = JOptionPane.showOptionDialog(
            this,
            "¿Qué desea analizar?",
            "Subredes aisladas",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            opciones,
            opciones[0]
    );
    if (op < 0) {
        return;
    }
    if (op == 0) {
        mostrarSubredes("red actual",analizador.detectarSubredesBFS());
    } else if (op == 1) {
        Estacion e = elegirEstacion(
                "Simular baja de estación",
                "Estación que se desconecta:",
                todasLasEstaciones()
        );
        if (e == null) {
            return;
        }
        mostrarSubredes(
                "sin la estación " + e.getNombre(),
                analizador.detectarSubredesSinEstacion(e.getId())
        );
    } else {
        String[] lineas = lineasExistentes();
        if (lineas.length == 0) {
            mostrarError("No existen líneas para analizar.");
            return;
        }
        // Mostrar las líneas con tilde en la interfaz.
        for (int i = 0; i < lineas.length; i++) {
            lineas[i] = lineas[i].replaceFirst(
                    "(?iu)^l[ií]nea\\s+",
                    "Línea "
            );
        }
        JComboBox<String> comboLinea =
                new JComboBox<String>(lineas);
        // Solo se permite seleccionar líneas existentes.
        comboLinea.setEditable(false);
        int seleccion = JOptionPane.showConfirmDialog(
                this,
                comboLinea,
                "Seleccionar línea que se desconectará",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (seleccion != JOptionPane.OK_OPTION) {
            return;
        }
        String lineaSeleccionada =
                (String) comboLinea.getSelectedItem();
        if (lineaSeleccionada == null) {
            mostrarError("Debe seleccionar una línea.");
            return;
        }
        // Convertir al formato interno sin tilde.
        String linea = lineaSeleccionada.replaceFirst(
                "(?iu)^l[ií]nea\\s+",
                "Linea "
        );
        mostrarSubredes(
                "sin la línea " + lineaSeleccionada,
                analizador.detectarSubredesSinLinea(linea)
        );
    }
}
    /**
     * Muestra en el registro las subredes encontradas.
     *
     * Cada subred es una ListaEnlazada dentro de otra lista. Si una
     * subred tiene una sola estación, se marca como aislada.
     *
     * @param descripcion texto que indica qué caso se está analizando.
     * @param subredes    lista que contiene las subredes encontradas.
     */
    private void mostrarSubredes(
        String descripcion,
        ListaEnlazada<ListaEnlazada<Estacion>> subredes) {
            mostrarMensaje(
                "Subredes (" + descripcion + "): "
             + subredes.obtenerTamano()
            );
        Nodo<ListaEnlazada<Estacion>> actual = subredes.getCabeza();
        int numeroSubred = 1;
        while (actual != null) {
        ListaEnlazada<Estacion> subred = actual.getDato();
        mostrarMensaje(
                "  #" + numeroSubred
                + (subred.obtenerTamano() == 1 ? " [AISLADA]" : "")
                + " (" + subred.obtenerTamano() + " estaciones): "
                + listaATexto(subred, ", ")
        );
        actual = actual.getSiguiente();
        numeroSubred++;
    }
}

    /**
     * Calcula y muestra los hubs de la red.
     * 
     * Un hub es una estación con muchas conexiones directas. El usuario indica
     * cuántos quiere ver y se muestran ordenados por centralidad de grado.
     */
    private void accionHubs() {
        if (grafo.numEstaciones() == 0) {
            mostrarError("No hay datos cargados.");
            return;
        }
        String texto = JOptionPane.showInputDialog(this, 
                "¿Cuántos hubs mostrar?", "5");
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

        ListaEnlazada hubs = analizador.calcularCentralidadGrado(k);
        mostrarMensaje("Hubs (top " + hubs.obtenerTamano() 
                + " por conexiones directas):");
        int puesto = 1;
        for (Nodo n = hubs.getCabeza(); n != null; n = n.getSiguiente()) {
            Estacion e = (Estacion) n.getDato();
            mostrarMensaje("  " + puesto++ + ". " + e + " - línea " 
                    + e.getLinea()
                    + " - " + e.getGrado() + " conexiones");
        }
    }

    /**
     * Abre un diálogo para elegir un archivo CSV.
     *
     * @param titulo título que se mostrará en el diálogo.
     * @return el archivo elegido, o {@code null} si el usuario cancela.
     */
    private File elegirArchivo(String titulo) {
        JFileChooser fc = new JFileChooser(ultimaCarpeta);
        fc.setDialogTitle(titulo);
        fc.setFileFilter(new FileNameExtensionFilter("Archivos CSV", "csv"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;

        File f = fc.getSelectedFile();
        ultimaCarpeta = f.getParentFile();
        return f;
    }

    /**
     * Muestra un cuadro de advertencia con opciones Sí/No.
     *
     * @param mensaje texto que se mostrará al usuario.
     * @return {@code true} si el usuario confirma; {@code false} en caso 
     * contrario.
     */
    private boolean confirmarAdvertencia(String mensaje) {
        int r = JOptionPane.showConfirmDialog(this, mensaje, "Advertencia",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return r == JOptionPane.YES_OPTION;
    }

    /**
     * Pide al usuario que elija una estación de una lista.
     *
     * @param titulo   título del diálogo.
     * @param mensaje  mensaje que se muestra antes de la lista.
     * @param opciones estaciones entre las que se puede elegir.
     * @return la estación elegida, o {@code null} si no hay opciones o se 
     * cancela.
     */
    private Estacion elegirEstacion(String titulo, String mensaje, 
            Estacion[] opciones) {
        if (opciones.length == 0) {
            mostrarError("No hay estaciones disponibles.");
            return null;
        }
        Object sel = JOptionPane.showInputDialog(this, mensaje, titulo,
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        return (Estacion) sel;
    }

    /**
     * Convierte la lista de estaciones del grafo en un arreglo.
     * 
     * Se usa para poder pasar las estaciones a los diálogos de Swing, que
     * trabajan mejor con arreglos.
     *
     * @return arreglo con todas las estaciones del grafo.
     */
    private Estacion[] todasLasEstaciones() {
        Estacion[] arreglo = new Estacion[grafo.numEstaciones()];
        int i = 0;
        for (Nodo n = grafo.getEstaciones().getCabeza(); n != null; 
                n = n.getSiguiente()) {
            arreglo[i++] = (Estacion) n.getDato();
        }
        return arreglo;
    }

    /**
     * Devuelve todas las estaciones menos una.
     *
     * @param excluida estación que no debe aparecer en el resultado.
     * @return arreglo con las estaciones restantes.
     */
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

    /**
     * Obtiene las estaciones vecinas directas de una estación.
     * <p>
     * Recorre las conexiones del origen y toma el destino de cada una.
     *
     * @param origen estación de la que se quieren conocer los vecinos.
     * @return arreglo con las estaciones vecinas.
     */
    private Estacion[] vecinosDe(Estacion origen) {
        ListaEnlazada conexiones = grafo.obtenerVecinos(origen);
        Estacion[] vecinos = new Estacion[conexiones.obtenerTamano()];
        int i = 0;
        for (Nodo n = conexiones.getCabeza(); n != null; n = n.getSiguiente()) {
            vecinos[i++] = ((Conexion) n.getDato()).getDestino();
        }
        return vecinos;
    }

    /**
     * Obtiene las líneas que existen actualmente en el grafo.
     *
     * @return arreglo con los nombres de las líneas.
     */
    private String[] lineasExistentes() {
        ListaEnlazada lineas = grafo.obtenerLineas();
        String[] arreglo = new String[lineas.obtenerTamano()];
        int i = 0;
        for (Nodo n = lineas.getCabeza(); n != null; n = n.getSiguiente()) {
            arreglo[i++] = (String) n.getDato();
        }
        return arreglo;
    }

    /**
     * Comprueba que un texto sea válido para guardarlo en el CSV.
     * <p>
     * El formato CSV usa comas y saltos de línea como separadores, por eso no
     * se permiten dentro de los campos.
     *
     * @param texto texto que se quiere validar.
     * @return {@code true} si el texto no está vacío y no contiene comas ni 
     * saltos de línea.
     */
    private boolean textoValido(String texto) {
        return texto.length() > 0 && texto.indexOf(',') < 0 && 
                texto.indexOf('\n') < 0;
    }

    /**
     * Convierte un texto en un tiempo de viaje.
     *
     * Acepta coma como separador decimal. Si el valor no es válido o no es
     * mayor que cero, devuelve {@code -1}.
     *
     * @param texto texto que contiene el tiempo.
     * @return el tiempo convertido, o {@code -1} si no es válido.
     */
    private double leerTiempo(String texto) {
        try {
            double t = Double.parseDouble(texto.trim().replace(',', '.'));
            return t > 0 ? t : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Pide un tiempo de viaje al usuario hasta que sea válido o cancele.
     *
     * @param mensaje mensaje que se mostrará en el diálogo.
     * @return el tiempo introducido, o {@code null} si el usuario cancela.
     */
    private Double pedirTiempo(String mensaje) {
        while (true) {
            String texto = JOptionPane.showInputDialog(this, mensaje, 
                    "Tiempo de viaje", JOptionPane.QUESTION_MESSAGE);
            if (texto == null) return null;

            double t = leerTiempo(texto);
            if (t > 0) return Double.valueOf(t);
            mostrarError("Ingrese un número mayor que 0 (por ejemplo 3.5).");
        }
    }

    /**
     * Convierte una lista enlazada en un texto legible.
     * <p>
     * Si el dato es una estación, se usa su nombre; si no, se usa su
     * representación normal con {@code toString()}.
     *
     * @param lista      lista que se quiere convertir.
     * @param separador  texto que se coloca entre los elementos.
     * @return cadena con todos los elementos de la lista.
     */
    private String listaATexto(ListaEnlazada lista, String separador) {
        StringBuilder sb = new StringBuilder();
        for (Nodo n = lista.getCabeza(); n != null; n = n.getSiguiente()) {
            Object d = n.getDato();
            sb.append(d instanceof Estacion ? ((Estacion) d).getNombre() : 
                    String.valueOf(d));
            if (n.getSiguiente() != null) sb.append(separador);
        }
        return sb.toString();
    }

    /**
     * Escribe una línea en el registro de la ventana.
     *
     * @param texto mensaje que se quiere mostrar.
     */
    public void mostrarMensaje(String texto) {
        areaLog.append(texto + "\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    /**
     * Muestra un cuadro de error.
     *
     * @param texto mensaje de error que se quiere mostrar.
     */
    public void mostrarError(String texto) {
        JOptionPane.showMessageDialog(this, texto, "Error", 
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Devuelve la tabla hash de estaciones.
     *
     * @return tabla hash usada por la ventana.
     */
    public TablaHash getTabla() { return tabla; }

    /**
     * Devuelve el grafo ponderado de la red.
     *
     * @return grafo usado por la ventana.
     */
    public GrafoPonderado getGrafo() { return grafo; }

    /**
     * Devuelve el gestor de archivos.
     *
     * @return gestor encargado de leer y guardar los CSV.
     */
    public GestorArchivo getGestor() { return gestor; }

    /**
     * Devuelve el analizador de la red.
     *
     * @return analizador que ejecuta los algoritmos sobre el grafo.
     */
    public AnalizadorRed getAnalizador() { return analizador; }

    /**
     * Devuelve el visualizador del grafo.
     *
     * @return visualizador encargado de dibujar la red.
     */
    public VisualizadorGrafo getVisualizador() { return visualizador; }

    private boolean textoSoloPalabrasYNumeros(String texto) {
    if (texto == null || texto.trim().isEmpty()) {
        return false;
    }
    return texto.matches(
            "[\\p{L}\\p{N}]+(?: +[\\p{L}\\p{N}]+)*"
    );
}
}
