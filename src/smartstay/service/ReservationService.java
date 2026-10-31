package smartstay.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import smartstay.model.BookingStatus;
import smartstay.model.Guest;
import smartstay.model.Reservation;
import smartstay.model.Room;
import smartstay.model.RoomType;
import smartstay.model.User;
import smartstay.security.AccessControl;
import smartstay.storage.DataStore;
import smartstay.util.BookingValidator;
import smartstay.util.InputValidator;
import smartstay.util.InvoiceCalculator;
import smartstay.util.SortUtil;

/**
 * Making, searching, modifying and cancelling reservations.
 *
 * Privacy rule: every list or search starts from getVisibleReservations().
 * For an administrator that is every booking; for a guest it is ONLY their
 * own bookings. So whatever a guest types in the search box (another booking
 * ID or another person's name), they can never see someone else's booking.
 */
public class ReservationService {

    /** Sort by check-in date, then booking ID. */
    public static final Comparator<Reservation> BY_CHECK_IN = new Comparator<Reservation>() {
        @Override
        public int compare(Reservation a, Reservation b) {
            int byDate = a.getCheckInDate().compareTo(b.getCheckInDate());
            return byDate != 0 ? byDate : a.getBookingId().compareTo(b.getBookingId());
        }
    };

    private final DataStore dataStore = DataStore.getInstance();
    private final RoomService roomService = new RoomService();

    // =====================================================================
    // Booking
    // =====================================================================

    /**
     * Makes a reservation for the logged-in guest.
     * Applies every coursework booking rule before saving.
     */
    public Reservation createReservation(RoomType roomType, String roomId, int numberOfGuests,
                                         LocalDate checkIn, LocalDate checkOut) {
        Guest guest = AccessControl.requireGuest();

        // Rule: room type, guests, check-in and check-out are all required.
        if (roomType == null) {
            throw new IllegalArgumentException("Please select a room type.");
        }
        if (InputValidator.isBlank(roomId)) {
            throw new IllegalArgumentException("Please select a room. Use 'Check Availability' first.");
        }
        BookingValidator.validateStayDates(checkIn, checkOut, LocalDate.now());
        BookingValidator.validateGuestCount(roomType, numberOfGuests);

        Room room = dataStore.findRoomById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room " + roomId + " was not found.");
        }
        if (room.getRoomType() != roomType) {
            throw new IllegalArgumentException("Room " + room.getRoomId() + " is not a " + roomType + " room.");
        }
        // Rule: cannot book a room that is not available (maintenance or overlapping booking).
        if (!roomService.isRoomAvailableForDates(room.getRoomId(), checkIn, checkOut, null)) {
            throw new IllegalArgumentException("Room " + room.getRoomId()
                    + " is not available for the selected dates. Please choose another room or date.");
        }

        // Rule: LKR 10,000 refundable advance is charged at booking time.
        BookingValidator.validateAdvance(InvoiceCalculator.BOOKING_ADVANCE);

        Reservation reservation = new Reservation(generateBookingId(), guest.getGuestId(), room.getRoomId(),
                roomType, numberOfGuests, checkIn, checkOut, LocalDateTime.now(),
                InvoiceCalculator.BOOKING_ADVANCE, BookingStatus.CONFIRMED, null);
        try {
            dataStore.addReservation(reservation);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save the reservation: " + ex.getMessage(), ex);
        }
        return reservation;
    }

    /** Booking IDs are BK00001, BK00002 ... and never reused. */
    public String previewNextBookingId() {
        return generateBookingId();
    }

    private String generateBookingId() {
        int highest = 0;
        for (Reservation reservation : dataStore.getReservations()) {
            try {
                highest = Math.max(highest, Integer.parseInt(reservation.getBookingId().replaceAll("\\D", "")));
            } catch (NumberFormatException ex) {
                // Ignore IDs without a number.
            }
        }
        return String.format("BK%05d", highest + 1);
    }

    // =====================================================================
    // Viewing and searching
    // =====================================================================

    /**
     * Guest-specific filtering (O(n)).
     * Administrators get every booking; guests get only their own.
     */
    public List<Reservation> getVisibleReservations() {
        User user = AccessControl.requireLogin();
        List<Reservation> visible = new ArrayList<>();
        for (Reservation reservation : dataStore.getReservations()) {
            if (user.isAdministrator() || reservation.getGuestId().equals(user.getUserId())) {
                visible.add(reservation);
            }
        }
        return sortByCheckInDate(visible);
    }

    /**
     * Reservation searching (linear search, O(n)).
     * keyword matches a booking ID or a guest name (partial, case-insensitive).
     * checkInDate (optional) keeps only bookings that start on that date.
     */
    public List<Reservation> searchReservations(String keyword, LocalDate checkInDate) {
        String term = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Reservation> results = new ArrayList<>();
        for (Reservation reservation : getVisibleReservations()) {
            boolean keywordMatch = term.isEmpty()
                    || reservation.getBookingId().toLowerCase().contains(term)
                    || getGuestName(reservation).toLowerCase().contains(term);
            boolean dateMatch = checkInDate == null || reservation.getCheckInDate().equals(checkInDate);
            if (keywordMatch && dateMatch) {
                results.add(reservation);
            }
        }
        return results;
    }

    /** One reservation by ID (O(1) HashMap) - only if the user may see it. */
    public Reservation getReservation(String bookingId) {
        Reservation reservation = dataStore.findReservationById(bookingId);
        AccessControl.requireReservationAccess(reservation);
        return reservation;
    }

    /** Guest name for a reservation the user is allowed to see. */
    public String getGuestName(Reservation reservation) {
        AccessControl.requireReservationAccess(reservation);
        Guest guest = dataStore.findGuestById(reservation.getGuestId());
        return guest == null ? "(unknown guest)" : guest.getFullName();
    }

    /** Sorting reservations by check-in date using merge sort (O(n log n)). */
    public List<Reservation> sortByCheckInDate(List<Reservation> reservations) {
        return SortUtil.mergeSort(reservations, BY_CHECK_IN);
    }

    // =====================================================================
    // Cancelling and modifying
    // =====================================================================

    /** Cancellation eligibility: confirmed AND more than 24 hours before check-in. */
    public boolean canChange(Reservation reservation) {
        return reservation.isActive()
                && BookingValidator.isChangeAllowed(reservation.getCheckInDate(), LocalDateTime.now());
    }

    /**
     * Cancels a booking. The booking is kept with status Cancelled (history
     * is preserved), it stops blocking the room, and the LKR 10,000 advance
     * is shown as refundable on its invoice.
     */
    public Reservation cancelReservation(String bookingId) {
        Reservation reservation = getReservation(bookingId);
        if (reservation.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking " + reservation.getBookingId() + " is already cancelled.");
        }
        BookingValidator.requireChangeAllowed(reservation.getCheckInDate(), LocalDateTime.now(), "cancelled");
        reservation.cancel(LocalDateTime.now());
        save();
        return reservation;
    }

    /**
     * Changes dates and/or number of guests. Allowed only more than 24 hours
     * before the ORIGINAL check-in, and the new details must pass every
     * booking rule (2 days in advance, no overlap, guest limit).
     */
    public Reservation modifyReservation(String bookingId, int newGuests, LocalDate newCheckIn,
                                         LocalDate newCheckOut) {
        Reservation reservation = getReservation(bookingId);
        if (!reservation.isActive()) {
            throw new IllegalArgumentException("Cancelled bookings cannot be modified.");
        }
        BookingValidator.requireChangeAllowed(reservation.getCheckInDate(), LocalDateTime.now(), "modified");
        BookingValidator.validateStayDates(newCheckIn, newCheckOut, LocalDate.now());
        BookingValidator.validateGuestCount(reservation.getRoomType(), newGuests);
        if (!roomService.isRoomAvailableForDates(reservation.getRoomId(), newCheckIn, newCheckOut,
                reservation.getBookingId())) {
            throw new IllegalArgumentException("Room " + reservation.getRoomId()
                    + " is not available for the new dates.");
        }

        int oldGuests = reservation.getNumberOfGuests();
        LocalDate oldCheckIn = reservation.getCheckInDate();
        LocalDate oldCheckOut = reservation.getCheckOutDate();
        reservation.setNumberOfGuests(newGuests);
        reservation.setStayDates(newCheckIn, newCheckOut);
        try {
            dataStore.saveReservations();
        } catch (IOException ex) {
            reservation.setNumberOfGuests(oldGuests);
            reservation.setStayDates(oldCheckIn, oldCheckOut);
            throw new IllegalStateException("Could not save the changes: " + ex.getMessage(), ex);
        }
        return reservation;
    }

    // =====================================================================
    // Dashboard statistics (calculated from real stored data)
    // =====================================================================

    public int countVisibleReservations() {
        return getVisibleReservations().size();
    }

    public int countUpcoming() {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Reservation reservation : getVisibleReservations()) {
            if (reservation.isCurrentOrUpcoming(today)) {
                count++;
            }
        }
        return count;
    }

    public int countCancelled() {
        int count = 0;
        for (Reservation reservation : getVisibleReservations()) {
            if (reservation.getStatus() == BookingStatus.CANCELLED) {
                count++;
            }
        }
        return count;
    }

    /** Current and upcoming confirmed bookings, soonest first. */
    public List<Reservation> getUpcomingReservations() {
        LocalDate today = LocalDate.now();
        List<Reservation> upcoming = new ArrayList<>();
        for (Reservation reservation : getVisibleReservations()) {
            if (reservation.isCurrentOrUpcoming(today)) {
                upcoming.add(reservation);
            }
        }
        return upcoming;
    }

    private void save() {
        try {
            dataStore.saveReservations();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save the changes: " + ex.getMessage(), ex);
        }
    }
}
