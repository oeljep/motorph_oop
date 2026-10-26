package payrollhub.model;

import payrollhub.contracts.SystemSupportHub;
import payrollhub.model.Employee;

public class InfoTech implements SystemSupportHub {

    @Override
    public boolean resetCredentials(int empID, String newPassword) {
        return false;
    }

    @Override
    public boolean resetCredentials(int empID, String newPassword, String adminToken) {
        return false;
    }

    @Override
    public boolean checkSystemHealth() {
        return true;
    }

    @Override
    public Position getRole() {
        return Position.INFO_TECH;
    }
}
