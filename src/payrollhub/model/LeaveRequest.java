package payrollhub.model;

import java.time.LocalDate;

public class LeaveRequest {

    private final String leaveID;
    private final int employeeID;
    private final String type;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String status;
    private final String reason;

    public LeaveRequest(String leaveID, int employeeID, String type,
            LocalDate startDate, LocalDate endDate, String status, String reason) {
        this.leaveID = leaveID;
        this.employeeID = employeeID;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.reason = reason;
    }

    public String getLeaveID() {
        return leaveID;
    }

    public int getEmployeeID() {
        return employeeID;
    }

    public String getType() {
        return type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
