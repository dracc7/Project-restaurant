package com.restaurant.reservation.dto;

import java.time.LocalDateTime;
import com.restaurant.reservation.model.SeatingPreference;
import com.restaurant.reservation.model.Reservation;
import com.restaurant.reservation.model.ReservationStatus;

public record ReservationResponse (
		Long id,
		String confirmationCode,
		String customerName,
		int partySize,
		LocalDateTime startTime,
		SeatingPreference seatingPreference,
		Integer tableNumber,
		ReservationStatus status
	) {

	public static ReservationResponse from(Reservation reservation) {
		
		Integer tableNumber = reservation.getAssignedTable() == null
				? null
				: reservation.getAssignedTable().getTableNumber();
		
		return new ReservationResponse(
				reservation.getId(),
				reservation.getConfirmationCode(),
				reservation.getCustomer().getName(),
				reservation.getPartySize(),
				reservation.getStartTime(),
				reservation.getSeatingPreference(),
				tableNumber,
				reservation.getStatus()
				
				);
				
		
	}

	
	
}



