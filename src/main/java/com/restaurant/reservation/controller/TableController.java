package com.restaurant.reservation.controller;

import com.restaurant.reservation.dto.ReservationResponse;
import com.restaurant.reservation.dto.TableResponse;
import com.restaurant.reservation.dto.TableStatusRequest;
import com.restaurant.reservation.model.TableStatus;
import com.restaurant.reservation.service.TableService;
import jakarta.validation.Valid;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final TableService service;

    public TableController(TableService service) {
        this.service = service;
    }

    @GetMapping
    public List<TableResponse> listTables(@RequestParam(name = "status", required = false) TableStatus status) {
        return service.listTables(status);
    }

    @GetMapping("/available")
    public List<TableResponse> listAvailableTables(@RequestParam("confirmationCode") String confirmationCode) {
        return service.listAvailableTables(confirmationCode);
    }

    @PutMapping("/{tableNumber}/status")
    public ResponseEntity<TableResponse> updateStatus(
            @PathVariable("tableNumber") int tableNumber,
            @Valid @RequestBody TableStatusRequest request) {
        return ResponseEntity.of(service.updateStatus(tableNumber, request.status()));
    }

    @PutMapping("/{tableNumber}/reservations/{confirmationCode}")
    public ReservationResponse assignReservation(
            @PathVariable("tableNumber") int tableNumber,
            @PathVariable("confirmationCode") String confirmationCode) {
        return service.assignReservation(tableNumber, confirmationCode);
    }

    @PutMapping("/{tableNumber}/reservations/{confirmationCode}/seated")
    public ReservationResponse seatReservation(
            @PathVariable("tableNumber") int tableNumber,
            @PathVariable("confirmationCode") String confirmationCode) {
        return service.seatReservation(tableNumber, confirmationCode);
    }

    @PutMapping("/{tableNumber}/reservations/{confirmationCode}/completed")
    public ReservationResponse completeReservation(
            @PathVariable("tableNumber") int tableNumber,
            @PathVariable("confirmationCode") String confirmationCode) {
        return service.completeReservation(tableNumber, confirmationCode);
    }

    @PutMapping("/reservations/{confirmationCode}/no-show")
    public ReservationResponse markNoShow(@PathVariable("confirmationCode") String confirmationCode) {
        return service.markNoShow(confirmationCode);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Void> handleConcurrentUpdate() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
