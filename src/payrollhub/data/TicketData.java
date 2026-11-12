package payrollhub.data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class TicketData {

    public static final String OPEN = "OPEN";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String RESOLVED = "RESOLVED";
    public static final String REJECTED = "REJECTED";

    private static final DateTimeFormatter CREATED_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String ticketId;
    private final int empNo;
    private final String subject;
    private final String message;
    private final String createdDate;
    private String processedBy;
    private String status;
    private String remarks;

    private TicketData(String ticketId, int empNo, String subject, String message,
            String createdDate, String processedBy, String status, String remarks) {
        if (ticketId == null || ticketId.isBlank() || empNo <= 0
                || subject == null || subject.isBlank()
                || message == null || message.isBlank()
                || createdDate == null || createdDate.isBlank()) {
            throw new IllegalArgumentException("Ticket details are invalid.");
        }
        this.ticketId = ticketId.trim();
        this.empNo = empNo;
        this.subject = singleLine(subject);
        this.message = singleLine(message);
        this.createdDate = createdDate.trim();
        this.processedBy = processedBy == null ? "" : processedBy.trim();
        this.remarks = remarks == null ? "" : remarks;
        setStatus(status);
    }

    public static TicketData newTicket(String ticketId, int empNo, String subject, String message) {
        return new TicketData(ticketId, empNo, subject, message,
                LocalDateTime.now().format(CREATED_FORMAT), "", OPEN, "");
    }

    public static TicketData fromCsvRow(String[] row) {
        if (row == null || row.length != 8) {
            throw new IllegalArgumentException("A ticket record must contain exactly 8 fields.");
        }
        return new TicketData(row[0], Integer.parseInt(row[1]), row[2],
                row[3], row[4], row[5], row[6], row[7]);
    }

    public String[] toCsvRow() {
        return new String[] {
                ticketId, Integer.toString(empNo), subject, message,
                createdDate, processedBy, status, remarks
        };
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getDisputeId() {
        return ticketId.startsWith("T") ? "D" + ticketId.substring(1) : ticketId;
    }

    public int getEmpNo() {
        return empNo;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public String getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    public boolean isActive() {
        return OPEN.equals(status) || IN_PROGRESS.equals(status);
    }

    public void addRemark(String remark) {
        if (remark == null || remark.isBlank()) {
            throw new IllegalArgumentException("A reply is required.");
        }
        String reply = singleLine(remark);
        remarks = remarks.isEmpty() ? reply : remarks + "\\n" + reply;
    }

    public void setProcessedBy(String processedBy) {
        if (processedBy == null || processedBy.isBlank()) {
            throw new IllegalArgumentException("The staff member processing the ticket is required.");
        }
        this.processedBy = processedBy.trim();
    }

    public void setStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of(OPEN, IN_PROGRESS, RESOLVED, REJECTED).contains(normalized)) {
            throw new IllegalArgumentException("Ticket status is invalid.");
        }
        this.status = normalized;
    }

    private static String singleLine(String value) {
        return value.trim().replace('\r', ' ').replace('\n', ' ');
    }
}
