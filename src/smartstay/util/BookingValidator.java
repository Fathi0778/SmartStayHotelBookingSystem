package smartstay.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import smartstay.model.RoomType;

/**
 * All SmartStay booking rules in one place.
 *
 * Each rule is a small, separate method so it is easy to read, test and
 * explain. Methods that fail throw IllegalArgumentException with a message
 * that can be shown directly to the user.
 */
public final class BookingValidator {

    /** Rule: guests must book at least 2 days in advance. */
    public static final int MIN_DAYS_IN_ADVANCE = 2;

    /** Rule: no changes or cancellations within 24 hours of check-in. */
    public static final int CHANGE_CUTOFF_HOURS = 24;

    /** Assumption: standard hotel check-in time is 2:00 PM. */
    public static final LocalTime CHECK_IN_TIME = LocalTime.of(14, 0);

    /** Assumption: the longest single booking allowed. */
    public static final int MAX_NIGHTS = 30;

    private BookingValidator() {
        // Utility class - no objects needed.
    }

    /**
     * Checks the stay dates for a NEW booking (or a modified one).
     *  - both dates must be given
     *  - check-in must be at least 2 days after today
     *  - check-out must be later than check-in
     *  - stay length must be reasonable
     */
    public static void validateStayDates(LocalDate checkIn, LocalDate checkOut, LocalDate today) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Please select both check-in and check-out dates.");
        }
        if (checkIn.isBefore(today)) {
            throw new IllegalArgumentException("Check-in date cannot be in the past.");
        }
        LocalDate earliestCheckIn = today.plusDays(MIN_DAYS_IN_ADVANCE);
        if (checkIn.isBefore(earliestCheckIn)) {
            throw new IllegalArgumentException("Bookings must be made at least " + MIN_DAYS_IN_ADVANCE
                    + " days in advance. The earliest check-in date is "
                    + FormatUtil.formatDate(earliestCheckIn) + ".");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be later than the check-in date.");
        }
        if (calculateNights(checkIn, checkOut) > MAX_NIGHTS) {
            throw new IllegalArgumentException("A single booking cannot be longer than " + MAX_NIGHTS + " nights.");
        }
    }

    /** Checks the number of guests against the room type's maximum. */
    public static void validateGuestCount(RoomType roomType, int numberOfGuests) {
        if (roomType == null) {
            throw new IllegalArgumentException("Please select a room type.");
        }
        if (numberOfGuests < 1) {
            throw new IllegalArgumentException("Number of guests must be at least 1.");
        }
        if (numberOfGuests > roomType.getMaximumGuests()) {
            throw new IllegalArgumentException("A " + roomType + " room can hold a maximum of "
                    + roomType.getMaximumGuests() + " guests (" + roomType.getStandardCapacity()
                    + " included + " + RoomType.MAX_EXTRA_GUESTS + " extra).");
        }
    }

    /** Number of nights between two dates. O(1). */
    public static long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * Date-overlap detection. Two stays overlap when each one starts before
     * the other one ends. Check-out day is free for the next guest, so a stay
     * ending on the 10th does NOT overlap a stay starting on the 10th.
     * O(1).
     */
    public static boolean datesOverlap(LocalDate startA, LocalDate endA, LocalDate startB, LocalDate endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    /** The moment after which a booking can no longer be changed or cancelled. */
    public static LocalDateTime getChangeDeadline(LocalDate checkIn) {
        return checkIn.atTime(CHECK_IN_TIME).minusHours(CHANGE_CUTOFF_HOURS);
    }

    /** Cancellation / modification eligibility: more than 24 hours before check-in. */
    public static boolean isChangeAllowed(LocalDate checkIn, LocalDateTime now) {
        return now.isBefore(getChangeDeadline(checkIn));
    }

    /** Throws a clear message if the 24-hour rule blocks the change. */
    public static void requireChangeAllowed(LocalDate checkIn, LocalDateTime now, String action) {
        if (!isChangeAllowed(checkIn, now)) {
            throw new IllegalArgumentException("This reservation cannot be " + action
                    + " because it is within " + CHANGE_CUTOFF_HOURS + " hours of check-in ("
                    + FormatUtil.formatDateTime(checkIn.atTime(CHECK_IN_TIME)) + ").");
        }
    }

    /** Booking advance validation: exactly LKR 10,000 must be charged. */
    public static void validateAdvance(BigDecimal advance) {
        if (advance == null || advance.compareTo(InvoiceCalculator.BOOKING_ADVANCE) != 0) {
            throw new IllegalArgumentException("A refundable booking advance of "
                    + FormatUtil.formatMoney(InvoiceCalculator.BOOKING_ADVANCE) + " is required.");
        }
    }
}
