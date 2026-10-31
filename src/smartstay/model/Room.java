package smartstay.model;

import java.math.BigDecimal;

/**
 * A single hotel room.
 *
 * The daily rate is NOT stored in the room. It is always taken from the
 * RoomType, so the rate is decided automatically by the room type and can
 * never be typed in wrongly.
 *
 * The stored status is only AVAILABLE or UNDER_MAINTENANCE (set by the
 * administrator). The RESERVED status is calculated by RoomService from
 * the room's active reservations.
 */
public class Room {

    private final String roomId;
    private RoomType roomType;
    private BedType bedType;
    private RoomStatus maintenanceStatus;

    public Room(String roomId, RoomType roomType, BedType bedType, RoomStatus maintenanceStatus) {
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID is required.");
        }
        this.roomId = roomId.trim().toUpperCase();
        setRoomType(roomType);
        setBedType(bedType);
        setMaintenanceStatus(maintenanceStatus);
    }

    public String getRoomId() {
        return roomId;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public final void setRoomType(RoomType roomType) {
        if (roomType == null) {
            throw new IllegalArgumentException("Room type is required.");
        }
        this.roomType = roomType;
    }

    public BedType getBedType() {
        return bedType;
    }

    public final void setBedType(BedType bedType) {
        if (bedType == null) {
            throw new IllegalArgumentException("Bed type is required.");
        }
        this.bedType = bedType;
    }

    /** The administrator-controlled status (Available or Under Maintenance). */
    public RoomStatus getMaintenanceStatus() {
        return maintenanceStatus;
    }

    public final void setMaintenanceStatus(RoomStatus status) {
        if (status == null || status == RoomStatus.RESERVED) {
            throw new IllegalArgumentException(
                    "A room can only be set to Available or Under Maintenance. "
                    + "Reserved is set automatically by bookings.");
        }
        this.maintenanceStatus = status;
    }

    public boolean isUnderMaintenance() {
        return maintenanceStatus == RoomStatus.UNDER_MAINTENANCE;
    }

    /** Daily rate comes from the central room package values. */
    public BigDecimal getDailyRate() {
        return roomType.getDailyRate();
    }

    @Override
    public String toString() {
        return roomId + " - " + roomType + " (" + bedType + ")";
    }
}
