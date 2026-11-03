package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;

/**
 * Landing tab of the dashboard. It shows only figures that already exist in
 * the CSV files, so every number on screen is real:
 *
 * - Management roles see company totals (headcount, basic payroll, average
 * salary, pending leave requests) and the employee list.
 * - Regular staff see their own position, pay and allowances, attendance days
 * in the latest month on file, and their pending leave requests.
 *
 * All reading goes through {@link CsvHandler}. Which view appears is decided
 * by {@link NavAccess#isManagement(String)}.
 */
public class GUIDashboardHome extends JPanel {

    private static final NumberFormat PESO = NumberFormat.getIntegerInstance(Locale.US);

    private final String role;
    private final int empNo;

    /**
     * Builds the dashboard for one user.
     *
     * @param role  the user's role from users.csv
     * @param empNo the user's employee number
     */
    public GUIDashboardHome(String role, int empNo) {
        this.role = role;
        this.empNo = empNo;
        setBackground(GUIUtility.PAGE_BACKGROUND);
        setLayout(new BorderLayout(0, GUIUtility.scale(24)));
        setBorder(BorderFactory.createEmptyBorder(
                GUIUtility.scale(40), GUIUtility.scale(60), GUIUtility.scale(40), GUIUtility.scale(60)));
        try {
            if (NavAccess.isManagement(role)) {
                buildManagementView();
            } else {
                buildPersonalView();
            }
        } catch (IOException | RuntimeException e) {
            removeAll();
            add(title("Dashboard"), BorderLayout.NORTH);
            add(new JLabel("Could not load dashboard data: " + e.getMessage()), BorderLayout.CENTER);
        }
    }

    // ---------------------- Management view ----------------------

    private void buildManagementView() throws IOException {
        List<EmployeeData> employees = CsvHandler.loadEmployees();

        double payroll = 0;
        int regular = 0;
        int probationary = 0;
        for (EmployeeData e : employees) {
            payroll += e.getBasicSalary();
            if (e.getStatus().equalsIgnoreCase("Regular")) {
                regular++;
            } else if (e.getStatus().equalsIgnoreCase("Probationary")) {
                probationary++;
            }
        }
        double average = employees.isEmpty() ? 0 : payroll / employees.size();
        int pending = CsvHandler.countLeaves("PENDING");

        JPanel cards = new JPanel(new GridLayout(1, 4, GUIUtility.scale(20), 0));
        cards.setOpaque(false);
        cards.add(new StatCard("Employees", String.valueOf(employees.size()),
                regular + " regular, " + probationary + " probationary"));
        cards.add(new StatCard("Monthly basic payroll", peso(payroll), "Basic salary, before allowances"));
        cards.add(new StatCard("Average basic salary", peso(average), "Per employee, per month"));
        cards.add(new StatCard("Pending leave requests", String.valueOf(pending), "Waiting for a decision"));

        JPanel top = new JPanel(new BorderLayout(0, GUIUtility.scale(20)));
        top.setOpaque(false);
        top.add(title("Payroll Overview"), BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(createEmployeeTable(employees), BorderLayout.CENTER);
    }

    private JPanel createEmployeeTable(List<EmployeeData> employees) {
        DefaultTableModel model = new DefaultTableModel(
                new String[] { "Employee #", "Name", "Position", "Status", "Basic salary" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (EmployeeData e : employees) {
            model.addRow(new Object[] {
                    e.getEmpNo(), e.getFullName(), e.getPosition(), e.getStatus(), peso(e.getBasicSalary())
            });
        }

        JTable table = new JTable(model);
        table.setFont(GUIUtility.bodyFont(Font.PLAIN, 13f));
        table.setRowHeight(GUIUtility.scale(36));
        table.setShowVerticalLines(false);
        table.setGridColor(GUIUtility.BORDER);
        table.setSelectionBackground(new Color(0xF3DDE3));
        table.setSelectionForeground(GUIUtility.FIELD_TEXT);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(GUIUtility.bodyFont(Font.BOLD, 13f));
        header.setBackground(Color.WHITE);
        header.setReorderingAllowed(false);

        // Right-align the salary column.
        javax.swing.table.DefaultTableCellRenderer right = new javax.swing.table.DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(right);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(GUIUtility.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel wrap = new JPanel(new BorderLayout(0, GUIUtility.scale(10)));
        wrap.setOpaque(false);
        wrap.add(GUIUtility.createHeaderLabel("Employee List"), BorderLayout.NORTH);
        wrap.add(scroll, BorderLayout.CENTER);
        return wrap;
    }

    // ---------------------- Personal view ----------------------

    private void buildPersonalView() throws IOException {
        Optional<EmployeeData> found = CsvHandler.findEmployee(empNo);
        if (found.isEmpty()) {
            add(title("My Overview"), BorderLayout.NORTH);
            add(new JLabel("No employee record found for employee number " + empNo + "."), BorderLayout.CENTER);
            return;
        }
        EmployeeData me = found.get();
        double allowances = me.getRiceSubsidy() + me.getPhoneAllowance() + me.getClothingAllowance();

        String month = CsvHandler.latestAttendanceMonth().orElse("");
        int days = month.isEmpty() ? 0 : CsvHandler.countAttendanceDays(empNo, month);
        String attendanceNote = month.isEmpty() ? "No attendance on file" : "Days logged in " + month;
        int pending = CsvHandler.countLeaves(empNo, "PENDING");

        JPanel cards = new JPanel(new GridLayout(2, 3, GUIUtility.scale(20), GUIUtility.scale(20)));
        cards.setOpaque(false);
        String reportsTo = me.getSupervisor().isEmpty() ? "Top of the organization"
                : "Reports to " + me.getSupervisor();
        cards.add(new StatCard("Position", me.getPosition(), reportsTo, true));
        cards.add(new StatCard("Employment status", me.getStatus(), "Employee #" + me.getEmpNo(), true));
        cards.add(new StatCard("Basic salary", peso(me.getBasicSalary()), "Per month"));
        cards.add(new StatCard("Allowances", peso(allowances), "Rice, phone and clothing, per month"));
        cards.add(new StatCard("Attendance", String.valueOf(days), attendanceNote));
        cards.add(new StatCard("Pending leave requests", String.valueOf(pending), "Waiting for a decision"));

        JPanel top = new JPanel(new BorderLayout(0, GUIUtility.scale(20)));
        top.setOpaque(false);
        top.add(title("Hi " + me.getFirstName() + ", here is your overview"), BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
    }

    // ---------------------- Helpers ----------------------

    private static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(GUIUtility.bodyFont(Font.BOLD, GUIUtility.scale(30)));
        label.setForeground(GUIUtility.FIELD_TEXT);
        return label;
    }

    private static String peso(double amount) {
        return "₱" + PESO.format(Math.round(amount));
    }

    /** The role this dashboard was built for (useful for tests and debugging). */
    public String getRole() {
        return role;
    }

    // ---------------------- Stat card ----------------------

    /** A white rounded card with a caption, a large value and a small note. */
    private static final class StatCard extends JPanel {

        StatCard(String caption, String value, String note) {
            this(caption, value, note, false);
        }

        /**
         * @param textValue true when the value is a word or phrase rather than
         *                  a number, so it is drawn smaller
         */
        StatCard(String caption, String value, String note, boolean textValue) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(
                    GUIUtility.scale(22), GUIUtility.scale(26), GUIUtility.scale(22), GUIUtility.scale(26)));

            JLabel captionLabel = new JLabel(caption);
            captionLabel.setFont(GUIUtility.bodyFont(Font.PLAIN, 13f));
            captionLabel.setForeground(GUIUtility.MUTED_TEXT);

            JLabel valueLabel = new JLabel(value);
            valueLabel.setFont(GUIUtility.bodyFont(Font.BOLD, textValue ? 20f : GUIUtility.scale(36)));
            valueLabel.setForeground(GUIUtility.FIELD_TEXT);

            JLabel noteLabel = new JLabel(note);
            noteLabel.setFont(GUIUtility.bodyFont(Font.PLAIN, 12f));
            noteLabel.setForeground(GUIUtility.MUTED_TEXT);

            add(captionLabel);
            add(javax.swing.Box.createVerticalStrut(GUIUtility.scale(10)));
            add(valueLabel);
            add(javax.swing.Box.createVerticalStrut(GUIUtility.scale(8)));
            add(noteLabel);
            add(javax.swing.Box.createVerticalGlue());
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width, Math.max(d.height, GUIUtility.scale(150)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = GUIUtility.scale(24);
            g2.setColor(GUIUtility.CARD_BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(GUIUtility.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
        }
    }
}
