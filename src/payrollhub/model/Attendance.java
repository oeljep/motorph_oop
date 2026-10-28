package payrollhub.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Attendance {

    private final int empNo;
    private final LocalDate date;
    private final LocalTime timeIn;
    private final LocalTime timeOut;

    public Attendance(int empNo, LocalDate date, LocalTime timeIn, LocalTime timeOut) {
        this.empNo = empNo;
        this.date = date;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
    }

    public int getEmpNo() {
        return empNo;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTimeIn() {
        return timeIn;
    }

    public LocalTime getTimeOut() {
        return timeOut;
    }

    // ---------------------- Additional Methods ----------------------

    public double getLateMinutes() {
        // *Computed by private logic, never supplied from outside.
        // Assumption: start 08:00 with a 10-minute grace period (confirm against
        // MotorPH policy).Returns 0 if on time. */
        LocalTime officialStartTime = LocalTime.of(8, 0);
        LocalTime gracePeriodEnd = officialStartTime.plusMinutes(10);

        if (timeIn.isAfter(gracePeriodEnd)) {
            return java.time.Duration.between(gracePeriodEnd, timeIn).toMinutes() > 0
                    ? java.time.Duration.between(gracePeriodEnd, timeIn).toMinutes()
                    : 0;
        } else {
            return 0;
        }
    }

    public double getTotalHoursWorked() {
        // Time between timeIn and timeOut less a one-hour unpaid break (assumption).
        // Never negative.
        java.time.Duration duration = java.time.Duration.between(timeIn, timeOut);
        double totalHours = duration.toHours() + (double) duration.toMinutesPart() / 60;
        return Math.max(0, totalHours - 1); // Subtract 1 hour for unpaid break
    }

    public double getOvertimeHours() {
        // Returns 0 when no overtime. Overtime needs supervisor approval in the process
        // flow, so the approval check belongs to the caller.
        double totalHoursWorked = getTotalHoursWorked();
        double regularHours = 8.0; // Assuming 8 hours is the standard workday
        return Math.max(0, totalHoursWorked - regularHours);
    }

}
