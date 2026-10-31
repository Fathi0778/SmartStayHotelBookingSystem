package smartstay.service;

import java.io.IOException;
import java.util.Arrays;
import smartstay.model.Guest;
import smartstay.model.User;
import smartstay.security.AccessControl;
import smartstay.security.AccessDeniedException;
import smartstay.security.PasswordUtil;
import smartstay.storage.DataStore;
import smartstay.util.InputValidator;

/**
 * Guest registration and guest look-ups.
 * All rules are here, not in the registration screen, so the screen only
 * collects input and shows messages.
 */
public class GuestService {

    private final DataStore dataStore = DataStore.getInstance();

    /**
     * Registers a new guest after validating every field.
     * Duplicate NIC/passport numbers and duplicate emails are rejected
     * using O(1) HashMap look-ups.
     * Returns the new guest together with a recovery code that is shown
     * once and never stored in plain text.
     */
    public AccountCreationResult registerGuest(String nicOrPassport, String fullName, String contactNumber,
                               String email, char[] password, char[] confirmPassword) {
        try {
            InputValidator.requireText(nicOrPassport, "NIC/Passport number");
            InputValidator.requireText(fullName, "Full name");
            InputValidator.requireText(contactNumber, "Contact number");
            InputValidator.requireText(email, "Email address");

            if (!InputValidator.isValidNicOrPassport(nicOrPassport)) {
                throw new IllegalArgumentException("Enter a valid NIC (e.g. 199912345678 or 991234567V) "
                        + "or passport number (e.g. N1234567).");
            }
            if (!InputValidator.isValidFullName(fullName)) {
                throw new IllegalArgumentException("Full name may only contain letters, spaces, '.', ''' and '-'.");
            }
            if (!InputValidator.isValidContactNumber(contactNumber)) {
                throw new IllegalArgumentException("Enter a valid contact number (e.g. 0771234567 or +94771234567).");
            }
            if (!InputValidator.isValidEmail(email)) {
                throw new IllegalArgumentException("Enter a valid email address (e.g. name@example.com).");
            }
            if (dataStore.findGuestByNic(nicOrPassport) != null) {
                throw new IllegalArgumentException("An account with this NIC/Passport number already exists.");
            }
            if (dataStore.findGuestByEmail(email) != null) {
                throw new IllegalArgumentException("An account with this email address already exists.");
            }
            String strengthError = PasswordUtil.checkStrength(password);
            if (strengthError != null) {
                throw new IllegalArgumentException(strengthError);
            }
            if (!Arrays.equals(password, confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }

            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(password, salt);
            String recoveryCode = PasswordUtil.generateRecoveryCode();
            String recoverySalt = PasswordUtil.generateSalt();
            String recoveryHash = PasswordUtil.hashRecoveryCode(recoveryCode, recoverySalt);
            Guest guest = new Guest(generateGuestId(), nicOrPassport, fullName,
                    contactNumber.trim().replace(" ", ""), email, hash, salt, recoveryHash, recoverySalt);
            dataStore.addGuest(guest);
            return new AccountCreationResult(guest, recoveryCode);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save your account: " + ex.getMessage(), ex);
        } finally {
            PasswordUtil.clear(password);
            PasswordUtil.clear(confirmPassword);
        }
    }

    /**
     * Returns guest details. A guest may only see their own details;
     * administrators may see any guest.
     */
    public Guest getGuestById(String guestId) {
        User user = AccessControl.requireLogin();
        if (!user.isAdministrator() && !user.getUserId().equals(guestId)) {
            throw new AccessDeniedException("You can only view your own account details.");
        }
        return dataStore.findGuestById(guestId);
    }

    /** Next ID such as G0001, G0002 ... based on the highest existing number. */
    private String generateGuestId() {
        int highest = 0;
        for (Guest guest : dataStore.getGuests()) {
            highest = Math.max(highest, parseNumber(guest.getGuestId()));
        }
        return String.format("G%04d", highest + 1);
    }

    private static int parseNumber(String id) {
        try {
            return Integer.parseInt(id.replaceAll("\\D", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
