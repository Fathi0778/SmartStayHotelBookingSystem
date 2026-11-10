package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Shows a new account recovery code ONE time.
 *
 * The user must tick "I have saved my recovery code" before the dialog can
 * be closed, because the code is not stored anywhere in readable form and
 * cannot be shown again.
 */
public class RecoveryCodeDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JCheckBox savedCheck = new JCheckBox("I have saved my recovery code in a safe place");

    public RecoveryCodeDialog(Window owner, String heading, final String recoveryCode) {
        super(owner, "Your Recovery Code", ModalityType.APPLICATION_MODAL);
        setIconImages(LogoMark.createWindowIcons());
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(UITheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(UITheme.createHeading(heading), BorderLayout.NORTH);
        top.add(new JLabel("<html><div style='width:390px;color:#6B778C'>This is your account recovery code. "
                + "You will need it if you ever forget your password. Write it down or copy it somewhere "
                + "safe now - <b>it will not be shown again</b>, and SmartStay staff cannot see it.</div></html>"),
                BorderLayout.CENTER);
        content.add(top, BorderLayout.NORTH);

        JLabel codeLabel = new JLabel(recoveryCode, SwingConstants.CENTER);
        codeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 28));
        codeLabel.setForeground(UITheme.NAVY);
        codeLabel.setOpaque(true);
        codeLabel.setBackground(UITheme.LIGHT_BLUE);
        codeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(UITheme.PRIMARY, 2, 4, 3, true),
                BorderFactory.createEmptyBorder(16, 10, 16, 10)));
        content.add(codeLabel, BorderLayout.CENTER);

        savedCheck.setOpaque(false);
        savedCheck.setFont(UITheme.FONT_BODY);
        savedCheck.setForeground(UITheme.TEXT);

        final AppButton copyButton = new AppButton("Copy Code", AppButton.Style.SECONDARY);
        final AppButton continueButton = new AppButton("Continue", AppButton.Style.PRIMARY);
        continueButton.setEnabled(false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(copyButton);
        buttons.add(continueButton);

        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.add(savedCheck, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);

        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(recoveryCode), null);
                  copyButton.setText("Copied");
            }
        });
        savedCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                continueButton.setEnabled(savedCheck.isSelected());
            }
        });
        continueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (savedCheck.isSelected()) {
                    dispose();
                } else {
                    savedCheck.setForeground(UITheme.DANGER);
                    UITheme.showError(RecoveryCodeDialog.this,
                            "Please save your recovery code, then tick the box to continue.");
                    savedCheck.setForeground(UITheme.TEXT);
                }
            }
        });

        content.setBackground(Color.WHITE);
        setContentPane(content);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}
