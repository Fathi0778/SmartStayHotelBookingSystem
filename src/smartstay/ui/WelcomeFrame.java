package smartstay.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import smartstay.model.RoomType;
import smartstay.service.AuthenticationService;
import smartstay.util.FormatUtil;

/**
 * The first screen of SmartStay.
 *
 * Top: a painted evening scene of a Sri Lankan beach hotel (drawn with
 * Java 2D, no image files) with the hotel name and an Exit button.
 * Bottom: three large tiles - Guest Login, Create Account and
 * Administrator Login - and a strip showing the four room packages,
 * read straight from RoomType so prices are never duplicated.
 */
public class WelcomeFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    // Colours used only on this screen (the rest come from UITheme).
    private static final Color SAND = new Color(250, 246, 239);
    private static final Color SUNSET = new Color(232, 121, 90);
    private static final Color GOLD = new Color(227, 176, 75);
    private static final Color NIGHT = new Color(8, 24, 45);

    private final AuthenticationService authService = new AuthenticationService();

    public WelcomeFrame() {
        super("SmartStay Hotel Booking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 700));
        setSize(1240, 780);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(SAND);
        root.add(createScene(), BorderLayout.CENTER);
        root.add(createChoices(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    // =====================================================================
    // Top: the evening hotel scene with the title
    // =====================================================================

    private JPanel createScene() {
        JPanel scene = new ScenePanel();
        scene.setLayout(new BorderLayout());
        scene.setBorder(BorderFactory.createEmptyBorder(22, 40, 30, 40));

        // Header row: logo + name on the left, Exit on the right.
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        brand.add(new LogoMark(38));
        JLabel brandName = new JLabel("SmartStay Hotels  •  Sri Lanka");
        brandName.setFont(UITheme.FONT_LABEL.deriveFont(14f));
        brandName.setForeground(new Color(225, 232, 242));
        brand.add(brandName);
        header.add(brand, BorderLayout.WEST);

        AppButton exitButton = new AppButton("Exit", AppButton.Style.DANGER);
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (UITheme.confirm(WelcomeFrame.this, "Do you want to exit SmartStay?", "Exit")) {
                    System.exit(0);
                }
            }
        });
        JPanel exitHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        exitHolder.setOpaque(false);
        exitHolder.add(exitButton);
        header.add(exitHolder, BorderLayout.EAST);
        scene.add(header, BorderLayout.NORTH);

        // Headline on the left of the scene.
        JPanel headline = new JPanel();
        headline.setOpaque(false);
        headline.setLayout(new BoxLayout(headline, BoxLayout.Y_AXIS));
        headline.add(Box.createVerticalGlue());

        JLabel welcome = new JLabel("WELCOME TO");
        welcome.setFont(UITheme.FONT_LABEL.deriveFont(14f));
        welcome.setForeground(GOLD);
        headline.add(welcome);
        headline.add(Box.createVerticalStrut(4));

        JLabel name = new JLabel("SmartStay Hotels");
        name.setFont(new Font("Georgia", Font.BOLD, 52));
        name.setForeground(Color.WHITE);
        headline.add(name);

        JLabel tagline = new JLabel("Your stay in Sri Lanka, made simple.");
        tagline.setFont(new Font("Georgia", Font.ITALIC, 22));
        tagline.setForeground(new Color(250, 222, 200));
        headline.add(tagline);
        headline.add(Box.createVerticalStrut(14));

        JLabel message = new JLabel("<html><div style='width:430px'>Find a room for your dates, book in "
                + "minutes and receive a clear, detailed invoice - for local and international guests."
                + "</div></html>");
        message.setFont(UITheme.FONT_BODY.deriveFont(15f));
        message.setForeground(new Color(232, 238, 246));
        headline.add(message);
        headline.add(Box.createVerticalGlue());

        scene.add(headline, BorderLayout.CENTER);
        return scene;
    }

    // =====================================================================
    // Bottom: three choice tiles and the room package strip
    // =====================================================================

    private JPanel createChoices() {
        JPanel bottom = new JPanel(new BorderLayout(0, 16));
        bottom.setBackground(SAND);
        bottom.setBorder(BorderFactory.createEmptyBorder(24, 40, 22, 40));

        JLabel prompt = new JLabel("How would you like to continue?");
        prompt.setFont(UITheme.FONT_HEADING.deriveFont(18f));
        prompt.setForeground(UITheme.TEXT);
        bottom.add(prompt, BorderLayout.NORTH);

        ChoiceTile guestTile = new ChoiceTile("GUESTS", "Guest Login", UITheme.PRIMARY, ChoiceTile.ICON_PERSON);
        ChoiceTile createTile = new ChoiceTile("NEW HERE?", "Create Guest Account", SUNSET, ChoiceTile.ICON_ADD);
        ChoiceTile adminTile = new ChoiceTile("HOTEL STAFF", "Administrator Login", NIGHT, ChoiceTile.ICON_KEY);

        JPanel tiles = new JPanel(new GridLayout(1, 3, 18, 0));
        tiles.setOpaque(false);
        tiles.add(guestTile);
        tiles.add(createTile);
        tiles.add(adminTile);
        bottom.add(tiles, BorderLayout.CENTER);
        bottom.add(createPackageStrip(), BorderLayout.SOUTH);

        guestTile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new GuestLoginFrame());
            }
        });
        createTile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new GuestRegistrationFrame());
            }
        });
        adminTile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openAdministratorLogin();
            }
        });
        return bottom;
    }

    /** One line listing the four packages, built from the RoomType enum. */
    private JPanel createPackageStrip() {
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        strip.setOpaque(false);
        JLabel title = new JLabel("ROOM PACKAGES");
        title.setFont(UITheme.FONT_LABEL.deriveFont(11f));
        title.setForeground(UITheme.MUTED);
        strip.add(title);
        for (RoomType type : RoomType.values()) {
            JLabel chip = new JLabel(type.getDisplayName() + "  "
                    + FormatUtil.formatMoney(type.getDailyRate()).replace(".00", "") + "/night"
                    + (type.hasComplimentaryBreakfast() ? "  •  breakfast" : ""));
            chip.setFont(UITheme.FONT_SMALL);
            chip.setForeground(UITheme.TEXT);
            chip.setOpaque(true);
            chip.setBackground(Color.WHITE);
            chip.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(230, 220, 204)),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)));
            strip.add(chip);
        }
        return strip;
    }

    /**
     * The very first time, no administrator exists yet, so the one-time
     * setup dialog is shown before the login page.
     */
    private void openAdministratorLogin() {
        if (authService.isAdministratorSetupRequired()) {
            AdminSetupDialog dialog = new AdminSetupDialog(this);
            dialog.setVisible(true);
            if (!dialog.isAccountCreated()) {
                return;
            }
        }
        navigateTo(new AdminLoginFrame());
    }

    private void navigateTo(JFrame next) {
        next.setVisible(true);
        dispose();
    }

    // =====================================================================
    // Choice tile: a large clickable card (a JButton, so it also works
    // with the keyboard - Tab to it and press Space)
    // =====================================================================

    private static class ChoiceTile extends JButton {

        private static final long serialVersionUID = 1L;
        static final int ICON_PERSON = 0;
        static final int ICON_ADD = 1;
        static final int ICON_KEY = 2;

        private final String category;
        private final String title;
        private final Color accent;
        private final int icon;

        ChoiceTile(String category, String title, Color accent, int icon) {
            this.category = category;
            this.title = title;
            this.accent = accent;
            this.icon = icon;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(300, 100));
            getAccessibleContext().setAccessibleName(title);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean hot = getModel().isRollover() || isFocusOwner();
            int w = getWidth();
            int h = getHeight();

            // Card with a soft shadow; the shadow grows on hover.
            g2.setColor(new Color(0, 0, 0, hot ? 26 : 12));
            g2.fillRoundRect(2, hot ? 5 : 3, w - 4, h - 6, 18, 18);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(1, 1, w - 3, h - 6, 18, 18);
            g2.setStroke(new BasicStroke(hot ? 2f : 1f));
            g2.setColor(hot ? accent : new Color(230, 220, 204));
            g2.drawRoundRect(1, 1, w - 3, h - 6, 18, 18);

            // Coloured top edge.
            g2.setColor(accent);
            g2.fillRoundRect(1, 1, w - 3, 6, 6, 6);

            // Icon circle.
            int cx = 22;
            int cy = (h - 6) / 2 - 26;
            g2.setColor(accent);
            g2.fillOval(cx, cy, 52, 52);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            paintIcon(g2, cx + 26, cy + 26);

            // Texts.
            int tx = cx + 70;
            g2.setFont(UITheme.FONT_LABEL.deriveFont(10.5f));
            g2.setColor(UITheme.MUTED);
            g2.drawString(category, tx, cy + 20);
            g2.setFont(UITheme.FONT_HEADING.deriveFont(18f));
            g2.setColor(UITheme.TEXT);
            g2.drawString(title, tx, cy + 43);

            // Arrow on the right (drawn, so it shows in every font).
            int ax = w - 30;
            int ay = (h - 6) / 2;
            g2.setColor(hot ? accent : new Color(180, 170, 155));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(ax - 10, ay, ax + 4, ay);
            g2.drawLine(ax - 2, ay - 6, ax + 4, ay);
            g2.drawLine(ax - 2, ay + 6, ax + 4, ay);
            g2.dispose();
        }

        /** Simple white line icons drawn around the centre point. */
        private void paintIcon(Graphics2D g2, int x, int y) {
            if (icon == ICON_PERSON) {
                g2.drawOval(x - 6, y - 13, 12, 12);
                g2.drawArc(x - 12, y + 2, 24, 22, 0, 180);
            } else if (icon == ICON_ADD) {
                g2.drawOval(x - 10, y - 13, 11, 11);
                g2.drawArc(x - 15, y + 1, 21, 20, 0, 180);
                g2.drawLine(x + 9, y - 4, x + 9, y + 8);
                g2.drawLine(x + 3, y + 2, x + 15, y + 2);
            } else {
                g2.drawOval(x - 13, y - 7, 13, 13);
                g2.drawLine(x, y, x + 13, y);
                g2.drawLine(x + 9, y, x + 9, y + 6);
                g2.drawLine(x + 13, y, x + 13, y + 5);
            }
        }
    }

    // =====================================================================
    // Painted scene: evening sky, sun, sea, beach hotel and palm trees
    // =====================================================================

    private static class ScenePanel extends JPanel {

        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int horizon = (int) (h * 0.74);

            // Sky: night blue at the top fading into a warm sunset at the horizon.
            g2.setPaint(new LinearGradientPaint(0, 0, 0, horizon,
                    new float[]{0f, 0.5f, 0.82f, 1f},
                    new Color[]{UITheme.NAVY, new Color(46, 58, 104), new Color(178, 92, 98), SUNSET}));
            g2.fillRect(0, 0, w, horizon);

            // A few stars (fixed seed, so they do not move when repainting).
            Random random = new Random(7);
            for (int i = 0; i < 70; i++) {
                int sx = random.nextInt(Math.max(1, w));
                int sy = random.nextInt(Math.max(1, horizon / 2));
                g2.setColor(new Color(255, 255, 255, 60 + random.nextInt(120)));
                int size = random.nextInt(3) == 0 ? 2 : 1;
                g2.fillOval(sx, sy, size, size);
            }

            // Setting sun with a glow, sitting on the horizon.
            int sunX = (int) (w * 0.60);
            g2.setPaint(new RadialGradientPaint(new Point2D.Float(sunX, horizon), 220,
                    new float[]{0f, 1f}, new Color[]{new Color(255, 200, 120, 150), new Color(255, 200, 120, 0)}));
            g2.fillOval(sunX - 220, horizon - 220, 440, 440);
            g2.setColor(new Color(255, 196, 110));
            g2.fillOval(sunX - 52, horizon - 52, 104, 104);

            // Sea.
            g2.setPaint(new LinearGradientPaint(0, horizon, 0, h,
                    new float[]{0f, 1f}, new Color[]{new Color(40, 54, 92), UITheme.NAVY}));
            g2.fillRect(0, horizon, w, h - horizon);

            // Sun reflection on the water.
            for (int i = 0; i < 8; i++) {
                int ry = horizon + 8 + i * 9;
                int half = 60 - i * 6;
                g2.setColor(new Color(255, 196, 110, 170 - i * 18));
                g2.fillRoundRect(sunX - half, ry, half * 2, 3, 3, 3);
            }

            // Beach on the right.
            GeneralPath beach = new GeneralPath();
            beach.moveTo(w * 0.70, h);
            beach.quadTo(w * 0.80, horizon + 8, w, horizon - 4);
            beach.lineTo(w, h);
            beach.closePath();
            g2.setColor(NIGHT);
            g2.fill(beach);

            paintHotel(g2, (int) (w * 0.80), horizon, w);
            paintPalm(g2, (int) (w * 0.74), h - 6, 150, -1);
            paintPalm(g2, (int) (w * 0.97), horizon + 2, 130, 1);

            // Darken the left side slightly so the white title is easy to read.
            g2.setPaint(new LinearGradientPaint(0, 0, w * 0.55f, 0,
                    new float[]{0f, 1f}, new Color[]{new Color(8, 24, 45, 120), new Color(8, 24, 45, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }

        /** A simple hotel building with warm, lit windows. */
        private void paintHotel(Graphics2D g2, int left, int horizon, int w) {
            int width = Math.min(170, w - left - 20);
            int top = horizon - 150;
            g2.setColor(NIGHT);
            g2.fillRect(left, top, width, 156);
            g2.fillRect(left + 20, top - 18, width - 40, 20);
            Random random = new Random(3);
            for (int row = 0; row < 7; row++) {
                for (int col = 0; col < 5; col++) {
                    boolean lit = random.nextInt(10) < 7;
                    g2.setColor(lit ? new Color(255, 210, 130) : new Color(30, 48, 78));
                    g2.fillRect(left + 14 + col * ((width - 28) / 5), top + 14 + row * 19, 14, 9);
                }
            }
        }

        /** A palm tree: curved trunk and fanned leaves. */
        private void paintPalm(Graphics2D g2, int baseX, int baseY, int height, int lean) {
            int topX = baseX + lean * 28;
            int topY = baseY - height;
            g2.setColor(NIGHT);
            g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            GeneralPath trunk = new GeneralPath();
            trunk.moveTo(baseX, baseY);
            trunk.quadTo(baseX + lean * 4, baseY - height / 2, topX, topY);
            g2.draw(trunk);
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int[][] leaves = {{-70, 20}, {-55, -18}, {-20, -34}, {25, -32}, {60, -12}, {72, 24}, {0, 30}};
            for (int[] leaf : leaves) {
                GeneralPath p = new GeneralPath();
                p.moveTo(topX, topY);
                p.quadTo(topX + leaf[0] * 0.5, topY + leaf[1] - 22, topX + leaf[0], topY + leaf[1] + 14);
                g2.draw(p);
            }
        }
    }
}
