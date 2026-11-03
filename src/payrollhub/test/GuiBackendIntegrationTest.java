package payrollhub.test;

import java.time.LocalTime;
import java.util.List;

import payrollhub.data.AttendanceData;
import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.data.LeaveData;
import payrollhub.data.TicketData;
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
}
