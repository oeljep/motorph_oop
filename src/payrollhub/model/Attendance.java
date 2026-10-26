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
}
