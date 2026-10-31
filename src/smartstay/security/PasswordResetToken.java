package smartstay.security;

import java.time.LocalDateTime;

/**
 * Proof that a recovery code was verified a moment ago.
 *
 * It is created only by PasswordRecoveryService after a correct recovery
 * code is entered, and the reset screen must hand it back to change the
 * password. It:
 *  - expires after a few minutes,
 *  - can be used only once,
 *  - stops working if the recovery code was changed in the meantime.
 */
public final class PasswordResetToken {

    public static final int VALID_MINUTES = 10;

    private final String userId;
    private final String recoveryHashAtVerification;
    private final LocalDateTime expiresAt;
    private boolean used;

    public PasswordResetToken(String userId, String recoveryHashAtVerification) {
        this.userId = userId;
        this.recoveryHashAtVerification = recoveryHashAtVerification;
        this.expiresAt = LocalDateTime.now().plusMinutes(VALID_MINUTES);
    }

    public String getUserId() {
        return userId;
    }

    public String getRecoveryHashAtVerification() {
        return recoveryHashAtVerification;
    }

    public boolean isUsable() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }

    public void markUsed() {
        this.used = true;
    }
}
