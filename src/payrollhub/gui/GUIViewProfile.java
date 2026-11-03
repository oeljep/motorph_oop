package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;

/**
 * Read-only profile of one employee, styled after the Figma "View profile"
 * frame: a large avatar and name, then Personal, Employment and Government
 * number sections.
 *
 * It is a content panel, not a window. {@link GUIDashboard} shows it inside
 * the shell (header and sidebar stay visible) when the user clicks the avatar
 * in {@link GUIHeader}. All data comes from {@link CsvHandler}.
 *
 * Figma also lists Department and Email address, but employee.csv has no such
 * columns, so they are not shown.
 */
public class GUIViewProfile extends JPanel {

    private static final DateTimeFormatter BIRTHDAY_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    /**
     * Builds the profile for one employee.
     *
     * @param empNo the employee number to show
     */
    public GUIViewProfile(int empNo) {
        setBackground(GUIUtility.PAGE_BACKGROUND);
        setLayout(new BorderLayout());
        try {
            Optional<EmployeeData> found = CsvHandler.findEmployee(empNo);
            if (found.isEmpty()) {
                showMessage("No employee record found for employee number " + empNo + ".");
                return;
            }
            build(found.get(), CsvHandler.loadEmployees());
        } catch (IOException | RuntimeException e) {
            showMessage("Could not load the profile: " + e.getMessage());
        }
    }

    // ---------------------- Layout ----------------------

    private void build(EmployeeData me, List<EmployeeData> everyone) {
        JPanel page = new JPanel(new GridBagLayout());
        page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(
                GUIUtility.scale(50), GUIUtility.scale(70), GUIUtility.scale(50), GUIUtility.scale(70)));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;

        c.gridx = 0;
        c.insets = new Insets(0, 0, 0, GUIUtility.scale(60));
        page.add(new BigAvatar(initialsOf(me)), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        page.add(createDetails(me, everyone), c);

        JScrollPane scroll = new JScrollPane(page,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createDetails(EmployeeData me, List<EmployeeData> everyone) {
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(me.getFullName());
        name.setFont(GUIUtility.bodyFont(Font.BOLD, GUIUtility.scale(56)));
        name.setForeground(GUIUtility.FIELD_TEXT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(name);
        details.add(Box.createVerticalStrut(GUIUtility.scale(30)));

        details.add(section("PERSONAL INFORMATION", new String[][] {
                { "Employee Name:", me.getFullName() },
                { "Employee ID:", String.valueOf(me.getEmpNo()) },
                { "Birthday:", me.getBirthday().format(BIRTHDAY_FORMAT) },
                { "Phone Number:", formatPhone(me.getPhone()) },
                { "Address:", me.getAddress() }
        }));
        details.add(Box.createVerticalStrut(GUIUtility.scale(36)));

        details.add(section("EMPLOYMENT INFORMATION", new String[][] {
                { "Status:", me.getStatus() },
                { "Position:", me.getPosition() },
                { "Immediate Supervisor:", supervisorOf(me, everyone) }
        }));
        details.add(Box.createVerticalStrut(GUIUtility.scale(36)));

        details.add(section("GOVERNMENT NUMBERS", new String[][] {
                { "Social Security Number:", formatSss(me.getSss()) },
                { "Philhealth Number:", me.getPhilhealth() },
                { "Tax Identification Number:", formatTin(me.getTin()) },
                { "Pag-ibig Number:", me.getPagibig() }
        }));
        return details;
    }

    /** One titled block of label/value rows. */
    private JPanel section(String title, String[][] rows) {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = new JLabel(title);
        heading.setFont(GUIUtility.bodyFont(Font.BOLD, GUIUtility.scale(26)));
        heading.setForeground(GUIUtility.BRAND_RED);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(heading);
        section.add(Box.createVerticalStrut(GUIUtility.scale(14)));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, GUIUtility.scale(10), 0);

        for (int i = 0; i < rows.length; i++) {
            c.gridy = i;

            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            JLabel label = new JLabel(rows[i][0]);
            label.setFont(GUIUtility.bodyFont(Font.PLAIN, GUIUtility.scale(22)));
            label.setForeground(GUIUtility.MUTED_TEXT);
            label.setPreferredSize(new Dimension(GUIUtility.scale(330), label.getPreferredSize().height));
            grid.add(label, c);

            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            grid.add(valueText(rows[i][1]), c);
        }
        section.add(grid);
        return section;
    }

    /** Read-only wrapping text, so a long address folds onto a second line. */
    private static JTextArea valueText(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        area.setFont(GUIUtility.bodyFont(Font.PLAIN, GUIUtility.scale(22)));
        area.setForeground(GUIUtility.FIELD_TEXT);
        return area;
    }

    private void showMessage(String message) {
        removeAll();
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        JLabel label = new JLabel(message);
        label.setFont(GUIUtility.bodyFont(Font.PLAIN, 16f));
        label.setForeground(GUIUtility.MUTED_TEXT);
        center.add(label);
        add(center, BorderLayout.CENTER);
    }

    // ---------------------- Display formatting ----------------------
    // The data layer keeps government numbers and phones as digits only.
    // Dashes are added here, for display, when the length matches the usual format.

    /** Shows a 10-digit SSS number as 12-3456789-0; other lengths are shown as stored. */
    static String formatSss(String digits) {
        if (digits.length() == 10) {
            return digits.substring(0, 2) + "-" + digits.substring(2, 9) + "-" + digits.substring(9);
        }
        return digits;
    }

    /** Shows a 12-digit TIN as 123-456-789-000; other lengths are shown as stored. */
    static String formatTin(String digits) {
        if (digits.length() == 12) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-"
                    + digits.substring(6, 9) + "-" + digits.substring(9);
        }
        return digits;
    }

    /** Shows a 9-digit phone number as 123-456-789; other lengths are shown as stored. */
    static String formatPhone(String digits) {
        if (digits.length() == 9) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return digits;
    }

    /** Supervisor as "First Last" when found in the employee file, else the stored text. */
    private static String supervisorOf(EmployeeData me, List<EmployeeData> everyone) {
        if (me.getSupervisor().isEmpty()) {
            return "None";
        }
        return CsvHandler.findSupervisor(me, everyone)
                .map(EmployeeData::getFullName)
                .orElse(me.getSupervisor());
    }

    private static String initialsOf(EmployeeData e) {
        return ("" + e.getFirstName().charAt(0) + e.getLastName().charAt(0)).toUpperCase();
    }

    // ---------------------- Custom-painted avatar ----------------------

    /** Large maroon circle with white initials. */
    private static final class BigAvatar extends JPanel {
        private final String initials;

        BigAvatar(String initials) {
            this.initials = initials;
            setOpaque(false);
            Dimension size = new Dimension(GUIUtility.scale(250), GUIUtility.scale(250));
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());
            g2.setColor(GUIUtility.BRAND_RED);
            g2.fillOval(0, 0, d, d);
            g2.setColor(Color.WHITE);
            g2.setFont(GUIUtility.bodyFont(Font.BOLD, d * 0.36f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initials, (d - fm.stringWidth(initials)) / 2,
                    (d - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
