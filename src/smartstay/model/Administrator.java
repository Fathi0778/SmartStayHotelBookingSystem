package smartstay.model;

/**
 * Hotel staff member who manages rooms, availability and reservations.
 * Administrators log in with a username. There is no public sign-up for
 * administrators; the first one is created during first-time setup.
 */
public class Administrator extends User {

    private final String username;

    public Administrator(String adminId, String username, String fullName, String passwordHash,
                         String passwordSalt, String recoveryCodeHash, String recoveryCodeSalt) {
        super(adminId, fullName, passwordHash, passwordSalt, recoveryCodeHash, recoveryCodeSalt);
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        this.username = username.trim().toLowerCase();
    }

    public String getUsername() {
        return username;
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMINISTRATOR;
    }

    @Override
    public String getLoginName() {
        return username;
    }
}
