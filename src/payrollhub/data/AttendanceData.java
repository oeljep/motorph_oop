package payrollhub.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class AttendanceData {

    private final int empNo;
    private final LocalDate date;
    private final LocalTime logIn;
    private final LocalTime logOut;

    public AttendanceData(int empNo, LocalDate date, LocalTime logIn, LocalTime logOut) {
        if (empNo <= 0 || date == null || logIn == null) {
            throw new IllegalArgumentException("A valid employee, date and check-in time are required.");
        }
        this.empNo = empNo;
        this.date = date;
        this.logIn = logIn;
        this.logOut = logOut;
    }

    public int getEmpNo() {
        return empNo;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getLogIn() {
        return logIn;
    }

    public LocalTime getLogOut() {
        return logOut;
    }

    public boolean isOpen() {
        return logOut == null;
    }

    public LocalDateTime getLogInDateTime() {
        return LocalDateTime.of(date, logIn);
    }
}
