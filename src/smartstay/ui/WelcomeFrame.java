package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import smartstay.model.RoomType;
import smartstay.service.AuthenticationService;
import smartstay.util.FormatUtil;

/**
 * The first screen. Lets the user choose Guest Login, Create Account,
 * Administrator Login or Exit. The left side introduces the hotel and shows
 * the room packages (read straight from RoomType - no duplicated prices).
 */
public class WelcomeFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final AuthenticationService authService = new AuthenticationService();

    public WelcomeFrame() {
        super("SmartStay Hotel Booking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 680));
        setSize(1240, 760);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.add(createHeroPanel(), BorderLayout.CENTER);
        root.add(createChoicePanel(), BorderLayout.EAST);
        setContentPane(root);
    }

    // =====================================================================
    // Left: hotel introduction
    // =====================================================================

    private JPanel createHeroPanel() {
        JPanel hero = new HeroBackground();
        hero.setLayout(new BorderLayout());
        hero.setBorder(BorderFactory.createEmptyBorder(48, 56, 40, 56));

        JPanel intro = new JPanel();
        intro.setOpaque(false);
        intro.setLayout(new BoxLayout(intro, BoxLayout.Y_AXIS));

        LogoMark logo = new LogoMark(64);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        intro.add(logo);
        intro.add(Box.createVerticalStrut(22));

        JLabel welcome = new JLabel("WELCOME TO");
        welcome.setFont(UITheme.FONT_LABEL.deriveFont(13f));
        welcome.setForeground(UITheme.ACCENT);
        intro.add(welcome);

        JLabel name = new JLabel("SmartStay Hotels");
        name.setFont(new Font("Georgia", Font.BOLD, 44));
        name.setForeground(Color.WHITE);
        intro.add(name);

        JLabel system = new JLabel("Hotel Booking System");
        system.setFont(UITheme.FONT_HEADING.deriveFont(Font.PLAIN, 20f));
        system.setForeground(new Color(200, 214, 233));
        intro.add(system);
        intro.add(Box.createVerticalStrut(20));

        JLabel message = new JLabel("<html><div style='width:430px'>Comfortable stays across Sri Lanka for local "
                + "and international guests. Find a room for your dates, book in minutes and "
                + "get a clear, detailed invoice.</div></html>");
        message.setFont(UITheme.FONT_BODY.deriveFont(14f));
        message.setForeground(new Color(220, 229, 241));
        intro.add(message);
        intro.add(Box.createVerticalStrut(22));

        intro.add(createFeature("Real-time room availability for your dates"));
        intro.add(createFeature("Secure guest accounts with private bookings"));
        intro.add(createFeature("Detailed invoices with discounts and tax"));

        hero.add(intro, BorderLayout.NORTH);
        hero.add(createPackagesStrip(), BorderLayout.SOUTH);
        return hero;
    }

    private JLabel createFeature(String text) {
        JLabel feature = new JLabel("\u2022   " + text);
        feature.setFont(UITheme.FONT_BODY.deriveFont(14f));
        feature.setForeground(Color.WHITE);
        feature.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        return feature;
    }

    /** Four small cards, one per room package, built from the RoomType enum. */
    private JPanel createPackagesStrip() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);
        JLabel heading = new JLabel("ROOM PACKAGES");
        heading.setFont(UITheme.FONT_LABEL);
        heading.setForeground(UITheme.ACCENT);
        wrapper.add(heading, BorderLayout.NORTH);

        JPanel strip = new JPanel(new GridLayout(1, RoomType.values().length, 12, 0));
        strip.setOpaque(false);
        for (RoomType type : RoomType.values()) {
            JPanel card = new TranslucentPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

            JLabel typeName = new JLabel(type.getDisplayName());
            typeName.setFont(UITheme.FONT_LABEL.deriveFont(13f));
            typeName.setForeground(Color.WHITE);
            JLabel rate = new JLabel(FormatUtil.formatMoney(type.getDailyRate()).replace(".00", ""));
            rate.setFont(UITheme.FONT_HEADING);
            rate.setForeground(Color.WHITE);
            JLabel perNight = new JLabel("per night • " + type.getTaxPercentage() + "% tax");
            perNight.setFont(UITheme.FONT_SMALL);
            perNight.setForeground(new Color(190, 205, 226));
            JLabel breakfast = new JLabel(type.hasComplimentaryBreakfast() ? "Breakfast included" : "Room only");
            breakfast.setFont(UITheme.FONT_SMALL);
            breakfast.setForeground(type.hasComplimentaryBreakfast() ? UITheme.ACCENT : new Color(190, 205, 226));

            card.add(typeName);
            card.add(Box.createVerticalStrut(4));
            card.add(rate);
            card.add(perNight);
            card.add(Box.createVerticalStrut(4));
            card.add(breakfast);
            strip.add(card);
        }
        wrapper.add(strip, BorderLayout.CENTER);
        return wrapper;
    }

    // =====================================================================
    // Right: choose how to continue
    // =====================================================================

    private JPanel createChoicePanel() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(UITheme.SURFACE);
        side.setPreferredSize(new Dimension(430, 0));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setPreferredSize(new Dimension(330, 470));

        JLabel title = UITheme.createTitle("Good to see you");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = UITheme.createMutedText("Choose how you would like to continue.");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(4));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(28));

        content.add(createSectionLabel("GUESTS"));
        AppButton guestLoginButton = new AppButton("Guest Login", AppButton.Style.PRIMARY);
        AppButton createAccountButton = new AppButton("Create Guest Account", AppButton.Style.SECONDARY);
        content.add(fullWidth(guestLoginButton));
        content.add(Box.createVerticalStrut(10));
        content.add(fullWidth(createAccountButton));
        content.add(Box.createVerticalStrut(28));

        content.add(createSectionLabel("HOTEL STAFF"));
        AppButton adminLoginButton = new AppButton("Administrator Login", AppButton.Style.SECONDARY);
        content.add(fullWidth(adminLoginButton));
        content.add(Box.createVerticalStrut(28));

        AppButton exitButton = new AppButton("Exit", AppButton.Style.DANGER);
        content.add(fullWidth(exitButton));
        content.add(Box.createVerticalStrut(18));

        JLabel footer = UITheme.createMutedText("SmartStay Hotels • Sri Lanka");
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(footer);

        guestLoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new GuestLoginFrame());
            }
        });
        createAccountButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new GuestRegistrationFrame());
            }
        });
        adminLoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openAdministratorLogin();
            }
        });
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (UITheme.confirm(WelcomeFrame.this, "Do you want to exit SmartStay?", "Exit")) {
                    System.exit(0);
                }
            }
        });

        side.add(content);
        return side;
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

    private JLabel createSectionLabel(String text) {
        JLabel label = UITheme.createFieldLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return label;
    }

    private JPanel fullWidth(AppButton button) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.setAlignmentX(Component.LEFT_ALIGNMENT);
        holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setPreferredSize(new Dimension(330, 44));
        holder.add(button);
        return holder;
    }

    private void navigateTo(JFrame next) {
        next.setVisible(true);
        dispose();
    }

    // =====================================================================
    // Painted backgrounds
    // =====================================================================

    /** Diagonal navy-to-blue gradient with soft decorative circles. */
    private static class HeroBackground extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, UITheme.NAVY, getWidth(), getHeight(), UITheme.PRIMARY));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 14));
            g2.fillOval(getWidth() - 260, -120, 420, 420);
            g2.fillOval(getWidth() - 160, getHeight() / 3, 260, 260);
            g2.dispose();
        }
    }

    /** Slightly see-through rounded card used for the package strip. */
    private static class TranslucentPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        TranslucentPanel() {
            setOpaque(false);
            setLayout(new FlowLayout());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 26));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.setColor(new Color(255, 255, 255, 50));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
