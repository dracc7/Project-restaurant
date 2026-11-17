package com.restaurant.reservation.repository;

import com.restaurant.reservation.model.Reservation;
import com.restaurant.reservation.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Database access for reservations.
 *
 * Free from JpaRepository: save, findById, findAll, count, deleteById.
 *
 * Don't delete reservations. Cancel them (reservation.cancel(), then
 * save) so staff keep a history and can see no-shows.
 *
 * "default" methods below are normal Java code inside the interface.
 * They combine the derived queries into easier, named helpers.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // =================== Derived queries (Spring writes the SQL) ===================

    /** How customers find their booking. Not case-sensitive. */
    Optional<Reservation> findByConfirmationCodeIgnoreCase(String code);

    /** Is this code already used? Pass to ConfirmationCodeGenerator.generateUnique. */
    boolean existsByConfirmationCode(String code);

    /** Every reservation, earliest first. */
    List<Reservation> findAllByOrderByStartTimeAsc();

    /** Reservations starting at or after "from" and before "to", earliest first. */
    List<Reservation> findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            LocalDateTime from, LocalDateTime to);

    /**
     * Reservations at one table with one of the given statuses.
     * "AssignedTable_Id" walks from the reservation to its table's id.
     */
    List<Reservation> findByAssignedTable_IdAndStatusInOrderByStartTimeAsc(
            Long tableId, Collection<ReservationStatus> statuses);

    // =================== Helpers built on top of them ===================

    /** Everything on one day, earliest first. For the staff reservations page. */
    default List<Reservation> findByDate(LocalDate date) {
        return findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                date.atStartOfDay(),                 // 00:00 that day
                date.plusDays(1).atStartOfDay());    // up to (not including) 00:00 the next day
    }

    /** Active (BOOKED or SEATED) reservations at one table. */
    default List<Reservation> findActiveByTable(Long tableId) {
        return findByAssignedTable_IdAndStatusInOrderByStartTimeAsc(tableId, ReservationStatus.ACTIVE);
    }

    /**
     * DOUBLE-BOOKING CHECK (FR-2): is this table free for the whole time slot?
     *
     * @param ignoreReservationId pass a reservation's own id when MODIFYING it,
     *                            so it doesn't clash with itself. Use null for new bookings.
     */
    default boolean isTableFree(Long tableId, LocalDateTime start, LocalDateTime end,
                                Long ignoreReservationId) {
        return findActiveByTable(tableId).stream()
                .filter(r -> !r.getId().equals(ignoreReservationId))   // skip the one being modified
                .noneMatch(r -> r.overlaps(start, end));               // free if NOTHING overlaps
    }
}
