package smartstay.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * A white panel with rounded corners and a light border - the "card"
 * used to group forms, tables and statistics on every screen.
 */
public class CardPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int ARC = 16;

    public CardPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBackground(UITheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.setColor(UITheme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.dispose();
        super.paintComponent(g);
    }
}
