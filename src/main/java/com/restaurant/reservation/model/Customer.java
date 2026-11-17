package com.restaurant.reservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A guest's contact details.
 *
 * @Embeddable means Customer does NOT get its own database table.
 * Its three fields are stored as extra columns inside whatever table
 * uses it (reservations and waitlist_entries). Customers don't have
 * accounts, so they don't need a table of their own.
 *
 * A customer proves a booking is theirs with the confirmation code
 * plus their phone or email (NFR-5). We keep only the minimum data
 * needed (NFR-8): name, phone, email.
 *
 * There are no setters: once created, a Customer never changes.
 */
@Embeddable
public class Customer {

    // @Column names the database column. "customer_" prefix keeps them
    // clear inside the reservations table (customer_name, not just name).
    @Column(name = "customer_name", nullable = false, length = 100)
    private String name;

    @Column(name = "customer_phone", length = 20)
    private String phone;   // may be null if they gave an email instead

    @Column(name = "customer_email", length = 254)
    private String email;   // may be null if they gave a phone instead

    /**
     * JPA REQUIRES an empty constructor so it can rebuild objects from
     * database rows. "protected" keeps teammates from calling it by
     * accident. Always use the constructor below.
     */
    protected Customer() { }

    public Customer(String name, String phone, String email) {
        // ---- Validation: refuse bad data right at the door ----
        if (isBlank(name)) {
            throw new IllegalArgumentException("Customer name is required.");
        }
        if (isBlank(phone) && isBlank(email)) {
            // We need SOME way to contact them for reminders and table-ready alerts
            throw new IllegalArgumentException("Customer needs a phone number or an email.");
        }

        this.name = name.trim();                                     // trim() removes stray spaces
        this.phone = isBlank(phone) ? null : normalizePhone(phone);
        this.email = isBlank(email) ? null : email.trim().toLowerCase();   // emails aren't case-sensitive
    }

    /**
     * Checks whether the phone number or email someone typed belongs to
     * this customer. Used when a customer looks up or cancels a booking (NFR-5).
     */
    public boolean matchesContact(String phoneOrEmail) {
        if (isBlank(phoneOrEmail)) {
            return false;
        }
        String typed = phoneOrEmail.trim();
        boolean emailMatches = email != null && email.equalsIgnoreCase(typed);
        boolean phoneMatches = phone != null && phone.equals(normalizePhone(typed));
        return emailMatches || phoneMatches;
    }

    // ----- Getters only -----
    public String getName()  { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return name + " (" + (phone != null ? phone : email) + ")";
    }

    // ----- Private helpers -----

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Keeps only the digits, so "(310) 555-1234" and "310-555-1234" match. */
    private static String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
}
