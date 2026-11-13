package payrollhub.test;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import payrollhub.data.AttendanceData;
import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.data.LeaveData;
import payrollhub.data.TicketData;
import payrollhub.gui.GUIDashboardHome;
import payrollhub.gui.GUIDisputeManager;
import payrollhub.gui.GUIEmployeeManager;
import payrollhub.gui.GUILeaveManager;
import payrollhub.gui.GUIPayslip;
import payrollhub.gui.GUITicketManager;
import payrollhub.service.AttendanceRules;
import payrollhub.service.PayslipCalculator;
import payrollhub.service.PayslipData;
import payrollhub.service.PayrollCalculator;
import payrollhub.service.PayrollCutoff;

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
        java.time.YearMonth payMonth = java.time.YearMonth.parse(months.get(0));
        PayslipData firstCutoff = PayslipCalculator.compute(employee, attendance, payMonth,
                PayrollCutoff.FIRST_HALF);
        PayslipData secondCutoff = PayslipCalculator.compute(employee, attendance, payMonth,
                PayrollCutoff.SECOND_HALF);
        check(firstCutoff.daysWorked() + secondCutoff.daysWorked() == slip.daysWorked(),
                "cutoff days add up to the month's worked days");
        check(closeTo(firstCutoff.grossIncome() - firstCutoff.overtimePay(),
                        employee.getBasicSalary() * firstCutoff.daysWorked() / 21.0)
                        && closeTo(secondCutoff.grossIncome() - secondCutoff.overtimePay(),
                                employee.getBasicSalary() * secondCutoff.daysWorked() / 21.0),
                "basic pay recalculates from attended days while overtime is shown separately");
        check(closeTo(firstCutoff.benefitsTotal(),
                        (employee.getRiceSubsidy() + employee.getPhoneAllowance()
                                + employee.getClothingAllowance()) * firstCutoff.daysWorked() / 21.0)
                        && closeTo(secondCutoff.benefitsTotal(),
                                (employee.getRiceSubsidy() + employee.getPhoneAllowance()
                                        + employee.getClothingAllowance())
                                        * secondCutoff.daysWorked() / 21.0),
                "benefits recalculate from attended days in each cutoff");
        check(firstCutoff.cutoffStart().equals(payMonth.atDay(1))
                        && firstCutoff.cutoffEnd().equals(payMonth.atDay(15))
                        && secondCutoff.cutoffStart().equals(payMonth.atDay(16))
                        && secondCutoff.cutoffEnd().equals(payMonth.atEndOfMonth()),
                "cutoff selections map to the first and second half of the month");
        PayrollCalculator payrollCalculator = new PayrollCalculator();
        double firstCutoffGross = firstCutoff.grossIncome() + firstCutoff.benefitsTotal();
        double firstCutoffTaxableIncome = Math.max(0, firstCutoffGross
                - payrollCalculator.finalSSS(firstCutoffGross)
                - payrollCalculator.finalPhilHealth(firstCutoffGross)
                - payrollCalculator.finalPagIbig(firstCutoffGross));
        double expectedCutoffDeductions = payrollCalculator.finalSSS(firstCutoff.grossIncome()
                        + firstCutoff.benefitsTotal())
                + payrollCalculator.finalPhilHealth(firstCutoff.grossIncome() + firstCutoff.benefitsTotal())
                + payrollCalculator.finalPagIbig(firstCutoff.grossIncome() + firstCutoff.benefitsTotal())
                + payrollCalculator.finalWithholdingTax(firstCutoffTaxableIncome);
        check(closeTo(firstCutoff.totalDeductions(), expectedCutoffDeductions),
                "statutory deductions recalculate from the selected cutoff gross");
        PayslipData noAttendance = PayslipCalculator.compute(employee, List.of(), payMonth,
                PayrollCutoff.FIRST_HALF);
        check(noAttendance.grossIncome() == 0 && noAttendance.benefitsTotal() == 0
                        && noAttendance.overtimeHours() == 0 && noAttendance.overtimePay() == 0
                        && noAttendance.totalDeductions() == 0 && noAttendance.takeHomePay() == 0,
                "a cutoff with no completed attendance produces no pay or deductions");

        EmployeeData overtimeEmployee = new EmployeeData(20001, "Test", "Overtime",
                LocalDate.of(1990, 1, 1), "Test address", "1234567890", "1234567890",
                "1234567890", "1234567890", "1234567890", "Regular", "Test", "",
                16800, 0, 0, 0);
        AttendanceData overtimeRecord = new AttendanceData(20001, payMonth.atDay(5),
                LocalTime.of(8, 0), LocalTime.of(18, 0));
        PayslipData overtimeSlip = PayslipCalculator.compute(overtimeEmployee, List.of(overtimeRecord),
                payMonth, PayrollCutoff.FIRST_HALF);
        check(closeTo(overtimeSlip.hoursWorked(), 9) && closeTo(overtimeSlip.overtimeHours(), 1),
                "completed attendance automatically counts hours beyond the 8-hour day as overtime");
        check(closeTo(overtimeSlip.overtimePay(), 125)
                        && closeTo(overtimeSlip.grossIncome(), 925)
                        && closeTo(overtimeSlip.takeHomePay(),
                                overtimeSlip.grossIncome() - overtimeSlip.totalDeductions()),
                "overtime is paid at 1.25 times the hourly rate and included in taxable gross and take-home pay");

        check(!CsvHandler.loadLeaves().isEmpty(), "leave requests load from leaves.csv");
        check(AttendanceRules.hoursWorked(LocalTime.of(8, 0), LocalTime.of(17, 0)) == 8,
                "attendance calculation applies the unpaid break");
        check(AttendanceRules.overtimeHours(LocalTime.of(8, 0), LocalTime.of(17, 0)) == 0
                        && AttendanceRules.overtimeHours(LocalTime.of(8, 0), LocalTime.of(18, 0)) == 1,
                "overtime calculation uses paid hours beyond the regular shift");
        check(AttendanceRules.lateMinutes(LocalTime.of(8, 11)) == 1,
                "attendance calculation applies the grace period");

        AtomicReference<GUIDashboardHome> managementHome = new AtomicReference<>();
        AtomicReference<GUIDashboardHome> adminHome = new AtomicReference<>();
        AtomicReference<GUIDashboardHome> hrHome = new AtomicReference<>();
        AtomicReference<GUIDashboardHome> personalHome = new AtomicReference<>();
        AtomicReference<GUIEmployeeManager> employeeManager = new AtomicReference<>();
        AtomicReference<GUIEmployeeManager> hrEmployeeManager = new AtomicReference<>();
        AtomicReference<GUILeaveManager> adminLeaveManager = new AtomicReference<>();
        AtomicReference<GUILeaveManager> hrLeaveManager = new AtomicReference<>();
        AtomicReference<javax.swing.JPanel> overtimePayslipPaper = new AtomicReference<>();
        AtomicReference<GUIDisputeManager> disputeManager = new AtomicReference<>();
        AtomicReference<GUITicketManager> ticketManager = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            managementHome.set(new GUIDashboardHome("Admin", empNo));
            adminHome.set(new GUIDashboardHome("Admin", 10003));
            hrHome.set(new GUIDashboardHome("HumanResources", 10006));
            personalHome.set(new GUIDashboardHome("Regular", empNo));
            employeeManager.set(new GUIEmployeeManager("Admin", 10003));
            hrEmployeeManager.set(new GUIEmployeeManager("HumanResources", 10006));
            adminLeaveManager.set(new GUILeaveManager("Admin", 10002));
            hrLeaveManager.set(new GUILeaveManager("HumanResources", 10002));
            overtimePayslipPaper.set(GUIPayslip.createPaper(overtimeSlip));
            disputeManager.set(new GUIDisputeManager(empNo));
            ticketManager.set(new GUITicketManager(empNo));
        });
        check(containsLabel(managementHome.get(), "Payroll Overview")
                        && containsLabel(managementHome.get(), "Pending leave requests"),
                "management dashboard displays company overview cards");
        JTable employeeTable = findTable(managementHome.get());
        Set<Integer> directReportEmpNos = new HashSet<>();
        for (EmployeeData report : CsvHandler.findDirectReports(empNo)) {
            directReportEmpNos.add(Integer.parseInt(report.getEmpNo()));
        }
        check(employeeTable != null && employeeIds(employeeTable).equals(directReportEmpNos),
                "Admin dashboard employee list displays only direct reports");
        Set<Integer> adminReports = Set.of(10010);
        JTable adminDashboardTable = findTable(adminHome.get());
        JTable adminEmployeeManagerTable = findTable(employeeManager.get());
        check(adminDashboardTable != null && employeeIds(adminDashboardTable).equals(adminReports),
                "Admin dashboard displays only employees reporting to that Admin");
        check(adminEmployeeManagerTable != null && employeeIds(adminEmployeeManagerTable).equals(adminReports),
                "Admin Employee Manager displays only employees reporting to that Admin");
        Set<Integer> hrReports = Set.of(10007);
        JTable hrDashboardTable = findTable(hrHome.get());
        JTable hrEmployeeManagerTable = findTable(hrEmployeeManager.get());
        check(hrDashboardTable != null && employeeIds(hrDashboardTable).equals(hrReports),
                "HumanResources dashboard displays only employees reporting to that user");
        check(hrEmployeeManagerTable != null && employeeIds(hrEmployeeManagerTable).equals(hrReports),
                "HumanResources Employee Manager displays only employees reporting to that user");
        check(containsLabel(personalHome.get(), "Basic salary")
                        && containsLabel(personalHome.get(), "Attendance")
                        && containsLabel(personalHome.get(), "Pending leave requests"),
                "staff dashboard displays personal payroll, attendance and leave summaries");
        Set<String> directReportLeaveIds = Set.of("L0005", "L0006", "L0015");
        check(leaveIds(adminLeaveManager.get()).equals(directReportLeaveIds),
                "Admin leave manager displays only direct reports' requests");
        check(leaveIds(hrLeaveManager.get()).equals(directReportLeaveIds),
                "HR leave manager displays only direct reports' requests");
        check(containsButton(adminLeaveManager.get(), "Review"),
                "Admin leave manager provides a review action");
        check(!containsButton(hrLeaveManager.get(), "Review"),
                "HumanResources does not receive leave decision controls");
        check(containsLabel(overtimePayslipPaper.get(), "Overtime Hours")
                        && containsLabel(overtimePayslipPaper.get(), "Overtime Pay (1.25x)"),
                "payslip displays calculated overtime hours and pay");
        JTable disputeTable = findTable(disputeManager.get());
        JTable ticketTable = findTable(ticketManager.get());
        check(disputeTable != null && "Dispute No.".equals(disputeTable.getColumnName(0)),
                "Dispute Manager labels shared cases with dispute numbers");
        check(ticketTable != null && "Ticket No.".equals(ticketTable.getColumnName(0)),
                "Ticket Manager labels shared cases with ticket numbers");

        TicketData ticket = TicketData.newTicket("T0001", empNo, "Test subject", "Test message");
        check("D0001".equals(ticket.getDisputeId()) && "T0001".equals(ticket.getTicketId()),
                "a shared case has D0001 and T0001 display identifiers");
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

    private static boolean closeTo(double actual, double expected) {
        return Math.abs(actual - expected) < 0.01;
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

    private static boolean containsButton(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton && text.equals(((JButton) component).getText())) {
                return true;
            }
            if (component instanceof Container && containsButton((Container) component, text)) {
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

    private static Set<Integer> employeeIds(JTable table) {
        Set<Integer> ids = new HashSet<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            ids.add(((Number) table.getValueAt(row, 0)).intValue());
        }
        return ids;
    }
}
