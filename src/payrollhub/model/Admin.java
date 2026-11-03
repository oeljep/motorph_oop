package payrollhub.model;

import java.io.IOException;
import java.time.LocalDate;

import payrollhub.contracts.AdminHub;
import payrollhub.contracts.UserHub;
import payrollhub.data.CsvHandler;

public class Admin extends Employee implements AdminHub, UserHub {

    protected Admin(int empNo, String lastName, String firstName, String address, String phone, LocalDate birthday,
            String supervisor, String status, String position, String sss, String philhealth, String tin,
            String pagibig, double basicSalary, double riceSubsidy, double phoneAllowance, double clothingAllowance) {
        super(empNo, lastName, firstName, address, phone, birthday, supervisor, status, position, sss, philhealth, tin,
                pagibig,
                basicSalary, riceSubsidy, phoneAllowance, clothingAllowance);
    }

    // The authority to Approve or Reject a request.
    public void updateLeaveStatus(LeaveRequest leaveID, String newStatus) {
        if (leaveID == null) {
            throw new IllegalArgumentException("Leave request is required.");
        }
        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("New status is required.");
        }
        leaveID.setStatus(newStatus.trim());
    }

    // Adds a new employee object to the persistent storage (CSV).
    @Override
    public void addEmployee(Employee employee) throws IOException {
        if (employee == null) {
            throw new IllegalArgumentException("Employee data is required.");
        }
        CsvHandler.addEmployee(employee);
    }

    // Calculates the next available unique ID for a new hire.
    public int generateNextEmpNo() {
        return CsvHandler.getNextEmpNo();
    }

    public boolean isEmployeeValid(Employee employee) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isEmployeeValid'");
    }

    // Modifies existing data for a specific employee in the database.
    @Override
    public void updateEmployee(int id, Employee employee) throws IOException {
        if (employee == null) {
            throw new IllegalArgumentException("Employee data is required.");
        }
        if (id != employee.getEmpNo()) {
            throw new IllegalArgumentException("Employee number must match the employee data.");
        }
        if (!CsvHandler.updateEmployee(id, employee)) {
            throw new IllegalArgumentException("Employee number " + id + " does not exist.");
        }
    }

    // Deletes an employee's record from the system.
    @Override
    public void removeEmployee(int id) throws IOException {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee number must be positive.");
        }
        if (!CsvHandler.removeEmployee(id)) {
            throw new IllegalArgumentException("Employee number " + id + " does not exist.");
        }
    }

    public Position getRole() {
        return Position.ADMIN;
    }

    @Override
    public boolean authenticate(String user, String pass) {

        throw new UnsupportedOperationException("Unimplemented method 'authenticate'");
    }

    @Override
    public double calculatePay() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculatePay'");
    }

    @Override
    public double calculateTotalHoursWorked() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculateTotalHoursWorked'");
    }

    // ------------------------- Unimplemented methods -----------------------

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

    @Override
    public boolean isPasswordValid(String pass) {
        return pass != null && pass.length() >= 8;
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
}
