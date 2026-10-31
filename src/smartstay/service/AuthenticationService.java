package smartstay.service;

import java.io.IOException;
import smartstay.model.Administrator;
import smartstay.model.Guest;
import smartstay.security.PasswordUtil;
import smartstay.security.SessionManager;
import smartstay.storage.DataStore;
import smartstay.util.InputValidator;

/**
 * Handles logging in, logging out and the one-time administrator setup.
 *
 * Security decisions:
 *  - There is no built-in admin password. The first time the system runs,
 *    the person installing it creates the administrator account. After
 *    that, the setup option is locked and admins cannot sign up publicly.
 *  - Login errors never say whether the username or the password was wrong,
 *    so an attacker cannot find out which accounts exist.
 *  - After 5 wrong attempts the login is locked for 60 seconds.
 */
public class AuthenticationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MILLIS = 60_000L;
    private static final String INVALID_LOGIN = "Incorrect login details. Please check and try again.";

    private final DataStore dataStore = DataStore.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    // Shared between all screens for this run of the application.
    private static int failedAttempts = 0;
    private static long lockedUntil = 0L;

    /** True until the first administrator account has been created. */
    public boolean isAdministratorSetupRequired() {
        return dataStore.getAdministrators().isEmpty();
    }

    /**
     * Creates the FIRST administrator. Refused if an administrator already
     * exists, so this cannot be used as a public admin sign-up.
     * Returns the new account together with its recovery code (shown once).
     */
    public AccountCreationResult createInitialAdministrator(String username, String fullName,
                                                    char[] password, char[] confirmPassword) {
        try {
            if (!isAdministratorSetupRequired()) {
                throw new IllegalStateException("An administrator account already exists.");
            }
            if (!InputValidator.isValidUsername(username)) {
                throw new IllegalArgumentException(
                        "Username must be 4-20 characters: lowercase letters, numbers, '.' or '_'.");
            }
            InputValidator.requireText(fullName, "Full name");
            if (!InputValidator.isValidFullName(fullName)) {
                throw new IllegalArgumentException("Full name may only contain letters, spaces, '.', ''' and '-'.");
            }
            String strengthError = PasswordUtil.checkStrength(password);
            if (strengthError != null) {
                throw new IllegalArgumentException(strengthError);
            }
            if (!java.util.Arrays.equals(password, confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }

            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(password, salt);
            String recoveryCode = PasswordUtil.generateRecoveryCode();
            String recoverySalt = PasswordUtil.generateSalt();
            String recoveryHash = PasswordUtil.hashRecoveryCode(recoveryCode, recoverySalt);
            Administrator admin = new Administrator("A001", username, fullName, hash, salt,
                    recoveryHash, recoverySalt);
            dataStore.addAdministrator(admin);
            return new AccountCreationResult(admin, recoveryCode);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save the administrator account: " + ex.getMessage(), ex);
        } finally {
            PasswordUtil.clear(password);
            PasswordUtil.clear(confirmPassword);
        }
    }

    /** Checks admin credentials and starts the session on success. */
    public Administrator loginAdministrator(String username, char[] password) {
        try {
            checkNotLocked();
            if (InputValidator.isBlank(username) || password == null || password.length == 0) {
                throw new IllegalArgumentException("Please enter both username and password.");
            }
            Administrator admin = dataStore.findAdministratorByUsername(username);
            if (admin == null || !PasswordUtil.verifyPassword(password, admin.getPasswordHash(),
                    admin.getPasswordSalt())) {
                recordFailedAttempt();
                throw new IllegalArgumentException(INVALID_LOGIN);
            }
            resetFailedAttempts();
            session.startSession(admin);
            return admin;
        } finally {
            PasswordUtil.clear(password);
        }
    }

    /** Checks guest credentials (email + password) and starts the session on success. */
    public Guest loginGuest(String email, char[] password) {
        try {
            checkNotLocked();
            if (InputValidator.isBlank(email) || password == null || password.length == 0) {
                throw new IllegalArgumentException("Please enter both email address and password.");
            }
            Guest guest = dataStore.findGuestByEmail(email);
            if (guest == null || !PasswordUtil.verifyPassword(password, guest.getPasswordHash(),
                    guest.getPasswordSalt())) {
                recordFailedAttempt();
                throw new IllegalArgumentException(INVALID_LOGIN);
            }
            resetFailedAttempts();
            session.startSession(guest);
            return guest;
        } finally {
            PasswordUtil.clear(password);
        }
    }

    /** Ends the session so no screen can act as the previous user. */
    public void logout() {
        session.endSession();
    }

    private void checkNotLocked() {
        long now = System.currentTimeMillis();
        if (now < lockedUntil) {
            long secondsLeft = (lockedUntil - now + 999) / 1000;
            throw new IllegalStateException("Too many failed attempts. Please wait "
                    + secondsLeft + " seconds and try again.");
        }
    }

    private static synchronized void recordFailedAttempt() {
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = System.currentTimeMillis() + LOCK_MILLIS;
            failedAttempts = 0;
        }
    }

    private static synchronized void resetFailedAttempts() {
        failedAttempts = 0;
        lockedUntil = 0L;
    }
}
