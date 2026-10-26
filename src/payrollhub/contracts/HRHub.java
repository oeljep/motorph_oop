package payrollhub.contracts;

import java.util.List;
import payrollhub.model.Employee;
import payrollhub.model.LeaveRequest;
import payrollhub.model.Position;

public interface HRHub {

    Position getRole();

    void updateEmployeeDetails(int empNo, Employee data);

    Employee viewEmployeeProfile(int empNo);

    void approveLeaveRequest(String leaveID);

    void rejectLeaveRequest(String leaveID, String reason);

    List<LeaveRequest> viewAllLeaveRequests();
}
