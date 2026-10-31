package smartstay.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextField;
import smartstay.security.PasswordResetToken;
import smartstay.service.PasswordRecoveryService;

/**
 * Guest "Forgot Password" - step 1 of 2.
 * The guest proves who they are with their email AND the recovery code
 * they saved when they registered.
 */
public class GuestForgotPasswordFrame extends AuthFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField emailField = UITheme.createTextField();
    private final JTextField recoveryCodeField = UITheme.createTextField();
    private final PasswordRecoveryService recoveryService = new PasswordRecoveryService();

    public GuestForgotPasswordFrame(String prefilledEmail) {
        super("Forgot Password", "Step 1 of 2 - verify your account with your recovery code.", 440);

        addField("Registered Email Address", emailField);
        addField("Recovery Code (e.g. K7QM-2XRP-9HTA)", recoveryCodeField);
        addFormComponent(createHint("Your recovery code was shown when you created your account (or after "
                + "your last password reset). Dashes and capital letters are optional."));

        AppButton verifyButton = new AppButton("Verify Recovery Code", AppButton.Style.PRIMARY);
        AppButton clearButton = new AppButton("Clear", AppButton.Style.SECONDARY);
        AppButton backButton = new AppButton("Back to Login", AppButton.Style.SECONDARY);
        addButtons(verifyButton, clearButton, backButton);

        if (prefilledEmail != null) {
            emailField.setText(prefilledEmail);
        }

        verifyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                verify();
            }
        });
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                emailField.setText("");
                recoveryCodeField.setText("");
                emailField.requestFocusInWindow();
            }
        });
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new GuestLoginFrame(emailField.getText().trim().isEmpty()
                        ? null : emailField.getText().trim()));
            }
        });
    }

    private void verify() {
        try {
            PasswordResetToken token = recoveryService.verifyGuestRecoveryCode(
                    emailField.getText(), recoveryCodeField.getText());
            navigateTo(new GuestResetPasswordFrame(token, emailField.getText().trim().toLowerCase()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            recoveryCodeField.setText("");
            UITheme.showError(this, ex.getMessage());
            recoveryCodeField.requestFocusInWindow();
        }
    }
}
