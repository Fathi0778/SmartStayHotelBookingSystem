package smartstay.model;

/**
 * Status of a reservation. Cancelled reservations are kept (not deleted)
 * so that the booking history is preserved.
 */
public enum BookingStatus {

    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BookingStatus fromName(String name) {
        for (BookingStatus status : values()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown booking status: " + name);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
