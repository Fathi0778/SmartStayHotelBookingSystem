package smartstay.model;

/**
 * Bed types available in SmartStay rooms.
 */
public enum BedType {

    SINGLE("Single"),
    DOUBLE("Double"),
    QUEEN("Queen"),
    KING("King");

    private final String displayName;

    BedType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Converts a stored name such as "KING" back into the enum safely. */
    public static BedType fromName(String name) {
        for (BedType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown bed type: " + name);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
