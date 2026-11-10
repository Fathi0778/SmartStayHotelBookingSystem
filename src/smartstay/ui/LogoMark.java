package smartstay.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/**
 * The SmartStay logo: a gold rounded square with a navy "S" and a small
 * roof line, drawn with Java 2D so no image files are needed.
 * The same drawing is used for the window icon in the title bar.
 */
public class LogoMark extends JComponent {

    private static final long serialVersionUID = 1L;
    private final int size;

    public LogoMark(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        draw(g2, size);
        g2.dispose();
    }

    /**
     * The logo in several sizes for the title bar and taskbar, so the
     * default Java icon is replaced by the SmartStay logo.
     */
    public static List<Image> createWindowIcons() {
        List<Image> icons = new ArrayList<>();
        int[] sizes = {16, 20, 24, 32, 40, 48, 64, 128};
        for (int iconSize : sizes) {
            BufferedImage image = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            draw(g2, iconSize);
            g2.dispose();
            icons.add(image);
        }
        return icons;
    }

    private static void draw(Graphics2D g2, int size) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(UITheme.ACCENT);
        g2.fillRoundRect(0, 0, size, size, size / 3, size / 3);

        // Roof line above the letter.
        g2.setColor(UITheme.NAVY);
        int roofTop = size / 6;
        int[] xs = {size / 4, size / 2, size * 3 / 4};
        int[] ys = {roofTop + size / 8, roofTop, roofTop + size / 8};
        g2.setStroke(new java.awt.BasicStroke(Math.max(2f, size / 18f)));
        g2.drawPolyline(xs, ys, 3);

        Font font = new Font("Georgia", Font.BOLD, (int) (size * 0.55));
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();
        String letter = "S";
        int x = (size - metrics.stringWidth(letter)) / 2;
        int y = size / 2 + metrics.getAscent() / 2 + size / 14;
        g2.drawString(letter, x, y);
    }
}