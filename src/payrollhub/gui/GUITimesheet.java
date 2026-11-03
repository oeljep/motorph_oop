package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import payrollhub.data.AttendanceData;
import payrollhub.data.CsvHandler;
import payrollhub.service.AttendanceRules;

/**
 * Time Tracker tab, after the Figma "Time Tracker" frame. It has two views:
 * <ul>
 * <li>Time Tracker: a running clock with a Clock in / Clock out button and the
 * employee's most recent days.</li>
 * <li>Time Log History: every attendance record for a chosen month, with
 * totals.</li>
 * </ul>
 * Records are read and written only through {@link CsvHandler}; hours and late
 * minutes come from {@link AttendanceRules}. Any employee can clock in and out
 * for themselves, so the tab is the same for every role.
 */
public class GUITimesheet extends JPanel {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    private static final String TRACKER = "tracker";
    private static final String HISTORY = "history";
    private static final String[] COLUMNS = { "Date", "Time in", "Time out", "Hours worked", "Late (min)" };

    private final int empNo;
    private final CardLayout views = new CardLayout();
    private final JPanel viewArea = new JPanel(views);
    private final JButton trackerTab = createTab("Time Tracker");
    private final JButton historyTab = createTab("Time Log History");

    private final JLabel elapsedLabel = new JLabel("00:00:00", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JButton clockButton = GUIUtility.createActionButton("Clock in", GUIUtility.BRAND_RED);
    private final DefaultTableModel recentModel = createModel();
    private final DefaultTableModel historyModel = createModel();
    private final JComboBox<String> monthBox = new JComboBox<>();
    private final JLabel totalsLabel = new JLabel(" ");
    private final Timer ticker = new Timer(1000, event -> updateElapsed());

    private AttendanceData today;
    private boolean loadingMonths;

    /**
     * Builds the tab for one employee.
     *
     * @param empNo the signed-in employee's number
     */
    public GUITimesheet(int empNo) {
        this.empNo = empNo;
        setBackground(GUIUtility.PAGE_BACKGROUND);
        setLayout(new BorderLayout(0, GUIUtility.scale(20)));
        setBorder(BorderFactory.createEmptyBorder(
                GUIUtility.scale(40), GUIUtility.scale(60), GUIUtility.scale(40), GUIUtility.scale(60)));

        viewArea.setOpaque(false);
        viewArea.add(createTrackerView(), TRACKER);
        viewArea.add(createHistoryView(), HISTORY);
        add(createTopBar(), BorderLayout.NORTH);
        add(viewArea, BorderLayout.CENTER);

        trackerTab.addActionListener(event -> showView(TRACKER));
        historyTab.addActionListener(event -> showView(HISTORY));
        clockButton.addActionListener(event -> onClock());
        monthBox.addActionListener(event -> {
            if (!loadingMonths) {
                loadHistory();
            }
        });

        showView(TRACKER);
        refreshToday();
        refreshRecent();
        loadMonths();
    }

    // ---------------------- Layout ----------------------

    private JPanel createTopBar() {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, GUIUtility.scale(24), 0));
        left.setOpaque(false);
        left.add(trackerTab);
        left.add(historyTab);

        JLabel date = new JLabel("Today, " + LocalDate.now().format(DAY_FORMAT));
        date.setFont(GUIUtility.bodyFont(Font.PLAIN, 15f));
        date.setForeground(GUIUtility.MUTED_TEXT);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(left, BorderLayout.WEST);
        bar.add(date, BorderLayout.EAST);
        return bar;
    }

    private JPanel createTrackerView() {
        elapsedLabel.setFont(GUIUtility.bodyFont(Font.BOLD, GUIUtility.scale(64)));
        elapsedLabel.setForeground(GUIUtility.FIELD_TEXT);
        elapsedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(GUIUtility.bodyFont(Font.PLAIN, 14f));
        statusLabel.setForeground(GUIUtility.MUTED_TEXT);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        clockButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel clockCard = GUIUtility.createCardPanel(null);
        clockCard.setLayout(new BoxLayout(clockCard, BoxLayout.Y_AXIS));
        clockCard.add(elapsedLabel);
        clockCard.add(Box.createVerticalStrut(GUIUtility.scale(8)));
        clockCard.add(statusLabel);
        clockCard.add(Box.createVerticalStrut(GUIUtility.scale(18)));
        clockCard.add(clockButton);

        JLabel recent = new JLabel("Recent days");
        recent.setFont(GUIUtility.bodyFont(Font.BOLD, 16f));

        JPanel recentBox = new JPanel(new BorderLayout(0, GUIUtility.scale(10)));
        recentBox.setOpaque(false);
        recentBox.add(recent, BorderLayout.NORTH);
        recentBox.add(GUIUtility.wrapTable(createTable(recentModel)), BorderLayout.CENTER);

        JPanel view = new JPanel(new BorderLayout(0, GUIUtility.scale(24)));
        view.setOpaque(false);
        view.add(clockCard, BorderLayout.NORTH);
        view.add(recentBox, BorderLayout.CENTER);
        return view;
    }

    private JPanel createHistoryView() {
        JLabel monthLabel = new JLabel("Month");
        monthLabel.setFont(GUIUtility.bodyFont(Font.PLAIN, 14f));
        totalsLabel.setFont(GUIUtility.bodyFont(Font.BOLD, 14f));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controls.setOpaque(false);
        controls.add(monthLabel);
        controls.add(monthBox);
        controls.add(Box.createHorizontalStrut(24));
        controls.add(totalsLabel);

        JPanel view = new JPanel(new BorderLayout(0, GUIUtility.scale(16)));
        view.setOpaque(false);
        view.add(controls, BorderLayout.NORTH);
        view.add(GUIUtility.wrapTable(createTable(historyModel)), BorderLayout.CENTER);
        return view;
    }

    private static JButton createTab(String text) {
        JButton tab = GUIUtility.createLinkButton(text, GUIUtility.MUTED_TEXT,
                GUIUtility.bodyFont(Font.PLAIN, GUIUtility.scale(26)));
        tab.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return tab;
    }

    private static DefaultTableModel createModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        GUIUtility.styleTable(table);
        return table;
    }

    private void showView(String name) {
        views.show(viewArea, name);
        boolean tracker = TRACKER.equals(name);
        styleTab(trackerTab, tracker);
        styleTab(historyTab, !tracker);
    }

    private static void styleTab(JButton tab, boolean active) {
        tab.setForeground(active ? GUIUtility.FIELD_TEXT : GUIUtility.MUTED_TEXT);
        tab.setFont(GUIUtility.bodyFont(active ? Font.BOLD : Font.PLAIN, GUIUtility.scale(26)));
        tab.setBorder(active
                ? BorderFactory.createMatteBorder(0, 0, 3, 0, GUIUtility.BRAND_RED)
                : BorderFactory.createEmptyBorder(0, 0, 3, 0));
    }

    // ---------------------- Clock ----------------------

    private void onClock() {
        try {
            LocalDate date = LocalDate.now();
            LocalTime time = LocalTime.now().withSecond(0).withNano(0);
            if (today == null) {
                CsvHandler.clockIn(empNo, date, time);
            } else if (today.isOpen()) {
                CsvHandler.clockOut(empNo, date, time);
            }
            refreshToday();
            refreshRecent();
            loadMonths();
        } catch (IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Time Tracker", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshToday() {
        try {
            today = CsvHandler.findAttendance(empNo, LocalDate.now()).orElse(null);
        } catch (IOException e) {
            today = null;
            statusLabel.setText("Could not read attendance: " + e.getMessage());
            return;
        }
        if (today == null) {
            ticker.stop();
            elapsedLabel.setText("00:00:00");
            statusLabel.setText("You have not clocked in today.");
            clockButton.setText("Clock in");
            clockButton.setEnabled(true);
        } else if (today.isOpen()) {
            statusLabel.setText("Clocked in at " + today.getLogIn().format(TIME_FORMAT));
            clockButton.setText("Clock out");
            clockButton.setEnabled(true);
            ticker.start();
            updateElapsed();
        } else {
            ticker.stop();
            updateElapsed();
            double hours = AttendanceRules.hoursWorked(today.getLogIn(), today.getLogOut());
            statusLabel.setText(String.format("Done for today. In %s, out %s, %.2f paid hours.",
                    today.getLogIn().format(TIME_FORMAT), today.getLogOut().format(TIME_FORMAT), hours));
            clockButton.setText("Done for today");
            clockButton.setEnabled(false);
        }
    }

    private void updateElapsed() {
        if (today == null) {
            return;
        }
        LocalDateTime end = today.isOpen()
                ? LocalDateTime.now()
                : LocalDateTime.of(today.getDate(), today.getLogOut());
        Duration elapsed = Duration.between(today.getLogInDateTime(), end);
        if (elapsed.isNegative()) {
            elapsed = Duration.ZERO;
        }
        elapsedLabel.setText(String.format("%02d:%02d:%02d",
                elapsed.toHours(), elapsed.toMinutesPart(), elapsed.toSecondsPart()));
    }

    @Override
    public void removeNotify() {
        ticker.stop();
        super.removeNotify();
    }

    // ---------------------- Data ----------------------

    private void refreshRecent() {
        List<AttendanceData> recent = new ArrayList<>();
        try {
            List<String> months = CsvHandler.loadAttendanceMonths(empNo);
            for (int i = months.size() - 1; i >= 0 && i >= months.size() - 2; i--) {
                recent.addAll(CsvHandler.loadAttendance(empNo, months.get(i)));
            }
        } catch (IOException e) {
            statusLabel.setText("Could not read attendance: " + e.getMessage());
        }
        recent.sort(Comparator.comparing(AttendanceData::getDate).reversed());
        fill(recentModel, recent.subList(0, Math.min(7, recent.size())));
    }

    private void loadMonths() {
        loadingMonths = true;
        Object previous = monthBox.getSelectedItem();
        monthBox.removeAllItems();
        try {
            List<String> months = CsvHandler.loadAttendanceMonths(empNo);
            Collections.reverse(months);
            for (String month : months) {
                monthBox.addItem(month);
            }
            if (previous != null && months.contains(previous.toString())) {
                monthBox.setSelectedItem(previous);
            }
        } catch (IOException e) {
            totalsLabel.setText("Could not read attendance: " + e.getMessage());
            loadingMonths = false;
            monthBox.setEnabled(false);
            historyModel.setRowCount(0);
            return;
        }
        monthBox.setEnabled(monthBox.getItemCount() > 0);
        loadingMonths = false;
        loadHistory();
    }

    private void loadHistory() {
        String month = (String) monthBox.getSelectedItem();
        historyModel.setRowCount(0);
        if (month == null) {
            totalsLabel.setText("No attendance records yet.");
            return;
        }
        try {
            List<AttendanceData> records = new ArrayList<>(CsvHandler.loadAttendance(empNo, month));
            records.sort(Comparator.comparing(AttendanceData::getDate).reversed());
            fill(historyModel, records);

            int days = 0;
            int late = 0;
            double hours = 0;
            for (AttendanceData a : records) {
                if (a.isOpen()) {
                    continue;
                }
                days++;
                hours += AttendanceRules.hoursWorked(a.getLogIn(), a.getLogOut());
                late += AttendanceRules.lateMinutes(a.getLogIn());
            }
            totalsLabel.setText(String.format("Days worked: %d     Hours: %.2f     Late: %,d min", days, hours, late));
        } catch (IOException e) {
            totalsLabel.setText("Could not read attendance: " + e.getMessage());
        }
    }

    private static void fill(DefaultTableModel model, List<AttendanceData> records) {
        model.setRowCount(0);
        for (AttendanceData a : records) {
            boolean open = a.isOpen();
            model.addRow(new Object[] {
                    a.getDate().format(DAY_FORMAT),
                    a.getLogIn().format(TIME_FORMAT),
                    open ? "In progress" : a.getLogOut().format(TIME_FORMAT),
                    open ? "-" : String.format("%.2f", AttendanceRules.hoursWorked(a.getLogIn(), a.getLogOut())),
                    AttendanceRules.lateMinutes(a.getLogIn())
            });
        }
    }
}
