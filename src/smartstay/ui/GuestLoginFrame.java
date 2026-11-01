package smartstay.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import smartstay.model.Guest;
import smartstay.service.AuthenticationService;

/**
 * Guest login: email + password. On success the guest session starts and
 * the Guest Dashboard opens.
 */
public class GuestLoginFrame extends AuthFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField emailField = UITheme.createTextField();
    private final JPasswordField passwordField = UITheme.createPasswordField();
    private final AuthenticationService authService = new AuthenticationService();

    public GuestLoginFrame() {
        this(null);
    }

    /** @param prefilledEmail email to fill in (used right after registration), or null */
    public GuestLoginFrame(String prefilledEmail) {
        super("Guest Login", "Sign in to search rooms, book and view your invoices.", 420);

        addField("Email Address", emailField);
        addField("Password", passwordField);
        addFormComponent(createShowPasswordToggle(passwordField));

        AppButton loginButton = new AppButton("Login", AppButton.Style.PRIMARY);
        AppButton createAccountButton = new AppButton("Create Account", AppButton.Style.SECONDARY);
        AppButton clearButton = new AppButton("Clear", AppButton.Style.SECONDARY);
        AppButton backButton = new AppButton("Back", AppButton.Style.SECONDARY);
        AppButton forgotButton = new AppButton("Forgot Password?", AppButton.Style.SECONDARY);
        addButtons(loginButton, clearButton, backButton);
        addFormComponent(createHint("New to SmartStay, or can't remember your password?"));
        addButtonRow(createAccountButton, forgotButton);

        if (prefilledEmail != null) {
            emailField.setText(prefilledEmail);
        }

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });
        forgotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new GuestForgotPasswordFrame(emailField.getText().trim().isEmpty()
                        ? null : emailField.getText().trim()));
            }
        });
        createAccountButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new GuestRegistrationFrame());
            }
        });
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                emailField.setText("");
                passwordField.setText("");
                emailField.requestFocusInWindow();
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
            Guest guest = authService.loginGuest(emailField.getText(), passwordField.getPassword());
            GuestDashboardFrame dashboard = new GuestDashboardFrame();
            navigateTo(dashboard);
            // Show the message on top of the dashboard that has just opened.
            UITheme.showMessageLater(dashboard, "Login Successful",
                    "Successfully logged in!\nWelcome, " + guest.getFullName() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            passwordField.setText("");
            UITheme.showError(this, ex.getMessage());
            passwordField.requestFocusInWindow();
        }
    }
}
