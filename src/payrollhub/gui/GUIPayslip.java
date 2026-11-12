package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import payrollhub.data.AttendanceData;
import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.data.TicketData;
import payrollhub.service.PayslipCalculator;
import payrollhub.service.PayrollCutoff;
import payrollhub.service.PayslipData;

/**
 * Payslip tab: the signed-in employee's own payslip for a chosen month, laid
 * out like the MotorPH payslip in the data workbook (earnings, benefits,
 * deductions, summary). An employee who disagrees with a figure can report it,
 * which files a ticket for the Ticket Manager.
 *
 * The amounts come from {@link PayslipCalculator}; this class only displays
 * them. {@link #createPaper(PayslipData)} is public so the Payroll Manager can
 * show the same payslip for any employee.
 */
public class GUIPayslip extends JPanel {

    private static final String ADDRESS = "7 Jupiter Avenue cor. F. Sandoval Jr., Bagong Nayon, Quezon City";
    private static final String CONTACT = "Phone: (028) 911-5071 / (028) 911-5072 / (028) 911-5073   Email: corporate@motorph.com";

    private final int empNo;
    private final JComboBox<String> monthBox = new JComboBox<>();
    private final JComboBox<PayrollCutoff> cutoffBox = new JComboBox<>(PayrollCutoff.values());
    private final JPanel paperHolder = new JPanel(new BorderLayout());
    private final JButton reportButton = GUIUtility.createActionButton("Report an issue", GUIUtility.INK_DARK);
    private EmployeeData employee;
    private String employeeLoadError;
    private boolean loadingMonths;

    /**
     * Builds the tab for one employee.
     *
     * @param empNo the signed-in employee's number
     */
    public GUIPayslip(int empNo) {
        this.empNo = empNo;
        setBackground(GUIUtility.PAGE_BACKGROUND);
        setLayout(new BorderLayout(0, GUIUtility.scale(20)));
        setBorder(BorderFactory.createEmptyBorder(
                GUIUtility.scale(40), GUIUtility.scale(60), GUIUtility.scale(40), GUIUtility.scale(60)));

        try {
            Optional<EmployeeData> found = CsvHandler.findEmployee(empNo);
            employee = found.orElse(null);
        } catch (IOException e) {
            employee = null;
            employeeLoadError = e.getMessage();
        }

        add(createTopBar(), BorderLayout.NORTH);
        paperHolder.setOpaque(false);
        JPanel centered = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centered.setOpaque(false);
        centered.add(paperHolder);
        JScrollPane scroll = new JScrollPane(centered);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        monthBox.addActionListener(event -> {
            if (!loadingMonths) {
                showPayslip();
            }
        });
        cutoffBox.addActionListener(event -> {
            if (!loadingMonths) {
                showPayslip();
            }
        });
        reportButton.addActionListener(event -> reportIssue());
        loadMonths();
    }

    // ---------------------- Layout ----------------------

    private JPanel createTopBar() {
        JLabel monthLabel = new JLabel("Pay month");
        monthLabel.setFont(GUIUtility.bodyFont(Font.PLAIN, 14f));
        JLabel cutoffLabel = new JLabel("Cutoff");
        cutoffLabel.setFont(GUIUtility.bodyFont(Font.PLAIN, 14f));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        controls.setOpaque(false);
        controls.add(monthLabel);
        controls.add(monthBox);
        controls.add(cutoffLabel);
        controls.add(cutoffBox);
        controls.add(reportButton);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(GUIUtility.createPageTitle("Payslip"), BorderLayout.WEST);
        bar.add(controls, BorderLayout.EAST);
        return bar;
    }

    // ---------------------- Data ----------------------

    private void loadMonths() {
        loadingMonths = true;
        monthBox.removeAllItems();
        try {
            List<String> months = CsvHandler.loadAttendanceMonths(empNo);
            Collections.reverse(months);
            for (String month : months) {
                monthBox.addItem(month);
            }
        } catch (IOException e) {
            showNotice("Could not read attendance: " + e.getMessage());
            loadingMonths = false;
            return;
        }
        loadingMonths = false;
        showPayslip();
    }

    private void showPayslip() {
        String period = (String) monthBox.getSelectedItem();
        reportButton.setEnabled(period != null);
        if (employeeLoadError != null) {
            showNotice("Could not read employee data: " + employeeLoadError);
            return;
        }
        if (employee == null) {
            showNotice("No employee record found for employee number " + empNo + ".");
            return;
        }
        if (period == null) {
            showNotice("There is no attendance on file yet, so there is no payslip to show.");
            return;
        }
        try {
            List<AttendanceData> records = CsvHandler.loadAttendance(empNo, period);
            PayslipData slip = PayslipCalculator.compute(employee, records,
                    YearMonth.parse(period), (PayrollCutoff) cutoffBox.getSelectedItem());
            paperHolder.removeAll();
            paperHolder.add(createPaper(slip), BorderLayout.CENTER);
            paperHolder.revalidate();
            paperHolder.repaint();
        } catch (IOException e) {
            showNotice("Could not read attendance: " + e.getMessage());
        }
    }

    private void showNotice(String text) {
        paperHolder.removeAll();
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(GUIUtility.bodyFont(Font.PLAIN, 15f));
        label.setForeground(GUIUtility.MUTED_TEXT);
        paperHolder.add(label, BorderLayout.CENTER);
        paperHolder.revalidate();
        paperHolder.repaint();
    }

    // ---------------------- Report an issue ----------------------

    private void reportIssue() {
        String period = selectedCutoffLabel();
        JTextField subject = new JTextField("Payslip concern - " + period, 30);
        JTextArea message = new JTextArea(5, 30);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(new JLabel("Subject"));
        form.add(subject);
        form.add(Box.createVerticalStrut(10));
        form.add(new JLabel("What looks wrong?"));
        form.add(new JScrollPane(message));

        int choice = JOptionPane.showConfirmDialog(this, form, "Report an issue",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            TicketData ticket = TicketData.newTicket(CsvHandler.nextTicketId(), empNo,
                    subject.getText(), message.getText());
            CsvHandler.saveTicket(ticket);
            JOptionPane.showMessageDialog(this, "Ticket " + ticket.getTicketId() + " was filed.",
                    "Report an issue", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Could not file the ticket: " + e.getMessage(),
                    "Report an issue", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------- The payslip itself ----------------------

    /**
     * Builds the printed-style payslip for one computed record.
     *
     * @param slip the computed payslip
     * @return a white panel ready to place in any container
     */
    public static JPanel createPaper(PayslipData slip) {
        JPanel paper = new JPanel();
        paper.setLayout(new BoxLayout(paper, BoxLayout.Y_AXIS));
        paper.setBackground(Color.WHITE);
        paper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIUtility.BORDER),
                BorderFactory.createEmptyBorder(28, 36, 28, 36)));

        JLabel brand = new JLabel("MotorPH");
        brand.setFont(GUIUtility.headingFont(Font.BOLD, 24f));
        brand.setForeground(GUIUtility.BRAND_RED);
        brand.setHorizontalAlignment(SwingConstants.CENTER);
        fullWidth(brand);
        paper.add(brand);
        paper.add(small(ADDRESS));
        paper.add(small(CONTACT));
        paper.add(Box.createVerticalStrut(16));

        JLabel title = new JLabel("EMPLOYEE PAYSLIP");
        title.setFont(GUIUtility.bodyFont(Font.BOLD, 16f));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        fullWidth(title);
        paper.add(title);
        paper.add(Box.createVerticalStrut(14));

        paper.add(row("PAYSLIP NO", slip.empNo() + "-" + slip.cutoffStart().toString().replace("-", "")
                + "-" + slip.cutoffEnd().toString().replace("-", ""), false));
        paper.add(row("PERIOD", slip.cutoffStart() + " to " + slip.cutoffEnd(), false));
        paper.add(row("EMPLOYEE ID", String.valueOf(slip.empNo()), false));
        paper.add(row("EMPLOYEE NAME", slip.employeeName(), false));
        paper.add(row("POSITION", slip.position(), false));

        paper.add(heading("EARNINGS"));
        paper.add(row("Monthly Rate", GUIUtility.formatPeso(slip.monthlyRate()), false));
        paper.add(row("Hourly Rate", GUIUtility.formatPeso(slip.hourlyRate()), false));
        paper.add(row("Days Worked", String.valueOf(slip.daysWorked()), false));
        paper.add(row("Hours Worked", String.format("%.2f", slip.hoursWorked()), false));
        paper.add(row("Late (minutes)", String.valueOf(slip.lateMinutes()), false));
        paper.add(row("Overtime Hours", String.format("%.2f", slip.overtimeHours()), false));
        paper.add(row("Overtime Pay (1.25x)", GUIUtility.formatPeso(slip.overtimePay()), false));
        paper.add(row("GROSS INCOME", GUIUtility.formatPeso(slip.grossIncome()), true));

        paper.add(heading("BENEFITS"));
        paper.add(row("Rice Subsidy", GUIUtility.formatPeso(slip.riceSubsidy()), false));
        paper.add(row("Phone Allowance", GUIUtility.formatPeso(slip.phoneAllowance()), false));
        paper.add(row("Clothing Allowance", GUIUtility.formatPeso(slip.clothingAllowance()), false));
        paper.add(row("TOTAL", GUIUtility.formatPeso(slip.benefitsTotal()), true));

        paper.add(heading("DEDUCTIONS"));
        paper.add(row("Social Security System", GUIUtility.formatPeso(slip.sss()), false));
        paper.add(row("PhilHealth", GUIUtility.formatPeso(slip.philHealth()), false));
        paper.add(row("Pag-IBIG", GUIUtility.formatPeso(slip.pagIbig()), false));
        paper.add(row("Withholding Tax", GUIUtility.formatPeso(slip.withholdingTax()), false));
        paper.add(row("TOTAL DEDUCTIONS", GUIUtility.formatPeso(slip.totalDeductions()), true));

        paper.add(heading("SUMMARY"));
        paper.add(row("Gross Income", GUIUtility.formatPeso(slip.grossIncome()), false));
        paper.add(row("Benefits", GUIUtility.formatPeso(slip.benefitsTotal()), false));
        paper.add(row("Deductions", GUIUtility.formatPeso(slip.totalDeductions()), false));
        JPanel takeHome = row("TAKE HOME PAY", GUIUtility.formatPeso(slip.takeHomePay()), true);
        for (Component part : takeHome.getComponents()) {
            part.setFont(GUIUtility.bodyFont(Font.BOLD, 16f));
            part.setForeground(GUIUtility.BRAND_RED);
        }
        paper.add(takeHome);
        paper.setPreferredSize(new Dimension(620, paper.getPreferredSize().height));
        return paper;
    }

    private String selectedCutoffLabel() {
        String month = (String) monthBox.getSelectedItem();
        PayrollCutoff cutoff = (PayrollCutoff) cutoffBox.getSelectedItem();
        if (month == null || cutoff == null) {
            return "selected cutoff";
        }
        YearMonth yearMonth = YearMonth.parse(month);
        return cutoff.startDate(yearMonth) + " to " + cutoff.endDate(yearMonth);
    }

    private static JLabel small(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(GUIUtility.bodyFont(Font.PLAIN, 11f));
        label.setForeground(GUIUtility.MUTED_TEXT);
        return fullWidth(label);
    }

    /** Lets a centred label span the whole paper so BoxLayout aligns it with the left-aligned rows. */
    private static JLabel fullWidth(JLabel label) {
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
        return label;
    }

    private static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(GUIUtility.bodyFont(Font.BOLD, 13f));
        label.setForeground(GUIUtility.BRAND_RED);
        label.setBorder(BorderFactory.createEmptyBorder(16, 0, 4, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JPanel row(String label, String value, boolean bold) {
        JLabel left = new JLabel(label);
        JLabel right = new JLabel(value, SwingConstants.RIGHT);
        Font font = GUIUtility.bodyFont(bold ? Font.BOLD : Font.PLAIN, 13f);
        left.setFont(font);
        right.setFont(font);
        left.setForeground(bold ? GUIUtility.FIELD_TEXT : GUIUtility.MUTED_TEXT);
        right.setForeground(GUIUtility.FIELD_TEXT);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
