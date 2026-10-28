package payrollhub.model;

public enum Position {

    // Each constant calls the constructor below: (label, approver)
    ADMIN("Admin", true),
    HR("Human Resources", true),
    ACCOUNTING("Accounting", false),
    IT("IT", false),
    REGULAR("Regular Staff", false);

    private final String label;
    private final boolean approver;

    // Enum constructors are always private, even without the keyword.
    Position(String label, boolean approver) {
        this.label = label;
        this.approver = approver;
    }

    /** Display name for the UI. */
    public String getLabel() {
        return label;
    }

    /** True for ADMIN and HR only (may approve leave requests). */
    public boolean isApprover() {
        return approver;
    }

    /** Overload 1: strict. Throws if nothing matches. */
    public static Position fromString(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Role text is required.");
        }
        String wanted = text.trim();
        for (Position p : values()) {
            if (p.name().equalsIgnoreCase(wanted) || p.label.equalsIgnoreCase(wanted)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + text);
    }

    /** Overload 2: lenient. Returns the fallback instead of throwing. */
    public static Position fromString(String text, Position fallback) {
        try {
            return fromString(text);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}