package payrollhub.model;

import java.io.File;
import java.time.LocalDate;

import payrollhub.contracts.FinanceHub;
import payrollhub.contracts.UserHub;

public class Accounting extends Employee implements FinanceHub, UserHub {

    protected Accounting(int empNo, String lastName, String firstName, String address, String phone, LocalDate birthday,
            String supervisor, String status, String position, String sss, String philhealth, String tin,
            String pagibig, double basicSalary, double riceSubsidy, double phoneAllowance, double clothingAllowance) {
        super(empNo, lastName, firstName, address, phone, birthday, supervisor, status, position, sss, philhealth, tin,
                pagibig,
                basicSalary, riceSubsidy, phoneAllowance, clothingAllowance);
    }

    // Triggers the high-level payroll logic for the current cycle.
    @Override
    public void batchProcessPayroll(String payPeriod) {
        if (payPeriod == null || payPeriod.isBlank()) {
            throw new IllegalArgumentException("Pay period must not be blank.");
        }

        generatePayslip(getEmpNo(), payPeriod);
    }

    // Produces a detailed breakdown of earnings/deductions.
    @Override
    public Payslip createPayslip(int empNo) {
        return generatePayslip(empNo);
    }

    // Produces a breakdown for a specific period.
    public Payslip createPayslip(int empNo, String period) {
        return generatePayslip(empNo, period);
    }

    // Creates a summary of withholding taxes for BIR compliance.
    public File generateTaxReport(String quarter) {
        return generateTaxReport(quarter);
    }

    // Reports total SSS, PhilHealth, and Pag-IBIG contributions.
    public SummaryData generateDeductionSummary(String month) {
        return generateDeductionSummary(month);
    }

    // Accesses the money variables inherited from the parent.
    protected double viewSalaryData() {
        return getBasicSalary() + getRiceSubsidy() + getPhoneAllowance() + getClothingAllowance();
    }

    // Aggregates SSS, PhilHealth, and Pag-IBIG for calculations.
    private double computeStatutoryDeductions(double gross) {
        return getSssDeduction(gross) + getPhilHealthDeduction(gross) + getPagibigDeduction(gross);
    }

    // Computes total pay including all allowances.
    public double calculateGrossSalary() {
        return getBasicSalary() + getRiceSubsidy() + getPhoneAllowance() + getClothingAllowance();
    }

    // Computes total pay including all allowances
    public double CalculatePay() {
        return (getHourlyRate() * calculateTotalHoursWorked()) + getRiceSubsidy()
                + getPhoneAllowance() + getClothingAllowance();
    }

    // Updates an employee’s basic salary. Allowances are updated through their own
    // setters.
    @Override
    public void updateSalaryRates(int empNo, double newBasicSalary) {
        if (empNo != getEmpNo()) {
            throw new IllegalArgumentException("Employee number does not match this accounting employee.");
        }
        if (!Double.isFinite(newBasicSalary) || newBasicSalary < 0) {
            throw new IllegalArgumentException("Basic salary must be a finite, non-negative amount.");
        }

        setBasicSalary(newBasicSalary);
    }

    // Mandates role identification for UI rendering.
    public Position getRole() {
        return Position.ACCOUNTING;
    }

    // Validates credentials for accounting access. @Override: Implements UserHub
    // contract. Returns true on success.
    @Override
    public boolean authenticate(String user, String pass) {
        return false;
    }

    // ------------------------ Unimplemented methods -----------------------

    @Override
    public boolean authenticate(String token) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'authenticate'");
    }

    @Override
    public boolean isPasswordValid(String pass) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isPasswordValid'");
    }

    @Override
    public int getPasswordStrength(String pass) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPasswordStrength'");
    }

    @Override
    public void resetPassword(String newPass) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resetPassword'");
    }

    @Override
    public void logout() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'logout'");
    }

    @Override
    public Payslip generatePayslip(int empNo) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generatePayslip'");
    }

    @Override
    public Payslip generatePayslip(int empNo, String period) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generatePayslip'");
    }

    @Override
    public double calculatePay() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculatePay'");
    }

    @Override
    public double getSssDeduction(double gross) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSssDeduction'");
    }

    @Override
    public double getPagibigDeduction(double gross) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPagibigDeduction'");
    }

    @Override
    public double getWithholdingTax(double gross) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWithholdingTax'");
    }

    @Override
    public double getPhilHealthDeduction(double gross) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPhilHealthDeduction'");
    }

    @Override
    public void logout(String token) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'logout'");
    }

    @Override
    public LeaveRequest applyLeave(String leaveID, int empNo, LocalDate startDate, LocalDate endDate, String reason) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'applyLeave'");
    }

    @Override
    public double calculateTotalHoursWorked() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculateTotalHoursWorked'");
    }

}
