package payrollhub.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Shared look-and-feel helpers for every Payroll Hub screen.
 */
public class GUIUtility {

    // ---------------------- Figma design tokens ----------------------

    /** Brand maroon used for panels (Figma #8C0C2D). */
    public static final Color BRAND_RED = new Color(0x8C0C2D);

    /** Dark plum used for primary buttons (Figma #2F2339). */
    public static final Color INK_DARK = new Color(0x2F2339);

    /** Placeholder text color (black at 31% opacity). */
    public static final Color PLACEHOLDER = new Color(0, 0, 0, 79);

    /** Text color typed into input fields. */
    public static final Color FIELD_TEXT = new Color(0x1E1E1E);

    /** Page background behind cards. */
    public static final Color PAGE_BACKGROUND = new Color(0xF5F5F5);

    /** Sidebar background. */
    public static final Color SIDEBAR_BACKGROUND = new Color(0xEFEFEF);

    /** Fill of the selected sidebar button (Figma #C9C9C9). */
    public static final Color NAV_ACTIVE = new Color(0xC9C9C9);

    /** Fill of the stat cards on the dashboard. */
    public static final Color CARD_BACKGROUND = Color.WHITE;

    /** Subtle line color for card and table borders. */
    public static final Color BORDER = new Color(0xDDDDDD);

    /** Muted text for captions and labels. */
    public static final Color MUTED_TEXT = new Color(0x6B6B6B);

    /**
     * Figma pixels are multiplied by this to get screen pixels in the shell
     * screens.
     */
    public static final double SCALE = 0.8;

    /**
     * Converts a Figma measurement to screen pixels.
     *
     * @param designPixels the size in the 1920px Figma design
     * @return the size on screen, rounded to a whole pixel
     */
    public static int scale(double designPixels) {
        return (int) Math.round(designPixels * SCALE);
    }

    private static final String[] BODY_FONTS = { "Inter", "Segoe UI", "SansSerif" };
    private static final String[] HEADING_FONTS = { "Merriweather", "Georgia", "Serif" };

    private GUIUtility() {
    }

    // ---------------------- Fonts ----------------------

    /**
     * Returns the body font (Inter, falling back to Segoe UI, then SansSerif).
     *
     * @param style a {@link Font} style constant such as {@link Font#BOLD}
     * @param size  the size in points
     * @return the font
     */
    public static Font bodyFont(int style, float size) {
        return new Font(firstInstalled(BODY_FONTS), style, 1).deriveFont(size);
    }

    /**
     * Returns the heading font (Merriweather, falling back to Georgia, then
     * Serif). Used for the Motor PH wordmark.
     *
     * @param style a {@link Font} style constant such as {@link Font#BOLD}
     * @param size  the size in points
     * @return the font
     */
    public static Font headingFont(int style, float size) {
        return new Font(firstInstalled(HEADING_FONTS), style, 1).deriveFont(size);
    }

    private static String firstInstalled(String[] candidates) {
        Set<String> installed = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String name : candidates) {
            if (installed.contains(name)) {
                return name;
            }
        }
        return candidates[candidates.length - 1];
    }

    // ---------------------- Existing factories ----------------------

    public static JButton createButton(String text, Color background) {
        if (text == null) {
            throw new IllegalArgumentException("Button text is required.");
        }
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    public static JButton createNavButton(String text) {
        return createButton(text, new Color(48, 63, 87));
    }

    public static JTextField createTextField(int columns) {
        if (columns <= 0) {
            throw new IllegalArgumentException("Text field columns must be positive.");
        }
        return new JTextField(columns);
    }

    public static JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        return label;
    }

    // ---------------------- Figma-styled components ----------------------

    /**
     * Creates a button with a rounded, flat fill and white bold text.
     *
     * @param text       the label
     * @param background the fill color
     * @param arc        the corner diameter in pixels
     * @return the styled button
     */
    public static JButton createPillButton(String text, Color background, int arc) {
        if (text == null) {
            throw new IllegalArgumentException("Button text is required.");
        }
        return new PillButton(text, background, arc);
    }

    /**
     * Creates a white, rounded text field that shows grey placeholder text
     * while empty.
     *
     * @param placeholder the hint shown when the field is empty
     * @param arc         the corner diameter in pixels
     * @param fontSize    the text size in points
     * @return the styled field
     */
    public static JTextField createRoundedTextField(String placeholder, int arc, float fontSize) {
        return new RoundedTextField(placeholder, arc, fontSize);
    }

    /**
     * Creates a white, rounded password field that shows grey placeholder text
     * while empty.
     *
     * @param placeholder the hint shown when the field is empty
     * @param arc         the corner diameter in pixels
     * @param fontSize    the text size in points
     * @return the styled field
     */
    public static JPasswordField createRoundedPasswordField(String placeholder, int arc, float fontSize) {
        return new RoundedPasswordField(placeholder, arc, fontSize);
    }

    /**
     * Creates a text-only link button (no fill, no border) for actions such as
     * "Forgot password".
     *
     * @param text  the label
     * @param color the text color
     * @param font  the font
     * @return the link-styled button
     */
    public static JButton createLinkButton(String text, Color color, Font font) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setForeground(color);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // ---------------------- Painting helpers ----------------------

    private static void paintRoundedFill(Graphics g, javax.swing.JComponent c, Color fill, int arc) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), arc, arc);
        g2.dispose();
    }

    private static void paintPlaceholder(Graphics g, javax.swing.JComponent c, String text) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(PLACEHOLDER);
        g2.setFont(c.getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = c.getInsets().left;
        int y = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
        g2.dispose();
    }

    private static void styleField(JTextField field, int arc, float fontSize) {
        field.setOpaque(false);
        field.setFont(bodyFont(Font.PLAIN, fontSize));
        field.setForeground(FIELD_TEXT);
        field.setCaretColor(FIELD_TEXT);
        int pad = Math.max(arc / 2, 12);
        field.setBorder(BorderFactory.createEmptyBorder(4, pad, 4, pad));
    }

    // ---------------------- Component classes ----------------------

    private static final class RoundedTextField extends JTextField {
        private final String placeholder;
        private final int arc;

        RoundedTextField(String placeholder, int arc, float fontSize) {
            this.placeholder = placeholder;
            this.arc = arc;
            styleField(this, arc, fontSize);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintRoundedFill(g, this, Color.WHITE, arc);
            super.paintComponent(g);
            if (getText().isEmpty()) {
                paintPlaceholder(g, this, placeholder);
            }
        }
    }

    private static final class RoundedPasswordField extends JPasswordField {
        private final String placeholder;
        private final int arc;

        RoundedPasswordField(String placeholder, int arc, float fontSize) {
            this.placeholder = placeholder;
            this.arc = arc;
            styleField(this, arc, fontSize);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintRoundedFill(g, this, Color.WHITE, arc);
            super.paintComponent(g);
            if (getPassword().length == 0) {
                paintPlaceholder(g, this, placeholder);
            }
        }
    }

    private static final class PillButton extends JButton {
        private final Color fill;
        private final int arc;

        PillButton(String text, Color fill, int arc) {
            super(text);
            this.fill = fill;
            this.arc = arc;
            setForeground(Color.WHITE);
            setFont(bodyFont(Font.BOLD, 13f));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color shade = getModel().isPressed() ? fill.darker()
                    : getModel().isRollover() ? fill.brighter() : fill;
            paintRoundedFill(g, this, shade, arc);
            super.paintComponent(g);
        }
    }
}
