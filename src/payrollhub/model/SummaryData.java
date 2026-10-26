package payrollhub.model;

/**
 * Monthly government-mandated deduction totals for tax-report compliance.
 * Immutable: all fields are private final with no setters.
 */
public class SummaryData {

    private final int month;
    private final int year;
    private final double totalSss;
    private final double totalPhilHealth;
    private final double totalPagIbig;
    private final double totalTax;

    /**
     * Creates a summary for one month and year.
     *
     * @param month            1 to 12
     * @param year             four-digit year
     * @param totalSss         total SSS contributions (non-negative)
     * @param totalPhilHealth  total PhilHealth contributions (non-negative)
     * @param totalPagIbig     total Pag-IBIG contributions (non-negative)
     * @param totalTax         total withholding tax (non-negative)
     */
    public SummaryData(int month, int year, double totalSss,
                       double totalPhilHealth, double totalPagIbig,
                       double totalTax) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be 1 to 12");
        }
        if (year < 1000 || year > 9999) {
            throw new IllegalArgumentException("year must be four digits");
        }
        if (totalSss < 0 || totalPhilHealth < 0
                || totalPagIbig < 0 || totalTax < 0) {
            throw new IllegalArgumentException("no negative amounts");
        }
        this.month = month;
        this.year = year;
        this.totalSss = totalSss;
        this.totalPhilHealth = totalPhilHealth;
        this.totalPagIbig = totalPagIbig;
        this.totalTax = totalTax;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public double getTotalSss() {
        return totalSss;
    }

    public double getTotalPhilHealth() {
        return totalPhilHealth;
    }

    public double getTotalPagIbig() {
        return totalPagIbig;
    }

    public double getTotalTax() {
        return totalTax;
    }

    /**
     * Derived on request, never stored, so it cannot disagree with its inputs.
     */
    public double getTotalDeductions() {
        return totalSss + totalPhilHealth + totalPagIbig + totalTax;
    }
}
