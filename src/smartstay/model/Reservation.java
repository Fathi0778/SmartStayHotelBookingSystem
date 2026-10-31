package smartstay.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A booking of one room by one guest for a range of dates.
 *
 * Associations:
 *  - Guest 1 ----- * Reservation   (guestId)
 *  - Room  1 ----- * Reservation   (roomId)
 *  - Reservation 1 ----- 1 Invoice (the invoice is calculated from it)
 *
 * The room type is copied into the reservation when it is made, so the
 * invoice of an old booking does not change if the room is later updated.
 */
public class Reservation {

    private final String bookingId;
    private final String guestId;
    private final String roomId;
    private final RoomType roomType;
    private int numberOfGuests;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private final LocalDateTime bookingDateTime;
    private final BigDecimal advancePaid;
    private BookingStatus status;
    private LocalDateTime cancelledDateTime;

    public Reservation(String bookingId, String guestId, String roomId, RoomType roomType,
                       int numberOfGuests, LocalDate checkInDate, LocalDate checkOutDate,
                       LocalDateTime bookingDateTime, BigDecimal advancePaid,
                       BookingStatus status, LocalDateTime cancelledDateTime) {
        if (bookingId == null || guestId == null || roomId == null || roomType == null
                || bookingDateTime == null || advancePaid == null || status == null) {
            throw new IllegalArgumentException("Reservation details are incomplete.");
        }
        this.bookingId = bookingId;
        this.guestId = guestId;
        this.roomId = roomId;
        this.roomType = roomType;
        this.bookingDateTime = bookingDateTime;
        this.advancePaid = advancePaid;
        this.status = status;
        this.cancelledDateTime = cancelledDateTime;
        setNumberOfGuests(numberOfGuests);
        setStayDates(checkInDate, checkOutDate);
    }

    /** Number of nights = days between check-in and check-out. */
    public long getNumberOfNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    /** Only confirmed reservations block a room. Cancelled ones do not. */
    public boolean isActive() {
        return status == BookingStatus.CONFIRMED;
    }

    /** True when the stay has not finished yet (check-out is today or later). */
    public boolean isCurrentOrUpcoming(LocalDate today) {
        return isActive() && !checkOutDate.isBefore(today);
    }

    /** Marks the booking as cancelled but keeps it for history. */
    public void cancel(LocalDateTime when) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledDateTime = when;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getGuestId() {
        return guestId;
    }

    public String getRoomId() {
        return roomId;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public final void setNumberOfGuests(int numberOfGuests) {
        if (numberOfGuests < 1) {
            throw new IllegalArgumentException("Number of guests must be at least 1.");
        }
        this.numberOfGuests = numberOfGuests;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    /** Dates are changed together so check-out can never be before check-in. */
    public final void setStayDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be later than check-in date.");
        }
        this.checkInDate = checkIn;
        this.checkOutDate = checkOut;
    }

    public LocalDateTime getBookingDateTime() {
        return bookingDateTime;
    }

    public BigDecimal getAdvancePaid() {
        return advancePaid;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCancelledDateTime() {
        return cancelledDateTime;
    }
}
