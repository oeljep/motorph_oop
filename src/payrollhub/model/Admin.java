package payrollhub.model;

import payrollhub.contracts.AdminHub;
import payrollhub.contracts.UserHub;

public class Admin implements AdminHub, UserHub {

    private String password;

    public Admin() {
        this.password = "";
    }

    @Override
    public void addEmployee(Employee emp) {
        // TODO: implement employee addition
    }

    @Override
    public int generateNextEmpNo() {
        return 0;
    }

    @Override
    public boolean isEmployeeValid(Employee emp) {
        return emp != null;
    }

    @Override
    public void updateEmployee(int id, Employee data) {
        // TODO: implement employee update
    }

    @Override
    public void removeEmployee(int id) {
        // TODO: implement employee removal
    }

    @Override
    public Position getRole() {
        return Position.ADMIN;
    }

    @Override
    public boolean authenticate(String user, String pass) {
        return false;
    }

    @Override
    public boolean authenticate(String token) {
        return false;
    }

    @Override
    public boolean isPasswordValid(String pass) {
        return pass != null && pass.length() >= 8;
    }

    @Override
    public int getPasswordStrength(String pass) {
        if (pass == null || pass.isEmpty()) {
            return 0;
        }
        int strength = 0;
        if (pass.length() >= 8) {
            strength++;
        }
        if (pass.matches(".*[A-Z].*")) {
            strength++;
        }
        if (pass.matches(".*[a-z].*")) {
            strength++;
        }
        if (pass.matches(".*[0-9].*")) {
            strength++;
        }
        if (pass.matches(".*[!@#$%^&*].*")) {
            strength++;
        }
        return Math.min(strength, 5);
    }

    @Override
    public void resetPassword(String newPass) {
        this.password = newPass;
    }

    @Override
    public void logout() {
        this.password = "";
    }
}
