package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;

/**
 * Employees tab: searchable employee list. Everyone who can open the tab can
 * view a profile; Add, Edit and Delete follow {@link ActionAccess}.
 */
public class GUIEmployees extends JPanel {

    private static final String[] COLUMNS = { "Emp #", "Last name", "First name", "Position", "Status", "Supervisor" };

    private final String role;
    private final int empNo;
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? Integer.class : String.class;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField searchField = GUIUtility.createTextField(16);

    /**
     * Builds the tab.
     *
     * @param role  the signed-in user's role
     * @param empNo the signed-in employee's number
     */
    public GUIEmployees(String role, int empNo) {
        this.role = role;
        this.empNo = empNo;
        setBackground(GUIUtility.PAGE_BACKGROUND);
        setLayout(new BorderLayout(0, GUIUtility.scale(20)));
        setBorder(BorderFactory.createEmptyBorder(
                GUIUtility.scale(40), GUIUtility.scale(60), GUIUtility.scale(40), GUIUtility.scale(60)));

        GUIUtility.styleTable(table);
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centered);
        table.getColumnModel().getColumn(0).setMaxWidth(90);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void apply() {
                String text = searchField.getText().trim();
                sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                apply();
            }
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);
        controls.add(new JLabel("Search"));
        controls.add(searchField);
        JButton view = GUIUtility.createActionButton("View", GUIUtility.INK_DARK);
        view.addActionListener(event -> viewSelected());
        controls.add(view);
        if (ActionAccess.canAddOrDeleteEmployee(role)) {
            JButton add = GUIUtility.createActionButton("Add", GUIUtility.BRAND_RED);
            add.addActionListener(event -> openForm(null));
            controls.add(add);
        }
        if (ActionAccess.canEditEmployee(role)) {
            JButton edit = GUIUtility.createActionButton("Edit", GUIUtility.INK_DARK);
            edit.addActionListener(event -> {
                EmployeeData e = selected();
                if (e != null) {
                    openForm(e);
                }
            });
            controls.add(edit);
        }
        if (ActionAccess.canAddOrDeleteEmployee(role)) {
            JButton delete = GUIUtility.createActionButton("Delete", GUIUtility.BRAND_RED);
            delete.addActionListener(event -> deleteSelected());
            controls.add(delete);
        }

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(GUIUtility.createPageTitle("Employees"), BorderLayout.WEST);
        top.add(controls, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        add(GUIUtility.wrapTable(table), BorderLayout.CENTER);
        reload();
    }

    private void reload() {
        model.setRowCount(0);
        try {
            List<EmployeeData> all = CsvHandler.loadEmployees();
            for (EmployeeData e : all) {
                model.addRow(new Object[] { e.getEmpNo(), e.getLastName(), e.getFirstName(),
                        e.getPosition(), e.getStatus(), e.getSupervisor() });
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not read employees: " + e.getMessage(),
                    "Employees", JOptionPane.ERROR_MESSAGE);
        }
    }

    private EmployeeData selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an employee first.", "Employees",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        int number = (Integer) model.getValueAt(table.convertRowIndexToModel(row), 0);
        try {
            return CsvHandler.findEmployee(number).orElse(null);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Employees", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void viewSelected() {
        EmployeeData e = selected();
        if (e == null) {
            return;
        }
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), e.getFullName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JScrollPane scroll = new JScrollPane(new GUIViewProfile(e.getEmpNo()));
        scroll.setBorder(null);
        dialog.add(scroll);
        dialog.setSize(900, 760);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openForm(EmployeeData existing) {
        GUIAddEmployee dialog = new GUIAddEmployee(SwingUtilities.getWindowAncestor(this), existing);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            reload();
        }
    }

    private void deleteSelected() {
        if (!ActionAccess.canAddOrDeleteEmployee(role)) {
            return;
        }
        EmployeeData e = selected();
        if (e == null) {
            return;
        }
        if (e.getEmpNo() == empNo) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own record.", "Employees",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete " + e.getFullName() + " and their login? This cannot be undone.",
                "Delete employee", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            CsvHandler.deleteEmployee(String.valueOf(e.getEmpNo()));
            CsvHandler.deleteUser(e.getEmpNo());
            reload();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not delete: " + ex.getMessage(), "Employees",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
