package com.restaurant.reservation.config;

import com.restaurant.reservation.model.Customer;
import com.restaurant.reservation.model.DiningTable;
import com.restaurant.reservation.model.Reservation;
import com.restaurant.reservation.model.SeatingArea;
import com.restaurant.reservation.model.SeatingPreference;
import com.restaurant.reservation.model.WaitlistEntry;
import com.restaurant.reservation.repository.ReservationRepository;
import com.restaurant.reservation.repository.TableRepository;
import com.restaurant.reservation.repository.WaitlistRepository;
import com.restaurant.reservation.util.ConfirmationCodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Fills an EMPTY database with a small sample restaurant when the app
 * starts, so everyone has data to test against right away.
 *
 * @Component = Spring creates this automatically.
 * CommandLineRunner = Spring calls run() once, right after startup.
 *
 * Turn it off with  app.sample-data=false  in application.properties.
 * It never runs if tables already exist, so it won't duplicate data
 * in PostgreSQL.
 */
@Component
public class SampleDataLoader implements CommandLineRunner {

    private final TableRepository tables;
    private final ReservationRepository reservations;
    private final WaitlistRepository waitlist;
    private final boolean enabled;

    /**
     * Spring passes in the repositories automatically ("dependency injection").
     * @Value reads app.sample-data from application.properties (false if missing).
     */
    public SampleDataLoader(TableRepository tables,
                            ReservationRepository reservations,
                            WaitlistRepository waitlist,
                            @Value("${app.sample-data:false}") boolean enabled) {
        this.tables = tables;
        this.reservations = reservations;
        this.waitlist = waitlist;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled || tables.count() > 0) {
            return;   // turned off, or data already exists
        }

        // ---------- Tables: 6 indoor, 3 outdoor ----------
        tables.save(new DiningTable(1, 2, SeatingArea.INDOOR));
        tables.save(new DiningTable(2, 2, SeatingArea.INDOOR));
        tables.save(new DiningTable(3, 4, SeatingArea.INDOOR));
        tables.save(new DiningTable(4, 4, SeatingArea.INDOOR));
        tables.save(new DiningTable(5, 6, SeatingArea.INDOOR));
        tables.save(new DiningTable(6, 8, SeatingArea.INDOOR));
        tables.save(new DiningTable(7, 2, SeatingArea.OUTDOOR));
        tables.save(new DiningTable(8, 4, SeatingArea.OUTDOOR));
        tables.save(new DiningTable(9, 4, SeatingArea.OUTDOOR));

        // ---------- Reservations for tomorrow evening ----------
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        addReservation(new Customer("Maria Lopez", "310-555-0101", null),
                4, tomorrow.atTime(LocalTime.of(18, 0)), SeatingPreference.OUTDOOR, 8);
        addReservation(new Customer("James Kim", null, "jkim@example.com"),
                2, tomorrow.atTime(LocalTime.of(19, 0)), SeatingPreference.INDOOR, 1);
        addReservation(new Customer("Aisha Brown", "424-555-0199", "aisha@example.com"),
                6, tomorrow.atTime(LocalTime.of(19, 30)), SeatingPreference.NO_PREFERENCE, 5);

        // ---------- A short waitlist, in the order they joined ----------
        LocalDateTime now = LocalDateTime.now();
        waitlist.save(new WaitlistEntry(new Customer("Chris Park", "213-555-0144", null),
                2, SeatingPreference.OUTDOOR, now.minusMinutes(20)));
        waitlist.save(new WaitlistEntry(new Customer("Dana White", "562-555-0177", null),
                4, SeatingPreference.NO_PREFERENCE, now.minusMinutes(12)));
        waitlist.save(new WaitlistEntry(new Customer("Eli Nguyen", null, "eli@example.com"),
                3, SeatingPreference.INDOOR, now.minusMinutes(5)));

        System.out.println("Sample data loaded: " + tables.count() + " tables, "
                + reservations.count() + " reservations, " + waitlist.count() + " waitlisted parties.");
    }

    /** Helper: create a reservation, assign a table by number, save it. */
    private void addReservation(Customer customer, int partySize, LocalDateTime when,
                                SeatingPreference preference, int tableNumber) {
        String code = ConfirmationCodeGenerator.generateUnique(reservations::existsByConfirmationCode);
        Reservation r = new Reservation(customer, partySize, when, preference, code);
        r.assignTable(tables.findByTableNumber(tableNumber).orElseThrow());
        reservations.save(r);
    }
}
