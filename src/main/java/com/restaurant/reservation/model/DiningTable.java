package com.restaurant.reservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * One physical table in the restaurant.
 *
 * @Entity = "store objects of this class in the database".
 * Each DiningTable becomes one row in the "dining_tables" table.
 *
 * Named DiningTable (not Table) so it doesn't clash with JPA's own
 * @Table annotation used just below.
 */
@Entity
@Table(name = "dining_tables")
public class DiningTable {

    // @Id = primary key. @GeneratedValue = the DATABASE picks the number
    // (1, 2, 3, ...) when the table is first saved. Never set it yourself.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @Version turns on "optimistic locking". Every save bumps this number.
     * If two staff members load the same table and both try to save a
     * change, the second save fails instead of silently overwriting the
     * first. That protects against conflicting updates (NFR-1).
     */
    @Version
    private Long version;

    // The number staff see on the floor ("Table 12").
    // unique = true: the DATABASE refuses two tables with the same number.
    @Column(nullable = false, unique = true)
    private int tableNumber;

    @Column(nullable = false)
    private int capacity;                  // how many people fit

    // EnumType.STRING stores the word "OUTDOOR", not a number like 1,
    // so the database is readable and safe if the enum order changes.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatingArea area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus status = TableStatus.READY;   // every new table starts READY

    /** Required by JPA. Use the constructor below instead. */
    protected DiningTable() { }

    public DiningTable(int tableNumber, int capacity, SeatingArea area) {
        if (tableNumber <= 0) {
            throw new IllegalArgumentException("Table number must be positive.");
        }
        this.tableNumber = tableNumber;
        setCapacity(capacity);   // reuse the setter so the capacity check lives in one place
        setArea(area);
    }

    /** "Can this table fit a party of N?" */
    public boolean canSeat(int partySize) {
        return partySize > 0 && partySize <= capacity;
    }

    /** "Is this table a good fit?" checks size AND seating preference together. */
    public boolean suits(int partySize, SeatingPreference preference) {
        return canSeat(partySize) && preference.matches(area);
    }

    // ----- Getters -----
    public Long getId()            { return id; }
    public int getTableNumber()    { return tableNumber; }
    public int getCapacity()       { return capacity; }
    public SeatingArea getArea()   { return area; }
    public TableStatus getStatus() { return status; }

    // ----- Setters (with checks) -----

    /** Staff change status from the employee page (FR-8). */
    public void setStatus(TableStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status can't be null.");
        }
        this.status = status;
    }

    /** Managers can edit tables (FR-10). */
    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be at least 1.");
        }
        this.capacity = capacity;
    }

    public void setArea(SeatingArea area) {
        if (area == null) {
            throw new IllegalArgumentException("Table must have a seating area.");
        }
        this.area = area;
    }

    @Override
    public String toString() {
        return "Table " + tableNumber + " [" + capacity + " seats, " + area + ", " + status + "]";
    }
}
