package smartstay.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import smartstay.model.BookingStatus;
import smartstay.model.Guest;
import smartstay.model.Invoice;
import smartstay.model.Reservation;
import smartstay.model.Room;
import smartstay.model.RoomType;

/**
 * Calculates the final bill, following the coursework order exactly:
 *
 *  Step 1  Base room charge   = daily rate x nights
 *  Step 2  Extra guest charge = extra guests x charge per guest x nights
 *  Step 3  Discount           = 12% of the BASE room charge if nights >= 5
 *  Step 4  Tax                = tax rate x (base - discount + extra guests)
 *  Step 5  Deduct the LKR 10,000 refundable advance (if it was paid)
 *  Step 6  Final payable      = total - advance (never below zero;
 *                               any difference is shown as a refund)
 *
 * BigDecimal is used for all money so there are no rounding errors
 * like 0.1 + 0.2 = 0.30000000000000004 that happen with double.
 */
public final class InvoiceCalculator {

    public static final BigDecimal BOOKING_ADVANCE = new BigDecimal("10000.00");
    public static final BigDecimal LONG_STAY_DISCOUNT_RATE = new BigDecimal("0.12");
    public static final int LONG_STAY_MIN_NIGHTS = 5;

    private static final int MONEY_SCALE = 2;

    private InvoiceCalculator() {
        // Utility class - no objects needed.
    }

    /** Step 1: base room charge. */
    public static BigDecimal calculateBaseCharge(RoomType roomType, long nights) {
        return money(roomType.getDailyRate().multiply(BigDecimal.valueOf(nights)));
    }

    /** How many guests are above the room's standard capacity. */
    public static int calculateExtraGuests(RoomType roomType, int numberOfGuests) {
        return Math.max(0, numberOfGuests - roomType.getStandardCapacity());
    }

    /** Step 2: extra guest charge (assumption: charged per extra guest, per night). */
    public static BigDecimal calculateExtraGuestCharge(RoomType roomType, int numberOfGuests, long nights) {
        int extraGuests = calculateExtraGuests(roomType, numberOfGuests);
        return money(roomType.getExtraGuestCharge()
                .multiply(BigDecimal.valueOf(extraGuests))
                .multiply(BigDecimal.valueOf(nights)));
    }

    /** Step 3: 12% discount on the base room charge for stays of 5+ nights. */
    public static BigDecimal calculateDiscount(BigDecimal baseCharge, long nights) {
        if (nights >= LONG_STAY_MIN_NIGHTS) {
            return money(baseCharge.multiply(LONG_STAY_DISCOUNT_RATE));
        }
        return money(BigDecimal.ZERO);
    }

    /** Step 4: tax on the discounted room charge plus extra guest charges. */
    public static BigDecimal calculateTax(RoomType roomType, BigDecimal taxableAmount) {
        return money(taxableAmount.multiply(roomType.getTaxRate()));
    }

    /**
     * Runs all six steps and returns a complete, read-only Invoice.
     * The room may be null if its record no longer exists; the reservation
     * keeps its own copy of the room type, so pricing is still correct.
     */
    public static Invoice calculate(Reservation reservation, Guest guest, Room room) {
        RoomType roomType = reservation.getRoomType();
        long nights = reservation.getNumberOfNights();
        int guests = reservation.getNumberOfGuests();

        // Step 1
        BigDecimal baseCharge = calculateBaseCharge(roomType, nights);
        // Step 2
        int extraGuests = calculateExtraGuests(roomType, guests);
        BigDecimal extraGuestCharge = calculateExtraGuestCharge(roomType, guests, nights);
        // Step 3
        BigDecimal discount = calculateDiscount(baseCharge, nights);
        // Step 4
        BigDecimal taxableAmount = money(baseCharge.subtract(discount).add(extraGuestCharge));
        BigDecimal tax = calculateTax(roomType, taxableAmount);
        BigDecimal totalCharge = money(taxableAmount.add(tax));

        // Steps 5 and 6
        BigDecimal advanceDeducted;
        BigDecimal finalPayable;
        BigDecimal refundDue;

        if (reservation.getStatus() == BookingStatus.CANCELLED) {
            // Cancelled booking: nothing is payable and the advance is returned.
            advanceDeducted = money(BigDecimal.ZERO);
            finalPayable = money(BigDecimal.ZERO);
            refundDue = money(reservation.getAdvancePaid());
        } else {
            advanceDeducted = money(reservation.getAdvancePaid().min(totalCharge));
            finalPayable = money(totalCharge.subtract(advanceDeducted));
            refundDue = money(reservation.getAdvancePaid().subtract(advanceDeducted));
        }

        return new Invoice(reservation, guest, room, LocalDateTime.now(), nights, extraGuests,
                baseCharge, extraGuestCharge, discount, taxableAmount, tax, totalCharge,
                advanceDeducted, finalPayable, refundDue);
    }

    /** Rounds a money value to 2 decimal places (half up, as on a receipt). */
    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
