/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package urbanroute.interfaz;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Ejecuta la GUI en el hilo de eventos de Swing por seguridad
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                VentanaPrincipal ventana = new VentanaPrincipal();
                ventana.setVisible(true); // Hace visible la ventana
            }
        });
    }
}