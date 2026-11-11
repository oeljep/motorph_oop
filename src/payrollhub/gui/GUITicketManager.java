package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * A simple desktop interface for managing employee support tickets.
 */
public class GUITicketManager extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JTextField employeeField = new JTextField();
    private final JTextField subjectField = new JTextField();
    private final JComboBox<String> statusBox = new JComboBox<>(
            new String[] { "Open", "In Progress", "Resolved" });
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] { "Ticket ID", "Employee", "Subject", "Status" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable ticketTable = new JTable(tableModel);
    private int nextTicketId = 1;

    public GUITicketManager() {
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("Ticket Details"));
        form.add(new JLabel("Employee:"));
        form.add(employeeField);
        form.add(new JLabel("Subject:"));
        form.add(subjectField);
        form.add(new JLabel("Status:"));
        form.add(statusBox);
        add(form, BorderLayout.NORTH);

        ticketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(ticketTable), BorderLayout.CENTER);

        JButton addButton = new JButton("Add Ticket");
        JButton updateButton = new JButton("Update Ticket");
        JButton deleteButton = new JButton("Delete Ticket");
        JButton clearButton = new JButton("Clear");
        JPanel buttons = new JPanel();
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);
        add(buttons, BorderLayout.SOUTH);

        addButton.addActionListener(event -> addTicket());
        updateButton.addActionListener(event -> updateTicket());
        deleteButton.addActionListener(event -> deleteTicket());
        clearButton.addActionListener(event -> clearForm());
        ticketTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedTicket();
            }
        });

        setSize(650, 400);
    }

    private void addTicket() {
        String employee = employeeField.getText().trim();
        String subject = subjectField.getText().trim();
        if (employee.isEmpty() || subject.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter an employee and subject.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        tableModel.addRow(new Object[] { nextTicketId++, employee, subject, statusBox.getSelectedItem() });
        clearForm();
    }

    private void updateTicket() {
        int row = ticketTable.getSelectedRow();
        String employee = employeeField.getText().trim();
        String subject = subjectField.getText().trim();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a ticket to update.");
            return;
        }
        if (employee.isEmpty() || subject.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter an employee and subject.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        tableModel.setValueAt(employee, row, 1);
        tableModel.setValueAt(subject, row, 2);
        tableModel.setValueAt(statusBox.getSelectedItem(), row, 3);
        clearForm();
    }

    private void deleteTicket() {
        int row = ticketTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a ticket to delete.");
            return;
        }
        tableModel.removeRow(row);
        clearForm();
    }

    private void loadSelectedTicket() {
        int row = ticketTable.getSelectedRow();
        if (row >= 0) {
            employeeField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
            subjectField.setText(String.valueOf(tableModel.getValueAt(row, 2)));
            statusBox.setSelectedItem(tableModel.getValueAt(row, 3));
        }
    }

    private void clearForm() {
        ticketTable.clearSelection();
        employeeField.setText("");
        subjectField.setText("");
        statusBox.setSelectedIndex(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Ticket Manager");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(new GUITicketManager());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}