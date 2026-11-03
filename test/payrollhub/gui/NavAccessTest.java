package payrollhub.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class NavAccessTest {

    private static final List<NavTab> REGULAR_TABS = List.of(
            NavTab.DASHBOARD,
            NavTab.TIME_TRACKER,
            NavTab.SCHEDULE,
            NavTab.PAYSLIP);

    @Test
    void matchesEveryRoleInTheAccessMatrix() {
        Map<String, List<NavTab>> expectedByRole = new LinkedHashMap<>();
        expectedByRole.put("Regular", REGULAR_TABS);
        expectedByRole.put("Admin", List.of(
                NavTab.DASHBOARD, NavTab.TIME_TRACKER, NavTab.SCHEDULE, NavTab.PAYSLIP,
                NavTab.EMPLOYEES, NavTab.TICKET_MANAGER));
        expectedByRole.put("HumanResources", List.of(
                NavTab.DASHBOARD, NavTab.TIME_TRACKER, NavTab.SCHEDULE, NavTab.PAYSLIP,
                NavTab.EMPLOYEES, NavTab.TICKET_MANAGER));
        expectedByRole.put("HumanResources Admin", List.of(
                NavTab.DASHBOARD, NavTab.TIME_TRACKER, NavTab.SCHEDULE, NavTab.PAYSLIP,
                NavTab.EMPLOYEES, NavTab.TICKET_MANAGER));
        expectedByRole.put("Accounting", List.of(
                NavTab.DASHBOARD, NavTab.TIME_TRACKER, NavTab.SCHEDULE, NavTab.PAYSLIP,
                NavTab.PAYROLL_MANAGER, NavTab.TICKET_MANAGER));
        expectedByRole.put("Accounting Admin", List.of(
                NavTab.DASHBOARD, NavTab.TIME_TRACKER, NavTab.SCHEDULE, NavTab.PAYSLIP,
                NavTab.EMPLOYEES, NavTab.PAYROLL_MANAGER, NavTab.TICKET_MANAGER));
        expectedByRole.put("Chief Admin", List.of(
                NavTab.DASHBOARD, NavTab.TIME_TRACKER, NavTab.SCHEDULE, NavTab.PAYSLIP,
                NavTab.EMPLOYEES, NavTab.PAYROLL_MANAGER, NavTab.TICKET_MANAGER));

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

    @Test
    void nullEmptyAndUnknownRolesReceiveOnlyRegularTabs() {
        assertEquals(REGULAR_TABS, NavAccess.tabsFor(null));
        assertEquals(REGULAR_TABS, NavAccess.tabsFor(""));
        assertEquals(REGULAR_TABS, NavAccess.tabsFor("unknown"));
        assertFalse(NavAccess.canOpen(null, NavTab.EMPLOYEES));
        assertFalse(NavAccess.canOpen("", NavTab.PAYROLL_MANAGER));
        assertFalse(NavAccess.isManagement(null));
        assertFalse(NavAccess.isManagement(""));
    }

    @Test
    void trimsRoleAndMatchesItWithoutCaseSensitivity() {
        assertEquals(NavAccess.tabsFor("Chief Admin"), NavAccess.tabsFor("  chief admin  "));
        assertTrue(NavAccess.canOpen("  chief admin  ", NavTab.EMPLOYEES));
        assertTrue(NavAccess.canOpen("  chief admin  ", NavTab.PAYROLL_MANAGER));
        assertTrue(NavAccess.canOpen("  chief admin  ", NavTab.TICKET_MANAGER));
    }

    private static boolean isManagementTab(NavTab tab) {
        return tab == NavTab.EMPLOYEES
                || tab == NavTab.PAYROLL_MANAGER
                || tab == NavTab.TICKET_MANAGER;
    }
}
