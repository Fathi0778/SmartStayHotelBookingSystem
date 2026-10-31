package smartstay.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * A rounded, flat button that looks the same on every operating system.
 * The Style decides the colors (polymorphism through an enum).
 */
public class AppButton extends JButton {

    private static final long serialVersionUID = 1L;

    /** The visual styles available for buttons. */
    public enum Style {
        PRIMARY(UITheme.PRIMARY, UITheme.PRIMARY_DARK, Color.WHITE, false),
        SECONDARY(UITheme.SURFACE, UITheme.LIGHT_BLUE, UITheme.PRIMARY, true),
        SUCCESS(UITheme.SUCCESS, new Color(22, 115, 72), Color.WHITE, false),
        DANGER(UITheme.DANGER, new Color(160, 45, 34), Color.WHITE, false),
        NAV(UITheme.NAVY, UITheme.NAVY_LIGHT, new Color(214, 224, 238), false);

        private final Color background;
        private final Color hoverBackground;
        private final Color foreground;
        private final boolean outlined;

        Style(Color background, Color hoverBackground, Color foreground, boolean outlined) {
            this.background = background;
            this.hoverBackground = hoverBackground;
            this.foreground = foreground;
            this.outlined = outlined;
        }
    }

    private final Style style;
    private boolean active;

    public AppButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(UITheme.FONT_LABEL.deriveFont(13f));
        setForeground(style.foreground);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (style == Style.NAV) {
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 16));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        } else {
            setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        }
    }

    /** Marks a sidebar button as the current page. */
    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.max(size.width, 96), Math.max(size.height, UITheme.FIELD_HEIGHT + 2));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        ButtonModel model = getModel();
        Color fill = style.background;
        if (!isEnabled()) {
            fill = style == Style.NAV ? style.background : new Color(200, 207, 218);
        } else if (active || model.isPressed() || model.isRollover()) {
            fill = style.hoverBackground;
        }

        int arc = style == Style.NAV ? 0 : 10;
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

               if (style.outlined) {
            // Draw the border slightly inside the edge so it is not cut off
            // on screens with Windows display scaling (e.g. 125%).
            g2.setColor(isEnabled() ? UITheme.PRIMARY : UITheme.BORDER);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setStroke(new java.awt.BasicStroke(1.2f));
            g2.draw(new java.awt.geom.RoundRectangle2D.Float(0.8f, 0.8f,
                    getWidth() - 1.6f, getHeight() - 1.6f, arc, arc));
        }
      
        if (style == Style.NAV && active) {
            g2.setColor(UITheme.ACCENT);
            g2.fillRect(0, 0, 4, getHeight());
        }
        g2.dispose();

        setForeground(active && style == Style.NAV ? Color.WHITE
                : (isEnabled() || style == Style.NAV ? style.foreground : Color.WHITE));
        super.paintComponent(g);
    }
}
