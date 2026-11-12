package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;

/**
 * Main window after sign-in. It is a shell: {@link GUIHeader} on top,
 * {@link GUISidebar} on the left, and a card area that shows one screen at a
 * time. The sidebar tabs come from {@link NavAccess}, so the role in
 * users.csv decides what each person can open.
 *
 * To plug a finished screen into a tab, return it from
 * {@link #createScreen(NavTab)}. Tabs without a screen yet show a placeholder.
 */
public class GUIDashboard extends JFrame {

    private static final String PROFILE_CARD = "PROFILE";

    private final GUIUser user;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final GUISidebar sidebar;
    private final java.util.Map<String, JPanel> screens = new java.util.HashMap<>();

    /**
     * Opens the dashboard for a signed-in user.
     *
     * @param user the authenticated user; its role decides the sidebar tabs
     */
    public GUIDashboard(GUIUser user) {
        super("Payroll Hub");
        this.user = user;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        content.setBackground(GUIUtility.PAGE_BACKGROUND);

        sidebar = new GUISidebar(user.getRole(), this::showTab);
        GUIHeader header = new GUIHeader(firstNameOf(user), this::logout, this::showProfile);

        JPanel root = new JPanel(new BorderLayout());
        root.add(header, BorderLayout.NORTH);
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        setMinimumSize(new Dimension(1100, 700));
        setSize(1320, 820);
        setLocationRelativeTo(null);
        sidebar.select(NavTab.DASHBOARD);
    }

    /**
     * Builds the screen for one tab.
     *
     * @param tab the tab being created
     * @return the panel to show when that tab is selected
     */
    private JPanel createScreen(NavTab tab) {
        switch (tab) {
            case DASHBOARD:
                return new GUIDashboardHome(user.getRole(), user.getEmpNo());
            case TIME_TRACKER:
                return new GUITimesheet(user.getEmpNo());
            case SCHEDULE:
                return new GUILeave(user.getRole(), user.getEmpNo());
            case PAYSLIP:
                return new GUIPayslip(user.getEmpNo());
            case EMPLOYEE_MANAGER:
                return new GUIEmployeeManager(user.getRole(), user.getEmpNo());
            case PAYROLL_MANAGER:
                return new GUIPayrollManager();
            case DISPUTE_MANAGER:
                return new GUIDisputeManager(user.getEmpNo());
            case LEAVE_MANAGER:
                return new GUILeaveManager(user.getRole(), user.getEmpNo());
            case TICKET_MANAGER:
                return new GUITicketManager(user.getEmpNo());
            default:
                return createPlaceholder(tab);
        }

    }

    /**
     * Opens a tab after re-checking the role, and rebuilds its screen so the
     * data is fresh each time.
     */
    private void showTab(NavTab tab) {
        if (!NavAccess.tabsFor(user.getRole()).contains(tab)) {
            return;
        }
        showCard(tab.name(), createScreen(tab));
    }

    private void showCard(String name, JPanel screen) {
        JPanel old = screens.put(name, screen);
        if (old != null) {
            content.remove(old);
        }
        content.add(screen, name);
        content.revalidate();
        cards.show(content, name);
    }

    private JPanel createPlaceholder(NavTab tab) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(GUIUtility.PAGE_BACKGROUND);
        JLabel label = new JLabel(tab.getLabel() + " is coming soon", SwingConstants.CENTER);
        label.setFont(GUIUtility.bodyFont(Font.PLAIN, 20f));
        label.setForeground(GUIUtility.MUTED_TEXT);
        panel.add(label);
        return panel;
    }

    /**
     * First name of the signed-in employee, or the role name if no record is found.
     */
    private static String firstNameOf(GUIUser user) {
        try {
            Optional<EmployeeData> employee = CsvHandler.findEmployee(user.getEmpNo());
            if (employee.isPresent()) {
                return employee.get().getFirstName();
            }
        } catch (IOException e) {
            System.err.println("Could not read employee record: " + e.getMessage());
        }
        return user.getRole();
    }

    /** Shows the signed-in user's profile and clears the sidebar highlight. */
    private void showProfile() {
        sidebar.clearSelection();
        showCard(PROFILE_CARD, new GUIViewProfile(user.getEmpNo()));
    }

    /** Returns to the sign-in screen and closes this window. */
    private void logout() {
        new GUILogin().setVisible(true);
        dispose();
    }
}
