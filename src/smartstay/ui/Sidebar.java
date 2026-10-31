package smartstay.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Navy navigation menu shown on the left of the dashboards.
 * The administrator and guest dashboards add DIFFERENT items, so a guest
 * never sees administrator options.
 */
public class Sidebar extends JPanel {

    private static final long serialVersionUID = 1L;

    public Sidebar(String sectionTitle) {
        setBackground(UITheme.NAVY);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(230, 0));
        setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));

        JLabel title = new JLabel(sectionTitle.toUpperCase());
        title.setFont(UITheme.FONT_SMALL.deriveFont(11f));
        title.setForeground(new Color(140, 160, 190));
        title.setBorder(BorderFactory.createEmptyBorder(0, 22, 10, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
    }

    /** Adds a menu item and returns it so the caller can mark it active. */
    public AppButton addItem(String text, ActionListener action) {
        AppButton button = new AppButton(text, AppButton.Style.NAV);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(action);
        add(button);
        return button;
    }

    /** Pushes the remaining items (e.g. Logout) to the bottom. */
    public void addFlexibleSpace() {
        add(Box.createVerticalGlue());
    }
}
