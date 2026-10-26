/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package payrollhub.service;

import java.io.IOException;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import payrollhub.data.CsvHandler;
import payrollhub.model.Attendance;

public class AttendanceService {

    private final Path csvFile;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("H:mm");

    public AttendanceService(String csvFilePath) {
        this.csvFile = Path.of(csvFilePath);
    }

    public void recordTimeIn(
            String empNo,
            LocalDateTime timeIn) throws IOException {
        recordTimeIn(Integer.parseInt(empNo), timeIn);
    }

    public void recordTimeIn(int empNo, LocalDateTime timeIn) throws IOException {
        if (empNo <= 0 || timeIn == null) {
            throw new IllegalArgumentException("A valid employee and check-in time are required.");
        }
        List<String[]> rows = readRows();
        String employeeNumber = Integer.toString(empNo);
        String date = timeIn.toLocalDate().format(DATE_FORMAT);
        for (String[] row : rows) {
            if (row.length >= 2 && row[0].equals(employeeNumber) && row[1].equals(date)) {
                if (row.length != 4) {
                    throw new IOException("Malformed attendance row for employee " + empNo + ".");
                }
                throw new IllegalStateException("A check-in is already recorded for today.");
            }
        }
        rows.add(new String[] {
                employeeNumber, date, timeIn.toLocalTime().format(TIME_FORMAT), ""
        });
        writeRows(rows);
    }

    public void recordTimeOut(int empNo, LocalDateTime timeOut) throws IOException {
        if (empNo <= 0 || timeOut == null) {
            throw new IllegalArgumentException("A valid employee and check-out time are required.");
        }
        List<String[]> rows = readRows();
        String employeeNumber = Integer.toString(empNo);
        String date = timeOut.toLocalDate().format(DATE_FORMAT);
        for (String[] row : rows) {
            if (row.length >= 2 && row[0].equals(employeeNumber) && row[1].equals(date)) {
                if (row.length != 4) {
                    throw new IOException("Malformed attendance row for employee " + empNo + ".");
                }
                if (row[2].isBlank()) {
                    throw new IllegalStateException("Check in before checking out.");
                }
                if (!row[3].isBlank()) {
                    throw new IllegalStateException("A check-out is already recorded for today.");
                }
                if (timeOut.toLocalTime().isBefore(LocalTime.parse(row[2], TIME_FORMAT))) {
                    throw new IllegalArgumentException("Check-out time cannot be before check-in.");
                }
                row[3] = timeOut.toLocalTime().format(TIME_FORMAT);
                writeRows(rows);
                return;
            }
        }
        throw new IllegalStateException("Check in before checking out.");
    }

    public List<Attendance> loadForMonth(int empNo, YearMonth month) throws IOException {
        if (empNo <= 0 || month == null) {
            throw new IllegalArgumentException("A valid employee and month are required.");
        }
        List<Attendance> entries = new ArrayList<>();
        List<String[]> rows = readRows();
        for (int index = 0; index < rows.size(); index++) {
            String[] row = rows.get(index);
            if (row.length != 4) {
                throw new IOException("Malformed attendance record at data row " + (index + 1) + ".");
            }
            if (!row[0].equals(Integer.toString(empNo))) {
                continue;
            }
            try {
                LocalDate date = LocalDate.parse(row[1], DATE_FORMAT);
                if (YearMonth.from(date).equals(month)) {
                    LocalTime timeIn = row[2].isBlank() ? null : LocalTime.parse(row[2], TIME_FORMAT);
                    LocalTime timeOut = row[3].isBlank() ? null : LocalTime.parse(row[3], TIME_FORMAT);
                    entries.add(new Attendance(empNo, date, timeIn, timeOut));
                }
            } catch (DateTimeParseException e) {
                throw new IOException("Invalid attendance date or time in " + csvFile + ".", e);
            }
        }
        return entries;
    }

    private List<String[]> readRows() throws IOException {
        if (!Files.exists(csvFile)) {
            return new ArrayList<>();
        }
        return CsvHandler.readAll(csvFile);
    }

    private void writeRows(List<String[]> rows) throws IOException {
        String[] header = Files.exists(csvFile) && Files.size(csvFile) > 0
                ? CsvHandler.readHeader(csvFile)
                : new String[] { "empNo", "date", "logIn", "logOut" };
        CsvHandler.writeAll(csvFile, header, rows);
    }
}