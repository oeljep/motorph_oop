package payrollhub.service;

import java.time.LocalDate;

public record PayslipData(
        int empNo,
        String employeeName,
        String position,
        String period,
        int daysWorked,
        double hoursWorked,
        int lateMinutes,
        double monthlyRate,
        double hourlyRate,
        double grossIncome,
        double overtimeHours,
        double overtimePay,
        double riceSubsidy,
        double phoneAllowance,
        double clothingAllowance,
        double benefitsTotal,
        double sss,
        double philHealth,
        double pagIbig,
        double withholdingTax,
        double totalDeductions,
        double takeHomePay,
        LocalDate cutoffStart,
        LocalDate cutoffEnd) {
}
