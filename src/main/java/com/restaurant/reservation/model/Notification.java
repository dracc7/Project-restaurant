
package com.restaurant.reservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/** A notification sent to a customer about a reservation or waitlist entry
 * - Each notification belongs to either a reservation or a waitlist entry, but 
 *   never both
 * - Customer contact info is stored directly so their associated notification
 *   history can be retrieved using either a phone number or email address */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version                                  
    private Long version;

    // Customer contact information used to retrieve notification history.
    // Either field may be null, but at least one must be provided.
    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "recipient_email", length = 254)
    private String recipientEmail;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Many notifications can belong to one reservation
    // Null when this notification concerns a waitlist entry
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    // Many notifications can belong to one waitlist entry
    // Null when this notification concerns a reservation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waitlist_entry_id")
    private WaitlistEntry waitlistEntry;

    /** Use the constructors below instead*/
    protected Notification() { }

    /** Creates a notification associated with a reservation */
    public Notification(String message, NotificationType type,
    			Reservation reservation) {
        this(reservation == null ? null : reservation.getCustomer(), message,
            type, reservation, null);
    }

    /** Creates a notification associated with a waitlist entry */
    public Notification(String message, NotificationType type,
    			WaitlistEntry waitlistEntry) {
        this(waitlistEntry == null ? null : waitlistEntry.getCustomer(), message,
            type, null, waitlistEntry);
    }

    /** Shared constructor containing validation and initialization */
    private Notification(Customer customer, String message, NotificationType type, 
    			Reservation reservation, WaitlistEntry waitlistEntry) {

    		// Validates associated customer
        if (customer == null) {
            throw new IllegalArgumentException("A notification needs a customer.");
        }

        // Validates associated contact info
        if (customer.getPhone() == null && customer.getEmail() == null) {
            throw new IllegalArgumentException(
            		"A notification needs a phone number or email.");
        }

        // Validates notification message
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
            		"A notification message is required.");
        }

        // Validates notification type
        if (type == null) {
            throw new IllegalArgumentException("A notification type is required.");
        }

        // A notification must belong to exactly one associated object.
        if ((reservation == null) == (waitlistEntry == null)) {
            throw new IllegalArgumentException("A notification must belong to "
            		+ "exactly one reservation or waitlist entry.");
        }

        this.recipientPhone = customer.getPhone();
        this.recipientEmail = customer.getEmail();
        this.message = message.trim();
        this.type = type;
        this.reservation = reservation;
        this.waitlistEntry = waitlistEntry;
        this.createdAt = LocalDateTime.now();
    }
    
    //  Getters
    public Long getId() {
        return id;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public WaitlistEntry getWaitlistEntry() {
        return waitlistEntry;
    }
}

