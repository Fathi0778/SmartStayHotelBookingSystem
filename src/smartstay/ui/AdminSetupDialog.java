package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import smartstay.security.PasswordUtil;
import smartstay.service.AccountCreationResult;
import smartstay.service.AuthenticationService;

/**
 * One-time administrator setup.
 *
 * Shown only when no administrator account exists (the first run).
 * This avoids hard-coding a publicly known admin password in the code.
 * Once an administrator exists, this dialog can never be opened again.
 */
public class AdminSetupDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField usernameField = UITheme.createTextField();
    private final JTextField fullNameField = UITheme.createTextField();
    private final JPasswordField passwordField = UITheme.createPasswordField();
    private final JPasswordField confirmField = UITheme.createPasswordField();
    private final AuthenticationService authService = new AuthenticationService();
    private boolean accountCreated;

    public AdminSetupDialog(JFrame owner) {
        super(owner, "First-time Administrator Setup", true);
        setIconImages(LogoMark.createWindowIcons());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(UITheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel heading = new JPanel(new BorderLayout(0, 4));
        heading.setOpaque(false);
        heading.add(UITheme.createHeading("Create the administrator account"), BorderLayout.NORTH);
        heading.add(new JLabel("<html><div style='width:380px;color:#6B778C'>No administrator exists yet. "
                + "Create the hotel's administrator account now. This setup appears only once - after "
                + "this, administrator accounts cannot be created from the application.</div></html>"),
                BorderLayout.CENTER);
        content.add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addRow(form, 0, "Username (4-20 lowercase letters/numbers)", usernameField);
        addRow(form, 1, "Full Name", fullNameField);
        addRow(form, 2, "Password", passwordField);
        addRow(form, 3, "Confirm Password", confirmField);
        JLabel rules = UITheme.createMutedText(PasswordUtil.PASSWORD_RULES);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 8;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 0, 0, 0);
        form.add(rules, c);
        content.add(form, BorderLayout.CENTER);

        AppButton createButton = new AppButton("Create Administrator", AppButton.Style.PRIMARY);
        AppButton cancelButton = new AppButton("Cancel", AppButton.Style.SECONDARY);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(createButton);
        content.add(buttons, BorderLayout.SOUTH);

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createAdministrator();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        getRootPane().setDefaultButton(createButton);

        setContentPane(content);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void createAdministrator() {
        try {
            AccountCreationResult result = authService.createInitialAdministrator(usernameField.getText(),
                    fullNameField.getText(), passwordField.getPassword(), confirmField.getPassword());
            accountCreated = true;
            new RecoveryCodeDialog(this, "Administrator recovery code", result.getRecoveryCode()).setVisible(true);
            UITheme.showSuccess(this, "Administrator account created.\nYou can now log in with username '"
                    + usernameField.getText().trim().toLowerCase() + "'.");
            dispose();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    public boolean isAccountCreated() {
        return accountCreated;
    }

    private void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row * 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(row == 0 ? 0 : 10, 0, 4, 0);
        form.add(UITheme.createFieldLabel(label), c);
        c.gridy = row * 2 + 1;
        c.insets = new Insets(0, 0, 0, 0);
        field.setPreferredSize(new java.awt.Dimension(380, UITheme.FIELD_HEIGHT));
        form.add(field, c);
    }
}
