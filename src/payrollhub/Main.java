package payrollhub;

import javax.swing.SwingUtilities;

import payrollhub.gui.GUILogin;
import payrollhub.model.Employee;

public class Main {

    public static void main(String[] args) {
        start();
    }

    // ---------------------- Starter Method ----------------------
    /** Starts the application by launching the login GUI. */

    public static void start() {
        SwingUtilities.invokeLater(() -> {
            GUILogin loginFrame = new GUILogin();
            loginFrame.setVisible(true);
        });
    }

    /**
     * Creates instances of CSVHandler, PayrollCalculator, and Attendance.
     * Robustness: Must handle FileNotFoundException if CSVs are missing.
     */
    private static void initialize(String username, String password) {
        if (username.equals("admin") && password.equals("admin")) {
            System.out.println("Admin login successful.");
            // Initialize admin-related components here
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    /**
     * Standard Login: Bridge between the UI and CSVHandler for credentials.
     * Overloading (1): Validates manual input against the system firewall.
     */
    private static Employee login(String username, String password) {
        // TODO: Implement login logic here, possibly checking against a CSV or database
    }

    /**
     * Token Login: Fast-tracks authentication for active sessions.
     * Overloading (2): Demonstrates model flexibility for automated access.
     */
    private static Employee login(String token) {
        // TODO: Implement token-based login logic here
    }

    /** Sends users to their specific UI. */
    private static void appropriateDashboard(Employee employee) {
        String role = employee.getRole();
        switch (role) {
            case "Admin":
                // Launch Admin Dashboard
                break;
            case "Manager":
                // Launch Manager Dashboard
                break;
            case "Employee":
                // Launch Employee Dashboard
                break;
            default:
                System.out.println("Unknown role: " + role);
        }
    }

    /** Resets the currentUser session and returns to the Login screen. */
    private static void logout(Employee currentUser) {
        currentUser.logout(currentUser.getToken());
        start(); // Return to login screen
    }

    /**
     * Closes all file connections and shuts down the program.
     * Integrity: Ensure no "Write" operations are pending in the CSV.
     */
    private static void exit() {
        System.out.println("Exiting application...");
        System.exit(0);
        // TODO: Implement file closure and cleanup logic here
    }

}
