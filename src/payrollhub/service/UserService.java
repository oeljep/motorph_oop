package payrollhub.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.gui.GUIUser;

public class UserService {

    private final Path usersFile;
    private final Path employeeFile;

    public UserService() {
        this(CsvHandler.USERS_FILE, CsvHandler.EMPLOYEE_FILE);
    }

    public UserService(Path usersFile, Path employeeFile) {
        if (usersFile == null || employeeFile == null) {
            throw new IllegalArgumentException("User and employee file paths are required.");
        }
        this.usersFile = usersFile;
        this.employeeFile = employeeFile;
    }

    public Optional<GUIUser> authenticate(String username, String password) throws IOException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        Optional<String[]> account = CsvHandler.authenticate(usersFile, username.trim(), password);
        if (account.isEmpty()) {
            return Optional.empty();
        }
        String[] row = account.get();
        if (row.length < 4) {
            throw new IOException("The matching user record is malformed.");
        }
        int empNo;
        try {
            empNo = Integer.parseInt(row[0]);
        } catch (NumberFormatException e) {
            throw new IOException("The matching user record has an invalid employee number.", e);
        }
        EmployeeData employee = CsvHandler.findEmployee(employeeFile, empNo).orElse(null);
        return Optional.of(new GUIUser(empNo, row[1], row[3], employee));
    }

    public boolean isPasswordValid(String password) {
        return password != null && password.length() >= 8;
    }

    public int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        int strength = 0;
        if (password.length() >= 8) {
            strength++;
        }
        if (password.matches(".*[A-Z].*")) {
            strength++;
        }
        if (password.matches(".*[a-z].*")) {
            strength++;
        }
        if (password.matches(".*[0-9].*")) {
            strength++;
        }
        if (password.matches(".*[!@#$%^&*].*")) {
            strength++;
        }
        return strength;
    }

    public boolean resetPassword(int empNo, String lastName, String newPassword) throws IOException {
        if (empNo <= 0 || lastName == null || lastName.isBlank()
                || !isPasswordValid(newPassword)) {
            return false;
        }
        Optional<EmployeeData> employee = CsvHandler.findEmployee(employeeFile, empNo);
        if (employee.isEmpty() || !employee.get().getLastName().equalsIgnoreCase(lastName.trim())) {
            return false;
        }

        List<String[]> accounts = CsvHandler.readAll(usersFile);
        String[] matchingAccount = null;
        for (String[] row : accounts) {
            if (row.length >= 4 && row[0].equals(Integer.toString(empNo))) {
                if (matchingAccount != null) {
                    throw new IOException("Multiple user records share employee number " + empNo + ".");
                }
                matchingAccount = row;
            }
        }
        if (matchingAccount == null) {
            return false;
        }
        matchingAccount[2] = newPassword;
        CsvHandler.writeAll(usersFile, CsvHandler.readHeader(usersFile), accounts);
        return true;
    }
}
