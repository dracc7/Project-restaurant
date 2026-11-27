package com.restaurant.reservation.service;

import com.restaurant.reservation.dto.ReservationResponse;
import com.restaurant.reservation.dto.TableResponse;
import com.restaurant.reservation.model.DiningTable;
import com.restaurant.reservation.model.Reservation;
import com.restaurant.reservation.model.ReservationStatus;
import com.restaurant.reservation.model.TableStatus;
import com.restaurant.reservation.repository.ReservationRepository;
import com.restaurant.reservation.repository.TableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class TableService {

    private final TableRepository tables;

    private final ReservationRepository reservations;

    public TableService(TableRepository tables, ReservationRepository reservations) {
        this.tables = tables;
        this.reservations = reservations;
    }

    public List<TableResponse> listTables(TableStatus status) {
        List<DiningTable> matchingTables = status == null
                ? tables.findAllByOrderByTableNumberAsc()
                : tables.findByStatusOrderByTableNumberAsc(status);
        return matchingTables.stream().map(TableResponse::from).toList();
    }

    @Transactional
    public Optional<TableResponse> updateStatus(int tableNumber, TableStatus status) {
        return tables.findByTableNumber(tableNumber).map(table -> {
            table.setStatus(status);
            return TableResponse.from(table);
        });
    }

    @Transactional(readOnly = true)
    public List<TableResponse> listAvailableTables(String confirmationCode) {
        Reservation reservation = findReservation(confirmationCode);
        requireBookedReservation(reservation);

        return tables.findAllByOrderByTableNumberAsc().stream()
                .filter(table -> table.getStatus() != TableStatus.UNAVAILABLE)
                .filter(table -> table.suits(reservation.getPartySize(), reservation.getSeatingPreference()))
                .filter(table -> isTableFree(table, reservation))
                .map(TableResponse::from)
                .toList();
    }

    @Transactional
    public ReservationResponse assignReservation(int tableNumber, String confirmationCode) {
        DiningTable table = lockTable(tableNumber);
        Reservation reservation = findReservation(confirmationCode);

        requireBookedReservation(reservation);
        if (table.getStatus() == TableStatus.UNAVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table is out of service.");
        }
        if (!table.suits(reservation.getPartySize(), reservation.getSeatingPreference())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table does not fit the party size or seating preference.");
        }
        if (!isTableFree(table, reservation)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table already has an overlapping reservation.");
        }

        reservation.assignTable(table);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse seatReservation(int tableNumber, String confirmationCode) {
        DiningTable table = lockTable(tableNumber);
        Reservation reservation = findReservation(confirmationCode);
        requireAssignedTable(reservation, table);

        if (reservation.getStatus() == ReservationStatus.SEATED && table.getStatus() == TableStatus.SEATED) {
            return ReservationResponse.from(reservation);
        }
        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only booked reservations can be seated.");
        }
        if (table.getStatus() != TableStatus.READY && table.getStatus() != TableStatus.RESERVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table is not ready for seating.");
        }
        if (reservations.findActiveByTable(table.getId()).stream()
                .anyMatch(other -> other.getStatus() == ReservationStatus.SEATED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another reservation is already seated at this table.");
        }

        reservation.markSeated();
        table.setStatus(TableStatus.SEATED);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse completeReservation(int tableNumber, String confirmationCode) {
        DiningTable table = lockTable(tableNumber);
        Reservation reservation = findReservation(confirmationCode);
        requireAssignedTable(reservation, table);

        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            return ReservationResponse.from(reservation);
        }
        if (reservation.getStatus() != ReservationStatus.SEATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only seated reservations can be completed.");
        }

        reservation.markCompleted();
        if (table.getStatus() == TableStatus.SEATED) {
            table.setStatus(TableStatus.READY);
        }
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse markNoShow(String confirmationCode) {
        Reservation reservation = findReservation(confirmationCode);
        if (reservation.getStatus() == ReservationStatus.NO_SHOW) {
            return ReservationResponse.from(reservation);
        }

        requireBookedReservation(reservation);
        reservation.markNoShow();
        return ReservationResponse.from(reservation);
    }

    private boolean isTableFree(DiningTable table, Reservation reservation) {
        return reservations.isTableFree(table.getId(), reservation.getStartTime(),
                reservation.getEndTime(), reservation.getId());
    }

    private void requireBookedReservation(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation must be booked.");
        }
    }

    private void requireAssignedTable(Reservation reservation, DiningTable table) {
        DiningTable assignedTable = reservation.getAssignedTable();
        if (assignedTable == null || !table.getId().equals(assignedTable.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation is not assigned to this table.");
        }
    }

    private DiningTable lockTable(int tableNumber) {
        return tables.findLockedByTableNumber(tableNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found."));
    }

    private Reservation findReservation(String confirmationCode) {
        return reservations.findByConfirmationCodeIgnoreCase(confirmationCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found."));
    }
}
