package com.restaurant.reservation.model;

/**
 * Where a TABLE physically is. Every table is either inside or outside.
 *
 * (Customers use SeatingPreference instead, because a customer is
 * allowed to say "I don't care", but a table can't be "anywhere".)
 */
public enum SeatingArea {
    INDOOR,
    OUTDOOR
}
