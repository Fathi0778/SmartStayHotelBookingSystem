package smartstay.model;

/**
 * Abstract parent class for every person who can log in to SmartStay.
 *
 * Administrator and Guest share an ID, a full name and secure password
 * details, so those are kept here (inheritance). Each subclass decides
 * its own role and login name (polymorphism through abstract methods).
 *
 * Only the salted password HASH is stored - never the plain password.
 * The account recovery code (used for "Forgot Password") is protected in
 * exactly the same way: only its salted hash is stored.
 */
public abstract class User {

    private final String userId;
    private String fullName;
    private String passwordHash;
    private String passwordSalt;
    private String recoveryCodeHash;
    private String recoveryCodeSalt;

    protected User(String userId, String fullName, String passwordHash, String passwordSalt,
                   String recoveryCodeHash, String recoveryCodeSalt) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required.");
        }
        this.userId = userId;
        setFullName(fullName);
        setPasswordDetails(passwordHash, passwordSalt);
        setRecoveryCodeDetails(recoveryCodeHash, recoveryCodeSalt);
    }

    /** Each subclass states which role it plays in the system. */
    public abstract UserRole getRole();

    /** The value the user types on the login screen (username or email). */
    public abstract String getLoginName();

    /** Convenience check used by the access-control layer. */
    public boolean isAdministrator() {
        return getRole() == UserRole.ADMINISTRATOR;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public final void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        this.fullName = fullName.trim();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    /** Hash and salt always change together, so they are set together. */
    public final void setPasswordDetails(String passwordHash, String passwordSalt) {
        if (passwordHash == null || passwordHash.isEmpty()
                || passwordSalt == null || passwordSalt.isEmpty()) {
            throw new IllegalArgumentException("Password hash and salt are required.");
        }
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
    }

    public String getRecoveryCodeHash() {
        return recoveryCodeHash;
    }

    public String getRecoveryCodeSalt() {
        return recoveryCodeSalt;
    }

    /** Replacing the hash makes the previous recovery code useless. */
    public final void setRecoveryCodeDetails(String recoveryCodeHash, String recoveryCodeSalt) {
        if (recoveryCodeHash == null || recoveryCodeHash.isEmpty()
                || recoveryCodeSalt == null || recoveryCodeSalt.isEmpty()) {
            throw new IllegalArgumentException("Recovery code hash and salt are required.");
        }
        this.recoveryCodeHash = recoveryCodeHash;
        this.recoveryCodeSalt = recoveryCodeSalt;
    }

    @Override
    public String toString() {
        return fullName + " (" + getRole() + ")";
    }
}
