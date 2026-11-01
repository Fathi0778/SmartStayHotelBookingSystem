package smartstay.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import smartstay.model.Administrator;
import smartstay.service.AuthenticationService;

/**
 * Administrator login: username + password.
 * Opens the Administrator Dashboard only after the service has verified
 * the credentials and started an administrator session.
 */
public class AdminLoginFrame extends AuthFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField usernameField = UITheme.createTextField();
    private final JPasswordField passwordField = UITheme.createPasswordField();
    private final AuthenticationService authService = new AuthenticationService();

    public AdminLoginFrame() {
        super("Administrator Login", "Hotel staff only. Sign in to manage rooms and reservations.", 420);

        addField("Username", usernameField);
        addField("Password", passwordField);
        addFormComponent(createShowPasswordToggle(passwordField));

        AppButton loginButton = new AppButton("Login", AppButton.Style.PRIMARY);
        AppButton clearButton = new AppButton("Clear", AppButton.Style.SECONDARY);
        AppButton backButton = new AppButton("Back", AppButton.Style.SECONDARY);
        addButtons(loginButton, clearButton, backButton);
        AppButton forgotButton = new AppButton("Forgot Password?", AppButton.Style.SECONDARY);
        addButtonRow(forgotButton);
        forgotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new AdminForgotPasswordFrame());
            }
        });

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearForm();
            }
        });
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new WelcomeFrame());
            }
        });
    }

    private void login() {
        try {
            Administrator admin = authService.loginAdministrator(usernameField.getText(),
                    passwordField.getPassword());
            AdminDashboardFrame dashboard = new AdminDashboardFrame();
            navigateTo(dashboard);
            // Show the message on top of the dashboard that has just opened.
            UITheme.showMessageLater(dashboard, "Login Successful",
                    "Successfully logged in!\nWelcome, " + admin.getFullName() + " (Administrator).");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            passwordField.setText("");
            UITheme.showError(this, ex.getMessage());
            passwordField.requestFocusInWindow();
        }
    }

    private void clearForm() {
        usernameField.setText("");
        passwordField.setText("");
        usernameField.requestFocusInWindow();
    }
}
