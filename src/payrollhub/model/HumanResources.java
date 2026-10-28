package payrollhub.model;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import payrollhub.contracts.HRHub;
import payrollhub.contracts.LeaveHub;
import payrollhub.model.Position;

public class HumanResources extends Employee implements HRHub, LeaveHub {

    // The 17 parameters as Employee attributes are inherited from the Employee
    // class.
    public HumanResources(int empNo, String lastName, String firstName,
            String address, String phone, LocalDate birthday,
            String supervisor, String status, String position,
            String sss, String philhealth, String tin, String pagibig,
            double basicSalary, double riceSubsidy,
            double phoneAllowance, double clothingAllowance) {
        super(empNo, lastName, firstName, address, phone, birthday,
                supervisor, status, position, sss, philhealth, tin, pagibig,
                basicSalary, riceSubsidy, phoneAllowance, clothingAllowance);
    }

    // --------------------- Pay ----------------------

    @Override
    public double calculatePay() {
        // (gethourlyRate * totalhoursWorked) + allowances
        return (getHourlyRate() * calculateTotalHoursWorked()) + getRiceSubsidy()
                + getPhoneAllowance() + getClothingAllowance();

    }

    // --------------------- HRHub ----------------------

    @Override
    public void updateEmployeeDetails(int empNo, Employee data) {
        // *Permission: Requires EMPLOYEE_UPDATE. empID must exist in CSV. */

    }

    @Override
    public Employee viewEmployeeProfile(int empNo) {
        return null;
    }

    @Override
    public void approveLeaveRequest(String leaveID) {
        // TODO: implement leave approval
    }

    @Override
    public void rejectLeaveRequest(String leaveID, String reason) {
        // TODO: implement leave rejection
    }

    // --------------------- LeaveHub ----------------------

    @Override
    public List<LeaveRequest> viewAllLeaveRequests() {
        return null;
    }

    @Override
    public void updateLeaveStatus(String leaveID, String newStatus) {
        // TODO: implement leave status update
    }
    // ------------------------- Role ------------------------------

    @Override
    public Position getRole() {
        return Position.HR;
    }

    // ------------------------ Unimplemented methods -----------------------

    @Override
    public LeaveRequest applyLeave(String leaveID, int empNo, LocalDate startDate, LocalDate endDate)
            throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'applyLeave'");
    }

    @Override
    public LeaveRequest applyLeave(String leaveID, int empNo, LocalDate startDate, LocalDate endDate, String type,
            String reason) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'applyLeave'");
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
