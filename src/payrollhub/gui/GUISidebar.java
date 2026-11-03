package payrollhub.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Left navigation menu, styled after the Figma "Menu": a column of pill
 * buttons, the selected one grey and bold. Its role-based tabs and selection
 * checks are both enforced by {@link NavAccess}.
 */
public class GUISidebar extends JPanel {

    private final Map<NavTab, NavButton> buttons = new EnumMap<>(NavTab.class);
    private final String role;
    private final Consumer<NavTab> onSelect;

    /**
     * Creates the sidebar.
     *
    * @param role     the signed-in user's role, used to determine allowed tabs
     * @param onSelect runs with the tab the user clicks
     */
    public GUISidebar(String role, Consumer<NavTab> onSelect) {
        this.role = role;
        this.onSelect = onSelect;
        setBackground(GUIUtility.SIDEBAR_BACKGROUND);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(
                GUIUtility.scale(21), GUIUtility.scale(21), GUIUtility.scale(21), GUIUtility.scale(21)));
        setPreferredSize(new Dimension(GUIUtility.scale(276), 0));

        for (NavTab tab : NavAccess.tabsFor(role)) {
            NavButton button = new NavButton(tab);
            button.addActionListener(event -> select(tab));
            buttons.put(tab, button);
            add(button);
            add(Box.createVerticalStrut(GUIUtility.scale(10)));
        }
        add(Box.createVerticalGlue());
    }

    /**
     * Highlights a tab and tells the owner to show it. Ignored if the tab is
     * not in this sidebar.
     *
     * @param tab the tab to open
     */
    public void select(NavTab tab) {
        if (!NavAccess.canOpen(role, tab) || !buttons.containsKey(tab)) {
            return;
        }
        buttons.forEach((key, button) -> button.setActive(key == tab));
        onSelect.accept(tab);
    }

    /**
     * Removes the highlight from every tab, for screens that are not a tab
     * (such as the profile page). Does not change what is shown.
     */
    public void clearSelection() {
        buttons.values().forEach(button -> button.setActive(false));
    }

    // ---------------------- Button ----------------------

    private static final class NavButton extends JButton {
        private static final int BUTTON_HEIGHT = GUIUtility.scale(52);
        private final NavTab tab;
        private boolean active;

        NavButton(NavTab tab) {
            super(tab.getLabel());
            this.tab = tab;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setHorizontalAlignment(LEFT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(LEFT_ALIGNMENT);
            setFont(GUIUtility.bodyFont(Font.PLAIN, GUIUtility.scale(20)));
            setForeground(GUIUtility.FIELD_TEXT);
            Dimension size = new Dimension(Integer.MAX_VALUE, BUTTON_HEIGHT);
            setMaximumSize(size);
            setPreferredSize(new Dimension(GUIUtility.scale(238), BUTTON_HEIGHT));
        }

        void setActive(boolean active) {
            this.active = active;
            setFont(GUIUtility.bodyFont(active ? Font.BOLD : Font.PLAIN, GUIUtility.scale(20)));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color fill = active ? GUIUtility.NAV_ACTIVE
                    : getModel().isRollover() ? new Color(0xF4F4F4) : Color.WHITE;
            g2.setColor(fill);
            int arc = GUIUtility.scale(50);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            int iconSize = GUIUtility.scale(30);
            int iconX = GUIUtility.scale(16);
            paintIcon(g2, tab, iconX, (getHeight() - iconSize) / 2, iconSize);

            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textX = iconX + iconSize + GUIUtility.scale(14);
            g2.drawString(getText(), textX, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ---------------------- Icons ----------------------

    /**
     * Draws a simple line icon on a 24 x 24 grid scaled to {@code size}. These
     * stand in for the Lucide icons in Figma, which Swing cannot render from SVG.
     */
    private static void paintIcon(Graphics2D g, NavTab tab, int x, int y, int size) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        double k = size / 24.0;
        g2.scale(k, k);
        g2.setColor(GUIUtility.FIELD_TEXT);
        g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (tab) {
            case DASHBOARD:
                g2.draw(new RoundRectangle2D.Double(3, 3, 7, 9, 2, 2));
                g2.draw(new RoundRectangle2D.Double(14, 3, 7, 5, 2, 2));
                g2.draw(new RoundRectangle2D.Double(14, 12, 7, 9, 2, 2));
                g2.draw(new RoundRectangle2D.Double(3, 16, 7, 5, 2, 2));
                break;
            case TIME_TRACKER: {
                g2.draw(new Line2D.Double(5, 3, 19, 3));
                g2.draw(new Line2D.Double(5, 21, 19, 21));
                Path2D left = new Path2D.Double();
                left.moveTo(7, 3);
                left.lineTo(7, 7);
                left.lineTo(12, 12);
                left.lineTo(7, 17);
                left.lineTo(7, 21);
                g2.draw(left);
                Path2D right = new Path2D.Double();
                right.moveTo(17, 3);
                right.lineTo(17, 7);
                right.lineTo(12, 12);
                right.lineTo(17, 17);
                right.lineTo(17, 21);
                g2.draw(right);
                break;
            }
            case SCHEDULE:
                g2.draw(new RoundRectangle2D.Double(3, 5, 18, 16, 3, 3));
                g2.draw(new Line2D.Double(3, 10, 21, 10));
                g2.draw(new Line2D.Double(8, 3, 8, 7));
                g2.draw(new Line2D.Double(16, 3, 16, 7));
                break;
            case PAYSLIP:
                g2.draw(new RoundRectangle2D.Double(5, 3, 14, 18, 2, 2));
                g2.draw(new Line2D.Double(8, 8, 16, 8));
                g2.draw(new Line2D.Double(8, 12, 16, 12));
                g2.draw(new Line2D.Double(8, 16, 13, 16));
                break;
            case EMPLOYEES:
                g2.draw(new Ellipse2D.Double(8, 3.5, 8, 8));
                g2.draw(new Arc2D.Double(4, 14, 16, 14, 0, 180, Arc2D.OPEN));
                break;
            case PAYROLL_MANAGER: {
                Path2D roof = new Path2D.Double();
                roof.moveTo(3, 9);
                roof.lineTo(12, 3);
                roof.lineTo(21, 9);
                roof.closePath();
                g2.draw(roof);
                for (int cx = 6; cx <= 18; cx += 4) {
                    g2.draw(new Line2D.Double(cx, 11.5, cx, 18));
                }
                g2.draw(new Line2D.Double(3, 21, 21, 21));
                break;
            }
            case TICKET_MANAGER:
                g2.draw(new RoundRectangle2D.Double(3, 6, 18, 12, 3, 3));
                g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[] { 2f, 2.5f }, 0f));
                g2.draw(new Line2D.Double(15, 7, 15, 17));
                break;
            default:
                break;
        }
        g2.dispose();
    }
}
