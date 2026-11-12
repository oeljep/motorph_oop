package payrollhub.service;

import java.time.YearMonth;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import payrollhub.data.AttendanceData;
import payrollhub.data.EmployeeData;

public final class PayslipCalculator {

    private static final double MONTHLY_WORK_HOURS = 168.0;
    private static final double OVERTIME_RATE = 1.25;

    private PayslipCalculator() {
    }

    public static PayslipData compute(EmployeeData employee, List<AttendanceData> attendance, String period) {
        YearMonth month = parseMonth(period);
        return compute(employee, attendance, month, month.atDay(1), month.atEndOfMonth());
    }

    public static PayslipData compute(EmployeeData employee, List<AttendanceData> attendance,
            YearMonth month, PayrollCutoff cutoff) {
        if (month == null || cutoff == null) {
            throw new IllegalArgumentException("Pay month and cutoff are required.");
        }
        return compute(employee, attendance, month, cutoff.startDate(month), cutoff.endDate(month));
    }

    public static PayslipData compute(EmployeeData employee, List<AttendanceData> attendance,
            YearMonth month, LocalDate cutoffStart, LocalDate cutoffEnd) {
        if (employee == null || attendance == null || month == null
                || cutoffStart == null || cutoffEnd == null) {
            throw new IllegalArgumentException("Employee, attendance, month and cutoff dates are required.");
        }
        if (cutoffEnd.isBefore(cutoffStart)
                || !YearMonth.from(cutoffStart).equals(month)
                || !YearMonth.from(cutoffEnd).equals(month)) {
            throw new IllegalArgumentException("Cutoff dates must be an ordered range within the pay month.");
        }

        return computeForRange(employee, attendance, month, cutoffStart, cutoffEnd);
    }

    private static YearMonth parseMonth(String period) {
        if (period == null || period.isBlank()) {
            throw new IllegalArgumentException("Pay period is required.");
        }
        YearMonth month;
        try {
            month = YearMonth.parse(period);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Pay period must use yyyy-MM format.", e);
        }
        return month;
    }

    private static PayslipData computeForRange(EmployeeData employee, List<AttendanceData> attendance,
            YearMonth month, LocalDate cutoffStart, LocalDate cutoffEnd) {
        int daysWorked = 0;
        int lateMinutes = 0;
        double hoursWorked = 0;
        double overtimeHours = 0;
        for (AttendanceData record : attendance) {
            if (record == null || record.getEmpNo() != employee.getEmpNo()) {
                throw new IllegalArgumentException("Attendance records must belong to the payslip employee.");
            }
            if (!YearMonth.from(record.getDate()).equals(month)) {
                throw new IllegalArgumentException("Attendance records must match the pay period.");
            }
            if (record.isOpen() || record.getDate().isBefore(cutoffStart)
                    || record.getDate().isAfter(cutoffEnd)) {
                continue;
            }
            daysWorked++;
            hoursWorked += AttendanceRules.hoursWorked(record.getLogIn(), record.getLogOut());
            overtimeHours += AttendanceRules.overtimeHours(record.getLogIn(), record.getLogOut());
            lateMinutes += AttendanceRules.lateMinutes(record.getLogIn());
        }

        double proration = daysWorked / 21.0;
        double rice = employee.getRiceSubsidy() * proration;
        double phone = employee.getPhoneAllowance() * proration;
        double clothing = employee.getClothingAllowance() * proration;
        double benefits = rice + phone + clothing;
        double basic = employee.getBasicSalary();
        double cutoffBasic = basic * proration;
        double overtimePay = overtimeHours * (basic / MONTHLY_WORK_HOURS) * OVERTIME_RATE;
        double grossIncome = cutoffBasic + overtimePay;
        double assessableGross = grossIncome + benefits;

        PayrollCalculator payroll = new PayrollCalculator();
        double sss = assessableGross == 0 ? 0 : payroll.finalSSS(assessableGross);
        double philHealth = assessableGross == 0 ? 0 : payroll.finalPhilHealth(assessableGross);
        double pagIbig = assessableGross == 0 ? 0 : payroll.finalPagIbig(assessableGross);
        double taxableIncome = Math.max(0, assessableGross - sss - philHealth - pagIbig);
        double tax = assessableGross == 0 ? 0 : payroll.finalWithholdingTax(taxableIncome);
        double deductions = sss + philHealth + pagIbig + tax;

        return new PayslipData(employee.getEmpNo(), employee.getFullName(), employee.getPosition(),
                month.toString(), daysWorked, hoursWorked, lateMinutes, basic,
                basic / MONTHLY_WORK_HOURS, grossIncome, overtimeHours, overtimePay, rice, phone, clothing, benefits,
                sss, philHealth, pagIbig, tax, deductions, assessableGross - deductions,
                cutoffStart, cutoffEnd);
    }
}
