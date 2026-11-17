package com.restaurant.reservation.repository;

import com.restaurant.reservation.model.WaitlistEntry;
import com.restaurant.reservation.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Database access for the waitlist.
 *
 * Free from JpaRepository: save, findById, findAll, count, deleteById.
 * Don't delete entries. Use entry.remove() or markSeated(), then save,
 * so we keep a history.
 */
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    /** Entries with the given statuses, oldest first (first come, first served). */
    List<WaitlistEntry> findByStatusInOrderByJoinedAtAsc(Collection<WaitlistStatus> statuses);

    /**
     * The current line: WAITING and NOTIFIED entries in the order they joined.
     * Main input for the queue organizer and wait-time estimate.
     */
    default List<WaitlistEntry> findActiveInOrder() {
        return findByStatusInOrderByJoinedAtAsc(WaitlistStatus.ACTIVE);
    }
}
