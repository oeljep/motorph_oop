package payrollhub.contracts;

import payrollhub.model.Position;

public interface UserHub {

    Position getRole();

    boolean authenticate(String user, String pass);

    boolean authenticate(String token);

    boolean isPasswordValid(String pass);

    int getPasswordStrength(String pass);

    void resetPassword(String newPass);

    void logout();
}
