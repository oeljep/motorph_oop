package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;

import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.service.PayslipCalculator;
import payrollhub.service.PayslipData;
import payrollhub.service.PayrollCutoff;

/**
 * Payroll Manager tab (Accounting roles): payroll for every employee in a
 * chosen month, with totals, a payslip viewer and CSV export. Amounts come
 * from {@link PayslipCalculator}; this class only displays them.
 */
public class GUIPayrollManager extends JPanel {

    private static final String[] COLUMNS = { "Emp #", "Name", "Position", "Days", "Hours",
            "OT Hours", "OT Pay", "Gross", "Benefits", "Deductions", "Net pay", "Cutoff" };

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            if (column == 0 || column == 3) {
                return Integer.class;
            }
            return column >= 4 && column <= 10 ? Double.class : String.class;
        }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<String> monthBox = new JComboBox<>();
    private final JComboBox<PayrollCutoff> cutoffBox = new JComboBox<>(PayrollCutoff.values());
    private final JTextField searchField = GUIUtility.createTextField(16);
    private final JLabel totalsLabel = new JLabel(" ");
    private final List<PayslipData> slips = new ArrayList<>();
    private boolean loading;

    /** Builds the tab. */
    public GUIPayrollManager() {
        setBackground(GUIUtility.PAGE_BACKGROUND);
        setLayout(new BorderLayout(0, GUIUtility.scale(20)));
        setBorder(BorderFactory.createEmptyBorder(
                GUIUtility.scale(40), GUIUtility.scale(60), GUIUtility.scale(40), GUIUtility.scale(60)));

        GUIUtility.styleTable(table);
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centered);
        table.getColumnModel().getColumn(0).setMaxWidth(90);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        DefaultTableCellRenderer money = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value instanceof Double ? GUIUtility.formatPeso((Double) value) : String.valueOf(value));
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        };
        table.getColumnModel().getColumn(1).setPreferredWidth(190);
        table.getColumnModel().getColumn(2).setPreferredWidth(190);
        table.getColumnModel().getColumn(3).setMaxWidth(70);
        for (int col = 6; col <= 10; col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(money);
        }
        DefaultTableCellRenderer hours = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value instanceof Double ? String.format(Locale.US, "%.2f", (Double) value) : String.valueOf(value));
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        };
        table.getColumnModel().getColumn(4).setCellRenderer(hours);
        table.getColumnModel().getColumn(5).setCellRenderer(hours);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void apply() {
                String text = searchField.getText().trim();
                sorter.setRowFilter(text.isEmpty() ? null
                        : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 0, 1, 2));
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                apply();
            }
        });

        JButton view = GUIUtility.createActionButton("View payslip", GUIUtility.BRAND_RED);
        JButton export = GUIUtility.createActionButton("Export CSV", GUIUtility.INK_DARK);
        view.addActionListener(event -> viewPayslip());
        export.addActionListener(event -> exportCsv());
        monthBox.addActionListener(event -> {
            if (!loading) {
                loadPayroll();
            }
        });
        cutoffBox.addActionListener(event -> {
            if (!loading) {
                loadPayroll();
            }
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        controls.setOpaque(false);
        controls.add(new JLabel("Pay month"));
        controls.add(monthBox);
        controls.add(new JLabel("Cutoff"));
        controls.add(cutoffBox);
        controls.add(new JLabel("Search"));
        controls.add(searchField);
        controls.add(view);
        controls.add(export);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(GUIUtility.createPageTitle("Payroll Manager"), BorderLayout.WEST);
        top.add(controls, BorderLayout.EAST);

        totalsLabel.setFont(GUIUtility.bodyFont(Font.BOLD, 14f));
        add(top, BorderLayout.NORTH);
        add(GUIUtility.wrapTable(table), BorderLayout.CENTER);
        add(totalsLabel, BorderLayout.SOUTH);

        loadMonths();
    }

    private void loadMonths() {
        loading = true;
        try {
            List<String> months = new ArrayList<>();
            for (EmployeeData e : CsvHandler.loadEmployees()) {
                for (String m : CsvHandler.loadAttendanceMonths(e.getEmpNo())) {
                    if (!months.contains(m)) {
                        months.add(m);
                    }
                }
            }
            Collections.sort(months, Collections.reverseOrder());
            for (String m : months) {
                monthBox.addItem(m);
            }
        } catch (IOException e) {
            totalsLabel.setText("Could not read data: " + e.getMessage());
            loading = false;
            return;
        }
        loading = false;
        loadPayroll();
    }

    private void loadPayroll() {
        model.setRowCount(0);
        slips.clear();
        String period = (String) monthBox.getSelectedItem();
        PayrollCutoff cutoff = (PayrollCutoff) cutoffBox.getSelectedItem();
        if (period == null || cutoff == null) {
            totalsLabel.setText("No attendance records yet.");
            return;
        }
        try {
            YearMonth month = YearMonth.parse(period);
            double gross = 0;
            double net = 0;
            for (EmployeeData e : CsvHandler.loadEmployees()) {
                PayslipData s = PayslipCalculator.compute(e,
                        CsvHandler.loadAttendance(e.getEmpNo(), period), month, cutoff);
                if (s.daysWorked() == 0) {
                    continue;
                }
                slips.add(s);
                gross += s.grossIncome();
                net += s.takeHomePay();
                model.addRow(new Object[] { s.empNo(), s.employeeName(), s.position(), s.daysWorked(),
                        s.hoursWorked(), s.overtimeHours(), s.overtimePay(), s.grossIncome(), s.benefitsTotal(),
                        s.totalDeductions(), s.takeHomePay(), s.cutoffStart() + " to " + s.cutoffEnd() });
            }
            totalsLabel.setText(String.format("%s to %s     Employees paid: %d     Total gross: %s     Total net: %s",
                    cutoff.startDate(month), cutoff.endDate(month), slips.size(),
                    GUIUtility.formatPeso(gross), GUIUtility.formatPeso(net)));
        } catch (IOException e) {
            totalsLabel.setText("Could not read data: " + e.getMessage());
        }
    }

    private PayslipData selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an employee first.", "Payroll",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        int empNo = (Integer) model.getValueAt(table.convertRowIndexToModel(row), 0);
        for (PayslipData s : slips) {
            if (s.empNo() == empNo) {
                return s;
            }
        }
        return null;
    }

    private void viewPayslip() {
        PayslipData slip = selected();
        if (slip == null) {
            return;
        }
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Payslip - " + slip.employeeName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JScrollPane scroll = new JScrollPane(GUIPayslip.createPaper(slip));
        scroll.setBorder(null);
        dialog.add(scroll);
        dialog.setSize(700, 780);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void exportCsv() {
        if (slips.isEmpty()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        PayrollCutoff cutoff = (PayrollCutoff) cutoffBox.getSelectedItem();
        chooser.setSelectedFile(new java.io.File("payroll-" + monthBox.getSelectedItem()
                + "-" + cutoff + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (PayslipData s : slips) {
            rows.add(new String[] { String.valueOf(s.empNo()), s.employeeName(), s.position(),
                    String.valueOf(s.daysWorked()), String.format(Locale.US, "%.2f", s.hoursWorked()),
                    String.format(Locale.US, "%.2f", s.overtimeHours()),
                    String.format(Locale.US, "%.2f", s.overtimePay()),
                    String.format(Locale.US, "%.2f", s.grossIncome()),
                    String.format(Locale.US, "%.2f", s.benefitsTotal()),
                    String.format(Locale.US, "%.2f", s.totalDeductions()),
                    String.format(Locale.US, "%.2f", s.takeHomePay()),
                    s.cutoffStart() + " to " + s.cutoffEnd() });
        }
        try {
            CsvHandler.writeAll(Paths.get(chooser.getSelectedFile().getPath()), COLUMNS, rows);
            JOptionPane.showMessageDialog(this, "Exported " + rows.size() + " rows.", "Payroll",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not export: " + e.getMessage(), "Payroll",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
