package payrollhub.model;

public class ITTicket {

    private int ticketID;
    private int empNo;
    private String issueType;
    private String description;
    private String status;
    private String dateCreated;
    private String dateResolved;

    public ITTicket(int ticketID, int empNo, String issueType, String description, String status, String dateCreated,
            String dateResolved) {
        this.ticketID = ticketID;
        this.empNo = empNo;
        this.issueType = issueType;
        this.description = description;
        this.status = status;
        this.dateCreated = dateCreated;
        this.dateResolved = dateResolved;
    }

    // Getters
    public int getTicketID() {
        return ticketID;
    }

    public int getEmpNo() {
        return empNo;
    }

    public String getIssueType() {
        return issueType;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public String getDateResolved() {
        return dateResolved;
    }

    // Setters
    public void setTicketID(int ticketID) {
        this.ticketID = ticketID;
    }

    public void setEmpNo(int empNo) {
        this.empNo = empNo;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setDateResolved(String dateResolved) {
        this.dateResolved = dateResolved;
    }

    // **-resolverName must not be blank
    // -allowed only while status is OPEN
    // -timestamp captured with LocalDateTime.now()
    public void resolve(String resolverName) {
        if (resolverName == null || resolverName.isEmpty()) {
            throw new IllegalArgumentException("Resolver name must not be blank.");
        }
        if (!"OPEN".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Ticket can only be resolved while status is OPEN.");
        }
        this.status = "RESOLVED";
        this.dateResolved = java.time.LocalDateTime.now().toString();
    }

    // Values containing commas must be quoted.
    public String toCSV(

    );
}
