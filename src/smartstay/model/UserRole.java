package smartstay.model;

/**
 * The two kinds of users that can sign in to SmartStay.
 */
public enum UserRole {

    ADMINISTRATOR("Administrator"),
    GUEST("Guest");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
