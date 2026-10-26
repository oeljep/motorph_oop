package payrollhub.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import payrollhub.contracts.LeaveHub;
import payrollhub.data.CsvHandler;
import payrollhub.model.LeaveRequest;

public class LeaveService implements LeaveHub {

    private final Path leaveFile;

    private static final String[] HEADER = {
            "leaveID", "employeeID", "type", "startDate", "endDate", "status", "reason"
    };

    public LeaveService() {
        this(CsvHandler.LEAVES_FILE);
    }

    public LeaveService(Path leaveFile) {
        if (leaveFile == null) {
            throw new IllegalArgumentException("Leave file path is required.");
        }
        this.leaveFile = leaveFile;
    }

    @Override
    public LeaveRequest applyLeave(String leaveID, int empNo,
            LocalDate startDate, LocalDate endDate) throws IOException {
        return applyLeave(leaveID, empNo, startDate, endDate, "Personal", "");
    }

    @Override
    public LeaveRequest applyLeave(String leaveID, int empNo,
            LocalDate startDate, LocalDate endDate, String reason) throws IOException {
        return applyLeave(leaveID, empNo, startDate, endDate, "Personal", reason);
    }

    @Override
    public LeaveRequest applyLeave(String leaveID, int empNo, LocalDate startDate,
            LocalDate endDate, String type, String reason) throws IOException {
        if (leaveID == null || leaveID.isBlank() || empNo <= 0
                || startDate == null || endDate == null || endDate.isBefore(startDate)
                || type == null || !List.of("Sick", "Vacation", "Personal").contains(type)) {
            throw new IllegalArgumentException("Leave request details are invalid.");
        }
        List<String[]> rows = readRows();
        for (String[] row : rows) {
            if (row.length > 0 && row[0].equals(leaveID)) {
                throw new IllegalArgumentException("Leave request ID already exists.");
            }
        }
        String[] row = {
                leaveID, Integer.toString(empNo), type,
                startDate.toString(), endDate.toString(), "PENDING",
                reason == null ? "" : reason.trim()
        };
        rows.add(row);
        writeRows(rows);
        return toLeaveRequest(row);
    }

    @Override
    public void updateLeaveStatus(String leaveID, String newStatus) throws IOException {
        if (leaveID == null || leaveID.isBlank() || newStatus == null
                || !List.of("APPROVED", "REJECTED", "PENDING").contains(newStatus)) {
            throw new IllegalArgumentException("Leave status update is invalid.");
        }
        List<String[]> rows = readRows();
        for (String[] row : rows) {
            if (row.length > 0 && row[0].equals(leaveID)) {
                if (row.length != HEADER.length) {
                    throw new IOException("Malformed leave request record for " + leaveID + ".");
                }
                row[5] = newStatus;
                writeRows(rows);
                return;
            }
        }
        throw new IllegalArgumentException("Leave request was not found.");
    }

    @Override
    public List<LeaveRequest> viewAllLeaveRequests() throws IOException {
        List<LeaveRequest> requests = new ArrayList<>();
        List<String[]> rows = readRows();
        for (int index = 0; index < rows.size(); index++) {
            String[] row = rows.get(index);
            if (row.length != HEADER.length) {
                throw new IOException("Malformed leave request at data row " + (index + 1) + ".");
            }
            try {
                requests.add(toLeaveRequest(row));
            } catch (IllegalArgumentException e) {
                throw new IOException("Malformed leave request at data row " + (index + 1) + ".", e);
            }
        }
        return requests;
    }

    private List<String[]> readRows() throws IOException {
        if (!Files.exists(leaveFile)) {
            return new ArrayList<>();
        }
        return CsvHandler.readAll(leaveFile);
    }

    private void writeRows(List<String[]> rows) throws IOException {
        CsvHandler.writeAll(leaveFile, HEADER, rows);
    }

    private LeaveRequest toLeaveRequest(String[] row) {
        try {
            return new LeaveRequest(row[0], Integer.parseInt(row[1]), row[2],
                    LocalDate.parse(row[3]), LocalDate.parse(row[4]), row[5], row[6]);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Malformed leave request record.", e);
        }
    }
}
