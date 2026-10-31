package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 * A dashboard card showing one number, e.g. "Total Rooms: 8".
 * A coloured strip on the left makes each statistic easy to spot.
 */
public class StatCard extends CardPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel valueLabel = new JLabel("0");
    private final Color accent;

    public StatCard(String title, String caption, Color accent) {
        super(new BorderLayout(0, 4));
        this.accent = accent;
        setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 16));

        JLabel titleLabel = UITheme.createFieldLabel(title.toUpperCase());
        valueLabel.setFont(UITheme.FONT_STAT);
        valueLabel.setForeground(UITheme.TEXT);
        JLabel captionLabel = UITheme.createMutedText(caption);

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
        add(captionLabel, BorderLayout.SOUTH);
    }

    public void setValue(int value) {
        valueLabel.setText(String.valueOf(value));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(accent);
        g2.fillRoundRect(0, 10, 5, getHeight() - 20, 4, 4);
        g2.dispose();
    }
}
