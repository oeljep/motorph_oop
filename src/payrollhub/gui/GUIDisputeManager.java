package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.data.TicketData;

/**
 * Ticket Manager tab: tickets employees filed (for example from the Payslip
 * tab). Staff open a ticket, add a reply, and mark it resolved or rejected.
 */
public class GUIDisputeManager extends JPanel {

    private static final String[] COLUMNS = { "Ticket ID", "Employee", "Subject", "Date",
            "Processed by", "Status", "Remarks" };
    private static final String[] FILTERS = { "All", TicketData.OPEN, TicketData.IN_PROGRESS,
            TicketData.RESOLVED, TicketData.REJECTED };

    private final int empNo;
    private final Map<Integer, String> names = new HashMap<>();
    private List<TicketData> tickets = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField searchField = GUIUtility.createTextField(14);
    private final JComboBox<String> statusBox = new JComboBox<>(FILTERS);

    /**
     * @param empNo the signed-in staff member's employee number
     */
    public GUIDisputeManager(int empNo) {
        this.empNo = empNo;
        setBackground(GUIUtility.PAGE_BACKGROUND);
        setLayout(new BorderLayout(0, GUIUtility.scale(20)));
        setBorder(BorderFactory.createEmptyBorder(
                GUIUtility.scale(40), GUIUtility.scale(60), GUIUtility.scale(40), GUIUtility.scale(60)));

        GUIUtility.styleTable(table);
        table.setRowSorter(sorter);
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        };
        searchField.getDocument().addDocumentListener(listener);
        statusBox.addActionListener(event -> applyFilter());

        JButton open = GUIUtility.createActionButton("Open ticket", GUIUtility.BRAND_RED);
        open.addActionListener(event -> openSelected());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);
        controls.add(new JLabel("Search"));
        controls.add(searchField);
        controls.add(statusBox);
        controls.add(open);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(GUIUtility.createPageTitle("Ticket Manager"), BorderLayout.WEST);
        top.add(controls, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        add(GUIUtility.wrapTable(table), BorderLayout.CENTER);
        reload();
    }

    private void applyFilter() {
        List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();
        String text = searchField.getText().trim();
        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
        String status = (String) statusBox.getSelectedItem();
        if (status != null && !"All".equals(status)) {
            filters.add(RowFilter.regexFilter("^" + status + "$", 5));
        }
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private void reload() {
        model.setRowCount(0);
        try {
            names.clear();
            for (EmployeeData e : CsvHandler.loadEmployees()) {
                names.put(e.getEmpNo(), e.getFullName());
            }
            tickets = new ArrayList<>(CsvHandler.loadTickets());
            tickets.sort(Comparator.comparing(TicketData::isActive).reversed()
                    .thenComparing(TicketData::getTicketId, Comparator.reverseOrder()));
            for (TicketData t : tickets) {
                String remarks = t.getRemarks().replace("\\n", " | ");
                model.addRow(new Object[] { t.getTicketId(),
                        names.getOrDefault(t.getEmpNo(), String.valueOf(t.getEmpNo())), t.getSubject(),
                        t.getCreatedDate(), t.getProcessedBy(), t.getStatus(), remarks });
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not read tickets: " + e.getMessage(), "Tickets",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a ticket first.", "Tickets",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = (String) model.getValueAt(table.convertRowIndexToModel(row), 0);
        for (TicketData t : tickets) {
            if (t.getTicketId().equals(id)) {
                showTicket(t);
                return;
            }
        }
    }

    private void showTicket(TicketData ticket) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Ticket " + ticket.getTicketId(),
                Dialog.ModalityType.APPLICATION_MODAL);
        String who = names.getOrDefault(ticket.getEmpNo(), String.valueOf(ticket.getEmpNo()));

        JLabel header = new JLabel("<html><b>" + escape(ticket.getSubject()) + "</b><br>From " + escape(who)
                + " on " + ticket.getCreatedDate() + " - " + ticket.getStatus() + "</html>");
        header.setFont(GUIUtility.bodyFont(Font.PLAIN, 14f));

        JPanel thread = new JPanel();
        thread.setLayout(new javax.swing.BoxLayout(thread, javax.swing.BoxLayout.Y_AXIS));
        thread.setBackground(java.awt.Color.WHITE);
        thread.add(bubble(who, ticket.getMessage(), false));
        for (String line : ticket.getRemarks().split("\\\\n")) {
            if (!line.isBlank()) {
                thread.add(bubble("Support", line, true));
            }
        }

        JTextArea reply = new JTextArea(3, 30);
        reply.setLineWrap(true);
        reply.setWrapStyleWord(true);
        JButton send = GUIUtility.createActionButton("Send reply", GUIUtility.INK_DARK);
        JButton resolve = GUIUtility.createActionButton("Resolve", new java.awt.Color(0x1E7A46));
        JButton reject = GUIUtility.createActionButton("Reject", GUIUtility.BRAND_RED);
        boolean active = ticket.isActive();
        send.setEnabled(active);
        resolve.setEnabled(active);
        reject.setEnabled(active);

        send.addActionListener(event -> {
            if (reply.getText().isBlank()) {
                return;
            }
            ticket.addRemark(reply.getText().trim());
            ticket.setProcessedBy(staffName());
            if (TicketData.OPEN.equals(ticket.getStatus())) {
                ticket.setStatus(TicketData.IN_PROGRESS);
            }
            if (persist(ticket)) {
                dialog.dispose();
            }
        });
        resolve.addActionListener(event -> close(ticket, TicketData.RESOLVED, reply, dialog));
        reject.addActionListener(event -> close(ticket, TicketData.REJECTED, reply, dialog));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.add(send);
        buttons.add(resolve);
        buttons.add(reject);
        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        south.add(new JScrollPane(reply), BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        JPanel north = new JPanel(new BorderLayout());
        north.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        north.add(header);
        dialog.add(north, BorderLayout.NORTH);
        dialog.add(new JScrollPane(thread), BorderLayout.CENTER);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.setSize(560, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void close(TicketData ticket, String status, JTextArea reply, JDialog dialog) {
        if (!reply.getText().isBlank()) {
            ticket.addRemark(reply.getText().trim());
        }
        ticket.setStatus(status);
        ticket.setProcessedBy(staffName());
        if (persist(ticket)) {
            dialog.dispose();
        }
    }

    private boolean persist(TicketData ticket) {
        try {
            CsvHandler.updateTicket(ticket);
            reload();
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save: " + e.getMessage(), "Tickets",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private String staffName() {
        return names.getOrDefault(empNo, String.valueOf(empNo));
    }

    private static JPanel bubble(String author, String text, boolean right) {
        JLabel label = new JLabel("<html><div style='width:320px'><b>" + escape(author) + "</b><br>"
                + escape(text) + "</div></html>");
        label.setFont(GUIUtility.bodyFont(Font.PLAIN, 13f));
        label.setOpaque(true);
        label.setBackground(right ? new java.awt.Color(0xF3DDE3) : new java.awt.Color(0xEFEFEF));
        label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JPanel row = new JPanel(new FlowLayout(right ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        row.add(label);
        row.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        return row;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
