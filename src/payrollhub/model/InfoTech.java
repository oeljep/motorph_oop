package payrollhub.model;

import java.time.LocalDate;

import payrollhub.contracts.SystemSupportHub;

public class InfoTech extends Employee implements SystemSupportHub {

    protected InfoTech(int empNo, String lastName, String firstName, String address, String phone, LocalDate birthday,
            String supervisor, String status, String position, String sss, String philhealth, String tin,
            String pagibig, double basicSalary, double riceSubsidy, double phoneAllowance, double clothingAllowance) {
        super(empNo, lastName, firstName, address, phone, birthday, supervisor, status, position, sss, philhealth, tin,
                pagibig,
                basicSalary, riceSubsidy, phoneAllowance, clothingAllowance);
    }

    @Override
    public boolean resetCredentials(int empID, String newPassword) {
        return false;
    }

    @Override
    public boolean resetCredentials(int empID, String newPassword, String adminToken) {
        return false;
    }

    @Override
    public boolean checkSystemHealth() {
        return true;
    }

    @Override
    public Position getRole() {
        return Position.IT;
    }

    // --------------- Inherited methods from Employee class ----------------------
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
    public boolean isPasswordValid(String password) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isPasswordValid'");
    }

    @Override
    public void resetPassword(String newPassword) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resetPassword'");
    }

    @Override
    public int getPasswordStrength(String password) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPasswordStrength'");
    }

    @Override
    public boolean authenticate(String username, String password) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'authenticate'");
    }

    @Override
    public boolean authenticate(String token) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'authenticate'");
    }

    @Override
    public void applyLeave(String leaveID, int empNo, LocalDate startDate, LocalDate endDate, String reason) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'applyLeave'");
    }

    @Override
    public double calculateTotalHoursWorked() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculateTotalHoursWorked'");
    }
}
