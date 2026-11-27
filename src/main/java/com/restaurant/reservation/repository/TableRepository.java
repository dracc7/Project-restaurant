package com.restaurant.reservation.repository;

import com.restaurant.reservation.model.DiningTable;
import com.restaurant.reservation.model.TableStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

/**
 * Database access for tables.
 *
 * Extending JpaRepository<DiningTable, Long> gives these for FREE:
 *     save(table)        insert or update
 *     findById(id)       Optional<DiningTable>
 *     findAll()          every table
 *     count()            how many
 *     deleteById(id)     remove (managers, FR-10)
 *
 * The methods below are "derived queries": Spring reads the method
 * NAME and writes the SQL for us. For example:
 *     findByStatusOrderByTableNumberAsc
 *       → SELECT * FROM dining_tables WHERE status = ? ORDER BY table_number ASC
 *
 * There is no class that implements this interface. Spring creates one
 * automatically when the app starts. To use it in your service, just
 * ask for it in your constructor (see README).
 */
public interface TableRepository extends JpaRepository<DiningTable, Long> {

    /** Look up by the number staff see on the floor. */
    Optional<DiningTable> findByTableNumber(int tableNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DiningTable> findLockedByTableNumber(int tableNumber);

    /** Is this table number already used? (Check before a manager adds a table.) */
    boolean existsByTableNumber(int tableNumber);

    /** All tables, sorted by table number. Use this for the staff floor view (FR-7). */
    List<DiningTable> findAllByOrderByTableNumberAsc();

    /** e.g. every READY table, sorted by number (FR-7, FR-12). */
    List<DiningTable> findByStatusOrderByTableNumberAsc(TableStatus status);
}
