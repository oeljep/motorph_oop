package payrollhub.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.service.UserService;

/**
 * Password recovery window, opened from the "Forgot password" link on the
 * login screen and styled like the login card.
 *
 * The user proves who they are with their user name, employee number and
 * birthday, then chooses a new password. The stored password is never shown.
 * After {@value #MAX_ATTEMPTS} failed identity checks the form locks until the
 * window is reopened. All file access goes through {@link CsvHandler} and the
 * password rules come from {@link UserService}.
 */
public class GUIForgotPass extends JFrame {

    private static final int MAX_ATTEMPTS = 5;
    private static final Color ERROR_TEXT = new Color(0xFFD9E0);

    private final UserService userService = new UserService();
    private final JTextField usernameField = GUIUtility.createRoundedTextField("Username", 30, 14f);
    private final JTextField empNoField = GUIUtility.createRoundedTextField("Employee number", 30, 14f);
    private final JTextField birthdayField = GUIUtility.createRoundedTextField("Birthday (YYYY-MM-DD)", 30, 14f);
    private final JPasswordField newPasswordField = GUIUtility.createRoundedPasswordField("New password", 30, 14f);
    private final JPasswordField confirmField = GUIUtility.createRoundedPasswordField("Confirm new password", 30, 14f);
    private final JLabel strengthLabel = new JLabel(" ");
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton resetButton = GUIUtility.createPillButton("Reset password", GUIUtility.INK_DARK, 24);
    private int failedAttempts;

    /**
     * Builds the recovery window.
     */
    public GUIForgotPass() {
        super("Payroll Hub - Reset password");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(GUIUtility.PAGE_BACKGROUND);
        page.setPreferredSize(new Dimension(620, 720));
        page.add(createCard());
        setContentPane(page);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(resetButton);
    }

    // ---------------------- Layout ----------------------

    private JPanel createCard() {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GUIUtility.BRAND_RED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(480, 640));

        JLabel title = new JLabel("Reset password");
        title.setForeground(Color.WHITE);
        title.setFont(GUIUtility.headingFont(Font.BOLD, 30f));

        JLabel subtitle = new JLabel("Confirm who you are, then choose a new password.");
        subtitle.setForeground(Color.WHITE);
        subtitle.setFont(GUIUtility.bodyFont(Font.PLAIN, 13f));

        strengthLabel.setForeground(Color.WHITE);
        strengthLabel.setFont(GUIUtility.bodyFont(Font.PLAIN, 12f));
        messageLabel.setForeground(ERROR_TEXT);
        messageLabel.setFont(GUIUtility.bodyFont(Font.BOLD, 12f));

        Dimension fieldSize = new Dimension(360, 42);
        for (JTextField field : new JTextField[] { usernameField, empNoField, birthdayField,
                newPasswordField, confirmField }) {
            field.setPreferredSize(fieldSize);
        }

        resetButton.setPreferredSize(new Dimension(170, 38));
        resetButton.addActionListener(event -> handleReset());
        JButton backButton = GUIUtility.createLinkButton("Back to log in", Color.WHITE,
                GUIUtility.bodyFont(Font.PLAIN, 13f));
        backButton.addActionListener(event -> backToLogin());

        newPasswordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateStrength();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateStrength();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateStrength();
            }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 8, 0);

        c.gridy = 0;
        card.add(title, c);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 24, 0);
        card.add(subtitle, c);

        JTextField[] fields = { usernameField, empNoField, birthdayField, newPasswordField, confirmField };
        c.insets = new Insets(0, 0, 14, 0);
        for (int i = 0; i < fields.length; i++) {
            c.gridy = 2 + i;
            card.add(fields[i], c);
        }

        c.gridy = 7;
        c.insets = new Insets(0, 4, 4, 0);
        card.add(strengthLabel, c);
        c.gridy = 8;
        card.add(messageLabel, c);

        c.gridy = 9;
        c.insets = new Insets(14, 0, 0, 0);
        card.add(resetButton, c);
        c.gridy = 10;
        c.insets = new Insets(14, 4, 0, 0);
        card.add(backButton, c);
        return card;
    }

    // ---------------------- Actions ----------------------

    private void updateStrength() {
        String password = new String(newPasswordField.getPassword());
        if (password.isEmpty()) {
            strengthLabel.setText(" ");
            return;
        }
        int score = userService.getPasswordStrength(password);
        String word = score <= 1 ? "Weak" : score <= 3 ? "Fair" : "Strong";
        strengthLabel.setText("Strength: " + word);
    }

    private void showError(String text) {
        messageLabel.setText(text);
    }

    /**
     * Checks the fields, verifies the identity, and saves the new password.
     */
    private void handleReset() {
        if (failedAttempts >= MAX_ATTEMPTS) {
            showError("Too many attempts. Close this window and try again later.");
            return;
        }
        String username = usernameField.getText().trim();
        String empText = empNoField.getText().trim();
        String birthdayText = birthdayField.getText().trim();
        String password = new String(newPasswordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (username.isEmpty() || empText.isEmpty() || birthdayText.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            showError("Fill in every field.");
            return;
        }
        LocalDate birthday;
        int empNo;
        try {
            birthday = LocalDate.parse(birthdayText);
            empNo = Integer.parseInt(empText);
        } catch (DateTimeParseException | NumberFormatException e) {
            showError("Enter the employee number in digits and the birthday as YYYY-MM-DD.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("The two passwords do not match.");
            return;
        }
        if (!userService.isPasswordValid(password)) {
            showError("That password is too weak. Use at least 8 characters with letters, numbers and symbols.");
            return;
        }

        try {
            Optional<String[]> account = CsvHandler.findUser(username);
            Optional<EmployeeData> employee = CsvHandler.findEmployee(empNo);
            boolean verified = account.isPresent() && employee.isPresent()
                    && account.get()[0].equals(String.valueOf(empNo))
                    && employee.get().getBirthday().equals(birthday);
            if (!verified) {
                failedAttempts++;
                if (failedAttempts >= MAX_ATTEMPTS) {
                    resetButton.setEnabled(false);
                    showError("Too many attempts. Close this window and try again later.");
                } else {
                    showError("We could not verify those details. Attempts left: "
                            + (MAX_ATTEMPTS - failedAttempts));
                }
                return;
            }
            if (account.get()[2].equals(password)) {
                showError("Choose a password that is different from your current one.");
                return;
            }
            CsvHandler.updatePassword(empNo, password);
        } catch (IOException e) {
            showError("Could not save the new password: " + e.getMessage());
            return;
        }

        JOptionPane.showMessageDialog(this, "Your password has been updated. You can log in now.",
                "Reset password", JOptionPane.INFORMATION_MESSAGE);
        backToLogin();
    }

    private void backToLogin() {
        new GUILogin().setVisible(true);
        dispose();
    }
}
