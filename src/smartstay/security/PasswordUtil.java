package smartstay.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Secure password hashing using PBKDF2 with HMAC-SHA256 (built into Java).
 *
 * How it works:
 *  1. A random 16-byte "salt" is created for every user.
 *  2. The password + salt are hashed 65,536 times by PBKDF2.
 *  3. Only the hash and the salt are saved - never the password.
 *  4. At login the typed password is hashed again with the same salt and
 *     the two hashes are compared.
 *
 * Because each user has a different salt, two users with the same password
 * still get completely different hashes.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Characters used in recovery codes. Letters and digits that look alike
     * (0/O, 1/I/L) are left out so codes are easy to copy by hand.
     */
    private static final char[] RECOVERY_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int RECOVERY_GROUPS = 3;
    private static final int RECOVERY_GROUP_LENGTH = 4;

    /** Minimum password rules shown on the registration screen. */
    public static final String PASSWORD_RULES =
            "At least 8 characters, with an uppercase letter, a lowercase letter and a number.";

    private PasswordUtil() {
        // Utility class - no objects needed.
    }

    /** Creates a new random salt, encoded as Base64 text for storage. */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Hashes a password with the given salt and returns Base64 text. */
    public static String hashPassword(char[] password, String saltBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Password hashing is not available on this Java version.", ex);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Checks a typed password against the stored hash.
     * MessageDigest.isEqual compares in constant time, so the time taken
     * does not reveal how many characters were correct.
     */
    public static boolean verifyPassword(char[] password, String storedHash, String saltBase64) {
        if (password == null || storedHash == null || saltBase64 == null) {
            return false;
        }
        String candidateHash = hashPassword(password, saltBase64);
        return MessageDigest.isEqual(
                Base64.getDecoder().decode(candidateHash),
                Base64.getDecoder().decode(storedHash));
    }

    /** Returns null if the password is strong enough, otherwise an error message. */
    public static String checkStrength(char[] password) {
        if (password == null || password.length < 8) {
            return "Password must be at least 8 characters long.";
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char c : password) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasUpper || !hasLower || !hasDigit) {
            return "Password must contain an uppercase letter, a lowercase letter and a number.";
        }
        return null;
    }

    /**
     * Creates a random recovery code such as "K7QM-2XRP-9HTA".
     * 12 characters from a 31-character alphabet gives about 59 bits of
     * randomness, so the code cannot realistically be guessed.
     */
    public static String generateRecoveryCode() {
        StringBuilder code = new StringBuilder();
        for (int group = 0; group < RECOVERY_GROUPS; group++) {
            if (group > 0) {
                code.append('-');
            }
            for (int i = 0; i < RECOVERY_GROUP_LENGTH; i++) {
                code.append(RECOVERY_ALPHABET[RANDOM.nextInt(RECOVERY_ALPHABET.length)]);
            }
        }
        return code.toString();
    }

    /**
     * Makes typed codes comparable: removes spaces and dashes and uses
     * capital letters, so "k7qm 2xrp 9hta" matches "K7QM-2XRP-9HTA".
     */
    public static char[] normalizeRecoveryCode(String typedCode) {
        if (typedCode == null) {
            return new char[0];
        }
        return typedCode.replaceAll("[\\s-]", "").toUpperCase().toCharArray();
    }

    /** Hashes a recovery code exactly like a password (PBKDF2 + its own salt). */
    public static String hashRecoveryCode(String recoveryCode, String saltBase64) {
        char[] normalized = normalizeRecoveryCode(recoveryCode);
        try {
            return hashPassword(normalized, saltBase64);
        } finally {
            clear(normalized);
        }
    }

    /** Wipes a password array from memory once it is no longer needed. */
    public static void clear(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }
}
