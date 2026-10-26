package payrollhub.model;

import payrollhub.contracts.AdminHub;
import payrollhub.contracts.UserHub;

public class Admin implements AdminHub, UserHub {

    private String password;

    public Admin() {
        this.password = "";
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
    public void logout() {
        this.password = "";
    }
}
