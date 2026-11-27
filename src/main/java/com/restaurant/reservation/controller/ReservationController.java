package com.restaurant.reservation.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.restaurant.reservation.dto.ReservationRequest;
import com.restaurant.reservation.dto.ReservationResponse;
import com.restaurant.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
	 
	 private final ReservationService service;
	 public ReservationController(ReservationService service) { this.service = service; }
	 
	 @PostMapping
	 public ReservationResponse book(@Valid @RequestBody ReservationRequest request) {
	    return ReservationResponse.from(service.book(request));
	   }
	 
	 @GetMapping("/{confirmationCode}")
     public ReservationResponse find(@PathVariable String confirmationCode) {
		 
		 return service.findByConfirmationCode(confirmationCode)
				 .map(ReservationResponse::from)
				 .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
	 }
	 
	 @PutMapping("/{confirmationCode}")
	 public ReservationResponse modify (@PathVariable String confirmationCode,
			 @Valid @RequestBody ReservationRequest request) {
		 
		 return ReservationResponse.from(
				 service.modify(confirmationCode, request));
				 
	 }
	 
	 @DeleteMapping("/{confirmationCode}")
	 public ReservationResponse delete (@PathVariable String confirmationCode) {
		 
		 return ReservationResponse.from(service.cancel(confirmationCode));
	 }
}
