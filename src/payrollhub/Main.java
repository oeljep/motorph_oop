package payrollhub;

import javax.swing.SwingUtilities;
import payrollhub.gui.GUILogin;

public class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GUILogin().setVisible(true));
    }
}
