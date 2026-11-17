package com.restaurant.reservation.model;

/**
 * What a CUSTOMER asks for when booking or joining the waitlist (FR-1, FR-11).
 *
 * Enums in Java can have methods too. matches() answers the question
 * "would this customer be happy at a table in this area?", which the
 * waitlist logic (FR-12) and table assignment both need.
 */
public enum SeatingPreference {
    INDOOR,
    OUTDOOR,
    NO_PREFERENCE;   // happy with either

    /**
     * @param area where the table is
     * @return true if a customer with this preference would accept that table
     */
    public boolean matches(SeatingArea area) {
        // "this" is the preference we're asking about (e.g. OUTDOOR)
        switch (this) {
            case NO_PREFERENCE:
                return true;                      // any table is fine
            case INDOOR:
                return area == SeatingArea.INDOOR;
            case OUTDOOR:
                return area == SeatingArea.OUTDOOR;
            default:
                return false;                     // never reached, but the compiler wants it
        }
    }
}
