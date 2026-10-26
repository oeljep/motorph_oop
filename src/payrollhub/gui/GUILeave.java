package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import payrollhub.contracts.LeaveHub;
import payrollhub.model.LeaveRequest;
import payrollhub.service.LeaveService;

public class GUILeave extends JFrame {

    private final GUIUser user;
    private final LeaveHub leaveHub = new LeaveService();
    private final JTextField startDateField = GUIUtility.createTextField(12);
    private final JTextField endDateField = GUIUtility.createTextField(12);
    private final JTextField reasonField = GUIUtility.createTextField(28);
    private final JComboBox<String> leaveTypeSelector =
            new JComboBox<>(new String[] { "Sick", "Vacation", "Personal" });
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] { "Request ID", "Employee #", "Type", "Start", "End", "Status", "Reason" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable leaveTable = new JTable(tableModel);

    public GUILeave(GUIUser user) {
        super("Payroll Hub - Leave requests");
        if (user == null) {
            throw new IllegalArgumentException("A signed-in user is required.");
        }
        this.user = user;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        add(createHeader(), BorderLayout.NORTH);
        add(new JScrollPane(leaveTable), BorderLayout.CENTER);
        add(createActions(), BorderLayout.SOUTH);
        setSize(900, 520);
        setLocationRelativeTo(null);
        reloadLeaveTable();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 0, 16));
        header.add(GUIUtility.createHeaderLabel("Leave requests"), BorderLayout.NORTH);
        JPanel requestForm = new JPanel(new FlowLayout(FlowLayout.LEFT));
        requestForm.add(new JLabel("Start (yyyy-MM-dd)"));
        requestForm.add(startDateField);
        requestForm.add(new JLabel("End (yyyy-MM-dd)"));
        requestForm.add(endDateField);
        requestForm.add(new JLabel("Type"));
        requestForm.add(leaveTypeSelector);
        requestForm.add(new JLabel("Reason"));
        requestForm.add(reasonField);
        header.add(requestForm, BorderLayout.SOUTH);
        return header;
    }

    private JPanel createActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submitButton = GUIUtility.createButton("Submit request", new java.awt.Color(40, 110, 75));
        submitButton.addActionListener(event -> submitLeaveRequest());
        JButton approveButton = GUIUtility.createNavButton("Approve");
        approveButton.setEnabled(user.canApproveLeave());
        approveButton.addActionListener(event -> updateSelectedRequest("APPROVED"));
        JButton rejectButton = GUIUtility.createNavButton("Reject");
        rejectButton.setEnabled(user.canApproveLeave());
        rejectButton.addActionListener(event -> updateSelectedRequest("REJECTED"));
        JButton reloadButton = GUIUtility.createNavButton("Reload");
        reloadButton.addActionListener(event -> reloadLeaveTable());
        JButton backButton = GUIUtility.createNavButton("Timesheet");
        backButton.addActionListener(event -> {
            GUITimesheet timesheet = new GUITimesheet();
            timesheet.setLoggedIn(user);
            timesheet.setVisible(true);
            dispose();
        });
        actions.add(submitButton);
        actions.add(approveButton);
        actions.add(rejectButton);
        actions.add(reloadButton);
        actions.add(backButton);
        return actions;
    }

    private void submitLeaveRequest() {
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(startDateField.getText().trim());
            end = LocalDate.parse(endDateField.getText().trim());
        } catch (java.time.DateTimeException e) {
            showError("Enter both dates in yyyy-MM-dd format.");
            return;
        }
        if (!validateDates(start, end)) {
            showError("The end date must not be before the start date.");
            return;
        }
        try {
            leaveHub.applyLeave("LR-" + UUID.randomUUID(), user.getEmpNo(),
                    start, end, leaveTypeSelector.getSelectedItem().toString(),
                    reasonField.getText().trim());
            startDateField.setText("");
            endDateField.setText("");
            reasonField.setText("");
            reloadLeaveTable();
        } catch (IOException | IllegalArgumentException e) {
            showError("Unable to submit leave request: " + e.getMessage());
        }
    }

    private boolean validateDates(LocalDate start, LocalDate end) {
        return start != null && end != null && !end.isBefore(start);
    }

    private void updateSelectedRequest(String status) {
        int selectedRow = leaveTable.getSelectedRow();
        if (!user.canApproveLeave()) {
            showError("This account cannot approve leave requests.");
            return;
        }
        if (selectedRow < 0) {
            showError("Select a leave request first.");
            return;
        }
        String leaveID = leaveTable.getValueAt(selectedRow, 0).toString();
        try {
            leaveHub.updateLeaveStatus(leaveID, status);
            reloadLeaveTable();
        } catch (IOException | IllegalArgumentException e) {
            showError("Unable to update leave request: " + e.getMessage());
        }
    }

    private void reloadLeaveTable() {
        tableModel.setRowCount(0);
        try {
            for (LeaveRequest request : leaveHub.viewAllLeaveRequests()) {
                if (user.canApproveLeave() || request.getEmployeeID() == user.getEmpNo()) {
                    tableModel.addRow(new Object[] {
                            request.getLeaveID(), request.getEmployeeID(), request.getType(),
                            request.getStartDate(), request.getEndDate(), request.getStatus(),
                            request.getReason()
                    });
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            showError("Unable to load leave requests: " + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Leave requests", JOptionPane.ERROR_MESSAGE);
    }
}
