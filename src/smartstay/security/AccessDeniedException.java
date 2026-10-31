package smartstay.security;

/**
 * Thrown when a user tries to perform an operation they are not allowed to,
 * for example a guest trying to delete a room or open another guest's booking.
 */
public class AccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(message);
    }
}
