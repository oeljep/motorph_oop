package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import payrollhub.service.UserService;

public class GUILogin extends JFrame {

    private final UserService userService = new UserService();
    private final JTextField usernameField = GUIUtility.createTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JLabel date = new JLabel();
    private final JLabel time = new JLabel();

    /**
     * Initializes the login window, builds its content and menu bar, sizes and
     * centers it, and fills in the current date and time.
     */
    public GUILogin() {
        super("Payroll Hub - Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(createContent());
        setJMenuBar(createMenuBar());
        pack();
        setLocationRelativeTo(null);
        date.setText("Date: " + LocalDate.now());
        time.setText("Time: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /**
     * Creates the login screen layout, including its background image, username
     * and password fields, and sign-in and password-reset actions.
     *
     * @return the completed login content panel
     */
    private JPanel createContent() {
        URL imageUrl = GUILogin.class.getResource("/images/login.jpg");
        if (imageUrl == null) {
            throw new IllegalStateException("Login background image not found: /images/login.jpg");
        }
        Image backgroundImage = new ImageIcon(imageUrl).getImage();
        int imageWidth = backgroundImage.getWidth(null);
        int imageHeight = backgroundImage.getHeight(null);
        JPanel content = new JPanel(new BorderLayout(12, 12)) {
            /**
             * Paints the background image across the full content panel.
             *
             * @param graphics the graphics context used to paint the panel
             */
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                graphics.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        if (imageWidth > 0 && imageHeight > 0) {
            content.setPreferredSize(new Dimension(imageWidth, imageHeight));
        }
        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(GUIUtility.createHeaderLabel("Payroll Hub"), BorderLayout.WEST);
        content.add(header, BorderLayout.NORTH);
        setLocation(new java.awt.Point(350, 90));
        setResizable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 4, 6, 4);
        constraints.anchor = GridBagConstraints.WEST;
        JLabel usernameLabel = new JLabel("Username");
        JLabel passwordLabel = new JLabel("Password");
        java.awt.Font labelFont = new java.awt.Font("Tahoma", java.awt.Font.BOLD, 10);
        java.awt.Color labelColor = java.awt.Color.WHITE;
        usernameLabel.setFont(labelFont);
        usernameLabel.setForeground(labelColor);
        passwordLabel.setFont(labelFont);
        passwordLabel.setForeground(labelColor);
        constraints.gridx = 0;
        constraints.gridy = 0;

        form.add(usernameLabel, constraints);
        constraints.gridx = 1;
        form.add(usernameField, constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;

        form.add(passwordLabel, constraints);
        constraints.gridx = 1;
        form.add(passwordField, constraints);
        content.add(form, BorderLayout.CENTER);
        JPanel actions = new JPanel();
        actions.setOpaque(false);

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

    /**
     * Creates the menu bar and places the date and time labels on its left side.
     *
     * @return the configured menu bar
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setLayout(new BorderLayout());
        JPanel dateTimePanel = new JPanel();
        dateTimePanel.setOpaque(false);
        dateTimePanel.add(date);
        dateTimePanel.add(Box.createHorizontalStrut(16));
        dateTimePanel.add(time);
        menuBar.add(dateTimePanel, BorderLayout.WEST);
        return menuBar;
    }

    /**
     * Validates the entered credentials, authenticates the user, and opens the
     * appropriate screen; any login errors are shown in a dialog.
     */
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

    /**
     * Checks whether a password meets the password rules defined by the user
     * service.
     *
     * @param pass the password to validate
     * @return {@code true} when the password is valid; otherwise {@code false}
     */
    public boolean isPasswordValid(String pass) {
        return userService.isPasswordValid(pass);
    }

    /**
     * Gets the password strength score calculated by the user service.
     *
     * @param pass the password to assess
     * @return the password strength score
     */
    public int getPasswordStrength(String pass) {
        return userService.getPasswordStrength(pass);
    }

    /**
     * Opens the dashboard for users who can manage employees or the timesheet
     * for regular employees; unsupported roles receive an error dialog.
     *
     * @param user the authenticated user whose role determines the destination
     */
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

    /**
     * Clears both credential fields, such as after an unsuccessful sign-in.
     */
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }
}
