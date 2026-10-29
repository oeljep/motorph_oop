package payrollhub.model;

import java.time.LocalDate;

import payrollhub.contracts.AdminHub;
import payrollhub.contracts.UserHub;

public class Admin extends Employee implements AdminHub, UserHub {

    protected Admin(int empNo, String lastName, String firstName, String address, String phone, LocalDate birthday,
            String supervisor, String status, String position, String sss, String philhealth, String tin,
            String pagibig, double basicSalary, double riceSubsidy, double phoneAllowance, double clothingAllowance) {
        super(empNo, lastName, firstName, address, phone, birthday, supervisor, status, position, sss, philhealth, tin,
                pagibig,
                basicSalary, riceSubsidy, phoneAllowance, clothingAllowance);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void updateEmployee(int id, Employee data) {
        // TODO: implement employee update
    }

    @Override
    public void removeEmployee(int id) {
        // TODO: implement employee removal
    }

    @Override
    public boolean isPasswordValid(String pass) {
        return pass != null && pass.length() >= 8;
    }

    @Override
    public int getPasswordStrength(String pass) {
        if (pass == null || pass.isEmpty()) {
            return 0;
        }
        int strength = 0;
        if (pass.length() >= 8) {
            strength++;
        }
        if (pass.matches(".*[A-Z].*")) {
            strength++;
        }
        if (pass.matches(".*[a-z].*")) {
            strength++;
        }
        if (pass.matches(".*[0-9].*")) {
            strength++;
        }
        if (pass.matches(".*[!@#$%^&*].*")) {
            strength++;
        }
        return Math.min(strength, 5);
    }

    // ------------------------- Unimplemented methods -----------------------

    @Override
    public Position getRole() {
        return Position.ADMIN;
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
    public void addEmployee(Employee emp) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addEmployee'");
    }

    @Override
    public int generateNextEmpNo() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateNextEmpNo'");
    }

    @Override
    public boolean isEmployeeValid(Employee emp) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isEmployeeValid'");
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
