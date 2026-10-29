package payrollhub.gui;

import java.util.Optional;
import payrollhub.data.EmployeeData;

public final class GUIUser {

    private final int empNo;
    private final String username;
    private final String role;
    private final EmployeeData employee;

    public GUIUser(int empNo, String username, String role, EmployeeData employee) {
        if (empNo <= 0 || username == null || username.isBlank()
                || role == null || role.isBlank()) {
            throw new IllegalArgumentException("Authenticated user details are incomplete.");
        }
        this.empNo = empNo;
        this.username = username;
        this.role = role;
        this.employee = employee;
    }

    public int getEmpNo() {
        return empNo;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public Optional<EmployeeData> getEmployee() {
        return Optional.ofNullable(employee);
    }

    public boolean canManageEmployees() {
        return role.equalsIgnoreCase("Admin") || isHumanResources();
    }

    public boolean canApproveLeave() {
        return role.equalsIgnoreCase("Admin") || isHumanResources();
    }

    private boolean isHumanResources() {
        return role.equalsIgnoreCase("HumanResources");
    }
}
