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
import javax.swing.Box;
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
 * Role-specific dashboard overview shown inside the dashboard shell.
 */
public class GUIDashboardHome extends JPanel {

    private static final NumberFormat PESO = NumberFormat.getIntegerInstance(Locale.US);

    private final String role;
    private final int empNo;

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
            add(GUIUtility.createPageTitle("Dashboard"), BorderLayout.NORTH);
            add(new JLabel("Could not load dashboard data: " + e.getMessage()), BorderLayout.CENTER);
        }
    }

    private void buildManagementView() throws IOException {
        List<EmployeeData> employees = CsvHandler.loadEmployees();
        List<EmployeeData> visibleEmployees = NavAccess.canOpen(role, NavTab.EMPLOYEE_MANAGER)
                ? CsvHandler.findDirectReports(empNo)
                : employees;

        double payroll = 0;
        int regular = 0;
        int probationary = 0;
        for (EmployeeData employee : employees) {
            payroll += employee.getBasicSalary();
            if (employee.getStatus().equalsIgnoreCase("Regular")) {
                regular++;
            } else if (employee.getStatus().equalsIgnoreCase("Probationary")) {
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
        add(createEmployeeTable(visibleEmployees), BorderLayout.CENTER);
    }

    private JPanel createEmployeeTable(List<EmployeeData> employees) {
        DefaultTableModel model = new DefaultTableModel(
                new String[] { "Employee #", "Name", "Position", "Status", "Basic salary" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (EmployeeData employee : employees) {
            model.addRow(new Object[] {
                    employee.getEmpNo(), employee.getFullName(), employee.getPosition(),
                    employee.getStatus(), peso(employee.getBasicSalary())
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

        javax.swing.table.DefaultTableCellRenderer right = new javax.swing.table.DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(right);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(GUIUtility.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel wrap = new JPanel(new BorderLayout(0, GUIUtility.scale(10)));
        wrap.setOpaque(false);
        wrap.add(sectionTitle("Employee List"), BorderLayout.NORTH);
        wrap.add(scroll, BorderLayout.CENTER);
        return wrap;
    }

    private void buildPersonalView() throws IOException {
        Optional<EmployeeData> found = CsvHandler.findEmployee(empNo);
        if (found.isEmpty()) {
            add(GUIUtility.createPageTitle("My Overview"), BorderLayout.NORTH);
            add(new JLabel("No employee record found for employee number " + empNo + "."), BorderLayout.CENTER);
            return;
        }
        EmployeeData employee = found.get();
        double allowances = employee.getRiceSubsidy()
                + employee.getPhoneAllowance() + employee.getClothingAllowance();

        List<String> attendanceMonths = CsvHandler.loadAttendanceMonths(empNo);
        String month = attendanceMonths.isEmpty()
                ? "" : attendanceMonths.get(attendanceMonths.size() - 1);
        int days = month.isEmpty() ? 0 : CsvHandler.countAttendanceDays(empNo, month);
        String attendanceNote = month.isEmpty() ? "No attendance on file" : "Days logged in " + month;
        int pending = CsvHandler.countLeaves(empNo, "PENDING");

        JPanel cards = new JPanel(new GridLayout(2, 3, GUIUtility.scale(20), GUIUtility.scale(20)));
        cards.setOpaque(false);
        String reportsTo = employee.getSupervisor().isEmpty() ? "Top of the organization"
                : "Reports to " + employee.getSupervisor();
        cards.add(new StatCard("Position", employee.getPosition(), reportsTo, true));
        cards.add(new StatCard("Employment status", employee.getStatus(),
                "Employee #" + employee.getEmpNo(), true));
        cards.add(new StatCard("Basic salary", peso(employee.getBasicSalary()), "Per month"));
        cards.add(new StatCard("Allowances", peso(allowances), "Rice, phone and clothing, per month"));
        cards.add(new StatCard("Attendance", String.valueOf(days), attendanceNote));
        cards.add(new StatCard("Pending leave requests", String.valueOf(pending), "Waiting for a decision"));

        JPanel top = new JPanel(new BorderLayout(0, GUIUtility.scale(20)));
        top.setOpaque(false);
        top.add(title("Hi " + employee.getFirstName() + ", here is your overview"), BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
    }

    private static JLabel title(String text) {
        JLabel label = GUIUtility.createPageTitle(text);
        label.setFont(GUIUtility.bodyFont(Font.BOLD, GUIUtility.scale(30)));
        return label;
    }

    private static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(GUIUtility.bodyFont(Font.BOLD, 18f));
        label.setForeground(GUIUtility.FIELD_TEXT);
        return label;
    }

    private static String peso(double amount) {
        return "₱" + PESO.format(Math.round(amount));
    }

    public String getRole() {
        return role;
    }

    private static final class StatCard extends JPanel {

        StatCard(String caption, String value, String note) {
            this(caption, value, note, false);
        }

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
            add(Box.createVerticalStrut(GUIUtility.scale(10)));
            add(valueLabel);
            add(Box.createVerticalStrut(GUIUtility.scale(8)));
            add(noteLabel);
            add(Box.createVerticalGlue());
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            return new Dimension(preferred.width, Math.max(preferred.height, GUIUtility.scale(150)));
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
