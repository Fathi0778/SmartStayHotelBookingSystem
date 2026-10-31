package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import smartstay.model.Room;
import smartstay.model.RoomType;
import smartstay.security.AccessControl;
import smartstay.service.RoomService;
import smartstay.util.BookingValidator;

/**
 * Guest screen: search rooms that are free for chosen dates.
 * Rooms under maintenance and rooms with overlapping confirmed bookings are
 * left out by RoomService.findAvailableRooms().
 */
public class AvailableRoomsFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;

    private final RoomService roomService = new RoomService();

    private final JComboBox<Object> roomTypeCombo;
    private final JSpinner guestsSpinner = UITheme.createNumberSpinner(1, 1, 6);
    private final JSpinner checkInSpinner;
    private final JSpinner checkOutSpinner;
    private final JLabel nightsLabel = UITheme.createHeading("1 night");
    private final JComboBox<String> sortCombo = UITheme.createComboBox(
            new String[]{"Price: low to high", "Price: high to low"});

    private final DefaultTableModel tableModel = UITheme.createReadOnlyModel(
            new String[]{"Room ID", "Room Type", "Bed Type", "Daily Rate", "Breakfast", "Max Guests", "Availability"});
    private final JTable resultsTable = UITheme.createTable(tableModel);
    private final JLabel resultLabel = UITheme.createMutedText("Choose your dates and press Search.");

    private List<Room> currentResults = new ArrayList<>();
    private LocalDate searchedCheckIn;
    private LocalDate searchedCheckOut;
    private int searchedGuests;

    public AvailableRoomsFrame() {
        super("Search Available Rooms", "Find a room for your dates. Bookings open "
                + BookingValidator.MIN_DAYS_IN_ADVANCE + " days ahead.");
        AccessControl.requireGuest();

        Object[] types = new Object[RoomType.values().length + 1];
        types[0] = "All Room Types";
        System.arraycopy(RoomType.values(), 0, types, 1, RoomType.values().length);
        roomTypeCombo = UITheme.createComboBox(types);

        LocalDate earliest = LocalDate.now().plusDays(BookingValidator.MIN_DAYS_IN_ADVANCE);
        checkInSpinner = UITheme.createDateSpinner(earliest);
        checkOutSpinner = UITheme.createDateSpinner(earliest.plusDays(1));

        AppButton backButton = new AppButton("Back to Dashboard", AppButton.Style.SECONDARY);
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToDashboard();
            }
        });
        addPageAction(backButton);

        getContentArea().add(createSearchCard(), BorderLayout.NORTH);
        getContentArea().add(createResultsCard(), BorderLayout.CENTER);
    }

    private JPanel createSearchCard() {
        CardPanel card = new CardPanel(new GridBagLayout());
        addColumn(card, 0, "Room Type", roomTypeCombo, 1.0);
        addColumn(card, 1, "Guests", guestsSpinner, 0.3);
        addColumn(card, 2, "Check-in Date", checkInSpinner, 0.8);
        addColumn(card, 3, "Check-out Date", checkOutSpinner, 0.8);
        addColumn(card, 4, "Stay", nightsLabel, 0.4);
        AppButton searchButton = new AppButton("Search Rooms", AppButton.Style.PRIMARY);
        addColumn(card, 5, " ", searchButton, 0.5);

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search();
            }
        });
        ChangeListener dateListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateNights();
            }
        };
        checkInSpinner.addChangeListener(dateListener);
        checkOutSpinner.addChangeListener(dateListener);
        return card;
    }

    private JPanel createResultsCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UITheme.createHeading("Available rooms"), BorderLayout.WEST);
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sortPanel.setOpaque(false);
        sortPanel.add(UITheme.createFieldLabel("Sort"));
        sortPanel.add(sortCombo);
        top.add(sortPanel, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.useStatusBadges(resultsTable, 6);
        card.add(UITheme.wrapTable(resultsTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(resultLabel, BorderLayout.WEST);
        AppButton bookButton = new AppButton("Book Selected Room", AppButton.Style.SUCCESS);
        bottom.add(bookButton, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);

        sortCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showResults();
            }
        });
        bookButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bookSelectedRoom();
            }
        });
        return card;
    }

    private void addColumn(JPanel panel, int column, String label, JComponent field, double weight) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = 0;
        c.weightx = weight;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, column == 0 ? 0 : 12, 4, 0);
        panel.add(UITheme.createFieldLabel(label), c);
        c.gridy = 1;
        c.insets = new Insets(0, column == 0 ? 0 : 12, 0, 0);
        panel.add(field, c);
    }

    private void updateNights() {
        long nights = BookingValidator.calculateNights(UITheme.getDate(checkInSpinner),
                UITheme.getDate(checkOutSpinner));
        nightsLabel.setText(nights > 0 ? nights + (nights == 1 ? " night" : " nights") : "Invalid");
        nightsLabel.setForeground(nights > 0 ? UITheme.TEXT : UITheme.DANGER);
    }

    /** Runs the availability search through the service (all rules checked there). */
    private void search() {
        try {
            RoomType type = roomTypeCombo.getSelectedItem() instanceof RoomType
                    ? (RoomType) roomTypeCombo.getSelectedItem() : null;
            searchedCheckIn = UITheme.getDate(checkInSpinner);
            searchedCheckOut = UITheme.getDate(checkOutSpinner);
            searchedGuests = (Integer) guestsSpinner.getValue();
            currentResults = roomService.findAvailableRooms(type, searchedCheckIn, searchedCheckOut, searchedGuests);
            showResults();
        } catch (IllegalArgumentException ex) {
            currentResults = new ArrayList<>();
            showResults();
            resultLabel.setText("No search results.");
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void showResults() {
        List<Room> rooms = roomService.sortByPrice(currentResults, sortCombo.getSelectedIndex() == 0);
        resultsTable.setRowSorter(null);
        tableModel.setRowCount(0);
        for (Room room : rooms) {
            tableModel.addRow(new Object[]{room.getRoomId(), room.getRoomType(), room.getBedType(),
                room.getDailyRate(), room.getRoomType().hasComplimentaryBreakfast() ? "Yes" : "No",
                room.getRoomType().getMaximumGuests(), "Available"});
        }
        resultsTable.setAutoCreateRowSorter(true);
        if (searchedCheckIn != null) {
            resultLabel.setText(rooms.isEmpty()
                    ? "No rooms are free for these dates. Try other dates or another room type."
                    : rooms.size() + " room(s) free. Select one and press 'Book Selected Room'.");
        }
    }

    private void bookSelectedRoom() {
        int viewRow = resultsTable.getSelectedRow();
        if (viewRow < 0) {
            UITheme.showError(this, "Please search and select a room first.");
            return;
        }
        String roomId = String.valueOf(tableModel.getValueAt(resultsTable.convertRowIndexToModel(viewRow), 0));
        Room room = roomService.findRoom(roomId);
        navigateTo(new ReservationFrame(room, searchedCheckIn, searchedCheckOut, searchedGuests));
    }
}
