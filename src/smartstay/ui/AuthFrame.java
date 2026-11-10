package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

/**
 * Parent class for the login and registration screens.
 *
 * Draws a navy band at the top with a white card in the middle and offers
 * simple methods (addField, addButtons...) so each login screen only
 * describes WHICH fields it needs, not how to lay them out.
 */
public abstract class AuthFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JPanel form = new JPanel(new GridBagLayout());
    private final int cardWidth;
    private int nextRow = 0;

    protected AuthFrame(String title, String subtitle, int cardWidth) {
        super("SmartStay Hotel Booking System - " + title);
        setIconImages(LogoMark.createWindowIcons());
        this.cardWidth = cardWidth;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 640));
        setSize(1240, 760);
        setLocationRelativeTo(null);

        JPanel background = new GradientBackground();
        background.setLayout(new GridBagLayout());

        CardPanel card = new CardPanel(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));

        // Card heading: logo, title, subtitle
        JPanel heading = new JPanel(new BorderLayout(0, 6));
        heading.setOpaque(false);
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logoRow.setOpaque(false);
        logoRow.add(new LogoMark(52));
        JLabel titleLabel = UITheme.createTitle(title);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel subtitleLabel = UITheme.createMutedText(subtitle);
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 2));
        texts.setOpaque(false);
        texts.add(titleLabel);
        texts.add(subtitleLabel);
        heading.add(logoRow, BorderLayout.NORTH);
        heading.add(texts, BorderLayout.CENTER);

        form.setOpaque(false);
        card.add(heading, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);

        background.add(card);
        setContentPane(background);
    }

    /** One label + field filling the whole width of the card. */
    protected void addField(String label, JComponent field) {
        GridBagConstraints c = baseConstraints(0, 2);
        c.insets = new Insets(nextRow == 0 ? 0 : 10, 0, 4, 0);
        form.add(UITheme.createFieldLabel(label), c);
        nextRow++;
        c = baseConstraints(0, 2);
        field.setPreferredSize(new Dimension(cardWidth - 68, UITheme.FIELD_HEIGHT));
        form.add(field, c);
        nextRow++;
    }

    /** Two label + field pairs side by side (used on the registration form). */
    protected void addFieldPair(String leftLabel, JComponent leftField, String rightLabel, JComponent rightField) {
        int half = (cardWidth - 68 - 16) / 2;
        GridBagConstraints c = baseConstraints(0, 1);
        c.insets = new Insets(nextRow == 0 ? 0 : 10, 0, 4, 8);
        form.add(UITheme.createFieldLabel(leftLabel), c);
        c = baseConstraints(1, 1);
        c.insets = new Insets(nextRow == 0 ? 0 : 10, 8, 4, 0);
        form.add(UITheme.createFieldLabel(rightLabel), c);
        nextRow++;
        leftField.setPreferredSize(new Dimension(half, UITheme.FIELD_HEIGHT));
        rightField.setPreferredSize(new Dimension(half, UITheme.FIELD_HEIGHT));
        c = baseConstraints(0, 1);
        c.insets = new Insets(0, 0, 0, 8);
        form.add(leftField, c);
        c = baseConstraints(1, 1);
        c.insets = new Insets(0, 8, 0, 0);
        form.add(rightField, c);
        nextRow++;
    }

    /** Any other component (hint text, checkbox...) across the full width. */
    protected void addFormComponent(JComponent component) {
        GridBagConstraints c = baseConstraints(0, 2);
        c.insets = new Insets(8, 0, 0, 0);
        form.add(component, c);
        nextRow++;
    }

    /** The main button across the full width, then smaller buttons in a row below. */
    protected void addButtons(AppButton mainButton, AppButton... otherButtons) {
        GridBagConstraints c = baseConstraints(0, 2);
        c.insets = new Insets(20, 0, 0, 0);
        mainButton.setPreferredSize(new Dimension(cardWidth - 68, 42));
        form.add(mainButton, c);
        nextRow++;
        getRootPane().setDefaultButton(mainButton); // Enter key presses the main button.

        if (otherButtons.length > 0) {
            addButtonRow(otherButtons);
        }
    }

    /** A row of equally sized buttons across the full width. */
    protected void addButtonRow(AppButton... buttons) {
        JPanel row = new JPanel(new GridLayout(1, buttons.length, 10, 0));
        row.setOpaque(false);
        for (AppButton button : buttons) {
            row.add(button);
        }
        GridBagConstraints c = baseConstraints(0, 2);
        c.insets = new Insets(10, 0, 0, 0);
        form.add(row, c);
        nextRow++;
    }

    /** "Show password" check box that reveals or hides the given fields. */
    protected JCheckBox createShowPasswordToggle(final JPasswordField... fields) {
        final JCheckBox toggle = new JCheckBox("Show password");
        toggle.setOpaque(false);
        toggle.setFont(UITheme.FONT_SMALL);
        toggle.setForeground(UITheme.MUTED);
        toggle.setFocusPainted(false);
        toggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (JPasswordField field : fields) {
                    field.setEchoChar(toggle.isSelected() ? (char) 0 : '•');
                }
            }
        });
        for (JPasswordField field : fields) {
            field.setEchoChar('•');
        }
        return toggle;
    }

    protected JLabel createHint(String text) {
        JLabel hint = new JLabel("<html><div style='width:" + (cardWidth - 90) + "px'>" + text + "</div></html>");
        hint.setFont(UITheme.FONT_SMALL);
        hint.setForeground(UITheme.MUTED);
        return hint;
    }

    protected void navigateTo(JFrame next) {
        next.setVisible(true);
        dispose();
    }

    private GridBagConstraints baseConstraints(int column, int width) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = nextRow;
        c.gridwidth = width;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    /** Navy gradient across the top part, light grey below. */
    private static class GradientBackground extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(UITheme.BACKGROUND);
            g2.fillRect(0, 0, getWidth(), getHeight());
            int bandHeight = (int) (getHeight() * 0.42);
            g2.setPaint(new GradientPaint(0, 0, UITheme.NAVY, getWidth(), bandHeight, UITheme.PRIMARY));
            g2.fillRect(0, 0, getWidth(), bandHeight);

            g2.setColor(new Color(255, 255, 255, 200));
            g2.setFont(new Font("Georgia", Font.BOLD, 18));
            g2.drawString("SmartStay", 28, 40);
            g2.setColor(UITheme.ACCENT);
            g2.setFont(UITheme.FONT_SMALL.deriveFont(Font.BOLD, 10f));
            g2.drawString("HOTEL BOOKING SYSTEM", 28, 56);
            g2.dispose();
        }
    }
}
