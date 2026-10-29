package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import payrollhub.data.CsvHandler;
import payrollhub.model.Attendance;
import payrollhub.service.AttendanceService;

public class GUITimesheet extends JFrame {

    private final AttendanceService attendanceService =
            new AttendanceService(CsvHandler.ATTENDANCE_FILE.toString());
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] { "Date", "Time in", "Time out", "Hours worked" }, 0) {
        /**
         * Keeps attendance rows read-only so the table cannot bypass attendance services.
         *
         * @param row the row being checked
         * @param column the column being checked
         * @return {@code false} because timesheet values are not edited directly
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable attendanceTable = new JTable(tableModel);
    private final JComboBox<String> monthSelector = new JComboBox<>();
    private final JLabel employeeLabel = new JLabel("No employee signed in");
    private final JButton employeeManagementButton = GUIUtility.createNavButton("Employee management");
    private GUIUser loggedIn;

    /**
     * Builds the timesheet window, adds its header, attendance table, and actions,
     * then loads the months available in attendance records.
     */
    public GUITimesheet() {
        super("Payroll Hub - Timesheet");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        add(createHeader(), BorderLayout.NORTH);
        add(new JScrollPane(attendanceTable), BorderLayout.CENTER);
        add(createActions(), BorderLayout.SOUTH);
        setSize(760, 480);
        setLocationRelativeTo(null);
        loadAvailableMonths();
    }

    /**
     * Creates the header with the screen title, signed-in employee label, and month
     * selector; changing the selected month refreshes the attendance table.
     *
     * @return the configured header panel
     */
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 8));
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 0, 16));
        header.add(GUIUtility.createHeaderLabel("Timesheet"), BorderLayout.NORTH);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(employeeLabel);
        controls.add(new JLabel("Month"));
        controls.add(monthSelector);
        monthSelector.addActionListener(event -> reloadTable());
        header.add(controls, BorderLayout.SOUTH);
        return header;
    }

    /**
     * Creates the action buttons and connects each one to its attendance or
     * navigation workflow; employee management is shown only when permitted.
     *
     * @return the configured action panel
     */
    private JPanel createActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton checkInButton = GUIUtility.createButton("Check in", new java.awt.Color(40, 110, 75));
        checkInButton.addActionListener(event -> checkIn());
        JButton checkOutButton = GUIUtility.createButton("Check out", new java.awt.Color(48, 63, 87));
        checkOutButton.addActionListener(event -> checkOut());
        JButton leaveButton = GUIUtility.createNavButton("Leave requests");
        leaveButton.addActionListener(event -> openLeaveScreen());
        employeeManagementButton.setVisible(false);
        employeeManagementButton.addActionListener(event -> {
            new GUIDashboard(loggedIn).setVisible(true);
            dispose();
        });
        JButton logoutButton = GUIUtility.createNavButton("Sign out");
        logoutButton.addActionListener(event -> signOut());
        actions.add(checkInButton);
        actions.add(checkOutButton);
        actions.add(leaveButton);
        actions.add(employeeManagementButton);
        actions.add(logoutButton);
        return actions;
    }

    /**
     * Sets the current user, updates their displayed identity and management
     * permissions, and loads their attendance for the selected month.
     *
     * @param emp the signed-in user
     * @throws IllegalArgumentException if {@code emp} is {@code null}
     */
    public void setLoggedIn(GUIUser emp) {
        if (emp == null) {
            throw new IllegalArgumentException("A signed-in user is required.");
        }
        loggedIn = emp;
        employeeManagementButton.setVisible(emp.canManageEmployees());
        employeeLabel.setText(emp.getEmployee()
                .map(employee -> employee.getFullName() + " (#" + emp.getEmpNo() + ")")
                .orElse(emp.getUsername() + " (#" + emp.getEmpNo() + ")"));
        reloadTable();
    }

    /**
     * Records the current user's check-in time, refreshes the available months
     * and table, and displays an error if recording fails.
     */
    private void checkIn() {
        if (loggedIn == null) {
            showError("Sign in before recording attendance.");
            return;
        }
        try {
            attendanceService.recordTimeIn(loggedIn.getEmpNo(), LocalDateTime.now());
            loadAvailableMonths();
            reloadTable();
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Records the current user's check-out time, refreshes the table, and displays
     * an error if recording fails or no user is signed in.
     */
    private void checkOut() {
        if (loggedIn == null) {
            showError("Sign in before recording attendance.");
            return;
        }
        try {
            attendanceService.recordTimeOut(loggedIn.getEmpNo(), LocalDateTime.now());
            reloadTable();
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Loads months represented in the attendance data, selects the previously
     * selected month when available, and otherwise selects the current month.
     */
    private void loadAvailableMonths() {
        try {
            List<String> months = CsvHandler.loadAvailableMonths(CsvHandler.ATTENDANCE_FILE.toString());
            if (months.isEmpty()) {
                months = List.of(YearMonth.now().toString());
            }
            String selectedMonth = (String) monthSelector.getSelectedItem();
            monthSelector.removeAllItems();
            months.forEach(monthSelector::addItem);
            if (selectedMonth != null && months.contains(selectedMonth)) {
                monthSelector.setSelectedItem(selectedMonth);
            } else {
                monthSelector.setSelectedItem(YearMonth.now().toString());
            }
        } catch (IOException e) {
            showError("Unable to load attendance months: " + e.getMessage());
        }
    }

    /**
     * Clears the table and reloads the signed-in user's attendance for the selected
     * month, displaying worked hours only when both time values are present.
     */
    private void reloadTable() {
        tableModel.setRowCount(0);
        if (loggedIn == null || monthSelector.getSelectedItem() == null) {
            return;
        }
        try {
            YearMonth month = YearMonth.parse((String) monthSelector.getSelectedItem());
            for (Attendance attendance : attendanceService.loadForMonth(loggedIn.getEmpNo(), month)) {
                Object timeIn = attendance.getTimeIn() == null ? "" : attendance.getTimeIn();
                Object timeOut = attendance.getTimeOut() == null ? "" : attendance.getTimeOut();
                Object hoursWorked = attendance.getTimeIn() == null || attendance.getTimeOut() == null
                        ? ""
                        : String.format("%.2f", java.time.Duration.between(
                                attendance.getTimeIn(), attendance.getTimeOut()).toMinutes() / 60.0);
                tableModel.addRow(new Object[] { attendance.getDate(), timeIn, timeOut, hoursWorked });
            }
        } catch (IOException | IllegalArgumentException e) {
            showError("Unable to reload attendance: " + e.getMessage());
        }
    }

    /**
     * Opens the leave-request screen for the signed-in user and closes this window;
     * if there is no signed-in user, shows an error instead.
     */
    private void openLeaveScreen() {
        if (loggedIn == null) {
            showError("Sign in before opening leave requests.");
            return;
        }
        new GUILeave(loggedIn).setVisible(true);
        dispose();
    }

    /**
     * Returns to the login screen and closes the current timesheet window.
     */
    private void signOut() {
        new GUILogin().setVisible(true);
        dispose();
    }

    /**
     * Displays an error dialog associated with this timesheet window.
     *
     * @param message the error message to show
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Timesheet", JOptionPane.ERROR_MESSAGE);
    }
}
