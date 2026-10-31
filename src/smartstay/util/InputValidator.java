package smartstay.util;

import java.util.regex.Pattern;

/**
 * Format checks for text typed by users (email, phone, NIC, room ID...).
 * Regular expressions are compiled once and reused.
 */
public final class InputValidator {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Sri Lankan mobile (07XXXXXXXX) or international format (+ and 9-15 digits). */
    private static final Pattern CONTACT_NUMBER =
            Pattern.compile("^(0\\d{9}|\\+\\d{9,15})$");

    /** Old NIC (9 digits + V/X), new NIC (12 digits) or passport (letter + 7-8 alphanumerics). */
    private static final Pattern NIC_OR_PASSPORT =
            Pattern.compile("^(\\d{9}[VvXx]|\\d{12}|[A-Za-z][A-Za-z0-9]{6,8})$");

    /** Room ID such as R101 or DX205: 1-3 letters followed by 2-4 digits. */
    private static final Pattern ROOM_ID =
            Pattern.compile("^[A-Z]{1,3}\\d{2,4}$");

    /** Admin username: 4-20 letters, digits, dots or underscores. */
    private static final Pattern USERNAME =
            Pattern.compile("^[a-z0-9._]{4,20}$");

    /** Letters, spaces, dots, apostrophes and hyphens only. */
    private static final Pattern FULL_NAME =
            Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,59}$");

    private InputValidator() {
        // Utility class - no objects needed.
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public static boolean isValidContactNumber(String number) {
        return number != null && CONTACT_NUMBER.matcher(number.trim().replace(" ", "")).matches();
    }

    public static boolean isValidNicOrPassport(String value) {
        return value != null && NIC_OR_PASSPORT.matcher(value.trim()).matches();
    }

    public static boolean isValidRoomId(String roomId) {
        return roomId != null && ROOM_ID.matcher(roomId.trim().toUpperCase()).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME.matcher(username.trim().toLowerCase()).matches();
    }

    public static boolean isValidFullName(String name) {
        return name != null && FULL_NAME.matcher(name.trim()).matches();
    }

    /** Throws a friendly error if a required field is empty. */
    public static void requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (value.contains("|")) {
            throw new IllegalArgumentException(fieldName + " cannot contain the '|' character.");
        }
    }
}
