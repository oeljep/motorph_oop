package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;
import payrollhub.data.LeaveData;

/**
 * Schedule tab, after the Figma "Schedule" frame: a month calendar that marks
 * the signed-in employee's time off, a form to file a leave request, and the
 * list of requests.
 *
 * Every employee sees and files their own requests. All file access goes
 * through {@link CsvHandler}.
 */
public class GUILeave extends JPanel {

    private static final Color APPROVED_FILL = new Color(0xF3DDE3);
    private static final Color PENDING_FILL = new Color(0xFFF1C9);
    private static final Color WEEKEND_FILL = new Color(0xEFEFEF);
    private static final Color OK_TEXT = new Color(0x1E7A46);
    private static final Color ERROR_TEXT = new Color(0xB3261E);
    private static final String[] COLUMNS = { "ID", "Employee", "Type", "From", "To", "Status" };
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM yy", Locale.ENGLISH);

    private final int empNo;
    private YearMonth month = YearMonth.now();

    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 4, 4));
    private final DefaultTableModel requestModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable requestTable = new JTable(requestModel) {
        @Override
        public String getToolTipText(MouseEvent event) {
            int row = rowAtPoint(event.getPoint());
            if (row < 0 || row >= shown.size()) {
                return null;
            }
            String reason = shown.get(convertRowIndexToModel(row)).getReason();
            return reason.isEmpty() ? null : reason;
        }
    };
    private final JComboBox<String> typeBox = new JComboBox<>(
            new String[] { "Vacation", "Sick", "Emergency", "Personal" });
    private final JSpinner startSpinner = createDateSpinner();
    private final JSpinner endSpinner = createDateSpinner();
    private final JTextField reasonField = GUIUtility.createTextField(18);
    private final JLabel formMessage = new JLabel(" ");

    private List<LeaveData> shown = new ArrayList<>();
    private List<LeaveData> mine = new ArrayList<>();
    private final Map<Integer, String> names = new HashMap<>();

    /**
     * Builds the tab for one employee.
     *
     * @param role  the signed-in user's role from users.csv
     * @param empNo the signed-in employee's number
     */
    public GUILeave(String role, int empNo) {
        this.empNo = empNo;

        setBackground(GUIUtility.PAGE_BACKGROUND);
        setLayout(new BorderLayout(0, GUIUtility.scale(20)));
        setBorder(BorderFactory.createEmptyBorder(
                GUIUtility.scale(40), GUIUtility.scale(60), GUIUtility.scale(40), GUIUtility.scale(60)));

        GUIUtility.styleTable(requestTable);
        requestTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        int[] widths = { 55, 140, 80, 80, 80, 70 };
        for (int i = 0; i < widths.length; i++) {
            requestTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        add(GUIUtility.createPageTitle("Schedule"), BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.weightx = 0.4;
        gc.insets = new Insets(0, 0, 0, GUIUtility.scale(24));
        columns.add(createCalendarCard(), gc);
        gc.gridx = 1;
        gc.weightx = 0.6;
        gc.insets = new Insets(0, 0, 0, 0);
        columns.add(createRightColumn(), gc);
        add(columns, BorderLayout.CENTER);

        reload();
    }

    // ---------------------- Layout ----------------------

    private JPanel createCalendarCard() {
        JButton previous = GUIUtility.createLinkButton("<", GUIUtility.FIELD_TEXT,
                GUIUtility.bodyFont(Font.BOLD, 20f));
        JButton next = GUIUtility.createLinkButton(">", GUIUtility.FIELD_TEXT,
                GUIUtility.bodyFont(Font.BOLD, 20f));
        previous.addActionListener(event -> changeMonth(-1));
        next.addActionListener(event -> changeMonth(1));
        monthLabel.setFont(GUIUtility.bodyFont(Font.BOLD, 18f));

        JPanel nav = new JPanel(new BorderLayout());
        nav.setOpaque(false);
        nav.add(previous, BorderLayout.WEST);
        nav.add(monthLabel, BorderLayout.CENTER);
        nav.add(next, BorderLayout.EAST);

        calendarGrid.setOpaque(false);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        legend.setOpaque(false);
        legend.add(legendItem(Color.WHITE, "Working day"));
        legend.add(legendItem(WEEKEND_FILL, "Weekend"));
        legend.add(legendItem(APPROVED_FILL, "Time off"));
        legend.add(legendItem(PENDING_FILL, "Pending"));

        JPanel card = GUIUtility.createCardPanel(new BorderLayout(0, GUIUtility.scale(14)));
        card.add(nav, BorderLayout.NORTH);
        card.add(calendarGrid, BorderLayout.CENTER);
        card.add(legend, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createRightColumn() {
        JPanel column = new JPanel(new BorderLayout(0, GUIUtility.scale(20)));
        column.setOpaque(false);
        column.add(createFormCard(), BorderLayout.NORTH);
        column.add(createRequestsCard(), BorderLayout.CENTER);
        return column;
    }

    private JPanel createFormCard() {
        JLabel heading = new JLabel("File a leave request");
        heading.setFont(GUIUtility.bodyFont(Font.BOLD, 16f));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 4, 12);
        addFormRow(grid, c, 0, "Type", typeBox);
        addFormRow(grid, c, 1, "From", startSpinner);
        addFormRow(grid, c, 2, "To", endSpinner);
        addFormRow(grid, c, 3, "Reason", reasonField);

        JButton submit = GUIUtility.createActionButton("Submit request", GUIUtility.BRAND_RED);
        submit.addActionListener(event -> submit());
        formMessage.setFont(GUIUtility.bodyFont(Font.PLAIN, 12f));

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        footer.setOpaque(false);
        footer.add(submit);
        footer.add(formMessage);

        JPanel card = GUIUtility.createCardPanel(new BorderLayout(0, 10));
        card.add(heading, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private static void addFormRow(JPanel grid, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        grid.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        grid.add(field, c);
    }

    private JPanel createRequestsCard() {
        JLabel heading = new JLabel("My requests");
        heading.setFont(GUIUtility.bodyFont(Font.BOLD, 16f));

        JPanel card = GUIUtility.createCardPanel(new BorderLayout(0, 10));
        card.add(heading, BorderLayout.NORTH);
        card.add(GUIUtility.wrapTable(requestTable), BorderLayout.CENTER);
        return card;
    }

    private static JPanel legendItem(Color color, String text) {
        JLabel swatch = new JLabel();
        swatch.setOpaque(true);
        swatch.setBackground(color);
        swatch.setBorder(BorderFactory.createLineBorder(GUIUtility.BORDER));
        swatch.setPreferredSize(new Dimension(14, 14));
        JLabel label = new JLabel(text);
        label.setFont(GUIUtility.bodyFont(Font.PLAIN, 12f));
        label.setForeground(GUIUtility.MUTED_TEXT);
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        item.setOpaque(false);
        item.add(swatch);
        item.add(label);
        return item;
    }

    private static JSpinner createDateSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        return spinner;
    }

    private static LocalDate toLocalDate(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // ---------------------- Data ----------------------

    private void reload() {
        try {
            names.clear();
            for (EmployeeData e : CsvHandler.loadEmployees()) {
                names.put(e.getEmpNo(), e.getFullName());
            }
            List<LeaveData> all = CsvHandler.loadLeaves();
            mine = new ArrayList<>();
            for (LeaveData leave : all) {
                if (leave.getEmpNo() == empNo) {
                    mine.add(leave);
                }
            }
            shown = new ArrayList<>(mine);
            shown.sort(Comparator.comparing(LeaveData::isPending).reversed()
                    .thenComparing(LeaveData::getStartDate, Comparator.reverseOrder()));
        } catch (IOException e) {
            setMessage("Could not read leave data: " + e.getMessage(), false);
        }
        refreshTable();
        refreshCalendar();
    }

    private void refreshTable() {
        requestModel.setRowCount(0);
        for (LeaveData leave : shown) {
            requestModel.addRow(new Object[] {
                    leave.getLeaveId(),
                    names.getOrDefault(leave.getEmpNo(), String.valueOf(leave.getEmpNo())),
                    leave.getType(),
                    leave.getStartDate().format(SHORT_DATE),
                    leave.getEndDate().format(SHORT_DATE),
                    capitalize(leave.getStatus())
            });
        }
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : text.charAt(0) + text.substring(1).toLowerCase(Locale.ROOT);
    }

    // ---------------------- Calendar ----------------------

    private void changeMonth(int delta) {
        month = month.plusMonths(delta);
        refreshCalendar();
    }

    private void refreshCalendar() {
        monthLabel.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear());
        calendarGrid.removeAll();
        for (DayOfWeek day : new DayOfWeek[] { DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY }) {
            JLabel header = new JLabel(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), SwingConstants.CENTER);
            header.setFont(GUIUtility.bodyFont(Font.BOLD, 12f));
            header.setForeground(GUIUtility.MUTED_TEXT);
            calendarGrid.add(header);
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() % 7; // Sunday first
        for (int i = 0; i < offset; i++) {
            calendarGrid.add(new JLabel());
        }
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            calendarGrid.add(createDayCell(month.atDay(day)));
        }
        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private JLabel createDayCell(LocalDate date) {
        JLabel cell = new JLabel(String.valueOf(date.getDayOfMonth()), SwingConstants.CENTER);
        cell.setOpaque(true);
        cell.setFont(GUIUtility.bodyFont(Font.PLAIN, 14f));
        cell.setPreferredSize(new Dimension(46, 46));
        cell.setBorder(BorderFactory.createLineBorder(GUIUtility.BORDER));

        DayOfWeek dow = date.getDayOfWeek();
        boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        cell.setBackground(weekend ? WEEKEND_FILL : Color.WHITE);
        cell.setForeground(weekend ? GUIUtility.MUTED_TEXT : GUIUtility.FIELD_TEXT);

        for (LeaveData leave : mine) {
            if (!leave.covers(date) || LeaveData.REJECTED.equals(leave.getStatus())) {
                continue;
            }
            boolean approved = LeaveData.APPROVED.equals(leave.getStatus());
            cell.setBackground(approved ? APPROVED_FILL : PENDING_FILL);
            cell.setFont(GUIUtility.bodyFont(Font.BOLD, 14f));
            cell.setToolTipText(leave.getType() + " leave (" + capitalize(leave.getStatus()) + ")");
            if (approved) {
                break; // approved wins over pending when they overlap
            }
        }
        if (date.equals(LocalDate.now())) {
            cell.setBorder(BorderFactory.createLineBorder(GUIUtility.BRAND_RED, 2));
        }
        return cell;
    }

    // ---------------------- Actions ----------------------

    private void setMessage(String text, boolean ok) {
        formMessage.setForeground(ok ? OK_TEXT : ERROR_TEXT);
        formMessage.setText(text);
    }

    private void submit() {
        LocalDate from = toLocalDate(startSpinner);
        LocalDate to = toLocalDate(endSpinner);
        String reason = reasonField.getText().trim();
        if (to.isBefore(from)) {
            setMessage("The end date cannot be earlier than the start date.", false);
            return;
        }
        if (reason.isEmpty()) {
            setMessage("Add a short reason.", false);
            return;
        }
        for (LeaveData existing : mine) {
            if (!LeaveData.REJECTED.equals(existing.getStatus()) && existing.overlaps(from, to)) {
                setMessage("You already have a request for those dates (" + existing.getLeaveId() + ").", false);
                return;
            }
        }
        try {
            LeaveData request = LeaveData.newRequest(CsvHandler.nextLeaveId(), empNo,
                    (String) typeBox.getSelectedItem(), from, to, reason);
            CsvHandler.saveLeave(request);
            reasonField.setText("");
            reload();
            setMessage("Request " + request.getLeaveId() + " filed. Waiting for a decision.", true);
        } catch (IOException | IllegalArgumentException e) {
            setMessage("Could not file the request: " + e.getMessage(), false);
        }
    }

}
