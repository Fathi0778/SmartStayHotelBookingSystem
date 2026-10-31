package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import smartstay.model.Reservation;
import smartstay.model.Room;
import smartstay.model.RoomStatus;
import smartstay.security.AccessControl;
import smartstay.service.ReservationService;
import smartstay.service.RoomService;

/**
 * Administrator home screen. Every number shown is calculated from the
 * stored data each time the screen opens or Refresh is pressed.
 */
public class AdminDashboardFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;

    private final RoomService roomService = new RoomService();
    private final ReservationService reservationService = new ReservationService();

    private final StatCard totalRoomsCard = new StatCard("Total Rooms", "All room records", UITheme.PRIMARY);
    private final StatCard availableCard = new StatCard("Available", "Ready to book", UITheme.SUCCESS);
    private final StatCard reservedCard = new StatCard("Reserved", "Current or upcoming stays", UITheme.WARNING);
    private final StatCard maintenanceCard = new StatCard("Maintenance", "Not bookable", UITheme.DANGER);
    private final StatCard reservationsCard = new StatCard("Reservations", "All bookings incl. cancelled",
            UITheme.ACCENT);

    private final DefaultTableModel upcomingModel = UITheme.createReadOnlyModel(
            new String[]{"Booking ID", "Guest", "Room", "Room Type", "Check-in", "Check-out", "Nights"});
    private final DefaultTableModel roomStatusModel = UITheme.createReadOnlyModel(
            new String[]{"Room", "Type", "Status"});

    public AdminDashboardFrame() {
        super("Administrator Dashboard", "Overview of rooms and reservations across the hotel.");
        AccessControl.requireAdministrator(); // The dashboard itself is protected too.

        Sidebar sidebar = new Sidebar("Administrator");
        sidebar.addItem("Dashboard", null).setActive(true);
        sidebar.addItem("Manage Rooms", open(RoomManagementFrame.Mode.MANAGE));
        sidebar.addItem("Room Availability", open(RoomManagementFrame.Mode.AVAILABILITY));
        sidebar.addItem("View All Rooms", open(RoomManagementFrame.Mode.VIEW_ALL));
        sidebar.addItem("Manage Reservations", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new ReservationManagementFrame(false));
            }
        });
        sidebar.addItem("Invoices", new ActionListener() {
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

        AppButton refreshButton = new AppButton("Refresh", AppButton.Style.SECONDARY);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshData();
            }
        });
        addPageAction(refreshButton);

        buildContent();
        refreshData();
    }

    private void buildContent() {
        JPanel stats = new JPanel(new GridLayout(1, 5, 14, 0));
        stats.setOpaque(false);
        stats.add(totalRoomsCard);
        stats.add(availableCard);
        stats.add(reservedCard);
        stats.add(maintenanceCard);
        stats.add(reservationsCard);
        getContentArea().add(stats, BorderLayout.NORTH);

        CardPanel upcomingCard = new CardPanel(new BorderLayout(0, 12));
        upcomingCard.add(UITheme.createHeading("Upcoming reservations"), BorderLayout.NORTH);
        upcomingCard.add(UITheme.wrapTable(UITheme.createTable(upcomingModel)), BorderLayout.CENTER);

        CardPanel roomsCard = new CardPanel(new BorderLayout(0, 12));
        roomsCard.setPreferredSize(new Dimension(330, 0));
        roomsCard.add(UITheme.createHeading("Room status"), BorderLayout.NORTH);
        JTable roomTable = UITheme.createTable(roomStatusModel);
        UITheme.useStatusBadges(roomTable, 2);
        roomsCard.add(UITheme.wrapTable(roomTable), BorderLayout.CENTER);

        JPanel lower = new JPanel(new BorderLayout(16, 0));
        lower.setOpaque(false);
        lower.add(upcomingCard, BorderLayout.CENTER);
        lower.add(roomsCard, BorderLayout.EAST);
        getContentArea().add(lower, BorderLayout.CENTER);
    }

    /** Re-calculates every statistic from the stored data. */
    private void refreshData() {
        totalRoomsCard.setValue(roomService.countAllRooms());
        availableCard.setValue(roomService.countRoomsByStatus(RoomStatus.AVAILABLE));
        reservedCard.setValue(roomService.countRoomsByStatus(RoomStatus.RESERVED));
        maintenanceCard.setValue(roomService.countRoomsByStatus(RoomStatus.UNDER_MAINTENANCE));
        reservationsCard.setValue(reservationService.countVisibleReservations());

        upcomingModel.setRowCount(0);
        List<Reservation> upcoming = reservationService.getUpcomingReservations();
        for (Reservation r : upcoming) {
            upcomingModel.addRow(new Object[]{r.getBookingId(), reservationService.getGuestName(r), r.getRoomId(),
                r.getRoomType(), r.getCheckInDate(),
                r.getCheckOutDate(), r.getNumberOfNights()});
        }

        roomStatusModel.setRowCount(0);
        for (Room room : roomService.sortByPrice(roomService.getAllRooms(), true)) {
            roomStatusModel.addRow(new Object[]{room.getRoomId(), room.getRoomType(),
                roomService.getCurrentStatus(room)});
        }
    }

    private ActionListener open(final RoomManagementFrame.Mode mode) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new RoomManagementFrame(mode));
            }
        };
    }
}
