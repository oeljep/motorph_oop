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
        // TODO Auto-generated constructor stub
    }

    @Override
    public void batchProcessPayroll(String payPeriod) {
        // TODO: implement batch payroll processing
    }

    @Override
    public void updateSalaryRates(int empNo, double newBasicSalary) {
        // TODO: implement salary rate update
    }

    // ------------------------ Unimplemented methods -----------------------

    @Override
    public Position getRole() {
        return Position.ACCOUNTING;
    }

    @Override
    public boolean authenticate(String user, String pass) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'authenticate'");
    }

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
    public File generateTaxReport(String quarter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateTaxReport'");
    }

    @Override
    public SummaryData generateDeductionSummary(String month) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateDeductionSummary'");
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
