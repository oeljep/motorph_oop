package payrollhub.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * Decides which sidebar tabs a user may open, based on the role stored in
 * users.csv (Admin, HumanResources, Accounting, InfoTech, Regular).
 *
 * This is the single place to change access. Screens never check role names
 * themselves; they ask this class. An unknown role gets the same tabs as a
 * Regular employee, so a typo in users.csv can never grant extra access.
 */
public final class NavAccess {

    private NavAccess() {
    }

    /** Tabs every signed-in user gets. */
    private static final NavTab[] STAFF = {
            NavTab.DASHBOARD, NavTab.TIME_TRACKER, NavTab.SCHEDULE, NavTab.PAYSLIP
    };

    /**
     * Lists the tabs for a role, in sidebar order.
     *
     * @param role the role text from users.csv (case and extra spaces ignored)
     * @return an unmodifiable list, never empty
     */
    public static List<NavTab> tabsFor(String role) {
        String key = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        EnumSet<NavTab> allowed = EnumSet.noneOf(NavTab.class);
        Collections.addAll(allowed, STAFF);

        switch (key) {
            case "humanresources":
                allowed.add(NavTab.EMPLOYEE_MANAGER);
                break;
            case "accounting":
                allowed.add(NavTab.PAYROLL_MANAGER);
                allowed.add(NavTab.DISPUTE_MANAGER);
                break;
            case "admin":
                allowed.add(NavTab.EMPLOYEE_MANAGER);
                allowed.add(NavTab.LEAVE_MANAGER);
                break;
            case "infotech":
                allowed.add(NavTab.TICKET_MANAGER);
                break;
            default:
                // Regular and anything unrecognised: staff tabs only.
                break;
        }

        List<NavTab> tabs = new ArrayList<>();
        for (NavTab tab : NavTab.values()) {
            if (allowed.contains(tab)) {
                tabs.add(tab);
            }
        }
        return Collections.unmodifiableList(tabs);
    }

    /**
     * Checks whether a role may open a particular tab.
     *
     * @param role the role text from users.csv
     * @param tab  the tab to check
     * @return true only when the tab is included in that role's access list
     */
    public static boolean canOpen(String role, NavTab tab) {
        return tab != null && tabsFor(role).contains(tab);
    }

    /**
     * Tells whether the dashboard should show company-wide figures instead of
     * the user's own. True when the role can open any management tab.
     *
     * @param role the role text from users.csv
     * @return true for management roles, false for regular staff
     */
    public static boolean isManagement(String role) {
        return canOpen(role, NavTab.EMPLOYEE_MANAGER)
                || canOpen(role, NavTab.PAYROLL_MANAGER)
                || canOpen(role, NavTab.DISPUTE_MANAGER);
    }

    public static boolean isTechnical(String role) {
        return canOpen(role, NavTab.TICKET_MANAGER);
    }

    public static boolean canAddOrDeleteEmployee(String role) {
        String normalized = normalize(role);
        return normalized.equals("humanresources");
    }

    public static boolean canEditEmployee(String role) {
        String normalized = normalize(role);
        return normalized.equals("admin") || normalized.equals("humanresources");
    }

    public static boolean canDecideLeave(String role) {
        String normalized = normalize(role);
        return normalized.equals("admin");
    }

    private static String normalize(String role) {
        return role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
    }
}
