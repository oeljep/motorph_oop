package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.data.LeaveData;

/**
 * Leave Management screen for approvers: every employee's leave requests with
 * Approve and Reject. Employees file and track their own requests in
 * {@link GUILeave}; deciding requests lives only here.
 *
 * Only roles allowed by {@link ActionAccess#canDecideLeave(String)} get the
 * Approve and Reject buttons, and nobody can decide their own request. All file
 * access goes through {@link CsvHandler}.
 */
public class GUILeaveManager extends JPanel {

    private static final String[] COLUMNS = { "ID", "Employee", "Type", "From", "To", "Status", "Reason" };
    private static final String[] FILTERS = { "All", "Pending", "Approved", "Rejected" };
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final String role;
    private final int empNo;
    private final Map<Integer, String> names = new HashMap<>();
    private List<LeaveData> leaves = new ArrayList<>();

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField searchField = GUIUtility.createTextField(14);
    private final JComboBox<String> statusBox = new JComboBox<>(FILTERS);
    private final JLabel summaryLabel = new JLabel(" ");

    /**
     * Builds the screen.
     *
     * @param role  the signed-in user's role from users.csv
     * @param empNo the signed-in employee's number
     */
    public GUILeaveManager(String role, int empNo) {
        this.role = role;
        this.empNo = empNo;
        setBackground(GUIUtility.PAGE_BACKGROUND);
        setLayout(new BorderLayout(0, GUIUtility.scale(20)));
        setBorder(BorderFactory.createEmptyBorder(
                GUIUtility.scale(40), GUIUtility.scale(60), GUIUtility.scale(40), GUIUtility.scale(60)));

        GUIUtility.styleTable(table);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(6).setPreferredWidth(220);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        statusBox.addActionListener(event -> applyFilter());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);
        controls.add(new JLabel("Search"));
        controls.add(searchField);
        controls.add(statusBox);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(GUIUtility.createPageTitle("Leave Management"), BorderLayout.WEST);
        top.add(controls, BorderLayout.EAST);

        summaryLabel.setFont(GUIUtility.bodyFont(java.awt.Font.BOLD, 14f));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(summaryLabel, BorderLayout.WEST);
        if (ActionAccess.canDecideLeave(role)) {
            JButton approve = GUIUtility.createActionButton("Approve", new Color(0x1E7A46));
            JButton reject = GUIUtility.createActionButton("Reject", GUIUtility.BRAND_RED);
            approve.addActionListener(event -> decide(LeaveData.APPROVED));
            reject.addActionListener(event -> decide(LeaveData.REJECTED));
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttons.setOpaque(false);
            buttons.add(approve);
            buttons.add(reject);
            bottom.add(buttons, BorderLayout.EAST);
        }

        add(top, BorderLayout.NORTH);
        add(GUIUtility.wrapTable(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        reload();
    }

    // ---------------------- Data ----------------------

    private void reload() {
        model.setRowCount(0);
        try {
            names.clear();
            for (EmployeeData e : CsvHandler.loadEmployees()) {
                names.put(e.getEmpNo(), e.getFullName());
            }
            leaves = new ArrayList<>(CsvHandler.loadLeaves());
            leaves.sort(Comparator.comparing(LeaveData::isPending).reversed()
                    .thenComparing(LeaveData::getStartDate, Comparator.reverseOrder()));
            int pending = 0;
            for (LeaveData leave : leaves) {
                if (leave.isPending()) {
                    pending++;
                }
                model.addRow(new Object[] {
                        leave.getLeaveId(),
                        names.getOrDefault(leave.getEmpNo(), String.valueOf(leave.getEmpNo())),
                        leave.getType(),
                        leave.getStartDate().format(DATE),
                        leave.getEndDate().format(DATE),
                        capitalize(leave.getStatus()),
                        leave.getReason() });
            }
            summaryLabel.setText(leaves.size() + " requests, " + pending + " waiting for a decision");
        } catch (IOException e) {
            summaryLabel.setText("Could not read leave data: " + e.getMessage());
        }
        applyFilter();
    }

    private void applyFilter() {
        List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();
        String text = searchField.getText().trim();
        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
        String status = (String) statusBox.getSelectedItem();
        if (status != null && !"All".equals(status)) {
            filters.add(RowFilter.regexFilter("^" + status + "$", 5));
        }
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : text.charAt(0) + text.substring(1).toLowerCase(Locale.ROOT);
    }

    // ---------------------- Actions ----------------------

    private void decide(String newStatus) {
        if (!ActionAccess.canDecideLeave(role)) {
            return; // the buttons only exist for approvers; this guards direct calls
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a request first.", "Leave Management",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = (String) model.getValueAt(table.convertRowIndexToModel(row), 0);
        LeaveData leave = null;
        for (LeaveData candidate : leaves) {
            if (candidate.getLeaveId().equals(id)) {
                leave = candidate;
            }
        }
        if (leave == null) {
            return;
        }
        if (!leave.isPending()) {
            JOptionPane.showMessageDialog(this,
                    "That request is already " + leave.getStatus().toLowerCase(Locale.ROOT) + ".",
                    "Leave Management", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (leave.getEmpNo() == empNo) {
            JOptionPane.showMessageDialog(this, "You cannot decide your own leave request.",
                    "Leave Management", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            CsvHandler.updateLeaveStatus(leave.getLeaveId(), newStatus);
            reload();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save the decision: " + e.getMessage(),
                    "Leave Management", JOptionPane.ERROR_MESSAGE);
        }
    }
}
