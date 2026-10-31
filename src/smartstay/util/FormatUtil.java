package smartstay.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Formats money and dates the same way on every screen.
 */
public final class FormatUtil {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private FormatUtil() {
        // Utility class - no objects needed.
    }

    /** e.g. 52752.5 -> "LKR 52,752.50" */
    public static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "LKR 0.00";
        }
        DecimalFormat format = new DecimalFormat("#,##0.00");
        return "LKR " + format.format(amount);
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_TIME);
    }
}
