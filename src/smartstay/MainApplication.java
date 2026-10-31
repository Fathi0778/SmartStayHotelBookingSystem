package smartstay;

import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import smartstay.storage.DataStore;
import smartstay.ui.WelcomeFrame;

/**
 * Starting point of the SmartStay Hotel Booking System.
 *
 * 1. Uses the operating system's look and feel so inputs feel familiar.
 * 2. Loads all saved data from the "data" folder.
 * 3. Opens the Welcome page.
 */
public class MainApplication {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // If the system look and feel is not available, Java's default is used.
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                startApplication();
            }
        });
    }

    private static void startApplication() {
        DataStore dataStore = DataStore.getInstance();
        try {
            dataStore.loadAll();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "SmartStay could not read its data folder:\n" + ex.getMessage()
                    + "\n\nPlease check that the folder is not read-only and try again.",
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        // Tell the user if any saved records were damaged and skipped.
        List<String> warnings = dataStore.getLoadWarnings();
        if (!warnings.isEmpty()) {
            StringBuilder message = new StringBuilder("Some saved records could not be read and were skipped:\n\n");
            for (String warning : warnings) {
                message.append("• ").append(warning).append('\n');
            }
            JOptionPane.showMessageDialog(null, message.toString(), "Data Warning", JOptionPane.WARNING_MESSAGE);
        }

        new WelcomeFrame().setVisible(true);
    }
}
