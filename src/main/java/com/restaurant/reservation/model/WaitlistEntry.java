package com.restaurant.reservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * One party waiting for a table. Stored in the "waitlist_entries" table.
 *
 * This class stores the data and the per-entry rules (counting misses
 * and skips). Deciding WHO gets the next table means looking at the
 * whole line, so that belongs in Keira's WaitlistService.
 */
@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version                                   // optimistic locking, see DiningTable
    private Long version;

    @Embedded                                  // customer_name, customer_phone, customer_email columns
    private Customer customer;

    @Column(nullable = false)
    private int partySize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatingPreference seatingPreference;

    @Column(nullable = false)
    private LocalDateTime joinedAt;            // keeps first-come, first-served order

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    private LocalDateTime notifiedAt;          // when we last told them a table was ready (may be null)

    @Column(nullable = false)
    private int missedResponses = 0;           // times they didn't respond in time

    @Column(nullable = false)
    private int timesSkipped = 0;              // times passed over for seating preference

    /** Required by JPA. Use the constructors below instead. */
    protected WaitlistEntry() { }

    /** Normal constructor: joins the line right now. */
    public WaitlistEntry(Customer customer, int partySize, SeatingPreference seatingPreference) {
        this(customer, partySize, seatingPreference, LocalDateTime.now());
    }

    /** With an explicit join time. Useful for sample data and tests. */
    public WaitlistEntry(Customer customer, int partySize,
                         SeatingPreference seatingPreference, LocalDateTime joinedAt) {
        if (customer == null) {
            throw new IllegalArgumentException("A waitlist entry needs a customer.");
        }
        if (partySize < 1 || partySize > RestaurantRules.MAX_PARTY_SIZE) {
            throw new IllegalArgumentException("Party size must be between 1 and "
                    + RestaurantRules.MAX_PARTY_SIZE + ".");
        }
        if (seatingPreference == null || joinedAt == null) {
            throw new IllegalArgumentException("Seating preference and join time are required.");
        }
        this.customer = customer;
        this.partySize = partySize;
        this.seatingPreference = seatingPreference;
        this.joinedAt = joinedAt;
    }

    // ===================================================================
    //  Rules for one entry
    // ===================================================================

    /** Is this table a good fit for this party (size + preference)? */
    public boolean fits(DiningTable table) {
        return table.suits(partySize, seatingPreference);
    }

    /** Has this party been skipped enough times to jump ahead? */
    public boolean hasPriority() {
        return timesSkipped >= RestaurantRules.MAX_TIMES_SKIPPED;
    }

    /** We told them a table is ready. Starts their response timer. */
    public void markNotified(LocalDateTime now) {
        requireStatus(WaitlistStatus.WAITING, "notify");
        status = WaitlistStatus.NOTIFIED;
        notifiedAt = now;
    }

    /** Has their response window run out? */
    public boolean responseWindowExpired(LocalDateTime now) {
        return status == WaitlistStatus.NOTIFIED
                && now.isAfter(notifiedAt.plus(RestaurantRules.WAITLIST_RESPONSE_WINDOW));
    }

    /** They didn't respond in time: back to WAITING, or REMOVED after too many misses. */
    public void recordMissedResponse() {
        requireStatus(WaitlistStatus.NOTIFIED, "record a missed response for");
        missedResponses++;
        status = (missedResponses >= RestaurantRules.MAX_MISSED_RESPONSES)
                ? WaitlistStatus.REMOVED
                : WaitlistStatus.WAITING;
        notifiedAt = null;
    }

    /** A table opened but didn't suit their preference, so we skipped them. */
    public void recordSkipped() {
        requireStatus(WaitlistStatus.WAITING, "skip");
        timesSkipped++;
    }

    /** They responded and sat down. */
    public void markSeated() {
        requireStatus(WaitlistStatus.NOTIFIED, "seat");
        status = WaitlistStatus.SEATED;
    }

    /** They left the line, or staff removed them. */
    public void remove() {
        if (!status.isActive()) {
            throw new IllegalStateException("Waitlist entry for " + customer.getName()
                    + " is already " + status + ".");
        }
        status = WaitlistStatus.REMOVED;
    }

    private void requireStatus(WaitlistStatus required, String action) {
        if (status != required) {
            throw new IllegalStateException("Can't " + action + " " + customer.getName()
                    + " because their entry is " + status + " (must be " + required + ").");
        }
    }

    // ===================================================================
    //  Getters
    // ===================================================================

    public Long getId()                             { return id; }
    public Customer getCustomer()                   { return customer; }
    public int getPartySize()                       { return partySize; }
    public SeatingPreference getSeatingPreference() { return seatingPreference; }
    public LocalDateTime getJoinedAt()              { return joinedAt; }
    public WaitlistStatus getStatus()               { return status; }
    public LocalDateTime getNotifiedAt()            { return notifiedAt; }
    public int getMissedResponses()                 { return missedResponses; }
    public int getTimesSkipped()                    { return timesSkipped; }

    @Override
    public String toString() {
        return customer.getName() + ", party of " + partySize + ", " + seatingPreference
                + " [" + status + ", skipped " + timesSkipped + ", missed " + missedResponses + "]";
    }
}
