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
 */
public class GestorArchivo {
    private VentanaPrincipal ventana; // Ventana para mostrar errores
    private int lineasInvalidas; // Contador de lineas invalidas en la carga

    /**
     * Construye un gestor de archivos e inicializa sus valores.
     */
    public GestorArchivo(VentanaPrincipal ventana) {
        this.ventana = ventana; // Guardamos la ventana principal
        this.lineasInvalidas = 0; // Inicializamos contador en cero
    }

    /**
     * Carga estaciones desde un archivo CSV y las añade al grafo.
     */
    public int cargarEstaciones(
            String ruta, GrafoPonderado grafo) throws IOException {
        return cargarEstacionesInterno(ruta, null, grafo);
    }

    /**
     * Carga estaciones verificando la tabla hash asociada al grafo.
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
     */
    private int cargarEstacionesInterno(
            String ruta,
            TablaHash tabla,
            GrafoPonderado grafo) throws IOException {
        validarParametros(ruta, grafo);
        lineasInvalidas = 0;
        ListaEnlazada<Estacion> pendientes = new ListaEnlazada<>();
        // Tabla auxiliar para detectar IDs repetidos en el archivo
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
                    // Comprueba si el ID ya fue leído en este archivo
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
                // Intenta agregar la estación al grafo principal
                if (!grafo.agregarEstacion(estacion)) {
                    reportarInvalida(
                            0,
                            estacion.getId()
                                    + " (estación ya existente en el grafo)"
                    );
                    continue;
                }
                agregadas.insertar(estacion);
                // Verifica la correspondencia de la tabla hash
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
            // Revierte los cambios si ocurre un error
            revertirEstaciones(grafo, agregadas);
            throw ex;
        }
        return cargadas;
    }

    /**
     * Revierte la incorporación de estaciones ante una falla.
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
                    // Valida que los datos de la conexión sean correctos
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
                    // Valida que ambas estaciones existan en el grafo
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
            // Inserta la conexión en el grafo ponderado
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
     */
    public void guardarCambios(
            String carpeta, GrafoPonderado grafo) throws IOException {
        guardarCambiosInterno(carpeta, null, grafo);
    }
}

/**
     * Guarda los archivos verificando la consistencia con la tabla hash.
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
        // Comprueba la coherencia antes de escribir
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
            // Reemplaza los originales solo si ambos temporales están listos
            reemplazarArchivos(
                    diccionarioTemporal,
                    diccionarioDestino,
                    grafoTemporal,
                    grafoDestino
            );
        } finally {
            // Elimina los archivos temporales creados
            eliminarTemporal(diccionarioTemporal);
            eliminarTemporal(grafoTemporal);
        }
    }

    /**
     * Verifica que todas las estaciones estén registradas en sus índices.
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
                     * para evitar duplicar las conexiones inversas.
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
            // Restaura los archivos anteriores si ocurre una falla
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
            // Elimina los archivos de respaldo temporales
            eliminarTemporal(respaldoDiccionario);
            eliminarTemporal(respaldoGrafo);
        }
    }

    /**
     * Mueve un archivo temporal a su ubicación definitiva de forma segura.
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
     * Abre un archivo CSV para lectura usando codificación UTF-8.
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
                    // Maneja comillas dobles escapadas dentro del texto
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
     * Valida que el encabezado del diccionario contenga las 5 columnas.
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
     * Normaliza un texto de encabezado para facilitar su comparación.
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
     * Escapa caracteres especiales para asegurar un formato CSV válido.
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
     */
    private boolean esIgnorable(String linea) {
        String texto = linea.trim();
        return texto.isEmpty() || texto.startsWith("#");
    }

    /**
     * Incrementa el contador de errores y reporta el mensaje a la ventana.
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
     */
    private void eliminarTemporal(File archivo) {
        if (archivo != null && archivo.exists()) {
            archivo.delete();
        }
    }

    /**
     * Devuelve la cantidad de líneas inválidas registradas.
     */
    public int getLineasInvalidas() {
        return lineasInvalidas;
    }

    /**
     * Devuelve la ventana principal asociada.
     */
    public VentanaPrincipal getVentana() {
        return ventana;
    }

    /**
     * Asigna una nueva ventana principal para notificaciones.
     */
    public void setVentana(VentanaPrincipal ventana) {
        this.ventana = ventana;
    }
