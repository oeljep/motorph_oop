package payrollhub.model;

import java.time.LocalDate;

public abstract class LeaveRequest {

    private final String leaveID;
    private final int employeeID;
    private final String type;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private String status;
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

    public String setStatus(String newStatus) {
        if (newStatus == null || newStatus.isEmpty()) {
            throw new IllegalArgumentException("Status is required.");
        }
        this.status = newStatus;
        return this.status;
    }

    public void approve() {
        setStatus("APPROVED");
    }

    public void reject() {
        setStatus("REJECTED");
    }

    // Ensures that the end date is not earlier than the start date.
    private void validateDates() {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be earlier than start date.");
        }
    }

}
