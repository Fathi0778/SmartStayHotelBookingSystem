package smartstay.security;

import java.time.LocalDateTime;
import smartstay.model.Guest;
import smartstay.model.User;

/**
 * Remembers who is logged in while the application is running.
 *
 * Singleton pattern: there is exactly one session for the desktop
 * application, so every screen and service shares the same SessionManager.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private User currentUser;
    private LocalDateTime loginTime;

    private SessionManager() {
        // Use getInstance() instead.
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /** Called by AuthenticationService only after a successful password check. */
    public void startSession(User user) {
        if (user == null) {
            throw new IllegalArgumentException("A user is required to start a session.");
        }
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
    }

    /** Clears the logged-in user. Called on logout. */
    public void endSession() {
        this.currentUser = null;
        this.loginTime = null;
    }

    /**
     * Ends the session if it belongs to the given user. Called after a
     * password reset so nobody stays logged in with the old password.
     */
    public void invalidateSessionsFor(String userId) {
        if (currentUser != null && currentUser.getUserId().equals(userId)) {
            endSession();
        }
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    /** Returns the logged-in guest, or null if the user is not a guest. */
    public Guest getCurrentGuest() {
        if (currentUser instanceof Guest) {
            return (Guest) currentUser;
        }
        return null;
    }

    public boolean isAdministratorLoggedIn() {
        return currentUser != null && currentUser.isAdministrator();
    }
}
