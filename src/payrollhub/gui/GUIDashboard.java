package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.List;
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

    private final GUIUser user;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final GUISidebar sidebar;

    /**
     * Opens the dashboard for a signed-in user.
     *
     * @param user the authenticated user; its role decides the sidebar tabs
     */
    public GUIDashboard(GUIUser user) {
        super("Payroll Hub");
        this.user = user;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        List<NavTab> tabs = NavAccess.tabsFor(user.getRole());
        for (NavTab tab : tabs) {
            content.add(createScreen(tab), tab.name());
        }
        content.setBackground(GUIUtility.PAGE_BACKGROUND);

        sidebar = new GUISidebar(user.getRole(), tab -> cards.show(content, tab.name()));
        GUIHeader header = new GUIHeader(firstNameOf(user), this::logout);

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
        if (!NavAccess.canOpen(user.getRole(), tab)) {
            throw new IllegalArgumentException("Role cannot open tab: " + tab);
        }

        switch (tab) {
            case DASHBOARD:
                return new GUIDashboardHome(user.getRole(), user.getEmpNo());
            // TODO: replace each placeholder with its real screen as it is built.
            case TIME_TRACKER:
            case SCHEDULE:
            case PAYSLIP:
            case EMPLOYEES:
            case PAYROLL_MANAGER:
            case TICKET_MANAGER:
            default:
                return createPlaceholder(tab);
        }
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

    /** First name of the signed-in employee, or the role name if no record is found. */
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

    /** Returns to the sign-in screen and closes this window. */
    private void logout() {
        new GUILogin().setVisible(true);
        dispose();
    }
}
