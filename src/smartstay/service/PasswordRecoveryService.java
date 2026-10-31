package smartstay.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import smartstay.model.Administrator;
import smartstay.model.Guest;
import smartstay.model.User;
import smartstay.security.PasswordResetToken;
import smartstay.security.PasswordUtil;
import smartstay.security.SessionManager;
import smartstay.storage.DataStore;
import smartstay.util.InputValidator;

/**
 * "Forgot Password" for guests and administrators, using recovery codes.
 *
 * How it works:
 *  1. When an account is created, a random recovery code is shown ONCE.
 *     Only its salted hash is saved (like a password).
 *  2. To reset, the user enters their email/username AND the recovery code.
 *     Knowing only the email or username is never enough.
 *  3. A correct code gives a short-lived, single-use PasswordResetToken.
 *  4. With the token the user sets a new password. The old recovery code is
 *     then replaced with a NEW one (shown once), and any session of that
 *     user is ended.
 *
 * Security:
 *  - The same error message is used whether the account exists or not, so
 *    the screen does not reveal which emails are registered.
 *  - After 5 wrong codes for the same email/username, recovery is locked
 *    for 15 minutes.
 */
public class PasswordRecoveryService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final String GUEST_ERROR = "The email address or recovery code is incorrect.";
    private static final String ADMIN_ERROR = "The username or recovery code is incorrect.";

    // Attempt counters are shared by all screens while the application runs.
    // HashMap gives O(1) look-up by "role:identifier".
    private static final Map<String, Integer> FAILED_ATTEMPTS = new HashMap<>();
    private static final Map<String, LocalDateTime> LOCKED_UNTIL = new HashMap<>();

    // A fixed salt used only to waste the same time when an account is missing.
    private static final String DUMMY_SALT = PasswordUtil.generateSalt();

    private final DataStore dataStore = DataStore.getInstance();

    // =====================================================================
    // Step 1: verify the recovery code
    // =====================================================================

    public PasswordResetToken verifyGuestRecoveryCode(String email, String recoveryCode) {
        if (InputValidator.isBlank(email) || InputValidator.isBlank(recoveryCode)) {
            throw new IllegalArgumentException("Please enter your email address and recovery code.");
        }
        Guest guest = dataStore.findGuestByEmail(email);
        return verify(guest, "GUEST:" + email.trim().toLowerCase(), recoveryCode, GUEST_ERROR);
    }

    public PasswordResetToken verifyAdministratorRecoveryCode(String username, String recoveryCode) {
        if (InputValidator.isBlank(username) || InputValidator.isBlank(recoveryCode)) {
            throw new IllegalArgumentException("Please enter the username and recovery code.");
        }
        Administrator admin = dataStore.findAdministratorByUsername(username);
        return verify(admin, "ADMIN:" + username.trim().toLowerCase(), recoveryCode, ADMIN_ERROR);
    }

    private PasswordResetToken verify(User user, String attemptKey, String recoveryCode, String genericError) {
        checkNotLocked(attemptKey);
        char[] typed = PasswordUtil.normalizeRecoveryCode(recoveryCode);
        boolean correct;
        try {
            if (user == null) {
                // Do the same amount of work so timing does not reveal that the account is missing.
                PasswordUtil.hashPassword(typed, DUMMY_SALT);
                correct = false;
            } else {
                correct = PasswordUtil.verifyPassword(typed, user.getRecoveryCodeHash(), user.getRecoveryCodeSalt());
            }
        } finally {
            PasswordUtil.clear(typed);
        }

        if (!correct) {
            int attemptsLeft = recordFailure(attemptKey);
            throw new IllegalArgumentException(attemptsLeft > 0
                    ? genericError + " " + attemptsLeft + " attempt(s) left."
                    : "Too many incorrect attempts. Password recovery is locked for " + LOCK_MINUTES + " minutes.");
        }
        clearFailures(attemptKey);
        return new PasswordResetToken(user.getUserId(), user.getRecoveryCodeHash());
    }

    // =====================================================================
    // Step 2: set the new password
    // =====================================================================

    /**
     * Sets a new password using a verified token and issues a new recovery
     * code. Returns the new code so the screen can show it once.
     */
    public AccountCreationResult resetPassword(PasswordResetToken token, char[] newPassword, char[] confirmPassword) {
        try {
            if (token == null || !token.isUsable()) {
                throw new IllegalStateException("This reset request has expired. "
                        + "Please verify your recovery code again.");
            }
            User user = findUser(token.getUserId());
            if (user == null || !user.getRecoveryCodeHash().equals(token.getRecoveryHashAtVerification())) {
                throw new IllegalStateException("This reset request is no longer valid. "
                        + "Please verify your recovery code again.");
            }
            String strengthError = PasswordUtil.checkStrength(newPassword);
            if (strengthError != null) {
                throw new IllegalArgumentException(strengthError);
            }
            if (!Arrays.equals(newPassword, confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }

            // Keep the old values so they can be restored if saving fails.
            String oldPasswordHash = user.getPasswordHash();
            String oldPasswordSalt = user.getPasswordSalt();
            String oldRecoveryHash = user.getRecoveryCodeHash();
            String oldRecoverySalt = user.getRecoveryCodeSalt();

            String passwordSalt = PasswordUtil.generateSalt();
            user.setPasswordDetails(PasswordUtil.hashPassword(newPassword, passwordSalt), passwordSalt);

            // The used code is replaced, so it can never be used again.
            String newRecoveryCode = PasswordUtil.generateRecoveryCode();
            String recoverySalt = PasswordUtil.generateSalt();
            user.setRecoveryCodeDetails(PasswordUtil.hashRecoveryCode(newRecoveryCode, recoverySalt), recoverySalt);

            try {
                if (user.isAdministrator()) {
                    dataStore.saveAdministrators();
                } else {
                    dataStore.saveGuests();
                }
            } catch (IOException ex) {
                user.setPasswordDetails(oldPasswordHash, oldPasswordSalt);
                user.setRecoveryCodeDetails(oldRecoveryHash, oldRecoverySalt);
                throw new IllegalStateException("Could not save the new password: " + ex.getMessage(), ex);
            }

            token.markUsed();
            SessionManager.getInstance().invalidateSessionsFor(user.getUserId());
            return new AccountCreationResult(user, newRecoveryCode);
        } finally {
            PasswordUtil.clear(newPassword);
            PasswordUtil.clear(confirmPassword);
        }
    }

    private User findUser(String userId) {
        User guest = dataStore.findGuestById(userId);
        return guest != null ? guest : dataStore.findAdministratorById(userId);
    }

    // =====================================================================
    // Attempt limiting
    // =====================================================================

    private static synchronized void checkNotLocked(String key) {
        LocalDateTime until = LOCKED_UNTIL.get(key);
        if (until != null && LocalDateTime.now().isBefore(until)) {
            long minutes = Math.max(1, ChronoUnit.MINUTES.between(LocalDateTime.now(), until) + 1);
            throw new IllegalStateException("Too many incorrect attempts. Please try again in about "
                    + minutes + " minute(s).");
        }
    }

    /** Counts a wrong attempt and returns how many attempts are left. */
    private static synchronized int recordFailure(String key) {
        int failures = FAILED_ATTEMPTS.containsKey(key) ? FAILED_ATTEMPTS.get(key) + 1 : 1;
        if (failures >= MAX_FAILED_ATTEMPTS) {
            LOCKED_UNTIL.put(key, LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            FAILED_ATTEMPTS.remove(key);
            return 0;
        }
        FAILED_ATTEMPTS.put(key, failures);
        return MAX_FAILED_ATTEMPTS - failures;
    }

    private static synchronized void clearFailures(String key) {
        FAILED_ATTEMPTS.remove(key);
        LOCKED_UNTIL.remove(key);
    }
}
