package payrollhub.model;

import java.util.List;
import payrollhub.contracts.HRHub;
import payrollhub.model.Employee;

public class HumanResources implements HRHub {

    @Override
    public void updateEmployeeDetails(int empNo, Employee data) {
        // TODO: implement employee details update
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

    @Override
    public List<LeaveRequest> viewAllLeaveRequests() {
        return null;
    }

    @Override
    public Position getRole() {
        return Position.HUMAN_RESOURCES;
    }
}
