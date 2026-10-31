package smartstay.model;

/**
 * Availability status of a room.
 *
 * AVAILABLE and UNDER_MAINTENANCE are set by the administrator and stored
 * with the room. RESERVED is worked out automatically by the system when the
 * room has an active (confirmed, not yet finished) reservation. This keeps
 * the maintenance status separate from date-specific bookings, so a room
 * that is reserved for next week can still be booked for this weekend.
 */
public enum RoomStatus {

    AVAILABLE("Available"),
    RESERVED("Reserved"),
    UNDER_MAINTENANCE("Under Maintenance");

    private final String displayName;

    RoomStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RoomStatus fromName(String name) {
        for (RoomStatus status : values()) {
            if (status.name().equalsIgnoreCase(name) || status.displayName.equalsIgnoreCase(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown room status: " + name);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
