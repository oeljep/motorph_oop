package payrollhub.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import payrollhub.gui.NavAccess;
import payrollhub.gui.NavTab;

class NavAccessTest {

    private static final List<NavTab> REGULAR_TABS = List.of(
            NavTab.DASHBOARD,
            NavTab.TIME_TRACKER,
            NavTab.SCHEDULE,
            NavTab.PAYSLIP);

    private NavAccessTest() {
    }

    public static void main(String[] args) {
        NavAccessTest test = new NavAccessTest();
        test.matchesEveryRoleInTheAccessMatrix();
        test.nullEmptyAndUnknownRolesReceiveOnlyRegularTabs();
        test.trimsRoleAndMatchesItWithoutCaseSensitivity();
        System.out.println("NavAccess checks passed.");
    }

    void matchesEveryRoleInTheAccessMatrix() {
        Map<String, List<NavTab>> expectedByRole = new LinkedHashMap<>();
        expectedByRole.put("Regular", REGULAR_TABS);
        expectedByRole.put("Admin", List.of(
                NavTab.DASHBOARD, NavTab.TIME_TRACKER, NavTab.SCHEDULE, NavTab.PAYSLIP,
                NavTab.EMPLOYEE_MANAGER, NavTab.LEAVE_MANAGER));
        expectedByRole.put("HumanResources", List.of(
                NavTab.DASHBOARD, NavTab.TIME_TRACKER, NavTab.SCHEDULE, NavTab.PAYSLIP,
                NavTab.EMPLOYEE_MANAGER));
        expectedByRole.put("Accounting", List.of(
                NavTab.DASHBOARD, NavTab.TIME_TRACKER, NavTab.SCHEDULE, NavTab.PAYSLIP,
                NavTab.PAYROLL_MANAGER, NavTab.DISPUTE_MANAGER));
        expectedByRole.put("InfoTech", List.of(
                NavTab.DASHBOARD, NavTab.TIME_TRACKER, NavTab.SCHEDULE, NavTab.PAYSLIP,
                NavTab.TICKET_MANAGER));

        for (Map.Entry<String, List<NavTab>> roleAccess : expectedByRole.entrySet()) {
            String role = roleAccess.getKey();
            List<NavTab> expected = roleAccess.getValue();

            assertEquals(expected, NavAccess.tabsFor(role), role);
            for (NavTab tab : NavTab.values()) {
                assertEquals(expected.contains(tab), NavAccess.canOpen(role, tab), role + " / " + tab);
            }
            assertEquals(expected.stream().anyMatch(NavAccessTest::isManagementTab),
                    NavAccess.isManagement(role), role);
        }
    }

    void nullEmptyAndUnknownRolesReceiveOnlyRegularTabs() {
        assertEquals(REGULAR_TABS, NavAccess.tabsFor(null));
        assertEquals(REGULAR_TABS, NavAccess.tabsFor(""));
        assertEquals(REGULAR_TABS, NavAccess.tabsFor("unknown"));
        assertFalse(NavAccess.canOpen(null, NavTab.EMPLOYEE_MANAGER));
        assertFalse(NavAccess.canOpen("", NavTab.PAYROLL_MANAGER));
        assertFalse(NavAccess.isManagement(null));
        assertFalse(NavAccess.isManagement(""));
    }

    void trimsRoleAndMatchesItWithoutCaseSensitivity() {
        assertEquals(NavAccess.tabsFor("Admin"), NavAccess.tabsFor("  admin  "));
        assertTrue(NavAccess.canOpen("Admin", NavTab.EMPLOYEE_MANAGER));
        assertFalse(NavAccess.canOpen("Admin", NavTab.DISPUTE_MANAGER));
        assertTrue(NavAccess.canOpen("Admin", NavTab.LEAVE_MANAGER));
        assertTrue(NavAccess.canDecideLeave("Admin"));
        assertFalse(NavAccess.canDecideLeave("HumanResources"));
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, "values differ");
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("expected condition to be true");
        }
    }

    private static void assertFalse(boolean condition) {
        if (condition) {
            throw new AssertionError("expected condition to be false");
        }
    }

    private static boolean isManagementTab(NavTab tab) {
        return tab == NavTab.EMPLOYEE_MANAGER
                || tab == NavTab.PAYROLL_MANAGER
                || tab == NavTab.DISPUTE_MANAGER;
    }
}
