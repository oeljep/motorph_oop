package payrollhub.test;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import payrollhub.data.AttendanceData;
import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.data.LeaveData;
import payrollhub.data.TicketData;
import payrollhub.gui.GUIDashboardHome;
import payrollhub.gui.GUILeaveManager;
import payrollhub.service.AttendanceRules;
import payrollhub.service.PayslipCalculator;
import payrollhub.service.PayslipData;

/** Read-only checks for the data and service APIs used by the Swing screens. */
public final class GuiBackendIntegrationTest {

    private GuiBackendIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        int empNo = 10001;
        List<String> months = CsvHandler.loadAttendanceMonths(empNo);
        check(!months.isEmpty(), "attendance months are available for an employee");

        List<AttendanceData> attendance = CsvHandler.loadAttendance(empNo, months.get(0));
        check(!attendance.isEmpty(), "attendance records load for a selected month");
        check(attendance.stream().allMatch(record -> record.getEmpNo() == empNo),
                "attendance is scoped to the signed-in employee");

        EmployeeData employee = CsvHandler.findEmployee(empNo)
                .orElseThrow(() -> new AssertionError("employee record loads from employee.csv"));
        PayslipData slip = PayslipCalculator.compute(employee, attendance, months.get(0));
        check(slip.empNo() == empNo && slip.period().equals(months.get(0)),
                "payslip calculation uses the employee and selected pay period");

        check(!CsvHandler.loadLeaves().isEmpty(), "leave requests load from leaves.csv");
        check(AttendanceRules.hoursWorked(LocalTime.of(8, 0), LocalTime.of(17, 0)) == 8,
                "attendance calculation applies the unpaid break");
        check(AttendanceRules.lateMinutes(LocalTime.of(8, 11)) == 1,
                "attendance calculation applies the grace period");

        AtomicReference<GUIDashboardHome> managementHome = new AtomicReference<>();
        AtomicReference<GUIDashboardHome> personalHome = new AtomicReference<>();
        AtomicReference<GUILeaveManager> adminLeaveManager = new AtomicReference<>();
        AtomicReference<GUILeaveManager> hrLeaveManager = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            managementHome.set(new GUIDashboardHome("Admin", empNo));
            personalHome.set(new GUIDashboardHome("Regular", empNo));
            adminLeaveManager.set(new GUILeaveManager("Admin", 10002));
            hrLeaveManager.set(new GUILeaveManager("HumanResources", 10002));
        });
        check(containsLabel(managementHome.get(), "Payroll Overview")
                        && containsLabel(managementHome.get(), "Pending leave requests"),
                "management dashboard displays company overview cards");
        JTable employeeTable = findTable(managementHome.get());
        check(employeeTable != null && employeeTable.getRowCount() == CsvHandler.loadEmployees().size(),
                "management dashboard table displays all employees");
        check(containsLabel(personalHome.get(), "Basic salary")
                        && containsLabel(personalHome.get(), "Attendance")
                        && containsLabel(personalHome.get(), "Pending leave requests"),
                "staff dashboard displays personal payroll, attendance and leave summaries");
        Set<String> directReportLeaveIds = Set.of("L0005", "L0006", "L0015");
        check(leaveIds(adminLeaveManager.get()).equals(directReportLeaveIds),
                "Admin leave manager displays only direct reports' requests");
        check(leaveIds(hrLeaveManager.get()).equals(directReportLeaveIds),
                "HR leave manager displays only direct reports' requests");

        TicketData ticket = TicketData.newTicket("T99999", empNo, "Test subject", "Test message");
        TicketData reloaded = TicketData.fromCsvRow(ticket.toCsvRow());
        reloaded.addRemark("Test reply");
        check(TicketData.OPEN.equals(reloaded.getStatus()) && reloaded.getRemarks().contains("Test reply"),
                "ticket data round-trips for filing and replies");

        System.out.println("GUI backend integration checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static boolean containsLabel(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel && ((JLabel) component).getText().contains(text)) {
                return true;
            }
            if (component instanceof Container && containsLabel((Container) component, text)) {
                return true;
            }
        }
        return false;
    }

    private static JTable findTable(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTable) {
                return (JTable) component;
            }
            if (component instanceof Container) {
                JTable table = findTable((Container) component);
                if (table != null) {
                    return table;
                }
            }
        }
        return null;
    }

    private static Set<String> leaveIds(GUILeaveManager manager) {
        JTable table = findTable(manager);
        if (table == null) {
            throw new AssertionError("leave manager table is available");
        }
        Set<String> ids = new HashSet<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            ids.add((String) table.getValueAt(row, 0));
        }
        return ids;
    }
}
