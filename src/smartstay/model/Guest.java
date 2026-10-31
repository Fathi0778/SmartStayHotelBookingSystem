package smartstay.model;

/**
 * A registered hotel customer. Guests log in with their email address.
 * A guest can have many reservations (one-to-many association, held by
 * ReservationService through the guest ID stored on each Reservation).
 */
public class Guest extends User {

    private final String nicOrPassport;
    private String contactNumber;
    private String email;

    public Guest(String guestId, String nicOrPassport, String fullName, String contactNumber,
                 String email, String passwordHash, String passwordSalt,
                 String recoveryCodeHash, String recoveryCodeSalt) {
        super(guestId, fullName, passwordHash, passwordSalt, recoveryCodeHash, recoveryCodeSalt);
        if (nicOrPassport == null || nicOrPassport.trim().isEmpty()) {
            throw new IllegalArgumentException("NIC/Passport number is required.");
        }
        this.nicOrPassport = nicOrPassport.trim().toUpperCase();
        setContactNumber(contactNumber);
        setEmail(email);
    }

    public String getGuestId() {
        return getUserId();
    }

    public String getNicOrPassport() {
        return nicOrPassport;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public final void setContactNumber(String contactNumber) {
        if (contactNumber == null || contactNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact number is required.");
        }
        this.contactNumber = contactNumber.trim();
    }

    public String getEmail() {
        return email;
    }

    public final void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        this.email = email.trim().toLowerCase();
    }

    @Override
    public UserRole getRole() {
        return UserRole.GUEST;
    }

    @Override
    public String getLoginName() {
        return email;
    }
}
