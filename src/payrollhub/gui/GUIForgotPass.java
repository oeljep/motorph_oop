package payrollhub.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import payrollhub.service.UserService;

public class GUIForgotPass extends JFrame {

    private final UserService userService = new UserService();
    private final JTextField employeeNoField = GUIUtility.createTextField(18);
    private final JTextField lastNameField = GUIUtility.createTextField(18);
    private final JPasswordField newPasswordField = new JPasswordField(18);
    private final JPasswordField confirmPasswordField = new JPasswordField(18);

    public GUIForgotPass() {
        super("Payroll Hub - Password reset");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(createContent());
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createContent() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 24, 18, 24));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 4, 6, 4);
        constraints.anchor = GridBagConstraints.WEST;
        addField(panel, constraints, 0, "Employee number", employeeNoField);
        addField(panel, constraints, 1, "Last name", lastNameField);
        addField(panel, constraints, 2, "New password", newPasswordField);
        addField(panel, constraints, 3, "Confirm password", confirmPasswordField);

        JPanel actions = new JPanel();
        JButton resetButton = GUIUtility.createButton("Reset password", new java.awt.Color(40, 110, 75));
        resetButton.addActionListener(event -> handleReset());
        JButton cancelButton = GUIUtility.createNavButton("Cancel");
        cancelButton.addActionListener(event -> {
            new GUILogin().setVisible(true);
            dispose();
        });
        actions.add(resetButton);
        actions.add(cancelButton);
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(actions, constraints);
        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints constraints, int row,
            String label, java.awt.Component field) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 1;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        panel.add(field, constraints);
    }

    private void handleReset() {
        String newPassword = new String(newPasswordField.getPassword());
        String confirmation = new String(confirmPasswordField.getPassword());
        int empNo;
        try {
            empNo = Integer.parseInt(employeeNoField.getText().trim());
        } catch (NumberFormatException e) {
            showInputError("Enter a valid employee number.");
            return;
        }
        if (!validateResetInput(empNo, newPassword, confirmation)) {
            showInputError("Check the required fields and ensure both passwords match and contain at least 8 characters.");
            return;
        }
        if (lastNameField.getText().isBlank()) {
            showInputError("Enter your last name.");
            return;
        }
        try {
            userService.resetPassword(empNo, lastNameField.getText(), newPassword);
            JOptionPane.showMessageDialog(this,
                    "If the details match an account, the password has been reset.",
                    "Password reset", JOptionPane.INFORMATION_MESSAGE);
            new GUILogin().setVisible(true);
            dispose();
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Unable to reset the password: " + e.getMessage(),
                    "Password reset error", JOptionPane.ERROR_MESSAGE);
        } finally {
            newPasswordField.setText("");
            confirmPasswordField.setText("");
        }
    }

    private boolean validateResetInput(int empNo, String newPass, String confirmPass) {
        return empNo > 0 && newPass != null && !newPass.isBlank()
                && confirmPass != null && !confirmPass.isBlank()
                && newPass.equals(confirmPass) && userService.isPasswordValid(newPass);
    }

    private void showInputError(String message) {
        JOptionPane.showMessageDialog(this, message, "Password reset",
                JOptionPane.WARNING_MESSAGE);
    }
}
