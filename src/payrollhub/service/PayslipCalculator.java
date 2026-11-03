package payrollhub.service;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import payrollhub.data.AttendanceData;
import payrollhub.data.EmployeeData;

public final class PayslipCalculator {

    private static final double MONTHLY_WORK_HOURS = 168.0;

    private PayslipCalculator() {
    }

    public static PayslipData compute(EmployeeData employee, List<AttendanceData> attendance, String period) {
        if (employee == null || attendance == null || period == null || period.isBlank()) {
            throw new IllegalArgumentException("Employee, attendance and pay period are required.");
        }
        YearMonth month;
        try {
            month = YearMonth.parse(period);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Pay period must use yyyy-MM format.", e);
        }

        int daysWorked = 0;
        int lateMinutes = 0;
        double hoursWorked = 0;
        for (AttendanceData record : attendance) {
            if (record == null || record.getEmpNo() != employee.getEmpNo()) {
                throw new IllegalArgumentException("Attendance records must belong to the payslip employee.");
            }
            if (!YearMonth.from(record.getDate()).equals(month)) {
                throw new IllegalArgumentException("Attendance records must match the pay period.");
            }
            if (record.isOpen()) {
                continue;
            }
            daysWorked++;
            hoursWorked += AttendanceRules.hoursWorked(record.getLogIn(), record.getLogOut());
            lateMinutes += AttendanceRules.lateMinutes(record.getLogIn());
        }

        double rice = employee.getRiceSubsidy();
        double phone = employee.getPhoneAllowance();
        double clothing = employee.getClothingAllowance();
        double benefits = rice + phone + clothing;
        double basic = employee.getBasicSalary();
        double assessableGross = basic + benefits;

        PayrollCalculator payroll = new PayrollCalculator();
        double sss = payroll.finalSSS(assessableGross);
        double philHealth = payroll.finalPhilHealth(assessableGross);
        double pagIbig = payroll.finalPagIbig(assessableGross);
        double tax = payroll.finalWithholdingTax(assessableGross);
        double deductions = sss + philHealth + pagIbig + tax;

        return new PayslipData(employee.getEmpNo(), employee.getFullName(), employee.getPosition(),
                month.toString(), daysWorked, hoursWorked, lateMinutes, basic,
                basic / MONTHLY_WORK_HOURS, basic, rice, phone, clothing, benefits,
                sss, philHealth, pagIbig, tax, deductions, assessableGross - deductions);
    }
}
