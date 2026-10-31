package smartstay.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import smartstay.model.Administrator;
import smartstay.model.BedType;
import smartstay.model.BookingStatus;
import smartstay.model.Guest;
import smartstay.model.Reservation;
import smartstay.model.Room;
import smartstay.model.RoomStatus;
import smartstay.model.RoomType;

/**
 * Saves and loads all SmartStay data using plain text files.
 *
 * Files are kept in a "data" folder next to the project (relative path,
 * no hard-coded C:\ paths). Each record is one line, with fields separated
 * by the '|' character, for example:
 *
 *   rooms.txt         R101|STANDARD|DOUBLE|AVAILABLE
 *
 * Data structures used:
 *  - ArrayList keeps every record in insertion order (good for tables and
 *    for looping through records when searching).
 *  - HashMap gives instant O(1) look-up by a unique key (room ID, booking ID,
 *    guest email...), instead of looping through the whole list.
 * The list and the map always hold the SAME objects and are updated together.
 *
 * Singleton: one DataStore is shared by all services.
 */
public final class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private static final String SEPARATOR = "|";
    private static final String SPLIT_REGEX = "\\|";
    private static final String EMPTY_VALUE = "-";
    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path dataFolder = Paths.get("data");
    private final Path administratorsFile = dataFolder.resolve("administrators.txt");
    private final Path guestsFile = dataFolder.resolve("guests.txt");
    private final Path roomsFile = dataFolder.resolve("rooms.txt");
    private final Path reservationsFile = dataFolder.resolve("reservations.txt");

    // ----- Collections (ArrayList for order, HashMap for fast look-up) -----
    private final List<Administrator> administrators = new ArrayList<>();
    private final Map<String, Administrator> administratorsByUsername = new HashMap<>();

    private final List<Guest> guests = new ArrayList<>();
    private final Map<String, Guest> guestsById = new HashMap<>();
    private final Map<String, Guest> guestsByEmail = new HashMap<>();
    private final Map<String, Guest> guestsByNic = new HashMap<>();

    private final List<Room> rooms = new ArrayList<>();
    private final Map<String, Room> roomsById = new HashMap<>();

    private final List<Reservation> reservations = new ArrayList<>();
    private final Map<String, Reservation> reservationsById = new HashMap<>();

    private final List<String> loadWarnings = new ArrayList<>();
    private final Set<Path> backedUpFiles = new HashSet<>();

    private DataStore() {
        // Use getInstance() instead.
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // =====================================================================
    // Loading
    // =====================================================================

    /** Loads every file. Missing files are fine - they just mean "no data yet". */
    public void loadAll() throws IOException {
        Files.createDirectories(dataFolder);
        loadWarnings.clear();
        backedUpFiles.clear();
        loadAdministrators();
        loadGuests();
        boolean firstRun = !Files.exists(roomsFile);
        loadRooms();
        loadReservations();
        if (firstRun) {
            addSampleRooms();
        }
    }

    /** Messages about lines that could not be read (corrupted records). */
    public List<String> getLoadWarnings() {
        return Collections.unmodifiableList(loadWarnings);
    }

    private void loadAdministrators() throws IOException {
        administrators.clear();
        administratorsByUsername.clear();
        List<String[]> records = readRecords(administratorsFile, 7);
        for (String[] f : records) {
            try {
                Administrator admin = new Administrator(f[0], f[1], f[2], f[3], f[4], f[5], f[6]);
                addAdministratorToCollections(admin);
            } catch (RuntimeException ex) {
                badRecord(administratorsFile, "invalid administrator record skipped (" + ex.getMessage() + ")");
            }
        }
    }

    private void loadGuests() throws IOException {
        guests.clear();
        guestsById.clear();
        guestsByEmail.clear();
        guestsByNic.clear();
        List<String[]> records = readRecords(guestsFile, 9);
        for (String[] f : records) {
            try {
                Guest guest = new Guest(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8]);
                if (guestsByEmail.containsKey(guest.getEmail()) || guestsById.containsKey(guest.getGuestId())) {
                    warn(guestsFile, "duplicate guest " + guest.getEmail() + " skipped");
                    continue;
                }
                addGuestToCollections(guest);
            } catch (RuntimeException ex) {
                badRecord(guestsFile, "invalid guest record skipped (" + ex.getMessage() + ")");
            }
        }
    }

    private void loadRooms() throws IOException {
        rooms.clear();
        roomsById.clear();
        List<String[]> records = readRecords(roomsFile, 4);
        for (String[] f : records) {
            try {
                Room room = new Room(f[0], RoomType.fromName(f[1]), BedType.fromName(f[2]),
                        RoomStatus.fromName(f[3]));
                if (roomsById.containsKey(room.getRoomId())) {
                    warn(roomsFile, "duplicate room " + room.getRoomId() + " skipped");
                    continue;
                }
                addRoomToCollections(room);
            } catch (RuntimeException ex) {
                badRecord(roomsFile, "invalid room record skipped (" + ex.getMessage() + ")");
            }
        }
    }

    private void loadReservations() throws IOException {
        reservations.clear();
        reservationsById.clear();
        List<String[]> records = readRecords(reservationsFile, 11);
        for (String[] f : records) {
            try {
                LocalDateTime cancelled = EMPTY_VALUE.equals(f[10]) ? null : LocalDateTime.parse(f[10]);
                Reservation reservation = new Reservation(f[0], f[1], f[2], RoomType.fromName(f[3]),
                        Integer.parseInt(f[4]), LocalDate.parse(f[5]), LocalDate.parse(f[6]),
                        LocalDateTime.parse(f[7]), new BigDecimal(f[8]),
                        BookingStatus.fromName(f[9]), cancelled);
                if (reservationsById.containsKey(reservation.getBookingId())) {
                    warn(reservationsFile, "duplicate booking " + reservation.getBookingId() + " skipped");
                    continue;
                }
                addReservationToCollections(reservation);
            } catch (RuntimeException ex) {
                badRecord(reservationsFile, "invalid reservation record skipped (" + ex.getMessage() + ")");
            }
        }
    }

    /**
     * Reads a file into rows of fields. Blank lines and lines starting with
     * '#' are ignored. Lines with the wrong number of fields are reported
     * and skipped. If any line is bad, a backup copy of the file is made
     * first so no data is ever lost when the file is saved again.
     */
    private List<String[]> readRecords(Path file, int expectedFields) throws IOException {
        List<String[]> records = new ArrayList<>();
        if (!Files.exists(file)) {
            return records;
        }
        boolean hasBadLine = false;
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] fields = trimmed.split(SPLIT_REGEX, -1);
                if (fields.length != expectedFields) {
                    hasBadLine = true;
                    warn(file, "line " + lineNumber + " has " + fields.length
                            + " fields instead of " + expectedFields + " and was skipped");
                    continue;
                }
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim();
                }
                records.add(fields);
            }
        }
        if (hasBadLine) {
            backupFile(file);
        }
        return records;
    }

    /** Reports a record that could not be used and backs up its file (once per load). */
    private void badRecord(Path file, String message) {
        warn(file, message);
        backupFile(file);
    }

    private void backupFile(Path file) {
        if (!backedUpFiles.add(file)) {
            return; // Already backed up during this load.
        }
        try {
            Path backup = file.resolveSibling(file.getFileName() + ".backup_"
                    + LocalDateTime.now().format(BACKUP_STAMP));
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            loadWarnings.add("A backup of " + file.getFileName() + " was saved as " + backup.getFileName());
        } catch (IOException ex) {
            loadWarnings.add("Could not back up " + file.getFileName() + ": " + ex.getMessage());
        }
    }

    private void warn(Path file, String message) {
        loadWarnings.add(file.getFileName() + ": " + message);
    }

    /** Creates a starter set of rooms the very first time the system runs. */
    private void addSampleRooms() throws IOException {
        addRoomToCollections(new Room("R101", RoomType.STANDARD, BedType.SINGLE, RoomStatus.AVAILABLE));
        addRoomToCollections(new Room("R102", RoomType.STANDARD, BedType.DOUBLE, RoomStatus.AVAILABLE));
        addRoomToCollections(new Room("R201", RoomType.DELUXE, BedType.QUEEN, RoomStatus.AVAILABLE));
        addRoomToCollections(new Room("R202", RoomType.DELUXE, BedType.KING, RoomStatus.AVAILABLE));
        addRoomToCollections(new Room("R301", RoomType.SUITE, BedType.KING, RoomStatus.AVAILABLE));
        addRoomToCollections(new Room("R302", RoomType.SUITE, BedType.QUEEN, RoomStatus.UNDER_MAINTENANCE));
        addRoomToCollections(new Room("R401", RoomType.FAMILY_SUITE, BedType.KING, RoomStatus.AVAILABLE));
        addRoomToCollections(new Room("R402", RoomType.FAMILY_SUITE, BedType.QUEEN, RoomStatus.AVAILABLE));
        saveRooms();
    }

    // =====================================================================
    // Saving (write to a temporary file, then replace - safer if the
    // application stops half-way through writing)
    // =====================================================================

    public void saveAdministrators() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# adminId|username|fullName|passwordHash|passwordSalt|recoveryCodeHash|recoveryCodeSalt");
        for (Administrator a : administrators) {
            lines.add(join(a.getUserId(), a.getUsername(), a.getFullName(), a.getPasswordHash(),
                    a.getPasswordSalt(), a.getRecoveryCodeHash(), a.getRecoveryCodeSalt()));
        }
        writeLines(administratorsFile, lines);
    }

    public void saveGuests() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# guestId|nicOrPassport|fullName|contactNumber|email|passwordHash|passwordSalt"
                + "|recoveryCodeHash|recoveryCodeSalt");
        for (Guest g : guests) {
            lines.add(join(g.getGuestId(), g.getNicOrPassport(), g.getFullName(), g.getContactNumber(),
                    g.getEmail(), g.getPasswordHash(), g.getPasswordSalt(),
                    g.getRecoveryCodeHash(), g.getRecoveryCodeSalt()));
        }
        writeLines(guestsFile, lines);
    }

    public void saveRooms() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# roomId|roomType|bedType|maintenanceStatus");
        for (Room r : rooms) {
            lines.add(join(r.getRoomId(), r.getRoomType().name(), r.getBedType().name(),
                    r.getMaintenanceStatus().name()));
        }
        writeLines(roomsFile, lines);
    }

    public void saveReservations() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# bookingId|guestId|roomId|roomType|guests|checkIn|checkOut|bookedAt|advance|status|cancelledAt");
        for (Reservation r : reservations) {
            String cancelled = r.getCancelledDateTime() == null ? EMPTY_VALUE : r.getCancelledDateTime().toString();
            lines.add(join(r.getBookingId(), r.getGuestId(), r.getRoomId(), r.getRoomType().name(),
                    String.valueOf(r.getNumberOfGuests()), r.getCheckInDate().toString(),
                    r.getCheckOutDate().toString(), r.getBookingDateTime().toString(),
                    r.getAdvancePaid().toPlainString(), r.getStatus().name(), cancelled));
        }
        writeLines(reservationsFile, lines);
    }

    private void writeLines(Path file, List<String> lines) throws IOException {
        Files.createDirectories(dataFolder);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(values[i]);
        }
        return builder.toString();
    }

    // =====================================================================
    // Administrators
    // =====================================================================

    public List<Administrator> getAdministrators() {
        return Collections.unmodifiableList(administrators);
    }

    public Administrator findAdministratorByUsername(String username) {
        return username == null ? null : administratorsByUsername.get(username.trim().toLowerCase());
    }

    /** Administrators are few, so a simple loop is enough here. O(n). */
    public Administrator findAdministratorById(String adminId) {
        for (Administrator admin : administrators) {
            if (admin.getUserId().equals(adminId)) {
                return admin;
            }
        }
        return null;
    }

    public void addAdministrator(Administrator admin) throws IOException {
        addAdministratorToCollections(admin);
        try {
            saveAdministrators();
        } catch (IOException ex) {
            administrators.remove(admin);
            administratorsByUsername.remove(admin.getUsername());
            throw ex;
        }
    }

    private void addAdministratorToCollections(Administrator admin) {
        administrators.add(admin);
        administratorsByUsername.put(admin.getUsername(), admin);
    }

    // =====================================================================
    // Guests
    // =====================================================================

    public List<Guest> getGuests() {
        return Collections.unmodifiableList(guests);
    }

    public Guest findGuestById(String guestId) {
        return guestId == null ? null : guestsById.get(guestId);
    }

    public Guest findGuestByEmail(String email) {
        return email == null ? null : guestsByEmail.get(email.trim().toLowerCase());
    }

    public Guest findGuestByNic(String nic) {
        return nic == null ? null : guestsByNic.get(nic.trim().toUpperCase());
    }

    public void addGuest(Guest guest) throws IOException {
        addGuestToCollections(guest);
        try {
            saveGuests();
        } catch (IOException ex) {
            // Undo the change in memory so memory and file stay the same.
            guests.remove(guest);
            guestsById.remove(guest.getGuestId());
            guestsByEmail.remove(guest.getEmail());
            guestsByNic.remove(guest.getNicOrPassport());
            throw ex;
        }
    }

    private void addGuestToCollections(Guest guest) {
        guests.add(guest);
        guestsById.put(guest.getGuestId(), guest);
        guestsByEmail.put(guest.getEmail(), guest);
        guestsByNic.put(guest.getNicOrPassport(), guest);
    }

    // =====================================================================
    // Rooms
    // =====================================================================

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public Room findRoomById(String roomId) {
        return roomId == null ? null : roomsById.get(roomId.trim().toUpperCase());
    }

    public void addRoom(Room room) throws IOException {
        addRoomToCollections(room);
        try {
            saveRooms();
        } catch (IOException ex) {
            rooms.remove(room);
            roomsById.remove(room.getRoomId());
            throw ex;
        }
    }

    public void removeRoom(Room room) throws IOException {
        rooms.remove(room);
        roomsById.remove(room.getRoomId());
        saveRooms();
    }

    private void addRoomToCollections(Room room) {
        rooms.add(room);
        roomsById.put(room.getRoomId(), room);
    }

    // =====================================================================
    // Reservations
    // =====================================================================

    public List<Reservation> getReservations() {
        return Collections.unmodifiableList(reservations);
    }

    public Reservation findReservationById(String bookingId) {
        return bookingId == null ? null : reservationsById.get(bookingId.trim().toUpperCase());
    }

    public void addReservation(Reservation reservation) throws IOException {
        addReservationToCollections(reservation);
        try {
            saveReservations();
        } catch (IOException ex) {
            reservations.remove(reservation);
            reservationsById.remove(reservation.getBookingId());
            throw ex;
        }
    }

    private void addReservationToCollections(Reservation reservation) {
        reservations.add(reservation);
        reservationsById.put(reservation.getBookingId(), reservation);
    }

    /** Location of the data folder, shown on screen to help with testing. */
    public String getDataFolderPath() {
        return dataFolder.toAbsolutePath().toString();
    }
}
