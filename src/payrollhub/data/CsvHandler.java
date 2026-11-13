package payrollhub.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;

import javax.swing.table.DefaultTableModel;

import payrollhub.model.Employee;

/**
 * The only class that reads or writes the CSV files. No screen parses CSV
 * itself. All methods are static and the class holds no state.
 *
 * Files (in src/data):
 * employee.csv empNo,lastName,firstName,birthday,address,phone,sss,philhealth,
 * tin,pagibig,status,position,supervisor,basicSalary,riceSubsidy,
 * phoneAllowance,clothingAllowance
 * attendance.csv empNo,date,logIn,logOut (date yyyy-MM-dd, times H:mm)
 * users.csv empNo,userName,password,role
 * leaves.csv leaveID,employeeID,type,startDate,endDate,status,reason
 */

public final class CsvHandler {

    public static final Path DATA_DIR = Paths.get("src", "data");
    public static final Path EMPLOYEE_FILE = DATA_DIR.resolve("employee.csv");
    public static final Path ATTENDANCE_FILE = DATA_DIR.resolve("attendance.csv");
    public static final Path USERS_FILE = DATA_DIR.resolve("users.csv");
    public static final Path LEAVES_FILE = DATA_DIR.resolve("leaves.csv");
    public static final Path TICKETS_FILE = DATA_DIR.resolve("tickets.csv");

    private static final String[] USER_HEADER = { "empNo", "userName", "password", "role" };
    private static final String[] LEAVE_HEADER = {
            "leaveID", "employeeID", "type", "startDate", "endDate", "status", "reason"
    };
    private static final String[] TICKET_HEADER = {
            "ticketID", "employeeID", "subject", "message", "createdDate",
            "processedBy", "status", "remarks"
    };
    private static final DateTimeFormatter ATTENDANCE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("H:mm");

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
                if (line.startsWith("\uFEFF"))
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

    private static final class CsvContents {
        private final String[] header;
        private final List<String[]> rows;

        private CsvContents(String[] header, List<String[]> rows) {
            this.header = header;
            this.rows = rows;
        }
    }

    private static CsvContents readContents(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String[] header = new String[0];
        List<String[]> rows = new ArrayList<>();
        boolean headerRead = false;

        for (String line : lines) {
            if (line.trim().isEmpty())
                continue;
            if (!headerRead) {
                if (line.startsWith("\uFEFF"))
                    line = line.substring(1);
                header = parseLine(line);
                headerRead = true;
            } else {
                rows.add(parseLine(line));
            }
        }
        return new CsvContents(header, rows);
    }

    /**
     * Adds a new record at the end of a file. The header is kept as it is.
     * 
     * @param empNo
     */
    public static void appendToCSV(String filePath, String... data) throws IOException {
        Path path = Paths.get(filePath);
        CsvContents contents = readContents(path);
        contents.rows.add(data);
        writeAll(path, contents.header, contents.rows);
    }

    /**
     * Replaces the record whose first column equals empNo. Returns false if not
     * found.
     */
    public static boolean updateCSV(String filePath, String empNo, String... data)
            throws IOException {
        Path path = Paths.get(filePath);
        CsvContents contents = readContents(path);
        int index = indexOfKey(contents.rows, empNo);
        if (index < 0)
            return false;
        contents.rows.set(index, data);
        writeAll(path, contents.header, contents.rows);
        return true;
    }

    /**
     * Removes the record whose first column equals empNo. Returns false if not
     * found.
     */
    public static boolean deleteFromCSV(String filePath, String empNo) throws IOException {
        Path path = Paths.get(filePath);
        CsvContents contents = readContents(path);
        int index = indexOfKey(contents.rows, empNo);
        if (index < 0)
            return false;
        contents.rows.remove(index);
        writeAll(path, contents.header, contents.rows);
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
        return findEmployee(EMPLOYEE_FILE, empNo);
    }

    public static Optional<EmployeeData> findEmployee(Path path, int empNo) throws IOException {
        String target = String.valueOf(empNo);
        for (EmployeeData e : loadEmployees(path)) {
            if (target.equals(e.getEmpNo()))
                return Optional.of(e);
        }
        return Optional.empty();
    }

    /**
     * Adds an employee model to employee.csv, converting it to the CSV data
     * representation before saving.
     */
    public static void addEmployee(Employee employee) throws IOException {
        if (employee == null) {
            throw new IllegalArgumentException("Employee data is required.");
        }
        saveEmployee(toEmployeeData(employee));
    }

    private static EmployeeData toEmployeeData(Employee employee) {
        return new EmployeeData(
                String.valueOf(employee.getEmpNo()), employee.getLastName(), employee.getFirstName(),
                employee.getBirthday(), employee.getAddress(), employee.getPhone(),
                employee.getSss(), employee.getPhilhealth(), employee.getTin(), employee.getPagibig(),
                employee.getStatus(), employee.getPosition(), employee.getSupervisor(),
                employee.getBasicSalary(), employee.getRiceSubsidy(),
                employee.getPhoneAllowance(), employee.getClothingAllowance());
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

    /**
     * Finds employees whose recorded supervisor is the specified employee.
     *
     * @param supervisorEmpNo employee number of the supervisor
     * @return the supervisor's direct reports, or an empty list when there are none
     * @throws IOException if employee data cannot be read or the supervisor does not exist
     */
    public static List<EmployeeData> findDirectReports(int supervisorEmpNo) throws IOException {
        if (supervisorEmpNo <= 0) {
            throw new IllegalArgumentException("Supervisor employee number must be positive.");
        }

        List<EmployeeData> employees = loadEmployees();
        String supervisorKey = String.valueOf(supervisorEmpNo);
        boolean supervisorExists = employees.stream()
                .anyMatch(employee -> supervisorKey.equals(employee.getEmpNo()));
        if (!supervisorExists) {
            throw new IOException("Supervisor employee " + supervisorEmpNo + " was not found.");
        }

        List<EmployeeData> directReports = new ArrayList<>();
        for (EmployeeData employee : employees) {
            if (!supervisorKey.equals(employee.getEmpNo())
                    && findSupervisor(employee, employees)
                            .map(supervisor -> supervisorKey.equals(supervisor.getEmpNo()))
                            .orElse(false)) {
                directReports.add(employee);
            }
        }
        return directReports;
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

    /**
     * Replaces the row for the given employee number using the employee model.
     * Returns false if that employee number does not exist.
     */
    public static boolean updateEmployee(int empNo, Employee employee) throws IOException {
        if (employee == null) {
            throw new IllegalArgumentException("Employee data is required.");
        }
        if (empNo <= 0 || employee.getEmpNo() != empNo) {
            throw new IllegalArgumentException("Employee number must match the employee data.");
        }
        return updateEmployee(toEmployeeData(employee));
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

    /** Removes the row with this employee number. Returns false if not found. */
    public static boolean removeEmployee(int empNo) throws IOException {
        if (empNo <= 0) {
            throw new IllegalArgumentException("Employee number must be positive.");
        }
        return deleteEmployee(Integer.toString(empNo));
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

    /** Returns the months with attendance for one employee, in ascending order. */
    public static List<String> loadAttendanceMonths(int empNo) throws IOException {
        if (empNo <= 0) {
            throw new IllegalArgumentException("Employee number must be positive.");
        }
        TreeSet<String> months = new TreeSet<>();
        List<String[]> rows = readAll(ATTENDANCE_FILE);
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length != 4) {
                throw new IOException("Malformed attendance record at data row " + (i + 1) + ".");
            }
            if (row[0].equals(Integer.toString(empNo))) {
                try {
                    months.add(YearMonth.from(LocalDate.parse(row[1])).toString());
                } catch (DateTimeParseException e) {
                    throw new IOException("Invalid attendance date at data row " + (i + 1) + ".", e);
                }
            }
        }
        return new ArrayList<>(months);
    }

    /** Loads one employee's attendance records for a yyyy-MM pay period. */
    public static List<AttendanceData> loadAttendance(int empNo, String period) throws IOException {
        if (empNo <= 0 || period == null || period.isBlank()) {
            throw new IllegalArgumentException("Employee number and attendance month are required.");
        }
        YearMonth month;
        try {
            month = YearMonth.parse(period);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Attendance month must use yyyy-MM format.", e);
        }

        List<AttendanceData> records = new ArrayList<>();
        List<String[]> rows = readAll(ATTENDANCE_FILE);
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length != 4) {
                throw new IOException("Malformed attendance record at data row " + (i + 1) + ".");
            }
            if (!row[0].equals(Integer.toString(empNo))) {
                continue;
            }
            try {
                LocalDate date = LocalDate.parse(row[1]);
                if (!YearMonth.from(date).equals(month)) {
                    continue;
                }
                LocalTime logIn = row[2].isBlank() ? null : LocalTime.parse(row[2], ATTENDANCE_TIME_FORMAT);
                LocalTime logOut = row[3].isBlank() ? null : LocalTime.parse(row[3], ATTENDANCE_TIME_FORMAT);
                records.add(new AttendanceData(empNo, date, logIn, logOut));
            } catch (DateTimeParseException | IllegalArgumentException e) {
                throw new IOException("Invalid attendance record at data row " + (i + 1) + ".", e);
            }
        }
        records.sort(Comparator.comparing(AttendanceData::getDate));
        return records;
    }

    public static Optional<AttendanceData> findAttendance(int empNo, LocalDate date) throws IOException {
        if (date == null) {
            throw new IllegalArgumentException("Attendance date is required.");
        }
        for (AttendanceData record : loadAttendance(empNo, YearMonth.from(date).toString())) {
            if (record.getDate().equals(date)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    public static void clockIn(int empNo, LocalDate date, LocalTime time) throws IOException {
        if (date == null || time == null) {
            throw new IllegalArgumentException("Check-in date and time are required.");
        }
        new payrollhub.service.AttendanceService(ATTENDANCE_FILE.toString())
                .recordTimeIn(empNo, LocalDateTime.of(date, time));
    }

    public static void clockOut(int empNo, LocalDate date, LocalTime time) throws IOException {
        if (date == null || time == null) {
            throw new IllegalArgumentException("Check-out date and time are required.");
        }
        new payrollhub.service.AttendanceService(ATTENDANCE_FILE.toString())
                .recordTimeOut(empNo, LocalDateTime.of(date, time));
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
        return authenticate(USERS_FILE, username, password);
    }

    public static Optional<String[]> authenticate(Path usersFile, String username, String password)
            throws IOException {
        for (String[] row : readAll(usersFile)) {
            if (row.length >= 4
                    && row[1].equalsIgnoreCase(username)
                    && row[2].equals(password)) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    public static Optional<String[]> findUser(String username) throws IOException {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        for (String[] row : readAll(USERS_FILE)) {
            if (row.length >= 4 && row[1].equalsIgnoreCase(username.trim())) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    public static void saveUser(int empNo, String username, String password, String role)
            throws IOException {
        if (empNo <= 0 || username == null || username.isBlank()
                || password == null || password.isBlank() || role == null || role.isBlank()) {
            throw new IllegalArgumentException("Employee number, username, password and role are required.");
        }
        List<String[]> rows = Files.exists(USERS_FILE) ? readAll(USERS_FILE) : new ArrayList<>();
        for (String[] row : rows) {
            if (row.length >= 2 && (row[0].equals(Integer.toString(empNo))
                    || row[1].equalsIgnoreCase(username.trim()))) {
                throw new IllegalArgumentException("Employee number or username is already in use.");
            }
        }
        rows.add(new String[] { Integer.toString(empNo), username.trim(), password, role.trim() });
        writeAll(USERS_FILE, existingHeader(USERS_FILE, USER_HEADER), rows);
    }

    public static boolean deleteUser(int empNo) throws IOException {
        if (empNo <= 0) {
            throw new IllegalArgumentException("Employee number must be positive.");
        }
        return deleteFromCSV(USERS_FILE.toString(), Integer.toString(empNo));
    }

    public static void updatePassword(int empNo, String newPassword) throws IOException {
        if (empNo <= 0 || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Employee number and new password are required.");
        }
        List<String[]> rows = readAll(USERS_FILE);
        for (String[] row : rows) {
            if (row.length >= 4 && row[0].equals(Integer.toString(empNo))) {
                row[2] = newPassword;
                writeAll(USERS_FILE, existingHeader(USERS_FILE, USER_HEADER), rows);
                return;
            }
        }
        throw new IOException("No login record found for employee " + empNo + ".");
    }

    private static String[] existingHeader(Path path, String[] fallback) throws IOException {
        if (Files.exists(path) && Files.size(path) > 0) {
            String[] header = readHeader(path);
            if (header.length > 0) {
                return header;
            }
        }
        return fallback;
    }

    public static List<LeaveData> loadLeaves() throws IOException {
        if (!Files.exists(LEAVES_FILE)) {
            return new ArrayList<>();
        }
        List<String[]> rows = readAll(LEAVES_FILE);
        List<LeaveData> leaves = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            try {
                leaves.add(LeaveData.fromCsvRow(rows.get(i)));
            } catch (RuntimeException e) {
                throw new IOException("Malformed leave record at data row " + (i + 1) + ".", e);
            }
        }
        return leaves;
    }

    public static String nextLeaveId() throws IOException {
        int next = 1;
        for (LeaveData leave : loadLeaves()) {
            next = Math.max(next, numericIdSuffix(leave.getLeaveId(), "L") + 1);
        }
        return String.format(Locale.ROOT, "L%04d", next);
    }

    public static void saveLeave(LeaveData leave) throws IOException {
        if (leave == null) {
            throw new IllegalArgumentException("Leave request is required.");
        }
        List<String[]> rows = Files.exists(LEAVES_FILE) ? readAll(LEAVES_FILE) : new ArrayList<>();
        for (String[] row : rows) {
            if (row.length > 0 && row[0].equals(leave.getLeaveId())) {
                throw new IllegalArgumentException("Leave request ID already exists.");
            }
        }
        rows.add(leave.toCsvRow());
        writeAll(LEAVES_FILE, existingHeader(LEAVES_FILE, LEAVE_HEADER), rows);
    }

    public static void updateLeaveStatus(String leaveId, String status) throws IOException {
        if (leaveId == null || leaveId.isBlank() || status == null
                || !List.of(LeaveData.PENDING, LeaveData.APPROVED, LeaveData.REJECTED).contains(status)) {
            throw new IllegalArgumentException("Leave status update is invalid.");
        }
        List<String[]> rows = readAll(LEAVES_FILE);
        for (String[] row : rows) {
            if (row.length != LEAVE_HEADER.length) {
                throw new IOException("Malformed leave record while updating status.");
            }
            if (row[0].equals(leaveId)) {
                row[5] = status;
                writeAll(LEAVES_FILE, existingHeader(LEAVES_FILE, LEAVE_HEADER), rows);
                return;
            }
        }
        throw new IOException("Leave request " + leaveId + " was not found.");
    }

    public static List<TicketData> loadTickets() throws IOException {
        if (!Files.exists(TICKETS_FILE)) {
            return new ArrayList<>();
        }
        List<String[]> rows = readAll(TICKETS_FILE);
        List<TicketData> tickets = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            try {
                tickets.add(TicketData.fromCsvRow(rows.get(i)));
            } catch (RuntimeException e) {
                throw new IOException("Malformed ticket record at data row " + (i + 1) + ".", e);
            }
        }
        return tickets;
    }

    public static String nextTicketId() throws IOException {
        int next = 1;
        for (TicketData ticket : loadTickets()) {
            next = Math.max(next, numericIdSuffix(ticket.getTicketId(), "T") + 1);
        }
        return String.format(Locale.ROOT, "T%04d", next);
    }

    public static void saveTicket(TicketData ticket) throws IOException {
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket is required.");
        }
        List<String[]> rows = Files.exists(TICKETS_FILE) ? readAll(TICKETS_FILE) : new ArrayList<>();
        for (String[] row : rows) {
            if (row.length > 0 && row[0].equals(ticket.getTicketId())) {
                throw new IllegalArgumentException("Ticket ID already exists.");
            }
        }
        rows.add(ticket.toCsvRow());
        writeAll(TICKETS_FILE, existingHeader(TICKETS_FILE, TICKET_HEADER), rows);
    }

    public static void updateTicket(TicketData ticket) throws IOException {
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket is required.");
        }
        List<String[]> rows = readAll(TICKETS_FILE);
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length != TICKET_HEADER.length) {
                throw new IOException("Malformed ticket record at data row " + (i + 1) + ".");
            }
            if (row[0].equals(ticket.getTicketId())) {
                rows.set(i, ticket.toCsvRow());
                writeAll(TICKETS_FILE, existingHeader(TICKETS_FILE, TICKET_HEADER), rows);
                return;
            }
        }
        throw new IOException("Ticket " + ticket.getTicketId() + " was not found.");
    }

    private static int numericIdSuffix(String value, String prefix) throws IOException {
        if (value == null || !value.startsWith(prefix) || value.length() == prefix.length()) {
            throw new IOException("Invalid record ID: " + value);
        }
        try {
            return Integer.parseInt(value.substring(prefix.length()));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid record ID: " + value, e);
        }
    }

    // =====================================================================
    // leaves.csv and attendance.csv: read-only counts for the dashboard
    // =====================================================================

    /**
     * Counts leave requests with the given status (for example "PENDING"),
     * across all employees. The comparison ignores case.
     */
    public static int countLeaves(String status) throws IOException {
        int count = 0;
        // leaveID,employeeID,type,startDate,endDate,status,reason
        for (String[] row : readAll(LEAVES_FILE)) {
            if (row.length > 5 && row[5].equalsIgnoreCase(status))
                count++;
        }
        return count;
    }

    /** Counts one employee's leave requests with the given status. */
    public static int countLeaves(int empNo, String status) throws IOException {
        int count = 0;
        for (String[] row : readAll(LEAVES_FILE)) {
            if (row.length > 5 && row[1].equals(String.valueOf(empNo))
                    && row[5].equalsIgnoreCase(status))
                count++;
        }
        return count;
    }

    /**
     * Counts the days an employee has an attendance record in a month.
     *
     * @param yearMonth a "yyyy-MM" string, as returned by loadAvailableMonths
     */
    public static int countAttendanceDays(int empNo, String yearMonth) throws IOException {
        int count = 0;
        // empNo,date,logIn,logOut
        for (String[] row : readAll(ATTENDANCE_FILE)) {
            if (row.length > 1 && row[0].equals(String.valueOf(empNo))
                    && row[1].startsWith(yearMonth))
                count++;
        }
        return count;
    }

    /**
     * Returns the newest "yyyy-MM" found in attendance.csv, or empty if the file
     * has no rows.
     */
    public static Optional<String> latestAttendanceMonth() throws IOException {
        List<String> months = loadAvailableMonths(ATTENDANCE_FILE.toString());
        return months.isEmpty() ? Optional.empty() : Optional.of(months.get(months.size() - 1));
    }

    public static int getNextEmpNo() {
        try {
            return getLastEmployeeNumber(EMPLOYEE_FILE.toString()) + 1;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read employee numbers.", e);
        }
    }

}
