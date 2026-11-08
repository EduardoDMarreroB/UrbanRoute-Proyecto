package urbanroute.interfaz;

import javax.swing.SwingUtilities;

import urbanroute.interfaz.VentanaPrincipal;

/**
 * Único punto de entrada de UrbanRoute.
 * Crea la ventana principal en el hilo de Swing.
 */
public class Main {

    /**
     * Inicia la aplicación.
     * @param args no se usan
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
