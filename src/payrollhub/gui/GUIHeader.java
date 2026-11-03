package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Top bar of every signed-in screen, styled after the Figma "Headboard":
 * Motor PH logo and wordmark on the left, greeting, "Log out" link and avatar
 * on the right. There is deliberately no search bar.
 *
 * The header holds no data of its own. The owner passes in the name to greet
 * and a callback to run when the user chooses Log out.
 */
public class GUIHeader extends JPanel {

    private static final String LOGO_RESOURCE = "/images/motorph-logo.png";
    private static final int HEADER_HEIGHT = GUIUtility.scale(139);

    private final JLabel greeting = new JLabel();
    private final Avatar avatar = new Avatar();

    /**
     * Creates the header with a clickable profile area.
     *
     * @param displayName the name shown in "Hi, ..." and used for the avatar
     *                    initials
     * @param onLogout    runs when the user clicks "Log out"
     * @param onProfile   runs when the user clicks the avatar or the greeting
     */
    public GUIHeader(String displayName, Runnable onLogout, Runnable onProfile) {
        setLayout(new BorderLayout());
        setBackground(GUIUtility.BRAND_RED);
        setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        setBorder(BorderFactory.createEmptyBorder(0, GUIUtility.scale(30), 0, GUIUtility.scale(22)));

        add(createBrand(), BorderLayout.WEST);
        add(createProfile(onLogout, onProfile), BorderLayout.EAST);
        setDisplayName(displayName);
    }

    /**
     * Updates the greeting and avatar initials.
     *
     * @param displayName the name to show; blank falls back to "there"
     */
    public final void setDisplayName(String displayName) {
        String name = displayName == null ? "" : displayName.trim();
        greeting.setText("Hi, " + (name.isEmpty() ? "there" : name));
        avatar.setInitials(initialsOf(name));
    }

    // ---------------------- Layout pieces ----------------------

    private JPanel createBrand() {
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));

        Image logo = loadLogo();
        if (logo != null) {
            brand.add(new LogoBadge(logo));
            brand.add(Box.createHorizontalStrut(GUIUtility.scale(24)));
        }
        brand.add(new BrandMark());
        return brand;
    }

    private JPanel createProfile(Runnable onLogout, Runnable onProfile) {
        makeClickable(avatar, onProfile);
        makeClickable(greeting, onProfile);
        greeting.setForeground(Color.WHITE);
        greeting.setFont(GUIUtility.bodyFont(Font.BOLD, GUIUtility.scale(26)));
        greeting.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JButton logout = GUIUtility.createLinkButton("Log out", Color.WHITE,
                GUIUtility.bodyFont(Font.PLAIN, 12f));
        logout.setAlignmentX(Component.RIGHT_ALIGNMENT);
        logout.addActionListener(event -> onLogout.run());

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Box.createVerticalGlue());
        text.add(greeting);
        text.add(logout);
        text.add(Box.createVerticalGlue());

        JPanel profile = new JPanel();
        profile.setOpaque(false);
        profile.setLayout(new BoxLayout(profile, BoxLayout.X_AXIS));
        profile.add(text);
        profile.add(Box.createHorizontalStrut(GUIUtility.scale(16)));
        profile.add(avatar);
        return profile;
    }

    /** Gives a component a hand cursor, a tooltip, and a click action. */
    private static void makeClickable(javax.swing.JComponent component, Runnable action) {
        component.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        component.setToolTipText("View profile");
        component.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (javax.swing.SwingUtilities.isLeftMouseButton(event)) {
                    action.run();
                }
            }
        });
    }

    private static Image loadLogo() {
        URL url = GUIHeader.class.getResource(LOGO_RESOURCE);
        return url == null ? null : new ImageIcon(url).getImage();
    }

    /** Up to two capital letters from the first and last word of a name. */
    private static String initialsOf(String name) {
        if (name.isEmpty()) {
            return "?";
        }
        String[] parts = name.split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    // ---------------------- Custom-painted pieces ----------------------

    /** "Motor PH" wordmark with the small "TM" mark. */
    private static final class BrandMark extends JPanel {
        private static final String TEXT = "Motor PH";
        private final Font mainFont = GUIUtility.headingFont(Font.BOLD, GUIUtility.scale(48));
        private final Font tmFont = GUIUtility.bodyFont(Font.PLAIN, GUIUtility.scale(15));

        BrandMark() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics main = getFontMetrics(mainFont);
            FontMetrics tm = getFontMetrics(tmFont);
            return new Dimension(main.stringWidth(TEXT) + tm.stringWidth("TM") + 6, HEADER_HEIGHT);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.setFont(mainFont);
            FontMetrics main = g2.getFontMetrics();
            int baseline = (getHeight() + main.getAscent() - main.getDescent()) / 2 + GUIUtility.scale(4);
            g2.drawString(TEXT, 0, baseline);
            g2.setFont(tmFont);
            g2.drawString("TM", main.stringWidth(TEXT) + 4,
                    baseline - main.getAscent() + g2.getFontMetrics().getAscent());
            g2.dispose();
        }
    }

    /** The logo, tilted -4.1 degrees with a soft shadow, as in the prototype. */
    private static final class LogoBadge extends JPanel {
        private final Image logo;

        LogoBadge(Image logo) {
            this.logo = logo;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(GUIUtility.scale(180), HEADER_HEIGHT);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            double width = GUIUtility.scale(150);
            double height = width * 362.0 / 543.0;
            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0;
            g2.transform(AffineTransform.getRotateInstance(Math.toRadians(-4.1), cx, cy));
            double x = cx - width / 2;
            double y = cy - height / 2;

            Shape box = new RoundRectangle2D.Double(x, y, width, height, 4, 4);
            g2.clip(box);
            int iw = logo.getWidth(null);
            int ih = logo.getHeight(null);
            double cover = Math.max(width / iw, height / ih);
            g2.drawImage(logo, (int) (cx - iw * cover / 2), (int) (cy - ih * cover / 2),
                    (int) (iw * cover), (int) (ih * cover), null);
            g2.dispose();
        }
    }

    /** Round white avatar showing the user's initials. */
    private static final class Avatar extends JPanel {
        private String initials = "?";

        Avatar() {
            setOpaque(false);
            Dimension size = new Dimension(GUIUtility.scale(86), GUIUtility.scale(86));
            setPreferredSize(size);
            setMaximumSize(size);
            setMinimumSize(size);
        }

        void setInitials(String initials) {
            this.initials = initials;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.fillOval(0, 0, d, d);
            g2.setColor(GUIUtility.BRAND_RED);
            g2.setFont(GUIUtility.bodyFont(Font.BOLD, d * 0.36f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initials, (d - fm.stringWidth(initials)) / 2,
                    (d - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
