package smartstay.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import smartstay.security.PasswordResetToken;
import smartstay.security.PasswordUtil;
import smartstay.service.AccountCreationResult;
import smartstay.service.PasswordRecoveryService;

/**
 * Administrator password recovery on a single screen with two steps:
 *  1. Enter username + the administrator recovery code -> Verify.
 *  2. The new password fields unlock -> Reset Password.
 * Knowing the username alone is never enough to reset the password.
 */
public class AdminForgotPasswordFrame extends AuthFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField usernameField = UITheme.createTextField();
    private final JTextField recoveryCodeField = UITheme.createTextField();
    private final JPasswordField newPasswordField = UITheme.createPasswordField();
    private final JPasswordField confirmField = UITheme.createPasswordField();
    private final JLabel stepLabel = UITheme.createMutedText("Step 1: verify the recovery code.");
    private final AppButton verifyButton = new AppButton("Verify Recovery Code", AppButton.Style.PRIMARY);
    private final AppButton resetButton = new AppButton("Reset Password", AppButton.Style.SUCCESS);
    private final JCheckBox showToggle;
    private final PasswordRecoveryService recoveryService = new PasswordRecoveryService();
    private transient PasswordResetToken token;

    public AdminForgotPasswordFrame() {
        super("Administrator Password Recovery",
                "Use the recovery code created during administrator setup.", 440);

        addField("Username", usernameField);
        addField("Administrator Recovery Code", recoveryCodeField);
        addButtonRow(verifyButton);
        addFormComponent(stepLabel);
        addField("New Password", newPasswordField);
        addField("Confirm New Password", confirmField);
        addFormComponent(createHint("Password rules: " + PasswordUtil.PASSWORD_RULES));
        showToggle = createShowPasswordToggle(newPasswordField, confirmField);
        addFormComponent(showToggle);

        AppButton backButton = new AppButton("Back to Login", AppButton.Style.SECONDARY);
        addButtons(resetButton, backButton);
        getRootPane().setDefaultButton(verifyButton);
        setPasswordStepEnabled(false);

        verifyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                verify();
            }
        });
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetPassword();
            }
        });
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new AdminLoginFrame());
            }
        });
    }

    private void verify() {
        try {
            token = recoveryService.verifyAdministratorRecoveryCode(usernameField.getText(),
                    recoveryCodeField.getText());
            usernameField.setEditable(false);
            recoveryCodeField.setEditable(false);
            verifyButton.setEnabled(false);
            stepLabel.setText("Verified. Step 2: enter a new password within "
                    + PasswordResetToken.VALID_MINUTES + " minutes.");
            stepLabel.setForeground(UITheme.SUCCESS);
            setPasswordStepEnabled(true);
            getRootPane().setDefaultButton(resetButton);
            newPasswordField.requestFocusInWindow();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            recoveryCodeField.setText("");
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void resetPassword() {
        try {
            AccountCreationResult result = recoveryService.resetPassword(token,
                    newPasswordField.getPassword(), confirmField.getPassword());
            new RecoveryCodeDialog(this, "Password changed - NEW administrator recovery code",
                    result.getRecoveryCode()).setVisible(true);
            UITheme.showSuccess(this, "The administrator password has been reset.\n"
                    + "The old recovery code no longer works. Please log in with the new password.");
            navigateTo(new AdminLoginFrame());
        } catch (IllegalArgumentException ex) {
            newPasswordField.setText("");
            confirmField.setText("");
            UITheme.showError(this, ex.getMessage());
        } catch (IllegalStateException ex) {
            UITheme.showError(this, ex.getMessage());
            navigateTo(new AdminForgotPasswordFrame());
        }
    }

    private void setPasswordStepEnabled(boolean enabled) {
        newPasswordField.setEnabled(enabled);
        confirmField.setEnabled(enabled);
        showToggle.setEnabled(enabled);
        resetButton.setEnabled(enabled);
    }
}
