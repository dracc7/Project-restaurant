package com.restaurant.reservation.model;

import java.util.List;

/**
 * The life cycle of a reservation.
 *
 *   BOOKED ──► SEATED ──► COMPLETED
 *     │
 *     ├──► CANCELLED   (customer or staff cancelled ahead of time)
 *     └──► NO_SHOW     (the party never arrived)
 *
 * Only BOOKED and SEATED reservations are "active", meaning they are
 * currently holding a table.
 */
public enum ReservationStatus {
    BOOKED,
    SEATED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    /**
     * The statuses that hold a table. The repository uses this list in
     * database queries ("WHERE status IN ('BOOKED', 'SEATED')").
     */
    public static final List<ReservationStatus> ACTIVE = List.of(BOOKED, SEATED);

    /** @return true if a reservation in this status is still holding a table */
    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
