package payrollhub.service;

import java.time.Duration;
import java.time.LocalTime;

public final class AttendanceRules {

    private static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    private static final long GRACE_MINUTES = 10;
    private static final long UNPAID_BREAK_MINUTES = 60;

    private AttendanceRules() {
    }

    public static double hoursWorked(LocalTime timeIn, LocalTime timeOut) {
        if (timeIn == null || timeOut == null || timeOut.isBefore(timeIn)) {
            throw new IllegalArgumentException("Valid check-in and check-out times are required.");
        }
        long elapsedMinutes = Duration.between(timeIn, timeOut).toMinutes();
        return Math.max(0, elapsedMinutes - UNPAID_BREAK_MINUTES) / 60.0;
    }

    public static int lateMinutes(LocalTime timeIn) {
        if (timeIn == null) {
            throw new IllegalArgumentException("A check-in time is required.");
        }
        LocalTime graceEnds = SHIFT_START.plusMinutes(GRACE_MINUTES);
        return timeIn.isAfter(graceEnds)
                ? (int) Duration.between(graceEnds, timeIn).toMinutes()
                : 0;
    }
}
