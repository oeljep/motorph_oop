package payrollhub.gui;

import java.util.Locale;

public final class ActionAccess {

    private ActionAccess() {
    }

    public static boolean canAddOrDeleteEmployee(String role) {
        String normalized = normalize(role);
        return normalized.equals("admin") || normalized.equals("chief admin");
    }

    public static boolean canEditEmployee(String role) {
        String normalized = normalize(role);
        return canAddOrDeleteEmployee(normalized)
                || normalized.equals("humanresources")
                || normalized.equals("human resources")
                || normalized.equals("humanresources admin");
    }

    public static boolean canDecideLeave(String role) {
        return canEditEmployee(role);
    }

    private static String normalize(String role) {
        return role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
    }
}
