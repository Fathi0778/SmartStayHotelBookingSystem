package smartstay.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import smartstay.util.FormatUtil;

/**
 * One place for the look of the whole application: colours, fonts and
 * small "factory" methods that create consistently styled components.
 * Changing a colour here changes it on every screen.
 */
public final class UITheme {

    // ---------- Colours (blue, white and neutral hotel theme) ----------
    public static final Color NAVY = new Color(11, 37, 69);
    public static final Color NAVY_LIGHT = new Color(23, 55, 96);
    public static final Color PRIMARY = new Color(29, 78, 137);
    public static final Color PRIMARY_DARK = new Color(20, 60, 108);
    public static final Color ACCENT = new Color(200, 160, 72);
    public static final Color BACKGROUND = new Color(244, 246, 250);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(221, 227, 236);
    public static final Color TEXT = new Color(27, 38, 53);
    public static final Color MUTED = new Color(107, 119, 140);
    public static final Color LIGHT_BLUE = new Color(232, 240, 251);
    public static final Color SELECTION = new Color(214, 228, 245);
    public static final Color STRIPE = new Color(248, 250, 253);
    public static final Color SUCCESS = new Color(30, 142, 90);
    public static final Color SUCCESS_LIGHT = new Color(223, 245, 233);
    public static final Color DANGER = new Color(192, 57, 43);
    public static final Color DANGER_LIGHT = new Color(251, 228, 225);
    public static final Color WARNING = new Color(196, 125, 14);
    public static final Color WARNING_LIGHT = new Color(253, 241, 220);

    // ---------- Fonts ----------
    private static final String FONT_FAMILY = "Segoe UI";
    public static final Font FONT_DISPLAY = new Font(FONT_FAMILY, Font.BOLD, 34);
    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font FONT_HEADING = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font FONT_LABEL = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_STAT = new Font(FONT_FAMILY, Font.BOLD, 28);

    public static final int FIELD_HEIGHT = 36;

    private UITheme() {
        // Utility class - no objects needed.
    }

    // =====================================================================
    // Labels
    // =====================================================================

    public static JLabel createTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_TITLE);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel createHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_HEADING);
        label.setForeground(TEXT);
        return label;
    }

    /** Small bold label shown above a form field. */
    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        label.setForeground(MUTED);
        return label;
    }

    public static JLabel createMutedText(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SMALL);
        label.setForeground(MUTED);
        return label;
    }

    // =====================================================================
    // Inputs
    // =====================================================================

    public static JTextField createTextField() {
        JTextField field = new JTextField();
        styleInput(field);
        return field;
    }

    public static JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        styleInput(field);
        return field;
    }

    /** Read-only field used to display values that the user cannot change. */
    public static JTextField createReadOnlyField(String text) {
        JTextField field = createTextField();
        field.setText(text);
        field.setEditable(false);
        field.setFocusable(false);
        field.setBackground(BACKGROUND);
        return field;
    }

    public static <T> JComboBox<T> createComboBox(T[] items) {
        JComboBox<T> combo = new JComboBox<>(items);
        combo.setFont(FONT_BODY);
        combo.setBackground(SURFACE);
        combo.setPreferredSize(new Dimension(160, FIELD_HEIGHT));
        return combo;
    }

    public static JSpinner createNumberSpinner(int value, int min, int max) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        spinner.setFont(FONT_BODY);
        spinner.setPreferredSize(new Dimension(90, FIELD_HEIGHT));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
        return spinner;
    }

    /** A date picker built from a standard JSpinner (no external library). */
    public static JSpinner createDateSpinner(LocalDate initialDate) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd MMM yyyy"));
        spinner.setFont(FONT_BODY);
        spinner.setPreferredSize(new Dimension(150, FIELD_HEIGHT));
        setDate(spinner, initialDate);
        return spinner;
    }

    public static LocalDate getDate(JSpinner dateSpinner) {
        Date date = (Date) dateSpinner.getValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static void setDate(JSpinner dateSpinner, LocalDate date) {
        dateSpinner.setValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    private static void styleInput(final JComponent field) {
        field.setFont(FONT_BODY);
        field.setForeground(TEXT);
        field.setPreferredSize(new Dimension(220, FIELD_HEIGHT));
        final Border normal = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10));
        final Border focused = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10));
        field.setBorder(normal);
        // Blue outline while the field is being typed in.
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(focused);
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(normal);
            }
        });
    }

    // =====================================================================
    // Tables
    // =====================================================================

    /** A table model whose cells cannot be edited by clicking them. */
    public static DefaultTableModel createReadOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            /**
             * Tells the table the real type of each column (numbers, dates,
             * money) so that clicking a column header sorts it correctly.
             */
            @Override
            public Class<?> getColumnClass(int column) {
                if (getRowCount() > 0 && getValueAt(0, column) != null) {
                    return getValueAt(0, column).getClass();
                }
                return Object.class;
            }
        };
    }

    /** Applies the striped, readable table style used everywhere. */
    public static JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(FONT_BODY);
        table.setForeground(TEXT);
        table.setRowHeight(32);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new StripedCellRenderer());
        table.setDefaultRenderer(Number.class, new StripedCellRenderer());

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 36));
        header.setDefaultRenderer(new HeaderRenderer());
        return table;
    }

    public static JScrollPane wrapTable(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(SURFACE);
        return scroll;
    }

    /** Colours a status column (Available, Reserved, Cancelled...) like a badge. */
    public static void useStatusBadges(JTable table, int columnIndex) {
        table.getColumnModel().getColumn(columnIndex).setCellRenderer(new StatusBadgeRenderer());
        table.getColumnModel().getColumn(columnIndex).setPreferredWidth(150);
    }

    /** Alternating row colours make long tables easier to read. */
    private static class StripedCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            if (!isSelected) {
                setBackground(row % 2 == 0 ? SURFACE : STRIPE);
                setForeground(TEXT);
            }
            // Money and dates are stored as real values and formatted only for display.
            if (value instanceof BigDecimal) {
                setText(FormatUtil.formatMoney((BigDecimal) value));
            } else if (value instanceof LocalDate) {
                setText(FormatUtil.formatDate((LocalDate) value));
            }
            setHorizontalAlignment(value instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(FONT_LABEL);
            setForeground(NAVY);
            setBackground(LIGHT_BLUE);
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            return this;
        }
    }

    private static class StatusBadgeRenderer extends StripedCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String text = value == null ? "" : value.toString();
            setFont(FONT_LABEL);
            setText("●  " + text);
            if (text.startsWith("Available") || text.startsWith("Confirmed") || text.startsWith("Yes")) {
                setForeground(SUCCESS);
            } else if (text.startsWith("Reserved") || text.startsWith("Locked")) {
                setForeground(WARNING);
            } else if (text.startsWith("Under") || text.startsWith("Cancelled") || text.startsWith("No")) {
                setForeground(DANGER);
            } else {
                setForeground(MUTED);
            }
            return this;
        }
    }

    // =====================================================================
    // Message dialogs (consistent titles and icons)
    // =====================================================================

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows an information message just after a new screen has appeared,
     * so the message sits on top of that screen (used for login/logout).
     */
    public static void showMessageLater(final Component parent, final String title, final String message) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public static boolean confirm(Component parent, String message, String title) {
        int choice = JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }
}
