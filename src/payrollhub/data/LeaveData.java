package payrollhub.data;

import java.time.LocalDate;
import java.util.Locale;

public final class LeaveData {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    private final String leaveId;
    private final int empNo;
    private final String type;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String status;
    private final String reason;

    private LeaveData(String leaveId, int empNo, String type, LocalDate startDate,
            LocalDate endDate, String status, String reason) {
        if (leaveId == null || leaveId.isBlank() || empNo <= 0
                || type == null || type.isBlank() || startDate == null || endDate == null
                || endDate.isBefore(startDate) || status == null || status.isBlank()
                || !status.trim().equalsIgnoreCase(PENDING)
                        && !status.trim().equalsIgnoreCase(APPROVED)
                        && !status.trim().equalsIgnoreCase(REJECTED)) {
            throw new IllegalArgumentException("Leave request details are invalid.");
        }
        this.leaveId = leaveId.trim();
        this.empNo = empNo;
        this.type = type.trim();
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status.trim().toUpperCase(Locale.ROOT);
        this.reason = reason == null ? "" : reason.trim();
    }

    public static LeaveData newRequest(String leaveId, int empNo, String type,
            LocalDate startDate, LocalDate endDate, String reason) {
        return new LeaveData(leaveId, empNo, type, startDate, endDate, PENDING, reason);
    }

    public static LeaveData fromCsvRow(String[] row) {
        if (row == null || row.length != 7) {
            throw new IllegalArgumentException("A leave record must contain exactly 7 fields.");
        }
        return new LeaveData(row[0], Integer.parseInt(row[1]), row[2],
                LocalDate.parse(row[3]), LocalDate.parse(row[4]), row[5], row[6]);
    }

    public String[] toCsvRow() {
        return new String[] {
                leaveId, Integer.toString(empNo), type, startDate.toString(),
                endDate.toString(), status, reason
        };
    }

    public String getLeaveId() {
        return leaveId;
    }

    public int getEmpNo() {
        return empNo;
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

    public boolean isPending() {
        return PENDING.equals(status);
    }

    public boolean covers(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean overlaps(LocalDate from, LocalDate to) {
        return from != null && to != null
                && !endDate.isBefore(from) && !startDate.isAfter(to);
    }
}
