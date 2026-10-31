package smartstay.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import smartstay.model.Guest;
import smartstay.security.PasswordUtil;
import smartstay.service.AccountCreationResult;
import smartstay.service.GuestService;

/**
 * Guest "Create Account" screen.
 * The screen only collects the input; all validation, duplicate checks and
 * password hashing happen inside GuestService.
 */
public class GuestRegistrationFrame extends AuthFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField nicField = UITheme.createTextField();
    private final JTextField fullNameField = UITheme.createTextField();
    private final JTextField contactField = UITheme.createTextField();
    private final JTextField emailField = UITheme.createTextField();
    private final JPasswordField passwordField = UITheme.createPasswordField();
    private final JPasswordField confirmField = UITheme.createPasswordField();
    private final GuestService guestService = new GuestService();

    public GuestRegistrationFrame() {
        super("Create Guest Account", "Register once to book rooms and manage your reservations.", 560);

        addFieldPair("NIC / Passport Number", nicField, "Full Name", fullNameField);
        addFieldPair("Contact Number", contactField, "Email Address", emailField);
        addFieldPair("Password", passwordField, "Confirm Password", confirmField);
        addFormComponent(createHint("Password rules: " + PasswordUtil.PASSWORD_RULES
                + "<br>Examples: NIC 199912345678 or 991234567V, passport N1234567, phone 0771234567."));
        addFormComponent(createShowPasswordToggle(passwordField, confirmField));

        AppButton createButton = new AppButton("Create Account", AppButton.Style.PRIMARY);
        AppButton clearButton = new AppButton("Clear", AppButton.Style.SECONDARY);
        AppButton backButton = new AppButton("Back to Login", AppButton.Style.SECONDARY);
        addButtons(createButton, clearButton, backButton);

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                register();
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
                navigateTo(new GuestLoginFrame());
            }
        });
    }

    private void register() {
        try {
            AccountCreationResult result = guestService.registerGuest(nicField.getText(),
                    fullNameField.getText(), contactField.getText(), emailField.getText(),
                    passwordField.getPassword(), confirmField.getPassword());
            Guest guest = (Guest) result.getUser();
            // The recovery code is shown once; only its hash was saved.
            new RecoveryCodeDialog(this, "Account created - save your recovery code",
                    result.getRecoveryCode()).setVisible(true);
            UITheme.showSuccess(this, "Welcome, " + guest.getFullName() + "!\n"
                    + "Your account has been created (Guest ID " + guest.getGuestId() + ").\n"
                    + "Please log in with your email address.");
            navigateTo(new GuestLoginFrame(guest.getEmail()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            passwordField.setText("");
            confirmField.setText("");
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void clearForm() {
        nicField.setText("");
        fullNameField.setText("");
        contactField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmField.setText("");
        nicField.requestFocusInWindow();
    }
}
