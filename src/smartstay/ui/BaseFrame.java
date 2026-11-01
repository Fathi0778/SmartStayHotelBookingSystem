package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import smartstay.model.User;
import smartstay.security.SessionManager;
import smartstay.service.AuthenticationService;

/**
 * Parent class for every main window (inheritance in the UI layer).
 *
 * It builds the parts every screen shares - window size, the navy header
 * with the logo and logged-in user, the page title area - so each screen
 * only has to build its own content. It also provides navigation and
 * logout so that code is not repeated on every screen.
 */
public abstract class BaseFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JPanel body = new JPanel(new BorderLayout());
    private final JPanel pageActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JPanel contentArea = new JPanel(new BorderLayout(16, 16));

    protected BaseFrame(String pageTitle, String pageSubtitle) {
        super("SmartStay Hotel Booking System - " + pageTitle);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 680));
        setSize(1240, 760);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BACKGROUND);
        root.add(createHeader(), BorderLayout.NORTH);

        body.setOpaque(false);
        body.add(createPage(pageTitle, pageSubtitle), BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        setContentPane(root);
    }

    /** Navy bar across the top: logo on the left, current user on the right. */
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 22));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        brand.add(new LogoMark(36));
        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("SmartStay");
        name.setFont(new Font("Georgia", Font.BOLD, 19));
        name.setForeground(Color.WHITE);
        JLabel tagline = new JLabel("HOTEL BOOKING SYSTEM");
        tagline.setFont(UITheme.FONT_SMALL.deriveFont(Font.BOLD, 10f));
        tagline.setForeground(UITheme.ACCENT);
        brandText.add(name);
        brandText.add(tagline);
        brand.add(brandText);
        header.add(brand, BorderLayout.WEST);

        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            JPanel userPanel = new JPanel();
            userPanel.setOpaque(false);
            userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
            JLabel userName = new JLabel(user.getFullName());
            userName.setFont(UITheme.FONT_LABEL.deriveFont(13f));
            userName.setForeground(Color.WHITE);
            userName.setAlignmentX(RIGHT_ALIGNMENT);
            JLabel role = new JLabel(user.getRole() + "  •  " + user.getLoginName());
            role.setFont(UITheme.FONT_SMALL);
            role.setForeground(new Color(170, 188, 212));
            role.setAlignmentX(RIGHT_ALIGNMENT);
            userPanel.add(userName);
            userPanel.add(role);
            header.add(userPanel, BorderLayout.EAST);
        }
        return header;
    }

    /** Page title, subtitle, action buttons (top right) and the content area. */
    private JPanel createPage(String pageTitle, String pageSubtitle) {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(UITheme.createTitle(pageTitle));
        titles.add(Box.createVerticalStrut(2));
        titles.add(UITheme.createMutedText(pageSubtitle));
        titleRow.add(titles, BorderLayout.WEST);
        pageActions.setOpaque(false);
        titleRow.add(pageActions, BorderLayout.EAST);

        contentArea.setOpaque(false);
        page.add(titleRow, BorderLayout.NORTH);
        page.add(contentArea, BorderLayout.CENTER);
        return page;
    }

    /** Where each screen puts its own components. */
    protected JPanel getContentArea() {
        return contentArea;
    }

    /** Adds a button to the top-right of the page (e.g. Back, Refresh). */
    protected void addPageAction(AppButton button) {
        pageActions.add(button);
    }

    /** Dashboards add a navigation sidebar on the left. */
    protected void setSidebar(Sidebar sidebar) {
        body.add(sidebar, BorderLayout.WEST);
    }

    /** Opens another screen and closes this one. */
    protected void navigateTo(JFrame next) {
        next.setVisible(true);
        dispose();
    }

    /** Opens the correct dashboard for whoever is logged in. */
    protected void goToDashboard() {
        navigateTo(createDashboardForCurrentUser());
    }

    /** Asks for confirmation, clears the session and returns to the Welcome page. */
    protected void logout() {
        if (UITheme.confirm(this, "Are you sure you want to log out?", "Logout")) {
            String name = SessionManager.getInstance().isLoggedIn()
                    ? SessionManager.getInstance().getCurrentUser().getFullName() : "";
            new AuthenticationService().logout();
            WelcomeFrame welcome = new WelcomeFrame();
            navigateTo(welcome);
            UITheme.showMessageLater(welcome, "Logout Successful",
                    "Successfully logged out!\nGoodbye" + (name.isEmpty() ? "." : ", " + name + "."));
        }
    }

    /** Admin dashboard, guest dashboard, or the welcome page if nobody is logged in. */
    public static JFrame createDashboardForCurrentUser() {
        SessionManager session = SessionManager.getInstance();
        if (session.isAdministratorLoggedIn()) {
            return new AdminDashboardFrame();
        }
        if (session.getCurrentGuest() != null) {
            return new GuestDashboardFrame();
        }
        return new WelcomeFrame();
    }
}
