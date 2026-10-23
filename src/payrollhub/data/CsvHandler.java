package payrollhub.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import javax.swing.table.DefaultTableModel;

/**
 * The only class that reads or writes the CSV files. No screen parses CSV
 * itself. All methods are static and the class holds no state.
 *
 * Files (in the data folder):
 * employee.csv empNo,lastName,firstName,birthday,address,phone,sss,philhealth,
 * tin,pagibig,status,position,supervisor,basicSalary,riceSubsidy,
 * phoneAllowance,clothingAllowance
 * attendance.csv empNo,date,logIn,logOut (date yyyy-MM-dd, times H:mm)
 * users.csv empNo,userName,password,role
 * leaves.csv leaveID,employeeID,type,startDate,endDate,status,reason
 */

public final class CsvHandler {

    public static final Path DATA_DIR = Paths.get("data");
    public static final Path EMPLOYEE_FILE = DATA_DIR.resolve("employee.csv");
    public static final Path ATTENDANCE_FILE = DATA_DIR.resolve("attendance.csv");
    public static final Path USERS_FILE = DATA_DIR.resolve("users.csv");
    public static final Path LEAVES_FILE = DATA_DIR.resolve("leaves.csv");

    public static final String[] EMPLOYEE_HEADER = {
            "empNo", "lastName", "firstName", "birthday", "address", "phone",
            "sss", "philhealth", "tin", "pagibig", "status", "position",
            "supervisor", "basicSalary", "riceSubsidy", "phoneAllowance", "clothingAllowance"
    };

    private CsvHandler() {
        // Prevent instantiation
    }

    /**
     * Splits one CSV line into trimmed fields. Handles quoted fields, commas
     * inside quotes, doubled quotes ("" becomes "), and keeps empty fields,
     * so a blank supervisor still counts as a field.
     */

    public static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    /** Joins fields into one CSV line, quoting any field that needs it. */
    public static String toCsvLine(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null)
            return "";
        boolean needsQuotes = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!needsQuotes)
            return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * Reads a CSV file and returns the data rows (header and blank lines skipped).
     */
    public static List<String[]> readAll(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<String[]> rows = new ArrayList<>();
        boolean headerSkipped = false;

        for (String line : lines) {
            if (!headerSkipped) {
                // Some Excel exports start with an invisible BOM character.
                if (line.startsWith(""))
                    line = line.substring(1);
                if (line.trim().isEmpty())
                    continue;
                headerSkipped = true;
                continue;
            }
            if (line.trim().isEmpty())
                continue;
            rows.add(parseLine(line));
        }
        return rows;
    }

    /** Reads only the header row of a CSV file. */
    public static String[] readHeader(Path path) throws IOException {
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.startsWith("﻿"))
                line = line.substring(1);
            if (!line.trim().isEmpty())
                return parseLine(line);
        }
        return new String[0];
    }

    /**
     * Writes header and rows to a temp file, then swaps it in, so a crash
     * mid-write cannot leave a half-written data file.
     */
    public static void writeAll(Path path, String[] header, List<String[]> rows)
            throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null)
            Files.createDirectories(parent);

        List<String> lines = new ArrayList<>();
        lines.add(toCsvLine(header));
        for (String[] row : rows)
            lines.add(toCsvLine(row));

        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.write(temp, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // =====================================================================
    // Generic record helpers (Method Dictionary: appendToCSV, updateCSV,
    // deleteFromCSV). Rows are matched on the first column.
    // =====================================================================

    /** Adds a new record at the end of a file. The header is kept as it is. */
    public static void appendToCSV(String filePath, String... data) throws IOException {
        Path path = Paths.get(filePath);
        String[] header = readHeader(path);
        List<String[]> rows = readAll(path);
        rows.add(data);
        writeAll(path, header, rows);
    }

    /**
     * Replaces the record whose first column equals empNo. Returns false if not
     * found.
     */
    public static boolean updateCSV(String filePath, String empNo, String... data)
            throws IOException {
        Path path = Paths.get(filePath);
        String[] header = readHeader(path);
        List<String[]> rows = readAll(path);
        int index = indexOfKey(rows, empNo);
        if (index < 0)
            return false;
        rows.set(index, data);
        writeAll(path, header, rows);
        return true;
    }

    /**
     * Removes the record whose first column equals empNo. Returns false if not
     * found.
     */
    public static boolean deleteFromCSV(String filePath, String empNo) throws IOException {
        Path path = Paths.get(filePath);
        String[] header = readHeader(path);
        List<String[]> rows = readAll(path);
        int index = indexOfKey(rows, empNo);
        if (index < 0)
            return false;
        rows.remove(index);
        writeAll(path, header, rows);
        return true;
    }

    private static int indexOfKey(List<String[]> rows, String key) {
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            if (r.length > 0 && r[0].equals(key))
                return i;
        }
        return -1;
    }

    // =====================================================================
    // employee.csv
    // =====================================================================

    public static List<EmployeeData> loadEmployees() throws IOException {
        return loadEmployees(EMPLOYEE_FILE);
    }

    /**
     * Loads all employees. A malformed row is skipped and reported on stderr with
     * its line number.
     */
    public static List<EmployeeData> loadEmployees(Path path) throws IOException {
        List<String[]> rows = readAll(path);
        List<EmployeeData> employees = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            int fileLine = i + 2; // +1 for the header, +1 because lines start at 1
            try {
                employees.add(EmployeeData.fromCsvRow(rows.get(i)));
            } catch (RuntimeException e) {
                System.err.println(path.getFileName() + " line " + fileLine
                        + " skipped: " + e.getMessage());
            }
        }
        return employees;
    }

    public static Optional<EmployeeData> findEmployee(int empNo) throws IOException {
        for (EmployeeData e : loadEmployees()) {
            if (e.getEmpNo() == empNo)
                return Optional.of(e);
        }
        return Optional.empty();
    }

    /**
     * Finds the supervisor of an employee. The file stores the supervisor as
     * "Last, First" text, so the match ignores case, commas and spaces. That
     * also handles the row written as "San, Jose Brad" for last name
     * "San Jose" and first name "Brad". Returns empty for the CEO (blank).
     */
    public static Optional<EmployeeData> findSupervisor(EmployeeData employee,
            List<EmployeeData> all) {
        String wanted = squash(employee.getSupervisor());
        if (wanted.isEmpty())
            return Optional.empty();
        for (EmployeeData e : all) {
            if (squash(e.getLastName() + e.getFirstName()).equals(wanted)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    private static String squash(String s) {
        return s == null ? "" : s.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
    }

    public static void saveEmployee(EmployeeData employee) throws IOException {
        saveEmployee(EMPLOYEE_FILE, employee);
    }

    /**
     * Adds a new employee. Throws IllegalArgumentException if the empNo already
     * exists.
     */
    public static void saveEmployee(Path path, EmployeeData employee) throws IOException {
        List<String[]> rows = readAll(path);
        if (indexOfKey(rows, String.valueOf(employee.getEmpNo())) >= 0) {
            throw new IllegalArgumentException(
                    "Employee number " + employee.getEmpNo() + " already exists.");
        }
        rows.add(employee.toCsvRow());
        writeAll(path, EMPLOYEE_HEADER, rows);
    }

    public static boolean updateEmployee(EmployeeData employee) throws IOException {
        return updateEmployee(EMPLOYEE_FILE, employee);
    }

    /** Replaces the row with the same empNo. Returns false if not found. */
    public static boolean updateEmployee(Path path, EmployeeData employee) throws IOException {
        List<String[]> rows = readAll(path);
        int index = indexOfKey(rows, String.valueOf(employee.getEmpNo()));
        if (index < 0)
            return false;
        rows.set(index, employee.toCsvRow());
        writeAll(path, EMPLOYEE_HEADER, rows);
        return true;
    }

    public static boolean deleteEmployee(String empNo) throws IOException {
        return deleteEmployee(EMPLOYEE_FILE, empNo);
    }

    /** Removes the row with this empNo. Returns false if not found. */
    public static boolean deleteEmployee(Path path, String empNo) throws IOException {
        List<String[]> rows = readAll(path);
        int index = indexOfKey(rows, empNo);
        if (index < 0)
            return false;
        rows.remove(index);
        writeAll(path, EMPLOYEE_HEADER, rows);
        return true;
    }

    /**
     * Finds the highest employee number in use (for the Add Employee screen).
     * Returns 0 if none.
     */
    public static int getLastEmployeeNumber(String filePath) throws IOException {
        int max = 0;
        for (String[] row : readAll(Paths.get(filePath))) {
            if (row.length == 0)
                continue;
            try {
                max = Math.max(max, Integer.parseInt(row[0]));
            } catch (NumberFormatException ignored) {
                // skip a non-numeric empNo
            }
        }
        return max;
    }

    /**
     * Fills a table model with the employee list: Employee #, Last Name,
     * First Name, SSS, PhilHealth, TIN, Pag-IBIG. Existing rows are cleared.
     */
    public static void loadEmployeeData(String filePath, DefaultTableModel model)
            throws IOException {
        // A model created without columns would silently drop the values.
        if (model.getColumnCount() == 0) {
            model.setColumnIdentifiers(new String[] {
                    "Employee #", "Last Name", "First Name",
                    "SSS #", "PhilHealth #", "TIN #", "Pag-IBIG #"
            });
        }
        model.setRowCount(0);
        for (EmployeeData e : loadEmployees(Paths.get(filePath))) {
            model.addRow(new Object[] {
                    e.getEmpNo(), e.getLastName(), e.getFirstName(),
                    e.getSss(), e.getPhilhealth(), e.getTin(), e.getPagibig()
            });
        }
    }

    // =====================================================================
    // attendance.csv
    // =====================================================================

    /**
     * Collects the distinct months found in an attendance file, as "yyyy-MM"
     * strings in ascending order (for the Timesheet month selector).
     */
    public static List<String> loadAvailableMonths(String filePath) throws IOException {
        TreeSet<String> months = new TreeSet<>();
        for (String[] row : readAll(Paths.get(filePath))) {
            // date is the second column, yyyy-MM-dd
            if (row.length > 1 && row[1].length() >= 7) {
                months.add(row[1].substring(0, 7));
            }
        }
        return new ArrayList<>(months);
    }

    // =====================================================================
    // users.csv
    // =====================================================================

    /**
     * Checks a login against users.csv (empNo,userName,password,role).
     * Returns the matching row, or empty if the credentials do not match.
     */

    public static Optional<String[]> authenticate(String username, String password)
        throws IOException {
            for (String[] row : readAll(USERS_FILE)) {
                if (row.length >= 4
                        && row[1].equalsIgnoreCase(username)
                        && row[2].equals(password)) {
                    return Optional.of(row);
                }
            }
            return Optional.empty();
        }

}
