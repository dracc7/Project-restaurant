package com.restaurant.reservation.model;

/**
 * The status of a physical table in the restaurant (requirement FR-7).
 *
 * An enum is a fixed list of allowed values. A table can ONLY ever be one
 * of these four, so typos like "Reservd" are impossible.
 */
public enum TableStatus {
    READY,        // empty and clean, can be given to a party
    RESERVED,     // being held for a party that is about to arrive
    SEATED,       // a party is currently sitting there
    UNAVAILABLE   // out of service (broken, closed section, etc.)
}
