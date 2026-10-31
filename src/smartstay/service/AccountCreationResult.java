package smartstay.service;

import smartstay.model.User;

/**
 * What the service returns after creating an account or resetting a
 * password: the user, plus the NEW recovery code in plain text.
 *
 * The plain code exists only in this object, for long enough to show it
 * on screen once. Only its hash is ever saved to the data files.
 */
public final class AccountCreationResult {

    private final User user;
    private final String recoveryCode;

    public AccountCreationResult(User user, String recoveryCode) {
        this.user = user;
        this.recoveryCode = recoveryCode;
    }

    public User getUser() {
        return user;
    }

    /** Show this to the user ONCE and then let the object be discarded. */
    public String getRecoveryCode() {
        return recoveryCode;
    }
}
