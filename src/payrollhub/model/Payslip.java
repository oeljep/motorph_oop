package payrollhub.model;

/**
 * A printable payslip for one employee and one cut-off period.
 * Immutable: all fields are private final with no setters.
 */
public class Payslip {

    private final PayrollBreakdown breakdown;
    private final String employeeName;
    private final String position;
    private final String cutOff;

    /**
     * Creates one printable payslip.
     *
     * @param breakdown    the locked payroll snapshot
     * @param employeeName the name printed on the payslip
     * @param position     the position printed on the payslip
     * @param cutOff       the cut-off period the payslip covers
     * @throws IllegalArgumentException if any value is missing or blank
     */
    public Payslip(PayrollBreakdown breakdown, String employeeName,
            String position, String cutOff) {
        if (breakdown == null) {
            throw new IllegalArgumentException("Breakdown is required");
        }
        if (employeeName == null || employeeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name is required");
        }
        if (position == null || position.trim().isEmpty()) {
            throw new IllegalArgumentException("Position is required");
        }
        if (cutOff == null || cutOff.trim().isEmpty()) {
            throw new IllegalArgumentException("Cut off must not be blank");
        }
        this.breakdown = breakdown;
        this.employeeName = employeeName.trim();
        this.position = position.trim();
        this.cutOff = cutOff.trim();
    }

    public PayrollBreakdown getBreakdown() {
        return breakdown;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getPosition() {
        return position;
    }

    public String getCutOff() {
        return cutOff;
    }
}
