package smartstay.model;

import java.math.BigDecimal;

/**
 * The four room packages offered by SmartStay (coursework Table 1).
 *
 * All package values (daily rate, breakfast, extra guest charge, tax rate)
 * are kept in this ONE place so that prices are never duplicated anywhere
 * else in the application. Every other class asks the RoomType for its price.
 *
 * Standard capacity (guests included in the daily rate) is an assumption
 * because the case study does not state room capacities.
 */
public enum RoomType {

    //            display name     daily rate  breakfast  extra guest  tax   capacity
    STANDARD("Standard",          "6000",     false,     "1000",      "0.10", 2),
    DELUXE("Deluxe",              "9000",     true,      "1500",      "0.12", 2),
    SUITE("Suite",                "14000",    true,      "2000",      "0.15", 3),
    FAMILY_SUITE("Family Suite",  "18000",    true,      "2500",      "0.15", 4);

    /** Maximum number of extra guests allowed above the standard capacity. */
    public static final int MAX_EXTRA_GUESTS = 2;

    private final String displayName;
    private final BigDecimal dailyRate;
    private final boolean complimentaryBreakfast;
    private final BigDecimal extraGuestCharge;
    private final BigDecimal taxRate;
    private final int standardCapacity;

    RoomType(String displayName, String dailyRate, boolean complimentaryBreakfast,
             String extraGuestCharge, String taxRate, int standardCapacity) {
        this.displayName = displayName;
        this.dailyRate = new BigDecimal(dailyRate);
        this.complimentaryBreakfast = complimentaryBreakfast;
        this.extraGuestCharge = new BigDecimal(extraGuestCharge);
        this.taxRate = new BigDecimal(taxRate);
        this.standardCapacity = standardCapacity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public boolean hasComplimentaryBreakfast() {
        return complimentaryBreakfast;
    }

    /** Charge per extra guest, per night. */
    public BigDecimal getExtraGuestCharge() {
        return extraGuestCharge;
    }

    /** Tax rate as a fraction, e.g. 0.12 for 12%. */
    public BigDecimal getTaxRate() {
        return taxRate;
    }

    /** Tax rate as a whole percentage for display, e.g. 12. */
    public int getTaxPercentage() {
        return taxRate.movePointRight(2).intValue();
    }

    /** Number of guests covered by the daily rate. */
    public int getStandardCapacity() {
        return standardCapacity;
    }

    /** Absolute maximum guests allowed in this room type. */
    public int getMaximumGuests() {
        return standardCapacity + MAX_EXTRA_GUESTS;
    }

    /** Converts a stored name such as "DELUXE" back into the enum safely. */
    public static RoomType fromName(String name) {
        for (RoomType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown room type: " + name);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
