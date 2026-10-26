package payrollhub.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class GUIUtility {

    private GUIUtility() {
    }

    public static JButton createButton(String text, Color background) {
        if (text == null) {
            throw new IllegalArgumentException("Button text is required.");
        }
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    public static JButton createNavButton(String text) {
        return createButton(text, new Color(48, 63, 87));
    }

    public static JTextField createTextField(int columns) {
        if (columns <= 0) {
            throw new IllegalArgumentException("Text field columns must be positive.");
        }
        return new JTextField(columns);
    }

    public static JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        return label;
    }

    public static JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    public static JPanel createEmployeeInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
        panel.add(createPersonalInfoPanel());
        panel.add(createFinancialInfoPanel());
        return panel;
    }

    public static JPanel createPersonalInfoPanel() {
        return createTitledPanel("Personal information");
    }

    public static JPanel createFinancialInfoPanel() {
        return createTitledPanel("Salary and allowances");
    }
}
