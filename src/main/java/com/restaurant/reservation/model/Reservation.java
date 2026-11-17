package com.restaurant.reservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * A booking made by a customer for a specific date and time.
 * Stored as one row in the "reservations" table.
 *
 * This class guards its own rules: you can't cancel a completed
 * reservation, and you can't assign a table that's too small. No
 * teammate can accidentally put a reservation into an impossible state.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version                                   // optimistic locking, see DiningTable
    private Long version;

    // What the customer uses to look up their booking. The database
    // refuses duplicates (unique = true).
    @Column(nullable = false, unique = true, length = 12)
    private String confirmationCode;

    // @Embedded: the Customer's fields become columns in THIS table
    // (customer_name, customer_phone, customer_email)
    @Embedded
    private Customer customer;

    @Column(nullable = false)
    private int partySize;

    @Column(nullable = false)
    private LocalDateTime startTime;           // date AND time together, e.g. 2026-11-20T19:30

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatingPreference seatingPreference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.BOOKED;

    /**
     * @ManyToOne: MANY reservations can point to ONE table (different times
     * of the night). In the database this is a "table_id" column holding the
     * table's id: a foreign key. Null until a table is assigned.
     */
    @ManyToOne
    @JoinColumn(name = "table_id")
    private DiningTable assignedTable;

    @Column(nullable = false)
    private LocalDateTime createdAt;           // when the booking was made

    /** Required by JPA. Use the constructor below instead. */
    protected Reservation() { }

    public Reservation(Customer customer, int partySize, LocalDateTime startTime,
                       SeatingPreference seatingPreference, String confirmationCode) {
        if (customer == null) {
            throw new IllegalArgumentException("A reservation needs a customer.");
        }
        if (confirmationCode == null || confirmationCode.isBlank()) {
            throw new IllegalArgumentException("A reservation needs a confirmation code.");
        }
        this.customer = customer;
        this.confirmationCode = confirmationCode.trim().toUpperCase();
        setPartySize(partySize);               // setters hold the checks
        setStartTime(startTime);
        setSeatingPreference(seatingPreference);
        this.createdAt = LocalDateTime.now();
    }

    // ===================================================================
    //  Time helpers: used to prevent double booking (FR-2)
    // ===================================================================

    /** When the table is free again, e.g. 7:00 PM + 90 min = 8:30 PM. */
    public LocalDateTime getEndTime() {
        return startTime.plus(RestaurantRules.RESERVATION_LENGTH);
    }

    /**
     * Does this reservation's time slot overlap another time slot?
     * Two slots overlap when each one starts before the other one ends.
     *   7:00-8:30 and 8:00-9:30  → overlap
     *   7:00-8:30 and 8:30-10:00 → NO overlap (back-to-back is fine)
     */
    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return startTime.isBefore(otherEnd) && otherStart.isBefore(getEndTime());
    }

    /** Convenience version: does this overlap another reservation? */
    public boolean overlaps(Reservation other) {
        return overlaps(other.getStartTime(), other.getEndTime());
    }

    // ===================================================================
    //  Status changes: each checks the change makes sense
    // ===================================================================

    /** Is this reservation currently holding a table? */
    public boolean isActive() {
        return status.isActive();
    }

    /** Customer or staff cancels. Only a BOOKED reservation can be cancelled. */
    public void cancel() {
        requireStatus(ReservationStatus.BOOKED, "cancel");
        status = ReservationStatus.CANCELLED;
    }

    /** The party arrived and sat down. Needs a table first. */
    public void markSeated() {
        requireStatus(ReservationStatus.BOOKED, "seat");
        if (assignedTable == null) {
            throw new IllegalStateException("Assign a table before seating reservation " + confirmationCode);
        }
        status = ReservationStatus.SEATED;
    }

    /** The party finished and left. */
    public void markCompleted() {
        requireStatus(ReservationStatus.SEATED, "complete");
        status = ReservationStatus.COMPLETED;
    }

    /** The party never showed up (employees mark this). */
    public void markNoShow() {
        requireStatus(ReservationStatus.BOOKED, "mark as no-show");
        status = ReservationStatus.NO_SHOW;
    }

    /**
     * Puts this reservation at a table. Refuses tables that are too small
     * or in the wrong area.
     *
     * NOTE: this does NOT check whether the table is free at this time.
     * That needs every reservation, so use
     * ReservationRepository.isTableFree(...) in the service first.
     */
    public void assignTable(DiningTable table) {
        if (table == null) {
            throw new IllegalArgumentException("Table can't be null.");
        }
        if (!table.suits(partySize, seatingPreference)) {
            throw new IllegalArgumentException(table + " doesn't suit a party of "
                    + partySize + " wanting " + seatingPreference);
        }
        this.assignedTable = table;
    }

    private void requireStatus(ReservationStatus required, String action) {
        if (status != required) {
            throw new IllegalStateException("Can't " + action + " reservation " + confirmationCode
                    + " because it is " + status + " (must be " + required + ").");
        }
    }

    // ===================================================================
    //  Getters and setters
    // ===================================================================

    public Long getId()                            { return id; }
    public String getConfirmationCode()            { return confirmationCode; }
    public Customer getCustomer()                  { return customer; }
    public int getPartySize()                      { return partySize; }
    public LocalDateTime getStartTime()            { return startTime; }
    public SeatingPreference getSeatingPreference() { return seatingPreference; }
    public ReservationStatus getStatus()           { return status; }
    public DiningTable getAssignedTable()          { return assignedTable; }   // may be null
    public LocalDateTime getCreatedAt()            { return createdAt; }

    /** Customers and staff can modify a booking. */
    public void setPartySize(int partySize) {
        if (partySize < 1 || partySize > RestaurantRules.MAX_PARTY_SIZE) {
            throw new IllegalArgumentException("Party size must be between 1 and "
                    + RestaurantRules.MAX_PARTY_SIZE + ".");
        }
        this.partySize = partySize;
    }

    public void setStartTime(LocalDateTime startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time is required.");
        }
        this.startTime = startTime;
    }

    public void setSeatingPreference(SeatingPreference seatingPreference) {
        if (seatingPreference == null) {
            throw new IllegalArgumentException("Seating preference is required (use NO_PREFERENCE).");
        }
        this.seatingPreference = seatingPreference;
    }

    @Override
    public String toString() {
        String table = (assignedTable == null) ? "no table yet" : "table " + assignedTable.getTableNumber();
        return "Reservation " + confirmationCode + ": " + customer.getName() + ", party of " + partySize
                + " at " + startTime + ", " + seatingPreference + ", " + table + " [" + status + "]";
    }
}
