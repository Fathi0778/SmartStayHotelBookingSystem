package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import smartstay.model.Guest;
import smartstay.model.Invoice;
import smartstay.model.Reservation;
import smartstay.model.Room;
import smartstay.model.RoomType;
import smartstay.security.AccessControl;
import smartstay.service.InvoiceService;
import smartstay.service.ReservationService;
import smartstay.service.RoomService;
import smartstay.util.BookingValidator;
import smartstay.util.FormatUtil;
import smartstay.util.InvoiceCalculator;

/**
 * Guest booking screen.
 * Flow: choose type, guests and dates -> Check Availability -> pick a room
 * -> Calculate Charges -> Confirm Reservation.
 * The screen never decides the rules itself; ReservationService checks
 * every rule again when the booking is confirmed.
 */
public class ReservationFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;
    private static final String NO_ROOM = "Press 'Check Availability'";

    private final ReservationService reservationService = new ReservationService();
    private final RoomService roomService = new RoomService();
    private final InvoiceService invoiceService = new InvoiceService();
    private final Guest guest;

    private final JTextField bookingIdField;
    private final JTextField guestField;
    private final JComboBox<RoomType> roomTypeCombo = UITheme.createComboBox(RoomType.values());
    private final JSpinner guestsSpinner = UITheme.createNumberSpinner(1, 1, 6);
    private final JLabel capacityHint = UITheme.createMutedText(" ");
    private final JSpinner checkInSpinner;
    private final JSpinner checkOutSpinner;
    private final JTextField nightsField = UITheme.createReadOnlyField("1");
    private final JComboBox<String> roomCombo = UITheme.createComboBox(new String[]{NO_ROOM});
    private final JTextField advanceField = UITheme.createReadOnlyField(
            FormatUtil.formatMoney(InvoiceCalculator.BOOKING_ADVANCE) + " (refundable, paid now)");
    private final JLabel statusLabel = UITheme.createMutedText(" ");

    // Charge summary values
    private final JLabel rateValue = valueLabel();
    private final JLabel nightsValue = valueLabel();
    private final JLabel baseValue = valueLabel();
    private final JLabel extraValue = valueLabel();
    private final JLabel discountValue = valueLabel();
    private final JLabel taxValue = valueLabel();
    private final JLabel totalValue = valueLabel();
    private final JLabel advanceValue = valueLabel();
    private final JLabel payableValue = valueLabel();
    private final JLabel extraCaption = UITheme.createMutedText("Extra guest charges");
    private final JLabel taxCaption = UITheme.createMutedText("Tax");
    private final JLabel refundNote = UITheme.createMutedText(" ");

    /** Open an empty booking form (from the dashboard menu). */
    public ReservationFrame() {
        this(null, null, null, 1);
    }

    /** Open the booking form already filled from the room search. */
    public ReservationFrame(Room selectedRoom, LocalDate checkIn, LocalDate checkOut, int guests) {
        super("Make a Reservation", "Booking requires room type, number of guests and your stay dates.");
        this.guest = AccessControl.requireGuest();

        LocalDate earliest = LocalDate.now().plusDays(BookingValidator.MIN_DAYS_IN_ADVANCE);
        checkInSpinner = UITheme.createDateSpinner(checkIn != null ? checkIn : earliest);
        checkOutSpinner = UITheme.createDateSpinner(checkOut != null ? checkOut : earliest.plusDays(1));
        bookingIdField = UITheme.createReadOnlyField(reservationService.previewNextBookingId()
                + "  (assigned on confirmation)");
        guestField = UITheme.createReadOnlyField(guest.getFullName() + "  •  " + guest.getEmail());

        AppButton backButton = new AppButton("Back", AppButton.Style.SECONDARY);
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToDashboard();
            }
        });
        addPageAction(backButton);

        getContentArea().add(createFormCard(), BorderLayout.CENTER);
        getContentArea().add(createSummaryCard(), BorderLayout.EAST);

        if (selectedRoom != null) {
            roomTypeCombo.setSelectedItem(selectedRoom.getRoomType());
            guestsSpinner.setValue(Math.max(1, guests));
        }
        addChangeListeners();
        updateNightsAndHint();
        resetSummary();

        // Coming from the search screen: check straight away and pre-select the room.
        if (selectedRoom != null) {
            checkAvailability(false);
            roomCombo.setSelectedItem(selectedRoom.getRoomId());
            calculateCharges(false);
        }
    }

    // =====================================================================
    // Layout
    // =====================================================================

    private JPanel createFormCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 14));
        card.add(UITheme.createHeading("Booking details"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addPair(form, 0, "Booking ID", bookingIdField, "Registered Guest", guestField);
        addPair(form, 1, "Room Type", roomTypeCombo, "Number of Guests", guestsSpinner);
        addPair(form, 2, "Check-in Date", checkInSpinner, "Check-out Date", checkOutSpinner);
        addPair(form, 3, "Number of Nights", nightsField, "Room ID", roomCombo);
        addPair(form, 4, "Booking Advance", advanceField, " ", capacityHint);

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = 20;
        filler.weighty = 1;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        form.add(spacer, filler);
        card.add(form, BorderLayout.CENTER);

        AppButton checkButton = new AppButton("Check Availability", AppButton.Style.PRIMARY);
        AppButton calculateButton = new AppButton("Calculate Charges", AppButton.Style.SECONDARY);
        AppButton confirmButton = new AppButton("Confirm Reservation", AppButton.Style.SUCCESS);
        AppButton clearButton = new AppButton("Clear", AppButton.Style.SECONDARY);

        JPanel buttons = new JPanel(new GridLayout(1, 4, 10, 0));
        buttons.setOpaque(false);
        buttons.add(checkButton);
        buttons.add(calculateButton);
        buttons.add(confirmButton);
        buttons.add(clearButton);

        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setOpaque(false);
        south.add(statusLabel, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);

        checkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkAvailability(true);
            }
        });
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateCharges(true);
            }
        });
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmReservation();
            }
        });
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearForm();
            }
        });
        return card;
    }

    private JPanel createSummaryCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(360, 0));
        card.add(UITheme.createHeading("Charge summary"), BorderLayout.NORTH);

        JPanel rows = new JPanel(new GridBagLayout());
        rows.setOpaque(false);
        int r = 0;
        r = addSummaryRow(rows, r, UITheme.createMutedText("Daily room rate"), rateValue);
        r = addSummaryRow(rows, r, UITheme.createMutedText("Number of nights"), nightsValue);
        r = addSummaryRow(rows, r, UITheme.createMutedText("Base room charge"), baseValue);
        r = addSummaryRow(rows, r, extraCaption, extraValue);
        r = addSummaryRow(rows, r, UITheme.createMutedText("Long-stay discount (12%)"), discountValue);
        r = addSummaryRow(rows, r, taxCaption, taxValue);
        r = addSeparator(rows, r);
        JLabel totalCaption = UITheme.createFieldLabel("TOTAL CHARGE");
        totalCaption.setForeground(UITheme.TEXT);
        r = addSummaryRow(rows, r, totalCaption, totalValue);
        r = addSummaryRow(rows, r, UITheme.createMutedText("Less booking advance"), advanceValue);
        r = addSeparator(rows, r);
        JLabel payableCaption = UITheme.createHeading("Payable at check-out");
        payableValue.setFont(UITheme.FONT_HEADING);
        payableValue.setForeground(UITheme.PRIMARY);
        r = addSummaryRow(rows, r, payableCaption, payableValue);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = r++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 0, 0);
        rows.add(refundNote, c);
        // Keep the rows at the top of the card.
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = r;
        filler.weighty = 1;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        rows.add(spacer, filler);
        card.add(rows, BorderLayout.CENTER);

        JLabel rules = new JLabel("<html><div style='width:255px'><b>Booking rules</b><br>"
                + "• Book at least " + BookingValidator.MIN_DAYS_IN_ADVANCE + " days in advance<br>"
                + "• " + FormatUtil.formatMoney(InvoiceCalculator.BOOKING_ADVANCE)
                + " refundable advance is charged now<br>"
                + "• " + InvoiceCalculator.LONG_STAY_MIN_NIGHTS + "+ nights: 12% off the room charge<br>"
                + "• No changes or cancellation within " + BookingValidator.CHANGE_CUTOFF_HOURS
                + " hours of check-in</div></html>");
        rules.setFont(UITheme.FONT_SMALL);
        rules.setForeground(UITheme.MUTED);
        rules.setOpaque(true);
        rules.setBackground(UITheme.LIGHT_BLUE);
        rules.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        card.add(rules, BorderLayout.SOUTH);
        return card;
    }

    private void addPair(JPanel form, int row, String leftLabel, JComponent left, String rightLabel,
                         JComponent right) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridy = row * 2;
        c.gridx = 0;
        c.insets = new Insets(row == 0 ? 0 : 14, 0, 4, 10);
        form.add(UITheme.createFieldLabel(leftLabel), c);
        c.gridx = 1;
        c.insets = new Insets(row == 0 ? 0 : 14, 10, 4, 0);
        form.add(UITheme.createFieldLabel(rightLabel), c);
        c.gridy = row * 2 + 1;
        c.gridx = 0;
        c.insets = new Insets(0, 0, 0, 10);
        form.add(left, c);
        c.gridx = 1;
        c.insets = new Insets(0, 10, 0, 0);
        form.add(right, c);
    }

    private int addSummaryRow(JPanel panel, int row, JLabel caption, JLabel value) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 5, 8);
        panel.add(caption, c);
        c.gridx = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(5, 8, 5, 0);
        panel.add(value, c);
        return row + 1;
    }

    private int addSeparator(JPanel panel, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        panel.add(new JSeparator(), c);
        return row + 1;
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("-");
        label.setFont(UITheme.FONT_BODY);
        label.setForeground(UITheme.TEXT);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    // =====================================================================
    // Behaviour
    // =====================================================================

    /** Any change to type, guests or dates means availability must be checked again. */
    private void addChangeListeners() {
        ChangeListener changed = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                inputsChanged();
            }
        };
        guestsSpinner.addChangeListener(changed);
        checkInSpinner.addChangeListener(changed);
        checkOutSpinner.addChangeListener(changed);
        roomTypeCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inputsChanged();
            }
        });
    }

    private void inputsChanged() {
        updateNightsAndHint();
        roomCombo.removeAllItems();
        roomCombo.addItem(NO_ROOM);
        resetSummary();
        setStatus("Details changed - press 'Check Availability'.", UITheme.MUTED);
    }

    private void updateNightsAndHint() {
        long nights = BookingValidator.calculateNights(UITheme.getDate(checkInSpinner),
                UITheme.getDate(checkOutSpinner));
        nightsField.setText(nights > 0 ? String.valueOf(nights) : "Check-out must be after check-in");
        RoomType type = (RoomType) roomTypeCombo.getSelectedItem();
        capacityHint.setText(type.getStandardCapacity() + " guests included, maximum "
                + type.getMaximumGuests() + " (extra " + FormatUtil.formatMoney(type.getExtraGuestCharge())
                + "/guest/night)");
    }

    /** Fills the Room ID list with rooms free for the chosen type and dates. */
    private void checkAvailability(boolean showMessages) {
        try {
            RoomType type = (RoomType) roomTypeCombo.getSelectedItem();
            List<Room> rooms = roomService.findAvailableRooms(type, UITheme.getDate(checkInSpinner),
                    UITheme.getDate(checkOutSpinner), (Integer) guestsSpinner.getValue());
            roomCombo.removeAllItems();
            if (rooms.isEmpty()) {
                roomCombo.addItem(NO_ROOM);
                setStatus("No " + type + " rooms are available for these dates.", UITheme.DANGER);
                if (showMessages) {
                    UITheme.showError(this, "Sorry, no " + type + " rooms are available for these dates.\n"
                            + "Please try different dates or another room type.");
                }
                return;
            }
            for (Room room : rooms) {
                roomCombo.addItem(room.getRoomId());
            }
          setStatus(rooms.size() + " " + type + " room(s) available. Choose a Room ID, then "
                    + "calculate charges.", UITheme.SUCCESS);
        } catch (IllegalArgumentException ex) {
            setStatus(ex.getMessage(), UITheme.DANGER);
            if (showMessages) {
                UITheme.showError(this, ex.getMessage());
            }
        }
    }

    /** Shows the price estimate using the same InvoiceCalculator as the real invoice. */
    private void calculateCharges(boolean showMessages) {
        try {
            Invoice preview = invoiceService.previewCharges((RoomType) roomTypeCombo.getSelectedItem(),
                    selectedRoomId(), (Integer) guestsSpinner.getValue(),
                    UITheme.getDate(checkInSpinner), UITheme.getDate(checkOutSpinner));
            RoomType type = preview.getReservation().getRoomType();
            rateValue.setText(FormatUtil.formatMoney(type.getDailyRate()));
            nightsValue.setText(String.valueOf(preview.getNumberOfNights()));
            baseValue.setText(FormatUtil.formatMoney(preview.getBaseRoomCharge()));
            extraCaption.setText("Extra guest charges (" + preview.getExtraGuests() + ")");
            extraValue.setText(FormatUtil.formatMoney(preview.getExtraGuestCharge()));
            discountValue.setText("- " + FormatUtil.formatMoney(preview.getDiscount()));
            taxCaption.setText("Tax (" + type.getTaxPercentage() + "%)");
            taxValue.setText(FormatUtil.formatMoney(preview.getTax()));
            totalValue.setText(FormatUtil.formatMoney(preview.getTotalCharge()));
            advanceValue.setText("- " + FormatUtil.formatMoney(preview.getAdvanceDeducted()));
            payableValue.setText(FormatUtil.formatMoney(preview.getFinalPayable()));
            refundNote.setText(preview.getRefundDue().signum() > 0
                    ? "Advance balance refunded to you: " + FormatUtil.formatMoney(preview.getRefundDue())
                    : (preview.isDiscountApplied() ? "Long-stay discount applied." : " "));
        } catch (IllegalArgumentException ex) {
            resetSummary();
            if (showMessages) {
                UITheme.showError(this, ex.getMessage());
            }
        }
    }

    private void confirmReservation() {
        String roomId = selectedRoomId();
        if (roomId == null) {
            UITheme.showError(this, "Please press 'Check Availability' and choose a Room ID first.");
            return;
        }
        calculateCharges(false);
        String message = "Confirm this reservation?\n\n"
                + "Room: " + roomId + " (" + roomTypeCombo.getSelectedItem() + ")\n"
                + "Guests: " + guestsSpinner.getValue() + "\n"
                + "Stay: " + FormatUtil.formatDate(UITheme.getDate(checkInSpinner)) + " to "
                + FormatUtil.formatDate(UITheme.getDate(checkOutSpinner)) + " (" + nightsField.getText()
                + " nights)\n"
                + "Total charge: " + totalValue.getText() + "\n\n"
                + "A refundable advance of " + FormatUtil.formatMoney(InvoiceCalculator.BOOKING_ADVANCE)
                + " will be charged now.";
        if (!UITheme.confirm(this, message, "Confirm Reservation")) {
            return;
        }
        try {
            Reservation reservation = reservationService.createReservation(
                    (RoomType) roomTypeCombo.getSelectedItem(), roomId, (Integer) guestsSpinner.getValue(),
                    UITheme.getDate(checkInSpinner), UITheme.getDate(checkOutSpinner));
            showConfirmation(reservation);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            UITheme.showError(this, ex.getMessage());
            checkAvailability(false);
        }
    }

    private void showConfirmation(Reservation reservation) {
        String[] options = {"View Invoice", "My Reservations", "Dashboard"};
        int choice = JOptionPane.showOptionDialog(this,
                "Your booking is confirmed!\n\nBooking ID: " + reservation.getBookingId()
                + "\nRoom " + reservation.getRoomId() + ", " + FormatUtil.formatDate(reservation.getCheckInDate())
                + " to " + FormatUtil.formatDate(reservation.getCheckOutDate())
                + "\nAdvance paid: " + FormatUtil.formatMoney(reservation.getAdvancePaid()),
                "Booking Confirmed", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);
        if (choice == 0) {
            navigateTo(new InvoiceFrame(reservation.getBookingId()));
        } else if (choice == 1) {
            navigateTo(new ReservationManagementFrame(false));
        } else {
            goToDashboard();
        }
    }

    private void clearForm() {
        LocalDate earliest = LocalDate.now().plusDays(BookingValidator.MIN_DAYS_IN_ADVANCE);
        roomTypeCombo.setSelectedIndex(0);
        guestsSpinner.setValue(1);
        UITheme.setDate(checkInSpinner, earliest);
        UITheme.setDate(checkOutSpinner, earliest.plusDays(1));
        inputsChanged();
        setStatus(" ", UITheme.MUTED);
    }

    private void resetSummary() {
        JLabel[] values = {rateValue, nightsValue, baseValue, extraValue, discountValue, taxValue,
            totalValue, advanceValue, payableValue};
        for (JLabel value : values) {
            value.setText("-");
        }
        extraCaption.setText("Extra guest charges");
        taxCaption.setText("Tax");
        refundNote.setText("Press 'Calculate Charges' to see the price.");
    }

    private String selectedRoomId() {
        Object selected = roomCombo.getSelectedItem();
        return selected == null || NO_ROOM.equals(selected) ? null : selected.toString();
    }

    private void setStatus(String text, Color colour) {
        statusLabel.setText(text);
        statusLabel.setForeground(colour);
    }
}
