package payrollhub.model;

import payrollhub.contracts.PayrollProcessor;
import payrollhub.contracts.UserHub;

public class Regular implements UserHub, PayrollProcessor {

    private String password;

    public Regular() {
        this.password = "";
    }

    @Override
    public Position getRole() {
        return Position.REGULAR;
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

    @Override
    public double calculatePay() {
        return 0.0;
    }

    @Override
    public double getSssDeduction(double gross) {
        return 0.0;
    }

    @Override
    public double getPagibigDeduction(double gross) {
        return 0.0;
    }

    @Override
    public double getWithholdingTax(double gross) {
        return 0.0;
    }

    @Override
    public double getPhilHealthDeduction(double gross) {
        return 0.0;
    }
}
