package com.restaurant.reservation.model;

import com.restaurant.reservation.util.ConfirmationCodeGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit tests for the rules inside the model classes.
 * No database, no Spring, so these run in well under a second.
 *
 * Eclipse: right-click this file → Run As → JUnit Test.
 *
 * Each @Test method is one check. assertTrue / assertFalse / assertEquals
 * fail the test if the condition is wrong. assertThrows passes only if
 * the code throws the expected exception (we WANT bad input rejected).
 */
class ModelRulesTest {

    // Shared test values: a fixed time so results never depend on the clock
    private static final LocalDateTime SIX_PM = LocalDateTime.of(2026, 12, 1, 18, 0);
    private final Customer maria = new Customer("Maria Lopez", "(310) 555-0101", null);

    // ======================= DiningTable =======================

    @Test
    void tableFitsPartiesUpToItsCapacity() {
        DiningTable table = new DiningTable(8, 4, SeatingArea.OUTDOOR);
        assertTrue(table.canSeat(4));
        assertFalse(table.canSeat(5));
        assertFalse(table.canSeat(0));
    }

    @Test
    void tableSuitsMatchingPreferenceOnly() {
        DiningTable outdoor = new DiningTable(8, 4, SeatingArea.OUTDOOR);
        assertTrue(outdoor.suits(2, SeatingPreference.NO_PREFERENCE));
        assertTrue(outdoor.suits(2, SeatingPreference.OUTDOOR));
        assertFalse(outdoor.suits(2, SeatingPreference.INDOOR));
    }

    @Test
    void newTableStartsReady() {
        assertEquals(TableStatus.READY, new DiningTable(1, 2, SeatingArea.INDOOR).getStatus());
    }

    @Test
    void invalidTablesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DiningTable(0, 4, SeatingArea.INDOOR));
        assertThrows(IllegalArgumentException.class, () -> new DiningTable(1, 0, SeatingArea.INDOOR));
        assertThrows(IllegalArgumentException.class, () -> new DiningTable(1, 4, null));
    }

    // ======================= Customer =======================

    @Test
    void phoneMatchesInAnyFormat() {
        assertTrue(maria.matchesContact("310-555-0101"));
        assertTrue(maria.matchesContact("3105550101"));
        assertFalse(maria.matchesContact("310-555-9999"));
    }

    @Test
    void emailMatchIgnoresCase() {
        Customer james = new Customer("James Kim", null, "JKim@Example.com");
        assertTrue(james.matchesContact("jkim@example.com"));
    }

    @Test
    void customerNeedsNameAndSomeContact() {
        assertThrows(IllegalArgumentException.class, () -> new Customer(" ", "3105550101", null));
        assertThrows(IllegalArgumentException.class, () -> new Customer("Nobody", null, " "));
    }

    // ======================= Reservation =======================

    @Test
    void reservationEndsAfterReservationLength() {
        Reservation r = new Reservation(maria, 4, SIX_PM, SeatingPreference.OUTDOOR, "ABC234");
        assertEquals(SIX_PM.plus(RestaurantRules.RESERVATION_LENGTH), r.getEndTime());
    }

    @Test
    void overlappingTimeSlotsAreDetected() {
        Reservation sixPm = new Reservation(maria, 4, SIX_PM, SeatingPreference.OUTDOOR, "ABC234");
        LocalDateTime sevenPm = SIX_PM.plusHours(1);
        // 6:00-7:30 vs 7:00-8:30 → clash
        assertTrue(sixPm.overlaps(sevenPm, sevenPm.plus(RestaurantRules.RESERVATION_LENGTH)));
        // 6:00-7:30 vs 7:30-9:00 → back to back, no clash
        LocalDateTime sevenThirty = sixPm.getEndTime();
        assertFalse(sixPm.overlaps(sevenThirty, sevenThirty.plus(RestaurantRules.RESERVATION_LENGTH)));
    }

    @Test
    void invalidReservationsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Reservation(maria, 0, SIX_PM, SeatingPreference.INDOOR, "ABC234"));
        assertThrows(IllegalArgumentException.class,
                () -> new Reservation(maria, RestaurantRules.MAX_PARTY_SIZE + 1, SIX_PM, SeatingPreference.INDOOR, "ABC234"));
        assertThrows(IllegalArgumentException.class,
                () -> new Reservation(maria, 2, null, SeatingPreference.INDOOR, "ABC234"));
        assertThrows(IllegalArgumentException.class,
                () -> new Reservation(maria, 2, SIX_PM, SeatingPreference.INDOOR, " "));
    }

    @Test
    void confirmationCodeIsStoredUppercase() {
        Reservation r = new Reservation(maria, 2, SIX_PM, SeatingPreference.INDOOR, "abc234");
        assertEquals("ABC234", r.getConfirmationCode());
    }

    @Test
    void tooSmallOrWrongAreaTableCannotBeAssigned() {
        Reservation party6 = new Reservation(maria, 6, SIX_PM, SeatingPreference.NO_PREFERENCE, "ABC234");
        assertThrows(IllegalArgumentException.class,
                () -> party6.assignTable(new DiningTable(8, 4, SeatingArea.OUTDOOR)));

        Reservation wantsIndoor = new Reservation(maria, 2, SIX_PM, SeatingPreference.INDOOR, "ABC235");
        assertThrows(IllegalArgumentException.class,
                () -> wantsIndoor.assignTable(new DiningTable(7, 2, SeatingArea.OUTDOOR)));
    }

    @Test
    void cancelOnlyWorksOnce() {
        Reservation r = new Reservation(maria, 4, SIX_PM, SeatingPreference.OUTDOOR, "ABC234");
        assertTrue(r.isActive());
        r.cancel();
        assertEquals(ReservationStatus.CANCELLED, r.getStatus());
        assertFalse(r.isActive());
        assertThrows(IllegalStateException.class, r::cancel);
    }

    @Test
    void fullLifeCycleBookedSeatedCompleted() {
        Reservation r = new Reservation(maria, 4, SIX_PM, SeatingPreference.OUTDOOR, "ABC234");
        assertThrows(IllegalStateException.class, r::markSeated);   // no table yet
        r.assignTable(new DiningTable(8, 4, SeatingArea.OUTDOOR));
        r.markSeated();
        assertTrue(r.isActive());
        r.markCompleted();
        assertFalse(r.isActive());
        assertThrows(IllegalStateException.class, r::markNoShow);   // too late for no-show
    }

    // ======================= WaitlistEntry =======================

    @Test
    void waitlistEntryFitsOnlyMatchingTables() {
        WaitlistEntry chris = new WaitlistEntry(maria, 2, SeatingPreference.OUTDOOR, SIX_PM);
        assertFalse(chris.fits(new DiningTable(1, 2, SeatingArea.INDOOR)));
        assertTrue(chris.fits(new DiningTable(7, 2, SeatingArea.OUTDOOR)));
    }

    @Test
    void partyGetsPriorityAfterEnoughSkips() {
        WaitlistEntry e = new WaitlistEntry(maria, 2, SeatingPreference.OUTDOOR, SIX_PM);
        for (int i = 0; i < RestaurantRules.MAX_TIMES_SKIPPED; i++) {
            assertFalse(e.hasPriority());
            e.recordSkipped();
        }
        assertTrue(e.hasPriority());
    }

    @Test
    void partyIsRemovedAfterTooManyMissedResponses() {
        WaitlistEntry e = new WaitlistEntry(maria, 4, SeatingPreference.NO_PREFERENCE, SIX_PM);
        for (int i = 0; i < RestaurantRules.MAX_MISSED_RESPONSES; i++) {
            e.markNotified(SIX_PM);
            e.recordMissedResponse();
        }
        assertEquals(WaitlistStatus.REMOVED, e.getStatus());
        assertFalse(e.getStatus().isActive());
    }

    @Test
    void responseWindowExpiresAfterTheLimit() {
        WaitlistEntry e = new WaitlistEntry(maria, 3, SeatingPreference.INDOOR, SIX_PM);
        e.markNotified(SIX_PM);
        assertFalse(e.responseWindowExpired(SIX_PM.plusMinutes(1)));
        assertTrue(e.responseWindowExpired(
                SIX_PM.plus(RestaurantRules.WAITLIST_RESPONSE_WINDOW).plusMinutes(1)));
    }

    @Test
    void notifiedPartyCanBeSeated() {
        WaitlistEntry e = new WaitlistEntry(maria, 3, SeatingPreference.INDOOR, SIX_PM);
        assertThrows(IllegalStateException.class, e::markSeated);   // must be notified first
        e.markNotified(SIX_PM);
        e.markSeated();
        assertEquals(WaitlistStatus.SEATED, e.getStatus());
    }

    // ======================= ConfirmationCodeGenerator =======================

    @Test
    void generatedCodesAreSixCharactersAndSkipTakenOnes() {
        String first = ConfirmationCodeGenerator.generate();
        assertEquals(6, first.length());
        // Pretend "first" is taken: the unique version must return something else
        String unique = ConfirmationCodeGenerator.generateUnique(code -> code.equals(first));
        assertFalse(unique.equals(first));
    }
}
