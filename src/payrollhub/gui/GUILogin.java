package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import payrollhub.service.UserService;

/**
 * Sign-in screen, styled after the "Login" frame of the MotorPH Figma
 * prototype: a maroon card with the form on the left and the tilted Motor PH
 * logo on the right.
 *
 * Layout values come from Figma (frame 369:1604) and are multiplied by
 * {@link #SCALE} so the 1920px design fits a normal window. Authentication and
 * routing stay in {@link UserService}; this class only collects input and
 * opens the next screen.
 */
public class GUILogin extends JFrame {

    /** Figma pixels are multiplied by this factor to get screen pixels. */
    private static final double SCALE = 0.8;

    /** Classpath location of the logo shown on the right of the card. */
    private static final String LOGO_RESOURCE = "/images/motorph-logo.png";

    // Figma measurements (design pixels).
    private static final double CARD_WIDTH = 1130.18; // red panel plus the logo overhang
    private static final double CARD_HEIGHT = 475;
    private static final double PANEL_WIDTH = 988.74; // the red rounded rectangle
    private static final double PANEL_RADIUS = 24.8;
    private static final double FORM_WIDTH = 494.37; // left half
    private static final double FIELD_WIDTH = 327;
    private static final double FIELD_HEIGHT = 47;
    private static final double FIELD_RADIUS = 20;

    private final UserService userService = new UserService();
    private final JTextField usernameField = GUIUtility.createRoundedTextField("Username", s(FIELD_RADIUS * 2),
            s(11.8) + 2f);
    private final JPasswordField passwordField = GUIUtility.createRoundedPasswordField("Password", s(FIELD_RADIUS * 2),
            s(11.8) + 2f);

    /**
     * Initializes the login window and builds its content.
     */
    public GUILogin() {
        super("Payroll Hub - Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(createContent());
        pack();
        setLocationRelativeTo(null);
    }

    /** Scales a Figma measurement to screen pixels. */
    private static int s(double designPixels) {
        return (int) Math.round(designPixels * SCALE);
    }

    // ---------------------- UI construction ----------------------

    /**
     * Builds the page: a light background with the login card centered on it.
     *
     * @return the content panel
     */
    private JPanel createContent() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(GUIUtility.PAGE_BACKGROUND);
        page.setPreferredSize(new Dimension(s(CARD_WIDTH) + 120, s(CARD_HEIGHT) + 160));
        page.add(createCard());
        return page;
    }

    /**
     * Builds the maroon card: form on the left, tilted logo painted on the right.
     *
     * @return the card panel
     */
    private JPanel createCard() {
        JPanel card = new LoginCard();
        card.setLayout(new BorderLayout());
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(s(CARD_WIDTH), s(CARD_HEIGHT)));
        card.add(createFormPanel(), BorderLayout.WEST);
        return card;
    }

    /**
     * Builds the left half: wordmark, username and password fields, the
     * "Forgot password" link and the Log in button.
     *
     * @return the form panel
     */
    private JPanel createFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(s(FORM_WIDTH), s(CARD_HEIGHT)));

        // Rounded down so rounding can never make the row wider than the form.
        int side = (s(FORM_WIDTH) - s(FIELD_WIDTH)) / 2;
        Dimension fieldSize = new Dimension(s(FIELD_WIDTH), s(FIELD_HEIGHT));
        usernameField.setPreferredSize(fieldSize);
        passwordField.setPreferredSize(fieldSize);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        // Wordmark
        c.gridy = 0;
        c.insets = new Insets(s(72), s(123.5), 0, 0);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        form.add(new Wordmark(), c);

        // Username
        c.gridy = 1;
        c.insets = new Insets(s(37.7), side, 0, side);
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(usernameField, c);

        // Password
        c.gridy = 2;
        c.insets = new Insets(s(37.4), side, 0, side);
        form.add(passwordField, c);

        // Forgot password (left) and Log in (right) share one row.
        JButton forgotButton = GUIUtility.createLinkButton("Forgot password", Color.WHITE,
                GUIUtility.bodyFont(Font.PLAIN, s(11.8) + 2f));
        forgotButton.addActionListener(event -> openForgotPassword());

        JButton loginButton = GUIUtility.createPillButton("Log in", GUIUtility.INK_DARK, s(24));
        loginButton.setFont(GUIUtility.bodyFont(Font.PLAIN, s(15.7) + 1f));
        loginButton.setPreferredSize(new Dimension(s(120.4), s(35.7)));
        loginButton.addActionListener(event -> handleLogin());
        getRootPane().setDefaultButton(loginButton);

        c.gridwidth = 1;
        c.gridy = 3;
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(s(23.4), side + s(14), 0, 0);
        form.add(forgotButton, c);

        c.gridx = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(s(23.4), 0, 0, side);
        form.add(loginButton, c);

        // Spacer pushes everything to the top.
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(new JPanel() {
            {
                setOpaque(false);
            }
        }, c);
        return form;
    }

    // ---------------------- Actions ----------------------

    /**
     * Opens the password recovery screen and closes this window.
     */
    private void openForgotPassword() {
        new GUIForgotPass().setVisible(true);
        dispose();
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
     * Opens the shared dashboard for the authenticated user. The dashboard
     * determines which tabs are available based on the user's role.
     *
     * @param user the authenticated user whose role determines tab access
     */
    private void openDashboardForRole(GUIUser user) {
        new GUIDashboard(user).setVisible(true);
        dispose();
    }

    /**
     * Clears both credential fields, such as after an unsuccessful sign-in.
     */
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }

    // ---------------------- Custom-painted pieces ----------------------

    /** "Motor PH" wordmark with the small "TM" mark, drawn in white. */
    private static final class Wordmark extends JPanel {
        private static final String TEXT = "Motor PH";

        private final Font mainFont = GUIUtility.headingFont(Font.BOLD, s(48));
        private final Font tmFont = GUIUtility.bodyFont(Font.PLAIN, s(15));

        Wordmark() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics main = getFontMetrics(mainFont);
            FontMetrics tm = getFontMetrics(tmFont);
            return new Dimension(main.stringWidth(TEXT) + tm.stringWidth("TM") + 4, s(77));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.setFont(mainFont);
            FontMetrics main = g2.getFontMetrics();
            int baseline = s(77) - main.getDescent() - s(8);
            g2.drawString(TEXT, 0, baseline);
            g2.setFont(tmFont);
            g2.drawString("TM", main.stringWidth(TEXT) + 4, g2.getFontMetrics().getAscent());
            g2.dispose();
        }
    }

    /**
     * Paints the maroon rounded panel, the white inset panel, and the tilted
     * logo (which overhangs the right edge, as in the prototype).
     */
    private static final class LoginCard extends JPanel {
        private final Image logo = loadLogo();

        private static Image loadLogo() {
            URL url = GUILogin.class.getResource(LOGO_RESOURCE);
            if (url == null) {
                return null;
            }

            Image source = new ImageIcon(url).getImage();
            RGBImageFilter removeBlackBackground = new RGBImageFilter() {
                {
                    canFilterIndexColorModel = true;
                }

                @Override
                public int filterRGB(int x, int y, int rgb) {
                    int red = (rgb >> 16) & 0xff;
                    int green = (rgb >> 8) & 0xff;
                    int blue = rgb & 0xff;
                    return red < 40 && green < 40 && blue < 40 ? rgb & 0x00ffffff : rgb;
                }
            };
            Image transparent = Toolkit.getDefaultToolkit().createImage(
                    new FilteredImageSource(source.getSource(), removeBlackBackground));
            return new ImageIcon(transparent).getImage();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            double radius = s(PANEL_RADIUS) * 2;
            g2.setColor(GUIUtility.BRAND_RED);
            g2.fill(new RoundRectangle2D.Double(0, 0, s(PANEL_WIDTH), s(CARD_HEIGHT), radius, radius));

            paintInsetPanel(g2);
            paintLogo(g2);
            g2.dispose();
        }

        /** White rounded panel with a soft inner shadow (Figma 369:1610). */
        private void paintInsetPanel(Graphics2D g2) {
            double x = s(FORM_WIDTH + 16.3);
            double y = s(9.3);
            double w = s(466.5);
            double h = s(455.6);
            double arc = s(26.5) * 2;
            Shape panel = new RoundRectangle2D.Double(x, y, w, h, arc, arc);

            g2.setColor(Color.WHITE);
            g2.fill(panel);

            Graphics2D inner = (Graphics2D) g2.create();
            inner.clip(panel);
            for (int i = 0; i < 8; i++) {
                inner.setColor(new Color(0, 0, 0, 22 - i * 2));
                inner.setStroke(new java.awt.BasicStroke(2f));
                inner.draw(new RoundRectangle2D.Double(x + i + 1, y + i + 1, w - 2 * i - 2, h - 2 * i - 2,
                        arc - i, arc - i));
            }
            inner.dispose();
        }

        /** Logo tilted by -4.1 degrees with a drop shadow. */
        private void paintLogo(Graphics2D g2) {
            double width = s(572.3);
            double height = s(381.5);
            double centerX = s(831.1);
            double centerY = s(230.4);

            Graphics2D t = (Graphics2D) g2.create();
            t.transform(AffineTransform.getRotateInstance(Math.toRadians(-4.1), centerX, centerY));
            double x = centerX - width / 2;
            double y = centerY - height / 2;

            Shape box = new RoundRectangle2D.Double(x, y, width, height, 6, 6);
            if (logo == null) {
                t.setColor(new Color(0xE9E4E6));
                t.fill(box);
                t.setColor(GUIUtility.BRAND_RED);
                t.setFont(GUIUtility.headingFont(Font.BOLD, s(40)));
                FontMetrics fm = t.getFontMetrics();
                String text = "Motor PH";
                t.drawString(text, (float) (centerX - fm.stringWidth(text) / 2.0),
                        (float) (centerY + fm.getAscent() / 3.0));
            } else {
                t.clip(box);
                int iw = logo.getWidth(null);
                int ih = logo.getHeight(null);
                double cover = Math.max(width / iw, height / ih);
                double dw = iw * cover;
                double dh = ih * cover;
                t.drawImage(logo, (int) (centerX - dw / 2), (int) (centerY - dh / 2), (int) dw, (int) dh, null);
            }
            t.dispose();
        }
    }
}
