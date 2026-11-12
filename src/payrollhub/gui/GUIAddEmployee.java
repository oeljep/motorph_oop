package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;

/**
 * Add / edit employee dialog. In add mode it also creates the employee's
 * login (default password {@value #DEFAULT_PASSWORD}). Opened from
 * {@link GUIEmployeeManager}; the caller checks NavAccess permissions first.
 */
public class GUIAddEmployee extends JDialog {

    private static final String DEFAULT_PASSWORD = "Motorph123";
    private static final String[] ROLES = { "Regular", "Accounting", "HumanResources",
            "Admin", "InfoTech" };

    private final EmployeeData existing;
    private boolean saved;

    private final JTextField empNoField = GUIUtility.createTextField(20);
    private final JTextField lastName = GUIUtility.createTextField(20);
    private final JTextField firstName = GUIUtility.createTextField(20);
    private final JTextField birthday = GUIUtility.createTextField(20);
    private final JTextField address = GUIUtility.createTextField(20);
    private final JTextField phone = GUIUtility.createTextField(20);
    private final JTextField sss = GUIUtility.createTextField(20);
    private final JTextField philhealth = GUIUtility.createTextField(20);
    private final JTextField tin = GUIUtility.createTextField(20);
    private final JTextField pagibig = GUIUtility.createTextField(20);
    private final JTextField status = GUIUtility.createTextField(20);
    private final JTextField position = GUIUtility.createTextField(20);
    private final JComboBox<String> supervisorBox = new JComboBox<>();
    private final JTextField basic = GUIUtility.createTextField(20);
    private final JTextField rice = GUIUtility.createTextField(20);
    private final JTextField phoneAllowance = GUIUtility.createTextField(20);
    private final JTextField clothing = GUIUtility.createTextField(20);
    private final JComboBox<String> roleBox = new JComboBox<>(ROLES);
    private final JTextField userName = GUIUtility.createTextField(20);
    private final JLabel message = new JLabel(" ");

    /**
     * @param owner    parent window
     * @param existing the employee to edit, or null to add a new one
     */
    public GUIAddEmployee(Window owner, EmployeeData existing) {
        super(owner, existing == null ? "Add employee" : "Edit employee", Dialog.ModalityType.APPLICATION_MODAL);
        this.existing = existing;

        supervisorBox.setEditable(true);
        supervisorBox.addItem("N/A");
        try {
            for (EmployeeData e : CsvHandler.loadEmployees()) {
                supervisorBox.addItem(e.getLastName() + ", " + e.getFirstName());
            }
        } catch (IOException e) {
            message.setText("Could not read employees: " + e.getMessage());
        }

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        int row = 0;
        row = add(grid, row, "Employee #", empNoField);
        row = add(grid, row, "Last Name", lastName);
        row = add(grid, row, "First Name", firstName);
        row = add(grid, row, "Birthday (YYYY-MM-DD)", birthday);
        row = add(grid, row, "Address", address);
        row = add(grid, row, "Phone", phone);
        row = add(grid, row, "SSS", sss);
        row = add(grid, row, "PhilHealth", philhealth);
        row = add(grid, row, "TIN", tin);
        row = add(grid, row, "Pag-Ibig", pagibig);
        row = add(grid, row, "Status", status);
        row = add(grid, row, "Position", position);
        row = add(grid, row, "Supervisor", supervisorBox);
        row = add(grid, row, "Basic Salary", basic);
        row = add(grid, row, "Rice Subsidy", rice);
        row = add(grid, row, "Phone Allowance", phoneAllowance);
        row = add(grid, row, "Clothing Allowance", clothing);
        if (existing == null) {
            row = add(grid, row, "Role", roleBox);
            row = add(grid, row, "Username", userName);
        }
        empNoField.setEditable(false);

        JButton save = GUIUtility.createActionButton("Save", GUIUtility.BRAND_RED);
        JButton cancel = GUIUtility.createActionButton("Cancel", GUIUtility.INK_DARK);
        save.addActionListener(event -> save());
        cancel.addActionListener(event -> dispose());
        message.setForeground(new java.awt.Color(0xB3261E));
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        footer.add(message);
        footer.add(cancel);
        footer.add(save);

        add(new JScrollPane(grid), BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        fillFields();
        setSize(520, 700);
        setLocationRelativeTo(owner);
    }

    private static int add(JPanel grid, int row, String label, java.awt.Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(4, 0, 4, 12);
        c.anchor = GridBagConstraints.WEST;
        grid.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        grid.add(field, c);
        return row + 1;
    }

    private void fillFields() {
        if (existing == null) {
            try {
                empNoField.setText(String.valueOf(
                        CsvHandler.getLastEmployeeNumber(CsvHandler.EMPLOYEE_FILE.toString()) + 1));
            } catch (IOException e) {
                message.setText(e.getMessage());
            }
            status.setText("New");
            return;
        }
        empNoField.setText(String.valueOf(existing.getEmpNo()));
        lastName.setText(existing.getLastName());
        firstName.setText(existing.getFirstName());
        birthday.setText(existing.getBirthday().toString());
        address.setText(existing.getAddress());
        phone.setText(existing.getPhone());
        sss.setText(existing.getSss());
        philhealth.setText(existing.getPhilhealth());
        tin.setText(existing.getTin());
        pagibig.setText(existing.getPagibig());
        status.setText(existing.getStatus());
        position.setText(existing.getPosition());
        supervisorBox.setSelectedItem(existing.getSupervisor());
        basic.setText(String.valueOf(existing.getBasicSalary()));
        rice.setText(String.valueOf(existing.getRiceSubsidy()));
        phoneAllowance.setText(String.valueOf(existing.getPhoneAllowance()));
        clothing.setText(String.valueOf(existing.getClothingAllowance()));
    }

    /** @return true when a record was added or changed */
    public boolean isSaved() {
        return saved;
    }

    private void save() {
        try {
            if (lastName.getText().isBlank() || firstName.getText().isBlank() || position.getText().isBlank()) {
                message.setText("Name and position are required.");
                return;
            }
            EmployeeData e = new EmployeeData(Integer.parseInt(empNoField.getText().trim()),
                    lastName.getText().trim(), firstName.getText().trim(),
                    LocalDate.parse(birthday.getText().trim()), address.getText().trim(), phone.getText().trim(),
                    sss.getText().trim(), philhealth.getText().trim(), tin.getText().trim(),
                    pagibig.getText().trim(), status.getText().trim(), position.getText().trim(),
                    String.valueOf(supervisorBox.getSelectedItem()).trim(),
                    Double.parseDouble(basic.getText().trim()), Double.parseDouble(rice.getText().trim()),
                    Double.parseDouble(phoneAllowance.getText().trim()),
                    Double.parseDouble(clothing.getText().trim()));
            if (existing != null) {
                CsvHandler.updateEmployee(e);
            } else {
                String user = userName.getText().trim().isEmpty() ? defaultUserName() : userName.getText().trim();
                if (CsvHandler.findUser(user).isPresent()) {
                    message.setText("Username '" + user + "' is taken.");
                    return;
                }
                CsvHandler.saveEmployee(e);
                CsvHandler.saveUser(e.getEmpNo(), user, DEFAULT_PASSWORD, (String) roleBox.getSelectedItem());
                JOptionPane.showMessageDialog(this, "Login created: " + user + " / " + DEFAULT_PASSWORD
                        + "\nAsk the employee to change the password.", "Employee added",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            saved = true;
            dispose();
        } catch (NumberFormatException ex) {
            message.setText("Salary and allowances must be numbers.");
        } catch (DateTimeParseException ex) {
            message.setText("Birthday must be YYYY-MM-DD.");
        } catch (IOException | IllegalArgumentException ex) {
            message.setText(ex.getMessage());
        }
    }

    private String defaultUserName() {
        String first = firstName.getText().trim().toLowerCase().replace(" ", "");
        String last = lastName.getText().trim().toLowerCase();
        return first + (last.isEmpty() ? "" : last.charAt(0));
    }
}
