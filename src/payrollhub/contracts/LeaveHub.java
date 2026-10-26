package payrollhub.contracts;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import payrollhub.model.LeaveRequest;

public interface LeaveHub {

    LeaveRequest applyLeave(String leaveID, int empNo,
                            LocalDate startDate, LocalDate endDate) throws IOException;

    LeaveRequest applyLeave(String leaveID, int empNo,
                            LocalDate startDate, LocalDate endDate, String reason) throws IOException;

    LeaveRequest applyLeave(String leaveID, int empNo, LocalDate startDate, LocalDate endDate,
                            String type, String reason) throws IOException;

    void updateLeaveStatus(String leaveID, String newStatus) throws IOException;

    List<LeaveRequest> viewAllLeaveRequests() throws IOException;
}
