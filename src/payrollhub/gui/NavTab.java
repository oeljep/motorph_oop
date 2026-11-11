package payrollhub.gui;

/**
 * Every section that can appear in the sidebar. The labels match the Figma
 * menu. Which roles see which tabs is decided in {@link NavAccess}, not here.
 */
public enum NavTab {
    DASHBOARD("Dashboard"),
    TIME_TRACKER("Time Tracker"),
    SCHEDULE("Schedule"),
    PAYSLIP("Payslip"),
    EMPLOYEE_MANAGER("Employee Manager"),
    PAYROLL_MANAGER("Payroll Manager"),
    DISPUTE_MANAGER("Dispute Manager"),
    LEAVE_MANAGER("Leave Manager"),
    TICKET_MANAGER("Ticket Manager");

    private final String label;

    NavTab(String label) {
        this.label = label;
    }

    /** @return the text shown on the sidebar button */
    public String getLabel() {
        return label;
    }
}
