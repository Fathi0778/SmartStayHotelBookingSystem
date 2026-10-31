package smartstay.security;

import smartstay.model.Guest;
import smartstay.model.Reservation;
import smartstay.model.User;

/**
 * Permission checks used inside the service layer.
 *
 * Hiding buttons in the GUI is not enough on its own. Every protected
 * service method calls one of these checks first, so the rule is enforced
 * even if a screen is opened in the wrong way.
 */
public final class AccessControl {

    private AccessControl() {
        // Utility class - no objects needed.
    }

    /** Any logged-in user. */
    public static User requireLogin() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new AccessDeniedException("Please log in to continue.");
        }
        return user;
    }

    /** Only an administrator may continue. */
    public static void requireAdministrator() {
        User user = requireLogin();
        if (!user.isAdministrator()) {
            throw new AccessDeniedException("Only administrators can perform this action.");
        }
    }

    /** Only a guest may continue. Returns that guest. */
    public static Guest requireGuest() {
        User user = requireLogin();
        if (!(user instanceof Guest)) {
            throw new AccessDeniedException("This action is only available to guest accounts.");
        }
        return (Guest) user;
    }

    /** True when the current user may see this reservation. */
    public static boolean canAccess(Reservation reservation) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || reservation == null) {
            return false;
        }
        return user.isAdministrator() || reservation.getGuestId().equals(user.getUserId());
    }

    /**
     * Administrators may access every reservation; a guest may only access
     * their own. The same message is used whether the booking exists or not,
     * so a guest cannot discover other people's booking IDs.
     */
    public static void requireReservationAccess(Reservation reservation) {
        requireLogin();
        if (!canAccess(reservation)) {
            throw new AccessDeniedException("Booking not found in your reservations.");
        }
    }
}
