package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import payrollhub.service.UserService;

public class GUILogin extends JFrame {

    private final UserService userService = new UserService();
    private final JTextField usernameField = GUIUtility.createTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);

    public GUILogin() {
        super("Payroll Hub - Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(createContent());
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 24, 20, 24));
        content.add(GUIUtility.createHeaderLabel("Payroll Hub"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 4, 6, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        form.add(new JLabel("Username"), constraints);
        constraints.gridx = 1;
        form.add(usernameField, constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        form.add(new JLabel("Password"), constraints);
        constraints.gridx = 1;
        form.add(passwordField, constraints);
        content.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        JButton loginButton = GUIUtility.createButton("Sign in", new java.awt.Color(40, 110, 75));
        loginButton.addActionListener(event -> handleLogin());
        JButton resetButton = GUIUtility.createNavButton("Forgot password");
        resetButton.addActionListener(event -> {
            new GUIForgotPass().setVisible(true);
            dispose();
        });
        actions.add(loginButton);
        actions.add(resetButton);
        content.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(loginButton);
        return content;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isBlank() || password.isBlank()) {
            JOptionPane.showMessageDialog(this, "Enter both your username and password.",
                    "Sign in", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Optional<GUIUser> user = userService.authenticate(username, password);
            if (user.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username or password is incorrect.",
                        "Sign in failed", JOptionPane.ERROR_MESSAGE);
                clearFields();
                return;
            }
            openDashboardForRole(user.get());
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Unable to sign in: " + e.getMessage(),
                    "Sign in error", JOptionPane.ERROR_MESSAGE);
        } finally {
            passwordField.setText("");
        }
    }

    public boolean isPasswordValid(String pass) {
        return userService.isPasswordValid(pass);
    }

    public int getPasswordStrength(String pass) {
        return userService.getPasswordStrength(pass);
    }

    private void openDashboardForRole(GUIUser user) {
        if (user.canManageEmployees()) {
            new GUIDashboard(user).setVisible(true);
            dispose();
            return;
        }
        if (user.getRole().equalsIgnoreCase("Employee")
                || user.getRole().equalsIgnoreCase("Regular")) {
            GUITimesheet timesheet = new GUITimesheet();
            timesheet.setLoggedIn(user);
            timesheet.setVisible(true);
            dispose();
            return;
        }
        JOptionPane.showMessageDialog(this, "This account has an unsupported role.",
                "Sign in error", JOptionPane.ERROR_MESSAGE);
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }
}
