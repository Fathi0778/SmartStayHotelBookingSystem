package smartstay.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import smartstay.model.BedType;
import smartstay.model.Reservation;
import smartstay.model.Room;
import smartstay.model.RoomStatus;
import smartstay.model.RoomType;
import smartstay.security.AccessControl;
import smartstay.storage.DataStore;
import smartstay.util.BookingValidator;
import smartstay.util.InputValidator;
import smartstay.util.SortUtil;

/**
 * Room management (administrator only) and room availability searching.
 *
 * Every method that changes a room calls AccessControl.requireAdministrator()
 * FIRST, so a guest can never add, update or delete a room, even if a room
 * screen were opened by mistake.
 */
public class RoomService {

    /** Sort rooms by daily rate, then by room ID when rates are equal. */
    public static final Comparator<Room> BY_PRICE = new Comparator<Room>() {
        @Override
        public int compare(Room a, Room b) {
            int byRate = a.getDailyRate().compareTo(b.getDailyRate());
            return byRate != 0 ? byRate : a.getRoomId().compareTo(b.getRoomId());
        }
    };

    private final DataStore dataStore = DataStore.getInstance();

    // =====================================================================
    // Administrator operations
    // =====================================================================

    public Room addRoom(String roomId, RoomType roomType, BedType bedType) {
        AccessControl.requireAdministrator();
        InputValidator.requireText(roomId, "Room ID");
        String cleanId = roomId.trim().toUpperCase();
        if (!InputValidator.isValidRoomId(cleanId)) {
            throw new IllegalArgumentException("Room ID must be 1-3 letters followed by 2-4 digits (e.g. R101).");
        }
        if (roomType == null || bedType == null) {
            throw new IllegalArgumentException("Please select a room type and a bed type.");
        }
        if (dataStore.findRoomById(cleanId) != null) {
            throw new IllegalArgumentException("Room ID " + cleanId + " already exists. Room IDs must be unique.");
        }
        Room room = new Room(cleanId, roomType, bedType, RoomStatus.AVAILABLE);
        try {
            dataStore.addRoom(room);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save the room: " + ex.getMessage(), ex);
        }
        return room;
    }

    /**
     * Updates room type and bed type. The room type cannot be changed while
     * the room has upcoming bookings, because those guests booked (and are
     * paying for) the original room type.
     */
    public Room updateRoom(String roomId, RoomType newType, BedType newBed) {
        AccessControl.requireAdministrator();
        Room room = getExistingRoom(roomId);
        if (newType == null || newBed == null) {
            throw new IllegalArgumentException("Please select a room type and a bed type.");
        }
        if (newType != room.getRoomType() && countUpcomingReservations(room.getRoomId()) > 0) {
            throw new IllegalArgumentException("Room type cannot be changed because room " + room.getRoomId()
                    + " has upcoming reservations. You can still change the bed type.");
        }
        RoomType oldType = room.getRoomType();
        BedType oldBed = room.getBedType();
        room.setRoomType(newType);
        room.setBedType(newBed);
        try {
            dataStore.saveRooms();
        } catch (IOException ex) {
            room.setRoomType(oldType);
            room.setBedType(oldBed);
            throw new IllegalStateException("Could not save the room: " + ex.getMessage(), ex);
        }
        return room;
    }

    /**
     * Removes a room. To protect booking history and invoices, a room that
     * appears in ANY reservation cannot be deleted; it can be set to
     * Under Maintenance instead.
     */
    public void deleteRoom(String roomId) {
        AccessControl.requireAdministrator();
        Room room = getExistingRoom(roomId);
        for (Reservation reservation : dataStore.getReservations()) {
            if (reservation.getRoomId().equals(room.getRoomId())) {
                throw new IllegalArgumentException("Room " + room.getRoomId()
                        + " has reservation records and cannot be deleted. "
                        + "Set it to Under Maintenance to stop new bookings.");
            }
        }
        try {
            dataStore.removeRoom(room);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete the room: " + ex.getMessage(), ex);
        }
    }

    /**
     * Changes the administrator-controlled status. A room with upcoming
     * guests cannot be put under maintenance, so confirmed bookings are
     * never silently broken.
     */
    public void changeAvailabilityStatus(String roomId, RoomStatus newStatus) {
        AccessControl.requireAdministrator();
        Room room = getExistingRoom(roomId);
        if (newStatus == RoomStatus.RESERVED) {
            throw new IllegalArgumentException("Reserved is set automatically when a guest books the room.");
        }
        if (newStatus == RoomStatus.UNDER_MAINTENANCE) {
            int upcoming = countUpcomingReservations(room.getRoomId());
            if (upcoming > 0) {
                throw new IllegalArgumentException("Room " + room.getRoomId() + " has " + upcoming
                        + " upcoming reservation(s). Cancel them before putting the room under maintenance.");
            }
        }
        RoomStatus oldStatus = room.getMaintenanceStatus();
        room.setMaintenanceStatus(newStatus);
        try {
            dataStore.saveRooms();
        } catch (IOException ex) {
            room.setMaintenanceStatus(oldStatus);
            throw new IllegalStateException("Could not save the room status: " + ex.getMessage(), ex);
        }
    }

    /** All rooms, for the administrator's room table. */
    public List<Room> getAllRooms() {
        AccessControl.requireAdministrator();
        return new ArrayList<>(dataStore.getRooms());
    }

    /**
     * Room searching (linear search, O(n)).
     * Matches the keyword against room ID, type and bed type, and applies
     * optional type and status filters. A null filter means "any".
     */
    public List<Room> searchRooms(String keyword, RoomType typeFilter, RoomStatus statusFilter) {
        AccessControl.requireAdministrator();
        String term = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Room> results = new ArrayList<>();
        for (Room room : dataStore.getRooms()) {
            boolean keywordMatch = term.isEmpty()
                    || room.getRoomId().toLowerCase().contains(term)
                    || room.getRoomType().getDisplayName().toLowerCase().contains(term)
                    || room.getBedType().getDisplayName().toLowerCase().contains(term);
            boolean typeMatch = typeFilter == null || room.getRoomType() == typeFilter;
            boolean statusMatch = statusFilter == null || getCurrentStatus(room) == statusFilter;
            if (keywordMatch && typeMatch && statusMatch) {
                results.add(room);
            }
        }
        return results;
    }

    /** Number of rooms currently in each status (for the admin dashboard). */
    public int countRoomsByStatus(RoomStatus status) {
        AccessControl.requireAdministrator();
        int count = 0;
        for (Room room : dataStore.getRooms()) {
            if (getCurrentStatus(room) == status) {
                count++;
            }
        }
        return count;
    }

    public int countAllRooms() {
        AccessControl.requireAdministrator();
        return dataStore.getRooms().size();
    }

    // =====================================================================
    // Availability (used by guests and administrators)
    // =====================================================================

    /**
     * The status shown on screen:
     *  - Under Maintenance if the admin set it,
     *  - Reserved if a confirmed booking is current or upcoming,
     *  - otherwise Available.
     */
    public RoomStatus getCurrentStatus(Room room) {
        if (room.isUnderMaintenance()) {
            return RoomStatus.UNDER_MAINTENANCE;
        }
        return countUpcomingReservations(room.getRoomId()) > 0 ? RoomStatus.RESERVED : RoomStatus.AVAILABLE;
    }

    /**
     * Room availability check for a date range (O(m), m = reservations).
     * A room is free when it is not under maintenance and no CONFIRMED
     * reservation overlaps the requested dates. Cancelled bookings are ignored.
     *
     * @param ignoreBookingId a booking to ignore (used when modifying that booking), or null
     */
    public boolean isRoomAvailableForDates(String roomId, LocalDate checkIn, LocalDate checkOut,
                                           String ignoreBookingId) {
        Room room = dataStore.findRoomById(roomId);
        if (room == null || room.isUnderMaintenance()) {
            return false;
        }
        for (Reservation reservation : dataStore.getReservations()) {
            if (!reservation.isActive() || !reservation.getRoomId().equals(room.getRoomId())) {
                continue;
            }
            if (reservation.getBookingId().equals(ignoreBookingId)) {
                continue;
            }
            if (BookingValidator.datesOverlap(checkIn, checkOut,
                    reservation.getCheckInDate(), reservation.getCheckOutDate())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds rooms a guest can book for the given dates.
     * typeFilter may be null for "all types". Results are sorted by price
     * (low to high) using merge sort. O(n x m) for n rooms, m reservations.
     */
    public List<Room> findAvailableRooms(RoomType typeFilter, LocalDate checkIn, LocalDate checkOut,
                                         int numberOfGuests) {
        AccessControl.requireLogin();
        BookingValidator.validateStayDates(checkIn, checkOut, LocalDate.now());
        if (numberOfGuests < 1) {
            throw new IllegalArgumentException("Number of guests must be at least 1.");
        }
        if (typeFilter != null) {
            BookingValidator.validateGuestCount(typeFilter, numberOfGuests);
        }
        List<Room> available = new ArrayList<>();
        for (Room room : dataStore.getRooms()) {
            if (typeFilter != null && room.getRoomType() != typeFilter) {
                continue;
            }
            if (numberOfGuests > room.getRoomType().getMaximumGuests()) {
                continue;
            }
            if (isRoomAvailableForDates(room.getRoomId(), checkIn, checkOut, null)) {
                available.add(room);
            }
        }
        return sortByPrice(available, true);
    }

    /** Sorting rooms by price using the hand-written merge sort (O(n log n)). */
    public List<Room> sortByPrice(List<Room> rooms, boolean lowestFirst) {
        Comparator<Room> order = lowestFirst ? BY_PRICE : java.util.Collections.reverseOrder(BY_PRICE);
        return SortUtil.mergeSort(rooms, order);
    }

    /** Room look-up by ID using the HashMap (O(1)). */
    public Room findRoom(String roomId) {
        AccessControl.requireLogin();
        return dataStore.findRoomById(roomId);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Room getExistingRoom(String roomId) {
        Room room = dataStore.findRoomById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room " + (roomId == null ? "" : roomId.trim().toUpperCase())
                    + " was not found.");
        }
        return room;
    }

    /** Confirmed reservations for this room that have not finished yet. */
    private int countUpcomingReservations(String roomId) {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Reservation reservation : dataStore.getReservations()) {
            if (reservation.getRoomId().equals(roomId) && reservation.isCurrentOrUpcoming(today)) {
                count++;
            }
        }
        return count;
    }
}
