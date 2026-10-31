package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import smartstay.model.Reservation;
import smartstay.security.AccessControl;
import smartstay.security.AccessDeniedException;
import smartstay.security.SessionManager;
import smartstay.service.ReservationService;
import smartstay.util.BookingValidator;
import smartstay.util.FormatUtil;

/**
 * Reservation list for BOTH roles.
 *  - Administrator: sees every booking ("Reservation Management").
 *  - Guest: sees only their own bookings ("My Reservations").
 * The difference comes from ReservationService, not from this screen, so a
 * guest cannot reach another guest's booking by typing its ID or name.
 */
public class ReservationManagementFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;

    private final ReservationService reservationService = new ReservationService();
    private final boolean administrator;

    private final JTextField searchField = UITheme.createTextField();
    private final JCheckBox dateFilterCheck = new JCheckBox("Check-in date");
    private final JSpinner dateFilterSpinner = UITheme.createDateSpinner(LocalDate.now());

    private final DefaultTableModel tableModel = UITheme.createReadOnlyModel(new String[]{
        "Booking ID", "Guest Name", "Room", "Room Type", "Guests", "Check-in", "Check-out", "Nights",
        "Status", "Can Change"});
    private final JTable table = UITheme.createTable(tableModel);
    private final javax.swing.JLabel countLabel = UITheme.createMutedText(" ");

    /** @param focusSearch put the cursor in the search box (used by "Search My Reservations") */
    public ReservationManagementFrame(boolean focusSearch) {
        super(SessionManager.getInstance().isAdministratorLoggedIn() ? "Reservation Management" : "My Reservations",
                SessionManager.getInstance().isAdministratorLoggedIn()
                ? "View, search, modify and cancel guest reservations."
                : "Search, modify or cancel your bookings and open their invoices.");
        AccessControl.requireLogin();
        this.administrator = SessionManager.getInstance().isAdministratorLoggedIn();

        AppButton backButton = new AppButton("Back to Dashboard", AppButton.Style.SECONDARY);
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToDashboard();
            }
        });
        addPageAction(backButton);

        getContentArea().add(createTableCard(), BorderLayout.CENTER);
        loadReservations(reservationService.getVisibleReservations());

        if (focusSearch) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    searchField.requestFocusInWindow();
                }
            });
        }
    }

    private JPanel createTableCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 12));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        searchField.setPreferredSize(new Dimension(230, UITheme.FIELD_HEIGHT));
        searchField.setToolTipText("Type a booking ID (e.g. BK00001) or a guest name");
        dateFilterCheck.setOpaque(false);
        dateFilterCheck.setFont(UITheme.FONT_LABEL);
        dateFilterCheck.setForeground(UITheme.MUTED);
        dateFilterSpinner.setEnabled(false);
        AppButton searchButton = new AppButton("Search", AppButton.Style.PRIMARY);
        AppButton showAllButton = new AppButton("Show All", AppButton.Style.SECONDARY);
        AppButton sortButton = new AppButton("Sort by Check-in", AppButton.Style.SECONDARY);

        toolbar.add(UITheme.createFieldLabel("Booking ID or guest name"));
        toolbar.add(searchField);
        toolbar.add(dateFilterCheck);
        toolbar.add(dateFilterSpinner);
        toolbar.add(searchButton);
        toolbar.add(showAllButton);
        toolbar.add(sortButton);
        card.add(toolbar, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.useStatusBadges(table, 8);
        UITheme.useStatusBadges(table, 9);
        if (!administrator) {
            // A guest's own name in every row adds nothing, so the column is hidden.
            table.removeColumn(table.getColumnModel().getColumn(1));
        }
        card.add(UITheme.wrapTable(table), BorderLayout.CENTER);

        AppButton detailsButton = new AppButton("View Details", AppButton.Style.SECONDARY);
        AppButton modifyButton = new AppButton("Modify", AppButton.Style.SECONDARY);
        AppButton cancelButton = new AppButton("Cancel Booking", AppButton.Style.DANGER);
        AppButton invoiceButton = new AppButton("View Invoice", AppButton.Style.PRIMARY);
        JPanel actions = new JPanel(new GridLayout(1, 4, 10, 0));
        actions.setOpaque(false);
        actions.add(detailsButton);
        actions.add(modifyButton);
        actions.add(cancelButton);
        actions.add(invoiceButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(countLabel, BorderLayout.WEST);
        bottom.add(actions, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);

        ActionListener search = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search();
            }
        };
        searchButton.addActionListener(search);
        searchField.addActionListener(search);
        dateFilterCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dateFilterSpinner.setEnabled(dateFilterCheck.isSelected());
            }
        });
        showAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.setText("");
                dateFilterCheck.setSelected(false);
                dateFilterSpinner.setEnabled(false);
                loadReservations(reservationService.getVisibleReservations());
            }
        });
        sortButton.addActionListener(search);
        detailsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showDetails();
            }
        });
        modifyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modifySelected();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelSelected();
            }
        });
        invoiceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Reservation selected = getSelectedReservation();
                if (selected != null) {
                    navigateTo(new InvoiceFrame(selected.getBookingId()));
                }
            }
        });
        return card;
    }

    // =====================================================================
    // Data
    // =====================================================================

    private void search() {
        LocalDate date = dateFilterCheck.isSelected() ? UITheme.getDate(dateFilterSpinner) : null;
        List<Reservation> results = reservationService.searchReservations(searchField.getText(), date);
        loadReservations(results);
        if (results.isEmpty()) {
            countLabel.setText("No reservations match your search.");
        }
    }

    /** Shows reservations sorted by check-in date (merge sort in the service). */
    private void loadReservations(List<Reservation> reservations) {
        List<Reservation> sorted = reservationService.sortByCheckInDate(reservations);
        table.setRowSorter(null);
        tableModel.setRowCount(0);
        for (Reservation r : sorted) {
            String changeText;
            if (!r.isActive()) {
                changeText = "No";
            } else if (reservationService.canChange(r)) {
                changeText = "Yes";
            } else {
                changeText = "Locked";
            }
            tableModel.addRow(new Object[]{r.getBookingId(), reservationService.getGuestName(r), r.getRoomId(),
                r.getRoomType(), r.getNumberOfGuests(), r.getCheckInDate(), r.getCheckOutDate(),
                r.getNumberOfNights(), r.getStatus(), changeText});
        }
        table.setAutoCreateRowSorter(true);
        countLabel.setText(sorted.size() + " reservation(s)");
    }

    private Reservation getSelectedReservation() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UITheme.showError(this, "Please select a reservation in the table first.");
            return null;
        }
        String bookingId = String.valueOf(tableModel.getValueAt(table.convertRowIndexToModel(viewRow), 0));
        try {
            return reservationService.getReservation(bookingId);
        } catch (AccessDeniedException ex) {
            UITheme.showError(this, ex.getMessage());
            return null;
        }
    }

    // =====================================================================
    // Actions
    // =====================================================================

    private void showDetails() {
        Reservation r = getSelectedReservation();
        if (r == null) {
            return;
        }
        String cancelledLine = r.getCancelledDateTime() == null ? ""
                : "<tr><td>Cancelled on</td><td>" + FormatUtil.formatDateTime(r.getCancelledDateTime()) + "</td></tr>";
        String html = "<html><table cellpadding='3'>"
                + "<tr><td><b>Booking ID</b></td><td><b>" + r.getBookingId() + "</b></td></tr>"
                + "<tr><td>Guest</td><td>" + reservationService.getGuestName(r) + "</td></tr>"
                + "<tr><td>Room</td><td>" + r.getRoomId() + " (" + r.getRoomType() + ")</td></tr>"
                + "<tr><td>Guests</td><td>" + r.getNumberOfGuests() + "</td></tr>"
                + "<tr><td>Check-in</td><td>" + FormatUtil.formatDate(r.getCheckInDate()) + " from "
                + BookingValidator.CHECK_IN_TIME + "</td></tr>"
                + "<tr><td>Check-out</td><td>" + FormatUtil.formatDate(r.getCheckOutDate()) + "</td></tr>"
                + "<tr><td>Nights</td><td>" + r.getNumberOfNights() + "</td></tr>"
                + "<tr><td>Booked on</td><td>" + FormatUtil.formatDateTime(r.getBookingDateTime()) + "</td></tr>"
                + "<tr><td>Advance paid</td><td>" + FormatUtil.formatMoney(r.getAdvancePaid()) + "</td></tr>"
                + "<tr><td>Status</td><td>" + r.getStatus() + "</td></tr>" + cancelledLine
                + "<tr><td>Changes allowed until</td><td>"
                + FormatUtil.formatDateTime(BookingValidator.getChangeDeadline(r.getCheckInDate())) + "</td></tr>"
                + "</table></html>";
        JOptionPane.showMessageDialog(this, html, "Reservation " + r.getBookingId(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void modifySelected() {
        Reservation r = getSelectedReservation();
        if (r == null) {
            return;
        }
        if (!reservationService.canChange(r)) {
            UITheme.showError(this, r.isActive()
                    ? "This booking cannot be modified because check-in is less than 24 hours away."
                    : "Cancelled bookings cannot be modified.");
            return;
        }
        JSpinner guests = UITheme.createNumberSpinner(r.getNumberOfGuests(), 1, r.getRoomType().getMaximumGuests());
        JSpinner checkIn = UITheme.createDateSpinner(r.getCheckInDate());
        JSpinner checkOut = UITheme.createDateSpinner(r.getCheckOutDate());
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(UITheme.createMutedText("Booking " + r.getBookingId() + " • Room " + r.getRoomId()
                + " (" + r.getRoomType() + ")"));
        panel.add(UITheme.createFieldLabel("Number of Guests (max " + r.getRoomType().getMaximumGuests() + ")"));
        panel.add(guests);
        panel.add(UITheme.createFieldLabel("Check-in Date"));
        panel.add(checkIn);
        panel.add(UITheme.createFieldLabel("Check-out Date"));
        panel.add(checkOut);

        int choice = JOptionPane.showConfirmDialog(this, panel, "Modify Reservation",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            reservationService.modifyReservation(r.getBookingId(), (Integer) guests.getValue(),
                    UITheme.getDate(checkIn), UITheme.getDate(checkOut));
            UITheme.showSuccess(this, "Booking " + r.getBookingId() + " was updated.\n"
                    + "Open the invoice to see the new charges.");
            search();
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void cancelSelected() {
        Reservation r = getSelectedReservation();
        if (r == null) {
            return;
        }
        if (!UITheme.confirm(this, "Cancel booking " + r.getBookingId() + " for room " + r.getRoomId() + "?\n"
                + "The booking will be kept in the history as Cancelled and the "
                + FormatUtil.formatMoney(r.getAdvancePaid()) + " advance will be refunded.", "Cancel Booking")) {
            return;
        }
        try {
            reservationService.cancelReservation(r.getBookingId());
            UITheme.showSuccess(this, "Booking " + r.getBookingId() + " has been cancelled.\n"
                    + "Refundable advance: " + FormatUtil.formatMoney(r.getAdvancePaid()));
            search();
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }
}
