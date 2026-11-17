package com.restaurant.reservation.repository;

import com.restaurant.reservation.model.Customer;
import com.restaurant.reservation.model.DiningTable;
import com.restaurant.reservation.model.Reservation;
import com.restaurant.reservation.model.RestaurantRules;
import com.restaurant.reservation.model.SeatingArea;
import com.restaurant.reservation.model.SeatingPreference;
import com.restaurant.reservation.model.TableStatus;
import com.restaurant.reservation.model.WaitlistEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the repositories against a REAL database (H2, created fresh for
 * the tests), so we know the queries and constraints actually work.
 *
 * @SpringBootTest starts the whole app for the test. Sample data is
 *   turned off so each test controls exactly what's in the database.
 * @Transactional rolls back every change after each test, so tests
 *   never affect each other.
 *
 * Eclipse: right-click → Run As → JUnit Test (takes a few seconds to start).
 */
@SpringBootTest(properties = "app.sample-data=false")
@Transactional
class RepositoryTest {

    // @Autowired: Spring hands the test the real repositories
    @Autowired private TableRepository tables;
    @Autowired private ReservationRepository reservations;
    @Autowired private WaitlistRepository waitlist;

    private static final LocalDate DAY = LocalDate.of(2026, 12, 1);
    private DiningTable table8;   // outdoor, seats 4

    @BeforeEach   // runs before EVERY test
    void setUp() {
        table8 = tables.save(new DiningTable(8, 4, SeatingArea.OUTDOOR));
        tables.save(new DiningTable(1, 2, SeatingArea.INDOOR));
        tables.save(new DiningTable(3, 4, SeatingArea.INDOOR));
    }

    /** Helper: save a booked reservation at table 8. */
    private Reservation bookTable8(String code, LocalDateTime start) {
        Reservation r = new Reservation(new Customer("Maria Lopez", "310-555-0101", null),
                4, start, SeatingPreference.OUTDOOR, code);
        r.assignTable(table8);
        return reservations.save(r);
    }

    // ======================= Tables =======================

    @Test
    void savingATableGivesItAnId() {
        assertNotNull(table8.getId());
    }

    @Test
    void findTablesByNumberAndInOrder() {
        assertTrue(tables.findByTableNumber(8).isPresent());
        assertFalse(tables.findByTableNumber(99).isPresent());
        assertTrue(tables.existsByTableNumber(1));

        List<DiningTable> all = tables.findAllByOrderByTableNumberAsc();
        assertEquals(List.of(1, 3, 8), all.stream().map(DiningTable::getTableNumber).toList());
    }

    @Test
    void findTablesByStatus() {
        table8.setStatus(TableStatus.SEATED);
        tables.save(table8);
        List<DiningTable> seated = tables.findByStatusOrderByTableNumberAsc(TableStatus.SEATED);
        assertEquals(1, seated.size());
        assertEquals(8, seated.get(0).getTableNumber());
    }

    @Test
    void databaseRejectsDuplicateTableNumbers() {
        // saveAndFlush sends the INSERT immediately, so the unique constraint fires now
        assertThrows(DataIntegrityViolationException.class,
                () -> tables.saveAndFlush(new DiningTable(8, 2, SeatingArea.INDOOR)));
    }

    // ======================= Reservations =======================

    @Test
    void findReservationByCodeIgnoringCase() {
        bookTable8("K7QM2P", DAY.atTime(18, 0));
        assertTrue(reservations.findByConfirmationCodeIgnoreCase("k7qm2p").isPresent());
        assertTrue(reservations.existsByConfirmationCode("K7QM2P"));
        assertFalse(reservations.existsByConfirmationCode("ZZZZZZ"));
    }

    @Test
    void databaseRejectsDuplicateConfirmationCodes() {
        bookTable8("K7QM2P", DAY.atTime(18, 0));
        Reservation copy = new Reservation(new Customer("Someone Else", "2135550000", null),
                2, DAY.atTime(20, 0), SeatingPreference.NO_PREFERENCE, "K7QM2P");
        assertThrows(DataIntegrityViolationException.class, () -> reservations.saveAndFlush(copy));
    }

    @Test
    void findReservationsByDateOnlyReturnsThatDay() {
        bookTable8("AAA222", DAY.atTime(20, 0));
        bookTable8("BBB333", DAY.atTime(17, 0));
        bookTable8("CCC444", DAY.plusDays(1).atTime(18, 0));   // the next day

        List<Reservation> onDay = reservations.findByDate(DAY);
        assertEquals(2, onDay.size());
        assertEquals("BBB333", onDay.get(0).getConfirmationCode());   // earliest first
    }

    @Test
    void doubleBookingCheckFindsClashes() {
        bookTable8("AAA222", DAY.atTime(18, 0));   // 6:00 to 7:30 PM

        LocalDateTime sevenPm = DAY.atTime(19, 0);
        LocalDateTime sevenThirty = DAY.atTime(19, 30);
        assertFalse(reservations.isTableFree(table8.getId(), sevenPm,
                sevenPm.plus(RestaurantRules.RESERVATION_LENGTH), null));
        assertTrue(reservations.isTableFree(table8.getId(), sevenThirty,
                sevenThirty.plus(RestaurantRules.RESERVATION_LENGTH), null));
    }

    @Test
    void modifyingAReservationDoesNotClashWithItself() {
        Reservation r = bookTable8("AAA222", DAY.atTime(18, 0));
        LocalDateTime later = DAY.atTime(18, 30);   // moving it 30 minutes later
        assertTrue(reservations.isTableFree(table8.getId(), later,
                later.plus(RestaurantRules.RESERVATION_LENGTH), r.getId()));
    }

    @Test
    void cancelledReservationFreesTheTable() {
        Reservation r = bookTable8("AAA222", DAY.atTime(18, 0));
        assertEquals(1, reservations.findActiveByTable(table8.getId()).size());

        r.cancel();
        reservations.save(r);

        assertTrue(reservations.findActiveByTable(table8.getId()).isEmpty());
        LocalDateTime sixPm = DAY.atTime(18, 0);
        assertTrue(reservations.isTableFree(table8.getId(), sixPm,
                sixPm.plus(RestaurantRules.RESERVATION_LENGTH), null));
    }

    // ======================= Waitlist =======================

    @Test
    void waitlistIsFirstComeFirstServedAndSkipsRemovedParties() {
        LocalDateTime now = DAY.atTime(18, 0);
        // Saved out of order on purpose: the query must sort by joinedAt
        WaitlistEntry dana  = waitlist.save(new WaitlistEntry(new Customer("Dana White", "5625550177", null),
                4, SeatingPreference.NO_PREFERENCE, now.minusMinutes(12)));
        WaitlistEntry chris = waitlist.save(new WaitlistEntry(new Customer("Chris Park", "2135550144", null),
                2, SeatingPreference.OUTDOOR, now.minusMinutes(20)));
        WaitlistEntry eli   = waitlist.save(new WaitlistEntry(new Customer("Eli Nguyen", null, "eli@example.com"),
                3, SeatingPreference.INDOOR, now.minusMinutes(5)));

        List<WaitlistEntry> line = waitlist.findActiveInOrder();
        assertEquals(List.of(chris.getId(), dana.getId(), eli.getId()),
                line.stream().map(WaitlistEntry::getId).toList());

        dana.remove();
        waitlist.save(dana);
        assertEquals(2, waitlist.findActiveInOrder().size());
    }
}
