package payrollhub.service;

import java.time.LocalDate;
import java.time.YearMonth;

/** The two fixed semi-monthly payroll periods. */
public enum PayrollCutoff {
    FIRST_HALF("1-15") {
        @Override
        public LocalDate startDate(YearMonth month) {
            return month.atDay(1);
        }

        @Override
        public LocalDate endDate(YearMonth month) {
            return month.atDay(15);
        }
    },
    SECOND_HALF("16-end") {
        @Override
        public LocalDate startDate(YearMonth month) {
            return month.atDay(16);
        }

        @Override
        public LocalDate endDate(YearMonth month) {
            return month.atEndOfMonth();
        }
    };

    private final String label;

    PayrollCutoff(String label) {
        this.label = label;
    }

    public abstract LocalDate startDate(YearMonth month);

    public abstract LocalDate endDate(YearMonth month);

    public static PayrollCutoff fromLabel(String label) {
        for (PayrollCutoff cutoff : values()) {
            if (cutoff.label.equals(label)) {
                return cutoff;
            }
        }
        throw new IllegalArgumentException("Unknown payroll cutoff: " + label);
    }

    @Override
    public String toString() {
        return label;
    }
}
