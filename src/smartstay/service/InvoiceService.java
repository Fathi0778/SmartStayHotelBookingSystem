package smartstay.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import smartstay.model.BookingStatus;
import smartstay.model.Guest;
import smartstay.model.Invoice;
import smartstay.model.Reservation;
import smartstay.model.Room;
import smartstay.model.RoomType;
import smartstay.security.AccessControl;
import smartstay.storage.DataStore;
import smartstay.util.BookingValidator;
import smartstay.util.InvoiceCalculator;

/**
 * Produces invoices. Guests can only get invoices for their OWN bookings,
 * because the reservation is fetched through an access check first.
 */
public class InvoiceService {

    private final DataStore dataStore = DataStore.getInstance();
    private final ReservationService reservationService = new ReservationService();

    /** The full invoice for a booking the current user may see. */
    public Invoice getInvoice(String bookingId) {
        Reservation reservation = reservationService.getReservation(bookingId);
        Guest guest = dataStore.findGuestById(reservation.getGuestId());
        Room room = dataStore.findRoomById(reservation.getRoomId());
        return InvoiceCalculator.calculate(reservation, guest, room);
    }

    /**
     * A price estimate shown on the booking screen BEFORE the booking is
     * confirmed. Nothing is saved.
     */
    public Invoice previewCharges(RoomType roomType, String roomId, int numberOfGuests,
                                  LocalDate checkIn, LocalDate checkOut) {
        Guest guest = AccessControl.requireGuest();
        BookingValidator.validateStayDates(checkIn, checkOut, LocalDate.now());
        BookingValidator.validateGuestCount(roomType, numberOfGuests);
        String previewRoomId = roomId == null ? "-" : roomId;
        Reservation preview = new Reservation("PREVIEW", guest.getGuestId(), previewRoomId, roomType,
                numberOfGuests, checkIn, checkOut, LocalDateTime.now(),
                InvoiceCalculator.BOOKING_ADVANCE, BookingStatus.CONFIRMED, null);
        return InvoiceCalculator.calculate(preview, guest, dataStore.findRoomById(previewRoomId));
    }
}
