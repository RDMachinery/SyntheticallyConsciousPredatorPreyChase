package main;

import javax.swing.SwingUtilities;
import ui.MainFrame;

/**
 * Predator/Prey Attention Demo
 * Two synthetic mind agents — same Aleksander architecture, opposed reward structures.
 * The attention cone on each agent shifts direction and narrows based on affective state.
 */
public class PredatorPreyDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
