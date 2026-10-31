package smartstay.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPasswordField;
import smartstay.security.PasswordResetToken;
import smartstay.security.PasswordUtil;
import smartstay.service.AccountCreationResult;
import smartstay.service.PasswordRecoveryService;

/**
 * Guest "Forgot Password" - step 2 of 2.
 * Opened only with a valid PasswordResetToken from step 1. After the new
 * password is saved, a NEW recovery code is shown (the old one no longer
 * works) and the guest returns to the login screen.
 */
public class GuestResetPasswordFrame extends AuthFrame {

    private static final long serialVersionUID = 1L;

    private final transient PasswordResetToken token;
    private final String email;
    private final JPasswordField newPasswordField = UITheme.createPasswordField();
    private final JPasswordField confirmField = UITheme.createPasswordField();
    private final PasswordRecoveryService recoveryService = new PasswordRecoveryService();

    public GuestResetPasswordFrame(PasswordResetToken token, String email) {
        super("Reset Password", "Step 2 of 2 - choose a new password for " + email, 440);
        this.token = token;
        this.email = email;

        addField("New Password", newPasswordField);
        addField("Confirm New Password", confirmField);
        addFormComponent(createHint("Password rules: " + PasswordUtil.PASSWORD_RULES
                + " This page expires " + PasswordResetToken.VALID_MINUTES + " minutes after verification."));
        addFormComponent(createShowPasswordToggle(newPasswordField, confirmField));

        AppButton resetButton = new AppButton("Reset Password", AppButton.Style.PRIMARY);
        AppButton cancelButton = new AppButton("Cancel", AppButton.Style.SECONDARY);
        addButtons(resetButton, cancelButton);

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetPassword();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new GuestLoginFrame(GuestResetPasswordFrame.this.email));
            }
        });
    }

    private void resetPassword() {
        try {
            AccountCreationResult result = recoveryService.resetPassword(token,
                    newPasswordField.getPassword(), confirmField.getPassword());
            new RecoveryCodeDialog(this, "Password changed - here is your NEW recovery code",
                    result.getRecoveryCode()).setVisible(true);
            UITheme.showSuccess(this, "Your password has been reset.\n"
                    + "Your old recovery code no longer works. Please log in with your new password.");
            navigateTo(new GuestLoginFrame(email));
        } catch (IllegalArgumentException ex) {
            newPasswordField.setText("");
            confirmField.setText("");
            UITheme.showError(this, ex.getMessage());
        } catch (IllegalStateException ex) {
            // Expired or already used token: start again from step 1.
            UITheme.showError(this, ex.getMessage());
            navigateTo(new GuestForgotPasswordFrame(email));
        }
    }
}
