package com.restaurant.reservation.model;

import java.time.Duration;

/**
 * Every "magic number" in the system lives here, in ONE place.
 *
 * Several of these are still OPEN QUESTIONS for the group (see the
 * proposal doc). When the group decides, change the number here and
 * the whole program follows. Nobody has to hunt through other files.
 *
 * "final" means the value can't be changed while the program runs.
 * "static" means you use it without creating an object:
 *      RestaurantRules.RESERVATION_LENGTH
 */
public final class RestaurantRules {

    // TODO (open question): how long does a reservation hold a table?
    public static final Duration RESERVATION_LENGTH = Duration.ofMinutes(90);

    // TODO (open question): how long does a waitlisted party have to respond?
    public static final Duration WAITLIST_RESPONSE_WINDOW = Duration.ofMinutes(10);

    // TODO (open question): how many missed responses before removal? (FR-13)
    public static final int MAX_MISSED_RESPONSES = 3;

    // TODO (open question): how many times can a party be skipped
    // for seating preference before it gets priority? (FR-14)
    public static final int MAX_TIMES_SKIPPED = 3;

    // Largest party we accept online. Bigger groups should call the restaurant.
    public static final int MAX_PARTY_SIZE = 12;

    // A private constructor stops anyone from writing "new RestaurantRules()".
    // This class is only a holder for constants.
    private RestaurantRules() { }
}
