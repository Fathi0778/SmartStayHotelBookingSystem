package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import smartstay.model.BedType;
import smartstay.model.Room;
import smartstay.model.RoomStatus;
import smartstay.model.RoomType;
import smartstay.security.AccessControl;
import smartstay.security.AccessDeniedException;
import smartstay.service.RoomService;
import smartstay.util.FormatUtil;

/**
 * Administrator screen for rooms. One class serves three menu options:
 *  - MANAGE:       add, update and delete rooms + change status
 *  - AVAILABILITY: change a room's availability status only
 *  - VIEW_ALL:     read-only list of every room with searching and sorting
 */
public class RoomManagementFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;

    /** Which job this screen was opened for. */
    public enum Mode {
        MANAGE("Room Management", "Add, update and remove room records."),
        AVAILABILITY("Room Availability", "Set rooms as Available or Under Maintenance."),
        VIEW_ALL("All Room Records", "Search and sort every room in the hotel.");

        private final String title;
        private final String subtitle;

        Mode(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private final Mode mode;
    private final RoomService roomService = new RoomService();

    // Search bar
    private final JTextField searchField = UITheme.createTextField();
    private final JComboBox<Object> typeFilter = UITheme.createComboBox(withAny("All Types", RoomType.values()));
    private final JComboBox<Object> statusFilter = UITheme.createComboBox(withAny("All Statuses", RoomStatus.values()));
    private final JComboBox<String> sortCombo = UITheme.createComboBox(
            new String[]{"Price: low to high", "Price: high to low"});

    // Table
    private final DefaultTableModel tableModel = UITheme.createReadOnlyModel(
            new String[]{"Room ID", "Room Type", "Bed Type", "Daily Rate", "Breakfast", "Max Guests", "Status"});
    private final JTable roomTable = UITheme.createTable(tableModel);
    private final JLabel countLabel = UITheme.createMutedText(" ");

    // Form
    private final JTextField roomIdField = UITheme.createTextField();
    private final JComboBox<RoomType> roomTypeCombo = UITheme.createComboBox(RoomType.values());
    private final JComboBox<BedType> bedTypeCombo = UITheme.createComboBox(BedType.values());
    private final JLabel packageInfo = new JLabel();
    private final JComboBox<RoomStatus> statusCombo = UITheme.createComboBox(
            new RoomStatus[]{RoomStatus.AVAILABLE, RoomStatus.UNDER_MAINTENANCE});
    private final JLabel currentStatusLabel = UITheme.createMutedText("Select a room from the table.");
    private final JLabel selectedRoomLabel = UITheme.createHeading("No room selected");
    private final AppButton applyButton = new AppButton("Apply Status", AppButton.Style.PRIMARY);

    public RoomManagementFrame(Mode mode) {
        super(mode.title, mode.subtitle);
        AccessControl.requireAdministrator();
        this.mode = mode;

        AppButton backButton = new AppButton("Back to Dashboard", AppButton.Style.SECONDARY);
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToDashboard();
            }
        });
        addPageAction(backButton);

        getContentArea().add(createTableCard(), BorderLayout.CENTER);
        if (mode != Mode.VIEW_ALL) {
            getContentArea().add(createFormCard(), BorderLayout.EAST);
        }
        updatePackageInfo();
        loadRooms();
    }

    // =====================================================================
    // Layout
    // =====================================================================

    private JPanel createTableCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 12));

        // Two toolbar rows: keyword search on top, filters and sorting below.
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.setOpaque(false);
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        JPanel toolbar = new JPanel(new GridLayout(2, 1, 0, 8));
        toolbar.setOpaque(false);
        searchField.setPreferredSize(new Dimension(260, UITheme.FIELD_HEIGHT));
        searchField.setToolTipText("Search by room ID, room type or bed type");
        typeFilter.setPreferredSize(new Dimension(150, UITheme.FIELD_HEIGHT));
        statusFilter.setPreferredSize(new Dimension(170, UITheme.FIELD_HEIGHT));
        sortCombo.setPreferredSize(new Dimension(170, UITheme.FIELD_HEIGHT));
        AppButton searchButton = new AppButton("Search", AppButton.Style.PRIMARY);
        AppButton resetButton = new AppButton("Reset", AppButton.Style.SECONDARY);

        searchRow.add(UITheme.createFieldLabel("Search"));
        searchRow.add(searchField);
        searchRow.add(searchButton);
        searchRow.add(resetButton);
        filterRow.add(UITheme.createFieldLabel("Filter"));
        filterRow.add(typeFilter);
        filterRow.add(statusFilter);
        filterRow.add(UITheme.createFieldLabel("Sort"));
        filterRow.add(sortCombo);
        toolbar.add(searchRow);
        toolbar.add(filterRow);
        card.add(toolbar, BorderLayout.NORTH);

        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.useStatusBadges(roomTable, 6);
        card.add(UITheme.wrapTable(roomTable), BorderLayout.CENTER);
        card.add(countLabel, BorderLayout.SOUTH);

        ActionListener search = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadRooms();
            }
        };
        searchButton.addActionListener(search);
        searchField.addActionListener(search);
        typeFilter.addActionListener(search);
        statusFilter.addActionListener(search);
        sortCombo.addActionListener(search);
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.setText("");
                typeFilter.setSelectedIndex(0);
                statusFilter.setSelectedIndex(0);
                sortCombo.setSelectedIndex(0);
                loadRooms();
            }
        });

        roomTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    fillFormFromSelection();
                }
            }
        });
        return card;
    }

    /**
     * The right-hand form. It sits in a scroll pane so nothing is cut off
     * on smaller screens or when the window is not maximised.
     */
    private JScrollPane createFormCard() {
        CardPanel card = new ScrollableCard();
        int row = 0;

        if (mode == Mode.MANAGE) {
            row = addToForm(card, row, UITheme.createHeading("Room details"), 0);
            row = addToForm(card, row, UITheme.createFieldLabel("Room ID (e.g. R101)"), 12);
            row = addToForm(card, row, roomIdField, 4);
            row = addToForm(card, row, UITheme.createFieldLabel("Room Type"), 10);
            row = addToForm(card, row, roomTypeCombo, 4);
            row = addToForm(card, row, UITheme.createFieldLabel("Bed Type"), 10);
            row = addToForm(card, row, bedTypeCombo, 4);

            packageInfo.setFont(UITheme.FONT_SMALL);
            packageInfo.setOpaque(true);
            packageInfo.setBackground(UITheme.LIGHT_BLUE);
            packageInfo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            row = addToForm(card, row, packageInfo, 10);

            AppButton addButton = new AppButton("Add Room", AppButton.Style.PRIMARY);
            AppButton updateButton = new AppButton("Update", AppButton.Style.SECONDARY);
            AppButton deleteButton = new AppButton("Delete", AppButton.Style.DANGER);
            AppButton clearButton = new AppButton("Clear", AppButton.Style.SECONDARY);
            JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
            buttons.setOpaque(false);
            buttons.add(addButton);
            buttons.add(updateButton);
            buttons.add(deleteButton);
            buttons.add(clearButton);
            row = addToForm(card, row, buttons, 14);
            row = addToForm(card, row, new JSeparator(), 16);

            addButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addRoom();
                }
            });
            updateButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateRoom();
                }
            });
            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteRoom();
                }
            });
            clearButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearForm();
                }
            });
            roomTypeCombo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updatePackageInfo();
                }
            });
        } else {
            row = addToForm(card, row, UITheme.createHeading("Change availability"), 0);
            JLabel steps = new JLabel("<html><div style='width:230px;color:#6B778C'>1. Click a room in the table."
                    + "<br>2. Choose the new status.<br>3. Press Apply Status.</div></html>");
            steps.setFont(UITheme.FONT_SMALL);
            row = addToForm(card, row, steps, 6);
            row = addToForm(card, row, UITheme.createFieldLabel("SELECTED ROOM"), 16);
            selectedRoomLabel.setForeground(UITheme.MUTED);
            row = addToForm(card, row, selectedRoomLabel, 4);
        }

        row = addToForm(card, row, UITheme.createHeading("Availability status"), mode == Mode.MANAGE ? 12 : 16);
        row = addToForm(card, row, currentStatusLabel, 4);
        row = addToForm(card, row, statusCombo, 8);
        applyButton.setEnabled(false); // Enabled once a room is selected.
        row = addToForm(card, row, applyButton, 8);
        JLabel note = new JLabel("<html><div style='width:225px;color:#6B778C'>\"Reserved\" is set "
                + "automatically while a room has a current or upcoming booking. Rooms under maintenance "
                + "cannot be booked.</div></html>");
        note.setFont(UITheme.FONT_SMALL);
        row = addToForm(card, row, note, 8);

        // Push everything to the top of the card.
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        card.add(new JPanel() {
            private static final long serialVersionUID = 1L;
            {
                setOpaque(false);
            }
        }, filler);

        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyStatus();
            }
        });

        JScrollPane scroll = new JScrollPane(card, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(350, 0));
        return scroll;
    }

    /** A card that always fills the scroll pane's width and scrolls only up and down. */
    private static class ScrollableCard extends CardPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        ScrollableCard() {
            super(new GridBagLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            // Fill the height when there is room; scroll when there is not.
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    private int addToForm(JPanel form, int row, JComponent component, int topGap) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(topGap, 0, 0, 0);
        form.add(component, c);
        return row + 1;
    }

    // =====================================================================
    // Data
    // =====================================================================

    /** Searches, filters and sorts rooms, then shows them in the table. */
    private void loadRooms() {
        try {
            RoomType type = typeFilter.getSelectedItem() instanceof RoomType
                    ? (RoomType) typeFilter.getSelectedItem() : null;
            RoomStatus status = statusFilter.getSelectedItem() instanceof RoomStatus
                    ? (RoomStatus) statusFilter.getSelectedItem() : null;
            List<Room> rooms = roomService.searchRooms(searchField.getText(), type, status);
            rooms = roomService.sortByPrice(rooms, sortCombo.getSelectedIndex() == 0);

            roomTable.setRowSorter(null);
            tableModel.setRowCount(0);
            for (Room room : rooms) {
                tableModel.addRow(new Object[]{room.getRoomId(), room.getRoomType(), room.getBedType(),
                    room.getDailyRate(), room.getRoomType().hasComplimentaryBreakfast() ? "Yes" : "No",
                    room.getRoomType().getMaximumGuests(), roomService.getCurrentStatus(room)});
            }
            roomTable.setAutoCreateRowSorter(true);
            countLabel.setText(rooms.size() + " room(s) shown");
        } catch (AccessDeniedException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private Room getSelectedRoom() {
        int viewRow = roomTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        String roomId = String.valueOf(tableModel.getValueAt(roomTable.convertRowIndexToModel(viewRow), 0));
        return roomService.findRoom(roomId);
    }

    private void fillFormFromSelection() {
        Room room = getSelectedRoom();
        if (room == null) {
            // Nothing selected (e.g. after a search reloads the table).
            applyButton.setEnabled(false);
            selectedRoomLabel.setText("No room selected");
            selectedRoomLabel.setForeground(UITheme.MUTED);
            currentStatusLabel.setText("Select a room from the table.");
            return;
        }
        roomIdField.setText(room.getRoomId());
        roomIdField.setEditable(false); // Room IDs are never changed after creation.
        roomIdField.setBackground(UITheme.BACKGROUND);
        roomTypeCombo.setSelectedItem(room.getRoomType());
        bedTypeCombo.setSelectedItem(room.getBedType());
        statusCombo.setSelectedItem(room.getMaintenanceStatus());
        selectedRoomLabel.setText(room.getRoomId() + "  \u2022  " + room.getRoomType() + ", " + room.getBedType());
        selectedRoomLabel.setForeground(UITheme.TEXT);
        currentStatusLabel.setText("Current status: " + roomService.getCurrentStatus(room));
        applyButton.setEnabled(true);
    }

    private void updatePackageInfo() {
        RoomType type = (RoomType) roomTypeCombo.getSelectedItem();
        if (type == null) {
            return;
        }
        packageInfo.setText("<html><b>Rate set automatically:</b> " + FormatUtil.formatMoney(type.getDailyRate())
                + " / night<br>Breakfast: " + (type.hasComplimentaryBreakfast() ? "Included" : "Not included")
                + " &nbsp;•&nbsp; Tax: " + type.getTaxPercentage() + "%<br>Includes "
                + type.getStandardCapacity() + " guests, extra guest "
                + FormatUtil.formatMoney(type.getExtraGuestCharge()) + "/night</html>");
    }

    // =====================================================================
    // Actions
    // =====================================================================

    private void addRoom() {
        try {
            Room room = roomService.addRoom(roomIdField.getText(), (RoomType) roomTypeCombo.getSelectedItem(),
                    (BedType) bedTypeCombo.getSelectedItem());
            UITheme.showSuccess(this, "Room " + room.getRoomId() + " was added.");
            clearForm();
            loadRooms();
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void updateRoom() {
        Room room = getSelectedRoom();
        if (room == null) {
            UITheme.showError(this, "Please select a room in the table to update.");
            return;
        }
        try {
            roomService.updateRoom(room.getRoomId(), (RoomType) roomTypeCombo.getSelectedItem(),
                    (BedType) bedTypeCombo.getSelectedItem());
            UITheme.showSuccess(this, "Room " + room.getRoomId() + " was updated.");
            loadRooms();
            selectRoomInTable(room.getRoomId());
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void deleteRoom() {
        Room room = getSelectedRoom();
        if (room == null) {
            UITheme.showError(this, "Please select a room in the table to delete.");
            return;
        }
        if (!UITheme.confirm(this, "Delete room " + room.getRoomId() + "? This cannot be undone.", "Delete Room")) {
            return;
        }
        try {
            roomService.deleteRoom(room.getRoomId());
            UITheme.showSuccess(this, "Room " + room.getRoomId() + " was deleted.");
            clearForm();
            loadRooms();
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void applyStatus() {
        Room room = getSelectedRoom();
        if (room == null) {
            UITheme.showError(this, "Please select a room in the table first.");
            return;
        }
        RoomStatus newStatus = (RoomStatus) statusCombo.getSelectedItem();
        try {
            roomService.changeAvailabilityStatus(room.getRoomId(), newStatus);
            UITheme.showSuccess(this, "Room " + room.getRoomId() + " is now " + newStatus + ".");
            loadRooms();
            selectRoomInTable(room.getRoomId());
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    /** Re-selects a room after the table has been reloaded. */
    private void selectRoomInTable(String roomId) {
        for (int viewRow = 0; viewRow < roomTable.getRowCount(); viewRow++) {
            int modelRow = roomTable.convertRowIndexToModel(viewRow);
            if (roomId.equals(String.valueOf(tableModel.getValueAt(modelRow, 0)))) {
                roomTable.setRowSelectionInterval(viewRow, viewRow);
                roomTable.scrollRectToVisible(roomTable.getCellRect(viewRow, 0, true));
                return;
            }
        }
    }

    private void clearForm() {
        roomTable.clearSelection();
        roomIdField.setText("");
        if (mode == Mode.MANAGE) {
            roomIdField.setEditable(true);
            roomIdField.setBackground(UITheme.SURFACE);
        }
        roomTypeCombo.setSelectedIndex(0);
        bedTypeCombo.setSelectedIndex(0);
        statusCombo.setSelectedIndex(0);
        currentStatusLabel.setText("Select a room from the table.");
        selectedRoomLabel.setText("No room selected");
        selectedRoomLabel.setForeground(UITheme.MUTED);
        applyButton.setEnabled(false);
        roomIdField.requestFocusInWindow();
    }

    /** Adds an "All ..." option in front of the enum values for the filters. */
    private static Object[] withAny(String anyLabel, Object[] values) {
        Object[] items = new Object[values.length + 1];
        items[0] = anyLabel;
        System.arraycopy(values, 0, items, 1, values.length);
        return items;
    }
}
