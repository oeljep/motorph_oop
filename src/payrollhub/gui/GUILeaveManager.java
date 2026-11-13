package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.data.LeaveData;

/**
 * Leave Management screen for approvers: direct reports' leave requests with
 * Approve and Reject. Employees file and track their own requests in
 * {@link GUILeave}; deciding requests lives only here.
 *
 * Only roles allowed by {@link NavAccess#canDecideLeave(String)} get the
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
        if (NavAccess.canDecideLeave(role)) {
            JButton review = GUIUtility.createActionButton("Review", GUIUtility.INK_DARK);
            review.addActionListener(event -> reviewSelected());
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.setOpaque(false);
            actions.add(review);
            bottom.add(actions, BorderLayout.EAST);
        }

        table.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "reviewLeave");
        table.getActionMap().put("reviewLeave", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                reviewSelected();
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    reviewSelected();
                }
            }
        });

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
            List<EmployeeData> employees = CsvHandler.loadEmployees();
            Set<Integer> directReportEmpNos = new HashSet<>();
            for (EmployeeData e : employees) {
                names.put(Integer.parseInt(e.getEmpNo()), e.getFullName());
                if (CsvHandler.findSupervisor(e, employees)
                        .map(supervisor -> Integer.parseInt(supervisor.getEmpNo()) == empNo)
                        .orElse(false)) {
                    directReportEmpNos.add(Integer.parseInt(e.getEmpNo()));
                }
            }
            leaves = new ArrayList<>();
            for (LeaveData leave : CsvHandler.loadLeaves()) {
                if (directReportEmpNos.contains(leave.getEmpNo())) {
                    leaves.add(leave);
                }
            }
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

    private void reviewSelected() {
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
        showReview(leave);
    }

    private void showReview(LeaveData leave) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Review Leave Request " + leave.getLeaveId(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel details = new JPanel(new java.awt.GridLayout(0, 2, 12, 10));
        details.setBorder(BorderFactory.createEmptyBorder(20, 20, 12, 20));
        addDetail(details, "Request ID", leave.getLeaveId());
        addDetail(details, "Employee", names.getOrDefault(leave.getEmpNo(), String.valueOf(leave.getEmpNo())));
        addDetail(details, "Leave type", leave.getType());
        addDetail(details, "Start date", leave.getStartDate().format(DATE));
        addDetail(details, "End date", leave.getEndDate().format(DATE));
        addDetail(details, "Status", capitalize(leave.getStatus()));

        JTextArea reason = new JTextArea(leave.getReason().isBlank() ? "No reason provided." : leave.getReason());
        reason.setEditable(false);
        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        reason.setBackground(java.awt.Color.WHITE);
        reason.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel reasonPanel = new JPanel(new BorderLayout(0, 6));
        reasonPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));
        reasonPanel.add(new JLabel("Reason"), BorderLayout.NORTH);
        reasonPanel.add(new JScrollPane(reason), BorderLayout.CENTER);

        JButton close = GUIUtility.createActionButton("Close", GUIUtility.INK_DARK);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        if (NavAccess.canDecideLeave(role) && leave.isPending() && leave.getEmpNo() != empNo) {
            JButton approve = GUIUtility.createActionButton("Approve", new Color(0x1E7A46));
            JButton reject = GUIUtility.createActionButton("Reject", GUIUtility.BRAND_RED);
            approve.addActionListener(event -> decide(leave, LeaveData.APPROVED, dialog));
            reject.addActionListener(event -> decide(leave, LeaveData.REJECTED, dialog));
            buttons.add(approve);
            buttons.add(reject);
        }
        close.addActionListener(event -> dialog.dispose());
        buttons.add(close);

        JPanel content = new JPanel(new BorderLayout());
        content.add(details, BorderLayout.NORTH);
        content.add(reasonPanel, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        dialog.setSize(520, 390);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static void addDetail(JPanel details, String label, String value) {
        details.add(new JLabel(label));
        details.add(new JLabel(value));
    }

    private void decide(LeaveData leave, String newStatus, JDialog dialog) {
        if (!NavAccess.canDecideLeave(role) || !leave.isPending() || leave.getEmpNo() == empNo) {
            return;
        }
        try {
            CsvHandler.updateLeaveStatus(leave.getLeaveId(), newStatus);
            reload();
            dialog.dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save the decision: " + e.getMessage(),
                    "Leave Management", JOptionPane.ERROR_MESSAGE);
        }
    }
}
