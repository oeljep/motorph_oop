package payrollhub;

import javax.swing.SwingUtilities;
import payrollhub.gui.GUILogin;

public class Main {

    private Main() {
    }

    public static void main(String[] args) {
        start();
    }

    /** Schedules the login window on Swing's event dispatch thread. */
    public static void start() {
        SwingUtilities.invokeLater(Main::showLogin);
    }

    private static void showLogin() {
        new GUILogin().setVisible(true);
    }
}
