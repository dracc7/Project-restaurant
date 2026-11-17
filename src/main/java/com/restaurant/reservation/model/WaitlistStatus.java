package com.restaurant.reservation.model;

import java.util.List;

/**
 * The life cycle of a waitlist entry (FR-11 to FR-15).
 *
 *   WAITING ──► NOTIFIED ──► SEATED
 *     ▲            │
 *     └────────────┘  (didn't respond in time, goes back to waiting)
 *
 *   Any active entry can also become REMOVED
 *   (left the line, or missed too many times).
 */
public enum WaitlistStatus {
    WAITING,    // in line
    NOTIFIED,   // told a table is ready; we're waiting for them to respond
    SEATED,     // got a table, done
    REMOVED;    // left the waitlist or was removed after too many misses

    /** The statuses that mean "still in line". Used in repository queries. */
    public static final List<WaitlistStatus> ACTIVE = List.of(WAITING, NOTIFIED);

    /** @return true if this entry is still in line (waiting or notified) */
    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
