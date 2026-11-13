package urbanroute.control;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import urbanroute.interfaz.VentanaPrincipal;
import urbanroute.modelo.Conexion;
import urbanroute.modelo.Estacion;
import urbanroute.modelo.GrafoPonderado;
import urbanroute.modelo.TablaHash;
import urbanroute.tda.ListaEnlazada;
import urbanroute.tda.Nodo;

/**
 * Gestiona la lectura y escritura de los archivos CSV de la red.
 * <p>
 * Se encarga de leer el diccionario de estaciones y el archivo de conexiones,
 * validando que el formato sea correcto y omitiendo las líneas que no lo sean.
 * También se encarga de guardar los cambios de forma segura usando archivos
 * temporales y respaldos para no perder datos si ocurre un error a mitad de
 * la escritura.
 */
public class GestorArchivo {

    /** Ventana principal, utilizada para mostrar los errores o avisos al usuario. */
    private VentanaPrincipal ventana;

    /** Contador de líneas que se omitieron durante la última carga por ser inválidas. */
    private int lineasInvalidas;

    /**
     * Construye un gestor de archivos asociado a una ventana principal.
     *
     * @param ventana ventana que se usará para notificar los errores.
     */
    public GestorArchivo(VentanaPrincipal ventana) {
        this.ventana = ventana;
        this.lineasInvalidas = 0;
    }

    /**
     * Carga estaciones desde un archivo CSV y las añade al grafo.
     *
     * @param ruta  ruta del archivo CSV que se quiere leer.
     * @param grafo grafo al que se agregarán las estaciones.
     * @return la cantidad de estaciones que se cargaron correctamente.
     * @throws IOException si hay un problema al leer el archivo.
     */
    public int cargarEstaciones(
            String ruta, GrafoPonderado grafo) throws IOException {
        return cargarEstacionesInterno(ruta, null, grafo);
    }

    /**
     * Carga estaciones verificando también la tabla hash asociada al grafo.
     *
     * @param ruta  ruta del archivo CSV que se quiere leer.
     * @param tabla tabla hash que debe coincidir con la del grafo.
     * @param grafo grafo al que se agregarán las estaciones.
     * @return la cantidad de estaciones que se cargaron correctamente.
     * @throws IOException              si hay un problema al leer el archivo.
     * @throws IllegalArgumentException si la tabla hash es nula.
     */
    public int cargarEstaciones(
            String ruta,
            TablaHash tabla,
            GrafoPonderado grafo) throws IOException {
        if (tabla == null) {
            throw new IllegalArgumentException(
                    "La tabla hash no puede ser nula."
            );
        }
        return cargarEstacionesInterno(ruta, tabla, grafo);
    }

    /**
     * Procesa y carga las estaciones validando el archivo de texto.
     * <p>
     * Lee el archivo completo en dos fases: primero valida y guarda las
     * estaciones en una lista temporal, y solo si todo el archivo está bien,
     * las agrega al grafo principal. Así se evita dejar el grafo a medias si
     * hay un error.
     *
     * @param ruta  ruta del archivo CSV.
     * @param tabla tabla hash para verificar la coherencia (puede ser nula).
     * @param grafo grafo al que se agregarán las estaciones.
     * @return la cantidad de estaciones cargadas.
     * @throws IOException si hay un error de lectura o el encabezado no es válido.
     */
    private int cargarEstacionesInterno(
            String ruta,
            TablaHash tabla,
            GrafoPonderado grafo) throws IOException {
        validarParametros(ruta, grafo);
        lineasInvalidas = 0;
        ListaEnlazada<Estacion> pendientes = new ListaEnlazada<>();

        // Tabla auxiliar para detectar IDs repetidos dentro del mismo archivo.
        TablaHash identificadoresLeidos = new TablaHash();
        boolean encabezadoEncontrado = false;
        try (BufferedReader lector = abrirLectura(ruta)) {
            String lineaActual;
            int numeroLinea = 0;
            while ((lineaActual = lector.readLine()) != null) {
                numeroLinea++;
                if (esIgnorable(lineaActual)) {
                    continue;
                }
                String[] columnas;
                try {
                    columnas = analizarLineaCSV(lineaActual);
                } catch (IOException ex) {
                    reportarInvalida(
                            numeroLinea,
                            "Formato CSV incorrecto: " + lineaActual
                    );
                    continue;
                }
                if (!encabezadoEncontrado) {
                    if (!validarEncabezadoEstaciones(columnas)) {
                        reportarInvalida(
                                numeroLinea,
                                "El encabezado del diccionario.csv no es válido."
                        );
                        throw new IOException(
                                "Encabezado incorrecto en diccionario.csv."
                        );
                    }
                    encabezadoEncontrado = true;
                    continue;
                }
                if (columnas.length != 5) {
                    reportarInvalida(numeroLinea, lineaActual);
                    continue;
                }
                try {
                    String id = columnas[0].trim();
                    String nombre = columnas[1].trim();
                    String lineaTransporte = columnas[2].trim();
                    String zona = columnas[3].trim();
                    int capacidad = Integer.parseInt(
                            columnas[4].trim()
                    );
                    Estacion estacion = new Estacion(
                            id,
                            nombre,
                            lineaTransporte,
                            zona,
                            capacidad
                    );

                    // Si el ID ya se leyó en este mismo archivo, no se repite.
                    if (identificadoresLeidos.contiene(id)) {
                        reportarInvalida(
                                numeroLinea,
                                lineaActual + " (ID repetido)"
                        );
                        continue;
                    }
                    identificadoresLeidos.insertar(id, estacion);
                    pendientes.insertar(estacion);
                } catch (IllegalArgumentException ex) {
                    reportarInvalida(
                            numeroLinea,
                            lineaActual + " (" + ex.getMessage() + ")"
                    );
                }
            }
        }
        if (!encabezadoEncontrado) {
            throw new IOException(
                    "El archivo no contiene un encabezado válido."
            );
        }
        ListaEnlazada<Estacion> agregadas = new ListaEnlazada<>();
        int cargadas = 0;
        try {
            for (Nodo<Estacion> nodo = pendientes.getCabeza();
                 nodo != null;
                 nodo = nodo.getSiguiente()) {
                Estacion estacion = nodo.getDato();

                // Intenta agregar la estación al grafo principal.
                if (!grafo.agregarEstacion(estacion)) {
                    reportarInvalida(
                            0,
                            estacion.getId()
                                    + " (estación ya existente en el grafo)"
                    );
                    continue;
                }
                agregadas.insertar(estacion);

                // Si nos pasaron una tabla hash externa, comprobamos que la estación
                // se haya insertado correctamente y que sea la misma instancia.
                if (tabla != null
                        && tabla.buscar(estacion.getId()) != estacion) {
                    throw new IOException(
                            "La tabla hash proporcionada no coincide "
                            + "con el índice interno del grafo."
                    );
                }
                cargadas++;
            }
        } catch (IOException ex) {
            // Si algo falla, se eliminan las estaciones que ya se habían agregado
            // para dejar el grafo como estaba antes de la carga.
            revertirEstaciones(grafo, agregadas);
            throw ex;
        }
        return cargadas;
    }

    /**
     * Revierte la incorporación de estaciones ante una falla.
     *
     * @param grafo     grafo del que se eliminarán las estaciones.
     * @param agregadas lista de estaciones que se habían agregado.
     */
    private void revertirEstaciones(
            GrafoPonderado grafo,
            ListaEnlazada<Estacion> agregadas) {
        for (Nodo<Estacion> nodo = agregadas.getCabeza();
             nodo != null;
             nodo = nodo.getSiguiente()) {

            grafo.eliminarEstacion(nodo.getDato().getId());
        }
    }

    /**
     * Carga las conexiones desde un archivo CSV al grafo.
     * <p>
     * Al igual que con las estaciones, primero se validan todas las líneas y
     * se guardan en una lista temporal. Solo cuando el archivo entero es
     * correcto, se agregan al grafo.
     *
     * @param ruta  ruta del archivo CSV de conexiones.
     * @param grafo grafo al que se agregarán las conexiones.
     * @return la cantidad de conexiones cargadas correctamente.
     * @throws IOException si hay un problema de lectura o el encabezado no es válido.
     */
    public int cargarConexiones(
            String ruta, GrafoPonderado grafo) throws IOException {
        validarParametros(ruta, grafo);
        lineasInvalidas = 0;
        ListaEnlazada<Conexion> pendientes = new ListaEnlazada<>();
        boolean encabezadoEncontrado = false;
        try (BufferedReader lector = abrirLectura(ruta)) {
            String lineaActual;
            int numeroLinea = 0;
            while ((lineaActual = lector.readLine()) != null) {
                numeroLinea++;
                if (esIgnorable(lineaActual)) {
                    continue;
                }
                String[] columnas;
                try {
                    columnas = analizarLineaCSV(lineaActual);
                } catch (IOException ex) {
                    reportarInvalida(
                            numeroLinea,
                            "Formato CSV incorrecto: " + lineaActual
                    );
                    continue;
                }
                if (!encabezadoEncontrado) {
                    if (!validarEncabezadoConexiones(columnas)) {
                        reportarInvalida(
                                numeroLinea,
                                "El encabezado de grafo.csv no es válido."
                        );
                        throw new IOException(
                                "Encabezado incorrecto en grafo.csv."
                        );
                    }
                    encabezadoEncontrado = true;
                    continue;
                }
                if (columnas.length != 3) {
                    reportarInvalida(numeroLinea, lineaActual);
                    continue;
                }
                try {
                    String idOrigen = columnas[0].trim();
                    String idDestino = columnas[1].trim();
                    double tiempo = Double.parseDouble(
                            columnas[2].trim()
                    );

                    // Validamos que no falten datos y que el tiempo sea un número real positivo.
                    if (idOrigen.isEmpty()
                            || idDestino.isEmpty()
                            || !Double.isFinite(tiempo)
                            || tiempo < 0) {
                        reportarInvalida(
                                numeroLinea,
                                lineaActual + " (datos de conexión inválidos)"
                        );
                        continue;
                    }
                    Estacion origen = grafo.buscarEstacion(idOrigen);
                    Estacion destino = grafo.buscarEstacion(idDestino);

                    // No se puede crear una conexión si alguna de las dos estaciones no existe.
                    if (origen == null || destino == null) {
                        reportarInvalida(
                                numeroLinea,
                                lineaActual + " (estación inexistente)"
                        );
                        continue;
                    }
                    Conexion conexion = new Conexion(
                            origen,
                            destino,
                            tiempo
                    );
                    pendientes.insertar(conexion);
                } catch (IllegalArgumentException ex) {
                    reportarInvalida(
                            numeroLinea,
                            lineaActual + " (" + ex.getMessage() + ")"
                    );
                }
            }
        }
        if (!encabezadoEncontrado) {
            throw new IOException(
                    "El archivo no contiene un encabezado válido."
            );
        }
        int cargadas = 0;
        for (Nodo<Conexion> nodo = pendientes.getCabeza();
             nodo != null;
             nodo = nodo.getSiguiente()) {
            Conexion conexion = nodo.getDato();

            // Se inserta la conexión comprobando si el grafo la acepta.
            if (grafo.agregarConexion(
                    conexion.getOrigen(),
                    conexion.getDestino(),
                    conexion.getTiempo())) {
                cargadas++;
            } else {
                reportarInvalida(
                        0,
                        conexion.toCSVString()
                                + " (no se pudo agregar la conexión)"
                );
            }
        }
        return cargadas;
    }

    /**
     * Guarda los cambios de la red en la carpeta especificada.
     *
     * @param carpeta ruta de la carpeta donde se guardarán los archivos.
     * @param grafo   grafo con los datos a guardar.
     * @throws IOException si ocurre un error al escribir los archivos.
     */
    public void guardarCambios(
            String carpeta, GrafoPonderado grafo) throws IOException {
        guardarCambiosInterno(carpeta, null, grafo);
    }

    /**
     * Guarda los archivos verificando la consistencia con la tabla hash.
     *
     * @param carpeta ruta de la carpeta donde se guardarán los archivos.
     * @param tabla   tabla hash que debe coincidir con la del grafo.
     * @param grafo   grafo con los datos a guardar.
     * @throws IOException              si ocurre un error al escribir.
     * @throws IllegalArgumentException si la tabla hash es nula.
     */
    public void guardarCambios(
            String carpeta,
            TablaHash tabla,
            GrafoPonderado grafo) throws IOException {
        if (tabla == null) {
            throw new IllegalArgumentException(
                    "La tabla hash no puede ser nula."
            );
        }
        guardarCambiosInterno(carpeta, tabla, grafo);
    }

    /**
     * Ejecuta la escritura y el reemplazo seguro de ambos archivos.
     * <p>
     * Para no corromper los archivos originales si hay un error, primero se
     * escribe todo en archivos temporales y, solo si ambos están bien, se
     * reemplazan los originales.
     *
     * @param carpeta ruta de la carpeta destino.
     * @param tabla   tabla hash para verificar coherencia (puede ser nula).
     * @param grafo   grafo con los datos a guardar.
     * @throws IOException si la carpeta no es válida o hay un fallo de escritura.
     */
    private void guardarCambiosInterno(
            String carpeta,
            TablaHash tabla,
            GrafoPonderado grafo) throws IOException {
        if (carpeta == null || carpeta.trim().isEmpty()) {
            throw new IOException(
                    "La carpeta de destino no puede estar vacía."
            );
        }
        if (grafo == null) {
            throw new IllegalArgumentException(
                    "El grafo no puede ser nulo."
            );
        }
        File directorio = new File(carpeta);
        if (!directorio.isDirectory()) {
            throw new IOException(
                    "La ruta indicada no es una carpeta válida: "
                    + carpeta
            );
        }

        // Comprobamos que los datos sean coherentes antes de empezar a escribir.
        validarConsistencia(grafo, tabla);
        File diccionarioDestino = new File(
                directorio,
                "diccionario.csv"
        );
        File grafoDestino = new File(
                directorio,
                "grafo.csv"
        );
        File diccionarioTemporal = null;
        File grafoTemporal = null;
        try {
            // Creamos archivos temporales en la misma carpeta para que el movimiento
            // final sea rápido y seguro.
            diccionarioTemporal = File.createTempFile(
                    "diccionario-",
                    ".tmp",
                    directorio
            );
            grafoTemporal = File.createTempFile(
                    "grafo-",
                    ".tmp",
                    directorio
            );
            escribirEstaciones(diccionarioTemporal, grafo);
            escribirConexiones(grafoTemporal, grafo);

            // Solo reemplazamos los originales cuando ambos temporales están listos.
            reemplazarArchivos(
                    diccionarioTemporal,
                    diccionarioDestino,
                    grafoTemporal,
                    grafoDestino
            );
        } finally {
            // Pase lo que pase, borramos los temporales para no dejar basura.
            eliminarTemporal(diccionarioTemporal);
            eliminarTemporal(grafoTemporal);
        }
    }

    /**
     * Verifica que todas las estaciones estén registradas en sus índices.
     * <p>
     * Si la estación está en la lista pero no en el índice del grafo (o en la
     * tabla hash externa), algo ha fallado y no se debe guardar.
     *
     * @param grafo grafo que se quiere comprobar.
     * @param tabla tabla hash externa a comparar (puede ser nula).
     * @throws IOException si se encuentra alguna inconsistencia.
     */
    private void validarConsistencia(
            GrafoPonderado grafo,
            TablaHash tabla) throws IOException {
        ListaEnlazada<Estacion> estaciones = grafo.getEstaciones();
        for (Nodo<Estacion> nodo = estaciones.getCabeza();
             nodo != null;
             nodo = nodo.getSiguiente()) {
            Estacion estacion = nodo.getDato();
            Estacion registrada = grafo.buscarEstacion(
                    estacion.getId()
            );
            if (registrada != estacion) {
                throw new IOException(
                        "La estación " + estacion.getId()
                        + " no coincide con el índice del grafo."
                );
            }
            if (tabla != null
                    && tabla.buscar(estacion.getId()) != estacion) {
                throw new IOException(
                        "La estación " + estacion.getId()
                        + " no coincide con la tabla hash recibida."
                );
            }
        }
    }

    /**
     * Escribe las estaciones en el archivo temporal del diccionario.
     *
     * @param archivo archivo temporal donde se escribirán los datos.
     * @param grafo   grafo del que se obtienen las estaciones.
     * @throws IOException si ocurre un error al escribir.
     */
    private void escribirEstaciones(
            File archivo, GrafoPonderado grafo) throws IOException {
        try (BufferedWriter escritor = abrirEscritura(archivo)) {
            escritor.write(
                    "ID_Estacion,Nombre,Linea,Zona,"
                    + "Capacidad_Pasajeros_Hora"
            );
            escritor.newLine();
            ListaEnlazada<Estacion> estaciones = grafo.getEstaciones();
            for (Nodo<Estacion> nodo = estaciones.getCabeza();
                 nodo != null;
                 nodo = nodo.getSiguiente()) {
                Estacion estacion = nodo.getDato();
                escritor.write(estacion.toCSVString());
                escritor.newLine();
            }
        }
    }

    /**
     * Escribe las conexiones en el archivo temporal del grafo.
     *
     * @param archivo archivo temporal donde se escribirán los datos.
     * @param grafo   grafo del que se obtienen las conexiones.
     * @throws IOException si ocurre un error al escribir.
     */
    private void escribirConexiones(
            File archivo, GrafoPonderado grafo) throws IOException {
        try (BufferedWriter escritor = abrirEscritura(archivo)) {
            escritor.write(
                    "Estacion_Origen,Estacion_Destino,Tiempo_Minutos"
            );
            escritor.newLine();
            ListaEnlazada<Estacion> estaciones = grafo.getEstaciones();
            for (Nodo<Estacion> nodoEstacion = estaciones.getCabeza();
                 nodoEstacion != null;
                 nodoEstacion = nodoEstacion.getSiguiente()) {
                Estacion estacion = nodoEstacion.getDato();
                ListaEnlazada<Conexion> conexiones =
                        grafo.obtenerVecinos(estacion);
                for (Nodo<Conexion> nodoConexion = conexiones.getCabeza();
                     nodoConexion != null;
                     nodoConexion = nodoConexion.getSiguiente()) {
                    Conexion conexion = nodoConexion.getDato();

                    /*
                     * En redes no dirigidas se escribe una sola orientación
                     * para evitar duplicar las conexiones inversas. Se usa
                     * comparar() para elegir siempre la misma dirección.
                     */
                    if (grafo.esDirigido()
                            || conexion.getOrigen().comparar(
                                    conexion.getDestino()
                            ) < 0) {
                        escritor.write(
                                escaparCSV(conexion.getOrigen().getId())
                                + ","
                                + escaparCSV(conexion.getDestino().getId())
                                + ","
                                + conexion.getTiempo()
                        );
                        escritor.newLine();
                    }
                }
            }
        }
    }

    /**
     * Reemplaza los archivos originales creando respaldos de seguridad.
     * <p>
     * Antes de sobrescribir, se hace una copia de los archivos actuales.
     * Si algo falla durante el reemplazo, se restauran esos respaldos.
     *
     * @param temporalDiccionario archivo temporal del diccionario.
     * @param destinoDiccionario  archivo definitivo del diccionario.
     * @param temporalGrafo       archivo temporal del grafo.
     * @param destinoGrafo        archivo definitivo del grafo.
     * @throws IOException si ocurre un error durante el proceso.
     */
    private void reemplazarArchivos(
            File temporalDiccionario,
            File destinoDiccionario,
            File temporalGrafo,
            File destinoGrafo) throws IOException {
        boolean existiaDiccionario = destinoDiccionario.exists();
        boolean existiaGrafo = destinoGrafo.exists();
        File respaldoDiccionario = null;
        File respaldoGrafo = null;
        boolean diccionarioReemplazado = false;
        boolean grafoReemplazado = false;
        try {
            if (existiaDiccionario) {
                respaldoDiccionario = File.createTempFile(
                        "respaldo-diccionario-",
                        ".tmp",
                        destinoDiccionario.getParentFile()
                );
                Files.copy(
                        destinoDiccionario.toPath(),
                        respaldoDiccionario.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
            if (existiaGrafo) {
                respaldoGrafo = File.createTempFile(
                        "respaldo-grafo-",
                        ".tmp",
                        destinoGrafo.getParentFile()
                );
                Files.copy(
                        destinoGrafo.toPath(),
                        respaldoGrafo.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
            moverArchivo(
                    temporalDiccionario,
                    destinoDiccionario
            );
            diccionarioReemplazado = true;
            moverArchivo(
                    temporalGrafo,
                    destinoGrafo
            );
            grafoReemplazado = true;
        } catch (IOException ex) {
            // Restaura los archivos anteriores si ocurre una falla.
            if (diccionarioReemplazado) {
                restaurarArchivo(
                        destinoDiccionario,
                        respaldoDiccionario,
                        existiaDiccionario,
                        ex
                );
            }
            if (grafoReemplazado) {
                restaurarArchivo(
                        destinoGrafo,
                        respaldoGrafo,
                        existiaGrafo,
                        ex
                );
            }
            throw ex;
        } finally {
            // Elimina los archivos de respaldo temporales.
            eliminarTemporal(respaldoDiccionario);
            eliminarTemporal(respaldoGrafo);
        }
    }

    /**
     * Mueve un archivo temporal a su ubicación definitiva de forma segura.
     * <p>
     * Intenta usar un movimiento atómico, que no deja el archivo a medias.
     * Si el sistema de archivos no lo soporta, hace un movimiento normal.
     *
     * @param origen  archivo temporal.
     * @param destino archivo definitivo.
     * @throws IOException si no se puede mover el archivo.
     */
    private void moverArchivo(File origen, File destino)
            throws IOException {
        try {
            Files.move(
                    origen.toPath(),
                    destino.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(
                    origen.toPath(),
                    destino.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    /**
     * Restaura un archivo desde su respaldo en caso de error.
     * <p>
     * Si el archivo no existía antes, simplemente se borra el que se había
     * creado a medias.
     *
     * @param destino archivo que se quiere restaurar.
     * @param respaldo archivo de respaldo (puede ser nulo).
     * @param existia indica si el archivo original existía antes.
     * @param causa excepción original a la que se le añadirá este error si falla.
     */
    private void restaurarArchivo(
            File destino,
            File respaldo,
            boolean existia,
            IOException causa) {
        try {
            if (existia && respaldo != null) {
                Files.copy(
                        respaldo.toPath(),
                        destino.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            } else {
                Files.deleteIfExists(destino.toPath());
            }
        } catch (IOException ex) {
            // Si también falla la restauración, se añade a la excepción original.
            causa.addSuppressed(ex);
        }
    }

    /**
     * Abre un archivo CSV para lectura usando codificación UTF-8.
     *
     * @param ruta ruta del archivo que se quiere leer.
     * @return lector configurado para el archivo.
     * @throws IOException si la ruta no es válida o no se puede leer.
     */
    private BufferedReader abrirLectura(String ruta) throws IOException {
        if (ruta == null || ruta.trim().isEmpty()) {
            throw new IOException(
                    "La ruta del archivo no puede estar vacía."
            );
        }
        File archivo = new File(ruta);
        if (!archivo.isFile() || !archivo.canRead()) {
            throw new IOException(
                    "No se puede leer el archivo: " + ruta
            );
        }
        return Files.newBufferedReader(
                archivo.toPath(),
                StandardCharsets.UTF_8
        );
    }

    /**
     * Abre un archivo para escritura usando codificación UTF-8.
     *
     * @param archivo archivo que se va a escribir.
     * @return escritor configurado para el archivo.
     * @throws IOException si no se puede abrir el archivo.
     */
    private BufferedWriter abrirEscritura(File archivo)
            throws IOException {
        return Files.newBufferedWriter(
                archivo.toPath(),
                StandardCharsets.UTF_8
        );
    }

    /**
     * Analiza un registro CSV interpretando campos entrecomillados.
     * <p>
     * Recorre la línea carácter a carácter controlando cuándo estamos dentro
     * de comillas. Soporta comas dentro de un campo y comillas dobles
     * escapadas ("") para representar una comilla literal.
     *
     * @param linea línea de texto en formato CSV.
     * @return arreglo con las columnas ya separadas y limpias.
     * @throws IOException si las comillas están mal puestas o falta cerrar alguna.
     */
    private String[] analizarLineaCSV(String linea) throws IOException {
        String[] columnas = new String[linea.length() + 1];
        StringBuilder campo = new StringBuilder();
        int cantidadColumnas = 0;
        boolean dentroComillas = false;
        boolean comillasCerradas = false;
        for (int i = 0; i < linea.length(); i++) {
            char actual = linea.charAt(i);
            if (dentroComillas) {
                if (actual == '"') {
                    // Si hay dos comillas seguidas, es una comilla escapada.
                    if (i + 1 < linea.length()
                            && linea.charAt(i + 1) == '"') {
                        campo.append('"');
                        i++;
                    } else {
                        // Si es una sola, se cierra el campo entrecomillado.
                        dentroComillas = false;
                        comillasCerradas = true;
                    }
                } else {
                    campo.append(actual);
                }
            } else {
                if (actual == ',') {
                    // Fin de la columna actual, pasamos a la siguiente.
                    columnas[cantidadColumnas++] =
                            campo.toString().trim();
                    campo.setLength(0);
                    comillasCerradas = false;
                } else if (actual == '"') {
                    // Solo se abre un campo entrecomillado si está al principio.
                    if (campo.toString().trim().isEmpty()
                            && !comillasCerradas) {
                        campo.setLength(0);
                        dentroComillas = true;
                    } else {
                        throw new IOException(
                                "Comillas en posición incorrecta."
                        );
                    }
                } else if (comillasCerradas) {
                    // Después de cerrar comillas solo puede haber espacios.
                    if (!Character.isWhitespace(actual)) {
                        throw new IOException(
                                "Contenido después de cerrar un campo."
                        );
                    }
                } else {
                    campo.append(actual);
                }
            }
        }
        if (dentroComillas) {
            throw new IOException(
                    "Campo entrecomillado sin cerrar."
            );
        }
        columnas[cantidadColumnas++] = campo.toString().trim();
        
        // Ajustamos el arreglo al tamaño real de columnas encontradas.
        String[] resultado = new String[cantidadColumnas];
        System.arraycopy(
                columnas,
                0,
                resultado,
                0,
                cantidadColumnas
        );
        return resultado;
    }

    /**
     * Valida que el encabezado del diccionario contenga las 5 columnas.
     *
     * @param columnas columnas de la primera línea del archivo.
     * @return {@code true} si el encabezado es correcto.
     */
    private boolean validarEncabezadoEstaciones(String[] columnas) {
        if (columnas.length != 5) {
            return false;
        }
        return normalizarEncabezado(columnas[0]).equals("idestacion")
                && normalizarEncabezado(columnas[1]).equals("nombre")
                && normalizarEncabezado(columnas[2]).equals("linea")
                && normalizarEncabezado(columnas[3]).equals("zona")
                && normalizarEncabezado(columnas[4])
                        .equals("capacidadpasajeroshora");
    }

    /**
     * Valida que el encabezado de conexiones contenga los campos correctos.
     *
     * @param columnas columnas de la primera línea del archivo.
     * @return {@code true} si el encabezado es correcto.
     */
    private boolean validarEncabezadoConexiones(String[] columnas) {
        if (columnas.length != 3) {
            return false;
        }
        String origen = normalizarEncabezado(columnas[0]);
        String destino = normalizarEncabezado(columnas[1]);
        String tiempo = normalizarEncabezado(columnas[2]);

        // Usamos contains para que acepte variaciones como "origen" o "ID_Origen".
        return origen.contains("origen")
                && destino.contains("destino")
                && tiempo.contains("tiempo");
    }

    /**
     * Normaliza un texto de encabezado para facilitar su comparación.
     * <p>
     * Quita tildes, espacios, guiones bajos y pasa todo a minúsculas para
     * que "ID_Estacion", "id estación" o "IDESTACION" se consideren iguales.
     *
     * @param texto texto original del encabezado.
     * @return texto normalizado para comparar.
     */
    private String normalizarEncabezado(String texto) {
        return texto.trim()
                .replace("\uFEFF", "") // Quita el BOM que a veces añade Excel
                .toLowerCase()
                .replace("_", "")
                .replace(" ", "")
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ñ", "n");
    }

    /**
     * Escapa caracteres especiales para asegurar un formato CSV válido.
     * <p>
     * Si el texto contiene comas, comillas o saltos de línea, se encierra
     * entre comillas dobles y se duplican las comillas internas.
     *
     * @param valor texto que se quiere escapar.
     * @return texto listo para escribir en el CSV.
     */
    private String escaparCSV(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(",")
                || valor.contains("\"")
                || valor.contains("\n")
                || valor.contains("\r")) {
            return "\""
                    + valor.replace("\"", "\"\"")
                    + "\"";
        }
        return valor;
    }

    /**
     * Valida que la ruta y el grafo recibidos no sean nulos o vacíos.
     *
     * @param ruta  ruta del archivo.
     * @param grafo grafo a comprobar.
     * @throws IllegalArgumentException si alguno no es válido.
     */
    private void validarParametros(
            String ruta, GrafoPonderado grafo) {
        if (ruta == null || ruta.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La ruta del archivo no puede estar vacía."
            );
        }
        if (grafo == null) {
            throw new IllegalArgumentException(
                    "El grafo no puede ser nulo."
            );
        }
    }

    /**
     * Comprueba si una línea del archivo es un comentario o está vacía.
     *
     * @param linea línea que se quiere comprobar.
     * @return {@code true} si la línea se debe ignorar.
     */
    private boolean esIgnorable(String linea) {
        String texto = linea.trim();
        return texto.isEmpty() || texto.startsWith("#");
    }

    /**
     * Incrementa el contador de errores y reporta el mensaje a la ventana.
     *
     * @param numeroLinea número de la línea con error (0 si no aplica).
     * @param contenido   texto descriptivo del problema.
     */
    private void reportarInvalida(
            int numeroLinea, String contenido) {
        lineasInvalidas++;
        if (ventana != null) {
            String mensaje;
            if (numeroLinea > 0) {
                mensaje = "Línea " + numeroLinea
                        + " omitida: " + contenido;
            } else {
                mensaje = "Registro omitido: " + contenido;
            }
            ventana.mostrarMensaje(mensaje);
        }
    }

    /**
     * Elimina un archivo temporal de forma segura si existe.
     *
     * @param archivo archivo que se quiere borrar.
     */
    private void eliminarTemporal(File archivo) {
        if (archivo != null && archivo.exists()) {
            archivo.delete();
        }
    }

    /**
     * Devuelve la cantidad de líneas inválidas registradas en la última carga.
     *
     * @return número de líneas omitidas.
     */
    public int getLineasInvalidas() {
        return lineasInvalidas;
    }

    /**
     * Devuelve la ventana principal asociada.
     *
     * @return ventana utilizada para mostrar mensajes.
     */
    public VentanaPrincipal getVentana() {
        return ventana;
    }

    /**
     * Asigna una nueva ventana principal para notificaciones.
     *
     * @param ventana nueva ventana principal.
     */
    public void setVentana(VentanaPrincipal ventana) {
        this.ventana = ventana;
    }
}
