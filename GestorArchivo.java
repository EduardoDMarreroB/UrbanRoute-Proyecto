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
 * Gestiona la lectura y escritura de los archivos CSV de UrbanRoute.
 *
 * diccionario.csv almacena los datos de las estaciones:
 * ID_Estacion,Nombre,Linea,Zona,Capacidad_Pasajeros_Hora.
 *
 * grafo.csv almacena las conexiones:
 * Estacion_Origen,Estacion_Destino,Tiempo_Minutos.
 *
 * Utiliza las estructuras propias del proyecto para incorporar
 * y recuperar estaciones y conexiones.
 *
 * Los campos CSV entrecomillados admiten comas y comillas escapadas.
 * No se admiten saltos de línea dentro de un mismo campo.
 */
public class GestorArchivo {

    /** Ventana utilizada para informar errores de lectura. */
    private VentanaPrincipal ventana;

    /** Cantidad de registros inválidos durante la última operación de carga. */
    private int lineasInvalidas;

    /**
     * Construye un gestor de archivos.
     *
     * @param ventana Ventana donde se mostrarán los mensajes.
     */
    public GestorArchivo(VentanaPrincipal ventana) {
        this.ventana = ventana;
        this.lineasInvalidas = 0;
    }

    /**
     * Carga estaciones desde un archivo CSV y las incorpora al grafo.
     *
     * El archivo debe contener cinco columnas:
     * ID_Estacion, Nombre, Linea, Zona y Capacidad.
     *
     * El grafo administra internamente la actualización de su lista
     * de estaciones y de su tabla hash.
     *
     * Esta operación agrega estaciones a la red existente. No elimina
     * automáticamente las estaciones que ya estaban registradas.
     *
     * @param ruta Ruta del archivo CSV.
     * @param grafo Grafo donde se incorporarán las estaciones.
     * @return Cantidad de estaciones incorporadas correctamente.
     * @throws IOException Si ocurre un error al leer el archivo
     * o su encabezado no es válido.
     * @throws IllegalArgumentException Si el grafo es nulo.
     */
    public int cargarEstaciones(
            String ruta, GrafoPonderado grafo) throws IOException {

        return cargarEstacionesInterno(ruta, null, grafo);
    }

    /**
     * Carga estaciones y comprueba que la tabla hash recibida
     * corresponda al índice utilizado por el grafo.
     *
     * Se conserva esta firma para mantener la compatibilidad
     * con las llamadas existentes desde la interfaz.
     *
     * La tabla no se modifica directamente, porque GrafoPonderado
     * ya administra su inserción.
     *
     * @param ruta Ruta del archivo CSV.
     * @param tabla Tabla hash que debe coincidir con el índice del grafo.
     * @param grafo Grafo donde se incorporarán las estaciones.
     * @return Cantidad de estaciones cargadas correctamente.
     * @throws IOException Si ocurre un error de lectura o si la tabla
     * recibida no coincide con el índice del grafo.
     * @throws IllegalArgumentException Si algún parámetro es nulo.
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
     * Procesa y carga las estaciones del archivo.
     *
     * Primero valida los registros y almacena temporalmente
     * las estaciones válidas. Después las incorpora al grafo.
     *
     * Esto evita modificar la red si ocurre un error de lectura
     * antes de terminar de procesar el archivo.
     *
     * @param ruta Ruta del archivo.
     * @param tabla Tabla hash utilizada para comprobar la identidad
     * de las estaciones, o null si no se solicita esa comprobación.
     * @param grafo Grafo de destino.
     * @return Cantidad de estaciones agregadas.
     * @throws IOException Si ocurre un error de lectura o inconsistencia.
     */
    private int cargarEstacionesInterno(
            String ruta,
            TablaHash tabla,
            GrafoPonderado grafo) throws IOException {

        validarParametros(ruta, grafo);

        lineasInvalidas = 0;

        ListaEnlazada<Estacion> pendientes = new ListaEnlazada<>();

        /*
         * Tabla auxiliar para identificar IDs repetidos dentro
         * del mismo archivo antes de modificar el grafo.
         */
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

                    // Comprueba identificadores repetidos en el archivo.
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

        /*
         * La modificación del grafo comienza después de terminar
         * la lectura del archivo.
         */
        ListaEnlazada<Estacion> agregadas = new ListaEnlazada<>();

        int cargadas = 0;

        try {

            for (Nodo<Estacion> nodo = pendientes.getCabeza();
                 nodo != null;
                 nodo = nodo.getSiguiente()) {

                Estacion estacion = nodo.getDato();

                if (!grafo.agregarEstacion(estacion)) {

                    reportarInvalida(
                            0,
                            estacion.getId()
                                    + " (estación ya existente en el grafo)"
                    );

                    continue;
                }

                agregadas.insertar(estacion);

                /*
                 * Si se proporcionó una tabla, debe recuperar
                 * exactamente la misma instancia almacenada
                 * por GrafoPonderado.
                 */
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

            // Se revierte la incorporación ante una inconsistencia.
            revertirEstaciones(grafo, agregadas);

            throw ex;
        }

        return cargadas;
    }

    /**
     * Revierte las estaciones agregadas durante una operación de carga.
     *
     * @param grafo Grafo que contiene las estaciones incorporadas.
     * @param agregadas Estaciones que deben eliminarse.
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
     * Carga las conexiones desde un archivo CSV.
     *
     * Las estaciones deben estar registradas previamente en el grafo.
     * La dirección y la simetría de las conexiones son responsabilidad
     * de GrafoPonderado.
     *
     * @param ruta Ruta del archivo CSV.
     * @param grafo Grafo donde se incorporarán las conexiones.
     * @return Cantidad de conexiones agregadas o actualizadas.
     * @throws IOException Si ocurre un error de lectura
     * o el encabezado no es válido.
     * @throws IllegalArgumentException Si el grafo es nulo.
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

        /*
         * Las conexiones se incorporan después de terminar la lectura.
         * GrafoPonderado se encarga de registrar la dirección inversa
         * cuando se trata de un grafo no dirigido.
         */
        for (Nodo<Conexion> nodo = pendientes.getCabeza();
             nodo != null;
             nodo = nodo.getSiguiente()) {

            Conexion conexion = nodo.getDato();

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
     * Guarda los archivos diccionario.csv y grafo.csv en una carpeta.
     *
     * Comprueba que las estaciones del grafo estén registradas
     * correctamente antes de generar los archivos.
     *
     * Utiliza archivos temporales y copias de respaldo para reducir
     * el riesgo de dejar los dos archivos en versiones diferentes
     * si ocurre un error durante el reemplazo.
     *
     * @param carpeta Carpeta donde se guardarán los archivos.
     * @param grafo Grafo que contiene la red.
     * @throws IOException Si la ruta no es válida, hay inconsistencias
     * o ocurre un error durante la escritura.
     * @throws IllegalArgumentException Si el grafo es nulo.
     */
    public void guardarCambios(
            String carpeta, GrafoPonderado grafo) throws IOException {

        guardarCambiosInterno(carpeta, null, grafo);
    }

    /**
     * Guarda los archivos y comprueba que la tabla hash recibida
     * contiene las mismas estaciones que el índice interno del grafo.
     *
     * Se conserva esta firma para mantener la compatibilidad
     * con la interfaz original.
     *
     * @param carpeta Carpeta donde se guardarán los archivos.
     * @param tabla Tabla hash que se comprobará.
     * @param grafo Grafo que contiene la red.
     * @throws IOException Si ocurre un error de escritura o las
     * estructuras no contienen las mismas estaciones.
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
     * Ejecuta la escritura y el reemplazo de ambos archivos.
     *
     * @param carpeta Carpeta de destino.
     * @param tabla Tabla hash opcional para comprobar consistencia.
     * @param grafo Grafo que se almacenará.
     * @throws IOException Si ocurre un error durante el proceso.
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

        // Comprueba la coherencia antes de escribir los archivos.
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

            // Ambos temporales están escritos antes de reemplazar originales.
            reemplazarArchivos(
                    diccionarioTemporal,
                    diccionarioDestino,
                    grafoTemporal,
                    grafoDestino
            );

        } finally {

            eliminarTemporal(diccionarioTemporal);
            eliminarTemporal(grafoTemporal);
        }
    }

    /**
     * Verifica que todas las estaciones del grafo estén correctamente
     * registradas en sus índices.
     *
     * @param grafo Grafo que se comprobará.
     * @param tabla Tabla hash adicional o null.
     * @throws IOException Si existe alguna diferencia entre las estructuras.
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
     * @param archivo Archivo temporal.
     * @param grafo Grafo que contiene las estaciones.
     * @throws IOException Si ocurre un error de escritura.
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
     * En redes no dirigidas se escribe una sola representación
     * de cada conexión física, evitando duplicar las inversas.
     *
     * @param archivo Archivo temporal.
     * @param grafo Grafo que contiene las conexiones.
     * @throws IOException Si ocurre un error de escritura.
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
                     * En grafos no dirigidos se escribe solamente
                     * la orientación cuyo origen tiene menor ID.
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
     * Reemplaza los dos archivos originales después de generar
     * correctamente sus versiones temporales.
     *
     * Antes de reemplazarlos, crea respaldos de los archivos existentes.
     * Si falla el segundo reemplazo, intenta restaurar el primero.
     *
     * @param temporalDiccionario Nuevo diccionario temporal.
     * @param destinoDiccionario Archivo definitivo del diccionario.
     * @param temporalGrafo Nuevo grafo temporal.
     * @param destinoGrafo Archivo definitivo del grafo.
     * @throws IOException Si falla el reemplazo o la recuperación.
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

            eliminarTemporal(respaldoDiccionario);
            eliminarTemporal(respaldoGrafo);
        }
    }

    /**
     * Mueve un archivo temporal a su ubicación definitiva.
     *
     * Intenta utilizar un movimiento atómico. Si el sistema
     * de archivos no lo permite, utiliza un movimiento normal.
     *
     * @param origen Archivo temporal.
     * @param destino Archivo definitivo.
     * @throws IOException Si no se puede realizar el movimiento.
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
     * Restaura un archivo desde su respaldo cuando existía
     * antes de iniciar el guardado.
     *
     * @param destino Archivo que debe restaurarse.
     * @param respaldo Copia de respaldo o null.
     * @param existia Indica si el archivo existía inicialmente.
     * @param causa Error original del reemplazo.
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
            causa.addSuppressed(ex);
        }
    }

    /**
     * Abre un archivo CSV para lectura en UTF-8.
     *
     * @param ruta Ruta del archivo.
     * @return Lector configurado en UTF-8.
     * @throws IOException Si el archivo no existe o no se puede leer.
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
     * Abre un archivo para escritura en UTF-8.
     *
     * @param archivo Archivo que se desea escribir.
     * @return Escritor configurado en UTF-8.
     * @throws IOException Si no se puede crear o abrir el archivo.
     */
    private BufferedWriter abrirEscritura(File archivo)
            throws IOException {

        return Files.newBufferedWriter(
                archivo.toPath(),
                StandardCharsets.UTF_8
        );
    }

    /**
     * Analiza un registro CSV respetando los campos entrecomillados.
     *
     * Permite comas dentro de campos entrecomillados y comillas
     * dobles escapadas mediante dos comillas consecutivas.
     *
     * @param linea Registro que se desea procesar.
     * @return Arreglo con las columnas encontradas.
     * @throws IOException Si existen comillas sin cerrar
     * o una estructura CSV incorrecta.
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

                    // Dos comillas consecutivas representan una comilla literal.
                    if (i + 1 < linea.length()
                            && linea.charAt(i + 1) == '"') {

                        campo.append('"');
                        i++;

                    } else {

                        dentroComillas = false;
                        comillasCerradas = true;
                    }

                } else {

                    campo.append(actual);
                }

            } else {

                if (actual == ',') {

                    columnas[cantidadColumnas++] =
                            campo.toString().trim();

                    campo.setLength(0);
                    comillasCerradas = false;

                } else if (actual == '"') {

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
     * Comprueba que el encabezado del diccionario tenga las
     * cinco columnas esperadas.
     *
     * @param columnas Columnas del encabezado.
     * @return true si el encabezado es correcto.
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
     * Comprueba que el encabezado de conexiones tenga
     * las columnas de origen, destino y tiempo.
     *
     * @param columnas Columnas del encabezado.
     * @return true si el encabezado es válido.
     */
    private boolean validarEncabezadoConexiones(String[] columnas) {

        if (columnas.length != 3) {
            return false;
        }

        String origen = normalizarEncabezado(columnas[0]);
        String destino = normalizarEncabezado(columnas[1]);
        String tiempo = normalizarEncabezado(columnas[2]);

        return origen.contains("origen")
                && destino.contains("destino")
                && tiempo.contains("tiempo");
    }

    /**
     * Normaliza un encabezado para facilitar su comparación.
     *
     * Elimina espacios, guiones bajos y diferencias entre
     * mayúsculas, minúsculas y vocales acentuadas.
     *
     * @param texto Encabezado original.
     * @return Encabezado normalizado.
     */
    private String normalizarEncabezado(String texto) {

        return texto.trim()
                .replace("\uFEFF", "")
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
     * Escapa un valor para escribirlo correctamente en formato CSV.
     *
     * @param valor Texto que se desea escapar.
     * @return Texto preparado para su escritura CSV.
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
     * Comprueba los parámetros básicos de una operación de carga.
     *
     * @param ruta Ruta del archivo.
     * @param grafo Grafo que recibirá la información.
     * @throws IllegalArgumentException Si algún parámetro es inválido.
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
     * Comprueba si una línea está vacía o representa un comentario.
     *
     * @param linea Línea que se desea comprobar.
     * @return true si debe ignorarse.
     */
    private boolean esIgnorable(String linea) {

        String texto = linea.trim();

        return texto.isEmpty() || texto.startsWith("#");
    }

    /**
     * Registra un error de lectura y notifica a la interfaz.
     *
     * @param numeroLinea Número de línea del archivo.
     * @param contenido Contenido o descripción del error.
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
     * Elimina un archivo temporal si existe.
     *
     * @param archivo Archivo que se desea eliminar.
     */
    private void eliminarTemporal(File archivo) {

        if (archivo != null && archivo.exists()) {
            archivo.delete();
        }
    }

    /**
     * Obtiene la cantidad de registros inválidos de la última carga.
     *
     * @return Número de registros omitidos.
     */
    public int getLineasInvalidas() {
        return lineasInvalidas;
    }

    /**
     * Obtiene la ventana asociada al gestor.
     *
     * @return Ventana principal o null si no está asignada.
     */
    public VentanaPrincipal getVentana() {
        return ventana;
    }

    /**
     * Modifica la ventana asociada al gestor.
     *
     * @param ventana Nueva ventana principal.
     */
    public void setVentana(VentanaPrincipal ventana) {
        this.ventana = ventana;
    }
}
