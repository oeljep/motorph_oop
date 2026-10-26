package payrollhub.contracts;

import payrollhub.model.Position;

public interface SystemSupportHub {

    Position getRole();

    boolean resetCredentials(int empID, String newPassword);

    boolean resetCredentials(int empID, String newPassword, String adminToken);

    boolean checkSystemHealth();
}
