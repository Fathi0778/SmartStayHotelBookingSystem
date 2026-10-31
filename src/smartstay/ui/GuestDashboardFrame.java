package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import smartstay.model.Guest;
import smartstay.model.Reservation;
import smartstay.model.RoomType;
import smartstay.security.AccessControl;
import smartstay.service.ReservationService;
import smartstay.util.BookingValidator;
import smartstay.util.FormatUtil;
import smartstay.util.InvoiceCalculator;

/**
 * Guest home screen. Shows ONLY the logged-in guest's own information and
 * only guest menu options - no administrator functions.
 */
public class GuestDashboardFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;

    private final ReservationService reservationService = new ReservationService();

    private final StatCard totalCard = new StatCard("My Reservations", "All your bookings", UITheme.PRIMARY);
    private final StatCard upcomingCard = new StatCard("Upcoming Stays", "Confirmed and not finished",
            UITheme.SUCCESS);
    private final StatCard cancelledCard = new StatCard("Cancelled", "Kept in your history", UITheme.DANGER);

    private final DefaultTableModel upcomingModel = UITheme.createReadOnlyModel(
            new String[]{"Booking ID", "Room", "Room Type", "Guests", "Check-in", "Check-out", "Nights", "Can Change"});

    public GuestDashboardFrame() {
        super(greeting(), "Plan your next stay, manage your bookings and view your invoices.");
        AccessControl.requireGuest();

        Sidebar sidebar = new Sidebar("Guest Menu");
        sidebar.addItem("Dashboard", null).setActive(true);
        sidebar.addItem("Search Available Rooms", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new AvailableRoomsFrame());
            }
        });
        sidebar.addItem("Make Reservation", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new ReservationFrame());
            }
        });
        sidebar.addItem("My Reservations", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new ReservationManagementFrame(false));
            }
        });
        sidebar.addItem("Search My Reservations", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new ReservationManagementFrame(true));
            }
        });
        sidebar.addItem("My Invoices", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new InvoiceFrame(null));
            }
        });
        sidebar.addFlexibleSpace();
        sidebar.addItem("Logout", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });
        setSidebar(sidebar);

        AppButton bookButton = new AppButton("Book a Room", AppButton.Style.PRIMARY);
        bookButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new AvailableRoomsFrame());
            }
        });
        addPageAction(bookButton);

        buildContent();
        refreshData();
    }

    private static String greeting() {
        Guest guest = AccessControl.requireGuest();
        String firstName = guest.getFullName().split(" ")[0];
        return "Welcome back, " + firstName;
    }

    private void buildContent() {
        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(totalCard);
        stats.add(upcomingCard);
        stats.add(cancelledCard);
        getContentArea().add(stats, BorderLayout.NORTH);

        CardPanel staysCard = new CardPanel(new BorderLayout(0, 12));
        staysCard.add(UITheme.createHeading("Your upcoming stays"), BorderLayout.NORTH);
        JTable table = UITheme.createTable(upcomingModel);
        UITheme.useStatusBadges(table, 7);
        staysCard.add(UITheme.wrapTable(table), BorderLayout.CENTER);
        staysCard.add(UITheme.createMutedText("Bookings can be changed or cancelled until 24 hours before "
                + "check-in (" + BookingValidator.CHECK_IN_TIME + ")."), BorderLayout.SOUTH);

        getContentArea().add(staysCard, BorderLayout.CENTER);
        getContentArea().add(createPackagesCard(), BorderLayout.EAST);
    }

    /** Room package details, read from RoomType so prices are never repeated. */
    private CardPanel createPackagesCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 10));
        card.setPreferredSize(new Dimension(290, 0));
        card.add(UITheme.createHeading("Room packages"), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (RoomType type : RoomType.values()) {
            JLabel line = new JLabel("<html><b style='color:#0B2545'>" + type.getDisplayName() + "</b> &nbsp;"
                    + "<span style='color:#1D4E89'>" + FormatUtil.formatMoney(type.getDailyRate()) + "/night</span>"
                    + "<br><span style='color:#6B778C'>"
                    + (type.hasComplimentaryBreakfast() ? "Breakfast included" : "No breakfast")
                    + " • " + type.getStandardCapacity() + " guests included"
                    + "<br>Extra guest " + FormatUtil.formatMoney(type.getExtraGuestCharge())
                    + "/night • Tax " + type.getTaxPercentage() + "%</span></html>");
            line.setFont(UITheme.FONT_SMALL);
            line.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));
            list.add(line);
        }
        card.add(list, BorderLayout.CENTER);

        JLabel rules = new JLabel("<html><span style='color:#6B778C'>Refundable advance of "
                + FormatUtil.formatMoney(InvoiceCalculator.BOOKING_ADVANCE) + " at booking. "
                + "Stay " + InvoiceCalculator.LONG_STAY_MIN_NIGHTS + "+ nights and save 12% on the room charge. "
                + "Book at least " + BookingValidator.MIN_DAYS_IN_ADVANCE + " days ahead.</span></html>");
        rules.setFont(UITheme.FONT_SMALL);
        card.add(rules, BorderLayout.SOUTH);
        return card;
    }

    private void refreshData() {
        totalCard.setValue(reservationService.countVisibleReservations());
        upcomingCard.setValue(reservationService.countUpcoming());
        cancelledCard.setValue(reservationService.countCancelled());

        upcomingModel.setRowCount(0);
        for (Reservation r : reservationService.getUpcomingReservations()) {
            upcomingModel.addRow(new Object[]{r.getBookingId(), r.getRoomId(), r.getRoomType(),
                r.getNumberOfGuests(), r.getCheckInDate(),
                r.getCheckOutDate(), r.getNumberOfNights(),
                reservationService.canChange(r) ? "Yes" : "Locked"});
        }
    }
}
