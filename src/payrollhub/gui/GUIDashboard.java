package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.service.PayrollCalculator;

public class GUIDashboard extends JFrame {

    private static final String[] FORM_FIELDS = {
            "empNo", "lastName", "firstName", "birthday", "address", "phone",
            "sss", "philhealth", "tin", "pagibig", "status", "position",
            "supervisor", "basicSalary", "riceSubsidy", "phoneAllowance", "clothingAllowance"
    };
    private static final String[] FORM_LABELS = {
            "Employee number", "Last name", "First name", "Birthday (yyyy-MM-dd)",
            "Address", "Phone", "SSS", "PhilHealth", "TIN", "Pag-IBIG",
            "Status", "Position", "Supervisor", "Basic salary", "Rice subsidy",
            "Phone allowance", "Clothing allowance"
    };

    private final GUIUser user;
    private final Map<String, JTextField> fields = new LinkedHashMap<>();
    private final DefaultTableModel tableModel = new DefaultTableModel(0, 0);
    private final JTable employeeTable = new JTable(tableModel);
    private final JTextField searchField = GUIUtility.createTextField(12);
    private final JLabel salaryResult = new JLabel(" ");

    public GUIDashboard(GUIUser user) {
        super("Payroll Hub - Employee management");
        if (user == null) {
            throw new IllegalArgumentException("A signed-in user is required.");
        }
        if (!user.canManageEmployees()) {
            throw new IllegalArgumentException("This account cannot manage employee records.");
        }
        this.user = user;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        add(createHeader(), BorderLayout.NORTH);
        add(new JScrollPane(employeeTable), BorderLayout.CENTER);
        add(createForm(), BorderLayout.EAST);
        add(createActions(), BorderLayout.SOUTH);
        employeeTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedEmployee();
            }
        });
        setSize(1240, 720);
        setLocationRelativeTo(null);
        resetForm();
        reloadTable();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 14, 0, 14));
        header.add(GUIUtility.createHeaderLabel("Employee management"), BorderLayout.WEST);
        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        search.add(new JLabel("Employee no."));
        search.add(searchField);
        JButton searchButton = GUIUtility.createNavButton("Search");
        searchButton.addActionListener(event -> searchEmployee());
        search.add(searchButton);
        header.add(search, BorderLayout.EAST);
        return header;
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 4));
        form.setBorder(javax.swing.BorderFactory.createTitledBorder("Employee details"));
        for (int i = 0; i < FORM_FIELDS.length; i++) {
            JTextField field = GUIUtility.createTextField(14);
            fields.put(FORM_FIELDS[i], field);
            form.add(new JLabel(FORM_LABELS[i]));
            form.add(field);
        }
        return form;
    }

    private JPanel createActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = GUIUtility.createButton("Add employee", new java.awt.Color(40, 110, 75));
        addButton.addActionListener(event -> addEmployee());
        JButton updateButton = GUIUtility.createNavButton("Save changes");
        updateButton.addActionListener(event -> updateEmployee());
        JButton deleteButton = GUIUtility.createNavButton("Delete");
        deleteButton.setEnabled(user.getRole().equalsIgnoreCase("Admin"));
        deleteButton.addActionListener(event -> deleteSelectedRow());
        JButton payrollButton = GUIUtility.createNavButton("Calculate gross pay");
        payrollButton.addActionListener(event -> calculateSalary());
        JButton leaveButton = GUIUtility.createNavButton("Leave requests");
        leaveButton.addActionListener(event -> {
            new GUILeave(user).setVisible(true);
            dispose();
        });
        JButton timesheetButton = GUIUtility.createNavButton("Timesheet");
        timesheetButton.addActionListener(event -> {
            GUITimesheet timesheet = new GUITimesheet();
            timesheet.setLoggedIn(user);
            timesheet.setVisible(true);
            dispose();
        });
        JButton reloadButton = GUIUtility.createNavButton("Reload");
        reloadButton.addActionListener(event -> reloadTable());
        JButton signOutButton = GUIUtility.createNavButton("Sign out");
        signOutButton.addActionListener(event -> {
            new GUILogin().setVisible(true);
            dispose();
        });
        actions.add(addButton);
        actions.add(updateButton);
        actions.add(deleteButton);
        actions.add(payrollButton);
        actions.add(leaveButton);
        actions.add(timesheetButton);
        actions.add(reloadButton);
        actions.add(salaryResult);
        actions.add(signOutButton);
        return actions;
    }

    public Integer getSelectedEmployeeNo() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        return Integer.valueOf(employeeTable.getValueAt(selectedRow, 0).toString());
    }

    public EmployeeData getFormData() {
        String empNoText = fieldValue("empNo");
        String lastName = fieldValue("lastName");
        String firstName = fieldValue("firstName");
        String birthdayText = fieldValue("birthday");
        String address = fieldValue("address");
        String phone = fieldValue("phone");
        String sss = fieldValue("sss");
        String philhealth = fieldValue("philhealth");
        String tin = fieldValue("tin");
        String pagibig = fieldValue("pagibig");
        String status = fieldValue("status");
        String position = fieldValue("position");
        String supervisor = fieldValue("supervisor");
        try {
            return new EmployeeData(
                    Integer.parseInt(empNoText), lastName, firstName,
                    LocalDate.parse(birthdayText), address, phone, sss, philhealth,
                    tin, pagibig, status, position, supervisor,
                    Double.parseDouble(fieldValue("basicSalary")),
                    Double.parseDouble(fieldValue("riceSubsidy")),
                    Double.parseDouble(fieldValue("phoneAllowance")),
                    Double.parseDouble(fieldValue("clothingAllowance")));
        } catch (NumberFormatException | java.time.DateTimeException e) {
            throw new IllegalArgumentException("Enter valid numeric amounts and a birthday in yyyy-MM-dd format.", e);
        }
    }

    private String fieldValue(String name) {
        String value = fields.get(name).getText().trim();
        if (value.isEmpty() && !name.equals("supervisor")) {
            throw new IllegalArgumentException(FORM_LABELS[java.util.List.of(FORM_FIELDS).indexOf(name)]
                    + " is required.");
        }
        return value;
    }

    private void searchEmployee() {
        try {
            int empNo = Integer.parseInt(searchField.getText().trim());
            EmployeeData employee = CsvHandler.findEmployee(empNo).orElse(null);
            if (employee == null) {
                JOptionPane.showMessageDialog(this, "No employee matched that number.",
                        "Search", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            fillForm(employee);
            for (int row = 0; row < employeeTable.getRowCount(); row++) {
                if (Integer.parseInt(employeeTable.getValueAt(row, 0).toString()) == empNo) {
                    employeeTable.setRowSelectionInterval(row, row);
                    break;
                }
            }
        } catch (NumberFormatException e) {
            showError("Enter a valid employee number.");
        } catch (IOException | IllegalArgumentException e) {
            showError("Unable to search employees: " + e.getMessage());
        }
    }

    private void calculateSalary() {
        try {
            double gross = new PayrollCalculator().calculateGrossPay(getFormData());
            salaryResult.setText(String.format("Gross monthly pay: %.2f", gross));
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void addEmployee() {
        try {
            EmployeeData employee = getFormData();
            CsvHandler.saveEmployee(employee);
            reloadTable();
            resetForm();
            JOptionPane.showMessageDialog(this, "Employee added.", "Employee management",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalArgumentException e) {
            showError("Unable to add employee: " + e.getMessage());
        }
    }

    private void updateEmployee() {
        if (!user.canManageEmployees()) {
            showError("This account cannot update employee records.");
            return;
        }
        Integer selectedEmployeeNo = getSelectedEmployeeNo();
        if (selectedEmployeeNo == null) {
            showError("Select an employee before saving changes.");
            return;
        }
        try {
            EmployeeData employee = getFormData();
            if (employee.getEmpNo() != selectedEmployeeNo) {
                showError("The employee number cannot be changed while updating a record.");
                return;
            }
            if (!CsvHandler.updateEmployee(employee)) {
                showError("The selected employee no longer exists.");
                return;
            }
            reloadTable();
        } catch (IOException | IllegalArgumentException e) {
            showError("Unable to update employee: " + e.getMessage());
        }
    }

    private void deleteSelectedRow() {
        if (!user.getRole().equalsIgnoreCase("Admin")) {
            showError("Only an administrator can delete employee records.");
            return;
        }
        Integer empNo = getSelectedEmployeeNo();
        if (empNo == null) {
            showError("Select an employee before deleting.");
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(this,
                "Delete employee " + empNo + "?", "Confirm deletion",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (!CsvHandler.deleteEmployee(empNo.toString())) {
                showError("The selected employee no longer exists.");
                return;
            }
            reloadTable();
            resetForm();
        } catch (IOException e) {
            showError("Unable to delete employee: " + e.getMessage());
        }
    }

    private void reloadTable() {
        try {
            CsvHandler.loadEmployeeData(CsvHandler.EMPLOYEE_FILE.toString(), tableModel);
        } catch (IOException e) {
            showError("Unable to load employee records: " + e.getMessage());
        }
    }

    private void showSelectedEmployee() {
        Integer empNo = getSelectedEmployeeNo();
        if (empNo == null) {
            return;
        }
        try {
            CsvHandler.findEmployee(empNo).ifPresent(this::fillForm);
        } catch (IOException e) {
            showError("Unable to load employee details: " + e.getMessage());
        }
    }

    private void fillForm(EmployeeData employee) {
        fields.get("empNo").setText(Integer.toString(employee.getEmpNo()));
        fields.get("lastName").setText(employee.getLastName());
        fields.get("firstName").setText(employee.getFirstName());
        fields.get("birthday").setText(employee.getBirthday().toString());
        fields.get("address").setText(employee.getAddress());
        fields.get("phone").setText(employee.getPhone());
        fields.get("sss").setText(employee.getSss());
        fields.get("philhealth").setText(employee.getPhilhealth());
        fields.get("tin").setText(employee.getTin());
        fields.get("pagibig").setText(employee.getPagibig());
        fields.get("status").setText(employee.getStatus());
        fields.get("position").setText(employee.getPosition());
        fields.get("supervisor").setText(employee.getSupervisor());
        fields.get("basicSalary").setText(Double.toString(employee.getBasicSalary()));
        fields.get("riceSubsidy").setText(Double.toString(employee.getRiceSubsidy()));
        fields.get("phoneAllowance").setText(Double.toString(employee.getPhoneAllowance()));
        fields.get("clothingAllowance").setText(Double.toString(employee.getClothingAllowance()));
    }

    private void resetForm() {
        for (JTextField field : fields.values()) {
            field.setText("");
        }
        try {
            fields.get("empNo").setText(Integer.toString(
                    CsvHandler.getLastEmployeeNumber(CsvHandler.EMPLOYEE_FILE.toString()) + 1));
        } catch (IOException e) {
            showError("Unable to determine the next employee number: " + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Employee management",
                JOptionPane.ERROR_MESSAGE);
    }
}
