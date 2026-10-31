package smartstay.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A detailed bill for one reservation.
 *
 * An Invoice is created only by InvoiceCalculator, which fills in every
 * amount. After that the invoice cannot be changed (all fields are final),
 * which makes it a safe, read-only record to display or print.
 */
public class Invoice {

    private final Reservation reservation;
    private final Guest guest;
    private final Room room;
    private final LocalDateTime generatedDateTime;

    private final long numberOfNights;
    private final int extraGuests;
    private final BigDecimal baseRoomCharge;
    private final BigDecimal extraGuestCharge;
    private final BigDecimal discount;
    private final BigDecimal taxableAmount;
    private final BigDecimal tax;
    private final BigDecimal totalCharge;
    private final BigDecimal advanceDeducted;
    private final BigDecimal finalPayable;
    private final BigDecimal refundDue;

    public Invoice(Reservation reservation, Guest guest, Room room, LocalDateTime generatedDateTime,
                   long numberOfNights, int extraGuests, BigDecimal baseRoomCharge,
                   BigDecimal extraGuestCharge, BigDecimal discount, BigDecimal taxableAmount,
                   BigDecimal tax, BigDecimal totalCharge, BigDecimal advanceDeducted,
                   BigDecimal finalPayable, BigDecimal refundDue) {
        this.reservation = reservation;
        this.guest = guest;
        this.room = room;
        this.generatedDateTime = generatedDateTime;
        this.numberOfNights = numberOfNights;
        this.extraGuests = extraGuests;
        this.baseRoomCharge = baseRoomCharge;
        this.extraGuestCharge = extraGuestCharge;
        this.discount = discount;
        this.taxableAmount = taxableAmount;
        this.tax = tax;
        this.totalCharge = totalCharge;
        this.advanceDeducted = advanceDeducted;
        this.finalPayable = finalPayable;
        this.refundDue = refundDue;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Guest getGuest() {
        return guest;
    }

    /** May be null only if the room record was removed (normally prevented). */
    public Room getRoom() {
        return room;
    }

    public LocalDateTime getGeneratedDateTime() {
        return generatedDateTime;
    }

    public long getNumberOfNights() {
        return numberOfNights;
    }

    public int getExtraGuests() {
        return extraGuests;
    }

    public BigDecimal getBaseRoomCharge() {
        return baseRoomCharge;
    }

    public BigDecimal getExtraGuestCharge() {
        return extraGuestCharge;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTaxableAmount() {
        return taxableAmount;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public BigDecimal getTotalCharge() {
        return totalCharge;
    }

    public BigDecimal getAdvanceDeducted() {
        return advanceDeducted;
    }

    public BigDecimal getFinalPayable() {
        return finalPayable;
    }

    public BigDecimal getRefundDue() {
        return refundDue;
    }

    public boolean isDiscountApplied() {
        return discount.signum() > 0;
    }
}
