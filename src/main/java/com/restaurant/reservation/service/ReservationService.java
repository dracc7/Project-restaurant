package com.restaurant.reservation.service;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.restaurant.reservation.dto.ReservationRequest;
import com.restaurant.reservation.model.Reservation;
import com.restaurant.reservation.model.RestaurantRules;
import com.restaurant.reservation.repository.ReservationRepository;
import com.restaurant.reservation.repository.TableRepository;
import org.springframework.transaction.annotation.Transactional;
import com.restaurant.reservation.util.ConfirmationCodeGenerator;
import com.restaurant.reservation.model.Customer;


@Service
public class ReservationService {

	private final ReservationRepository reservations;
	private final TableRepository tables;
	
	public ReservationService(ReservationRepository reservations, TableRepository tables) {
		
		this.reservations = reservations;
		this.tables = tables;
	}
	
	public Reservation book(ReservationRequest request) {
					
		Customer customer = new Customer(
				request.name(),
				request.phone(),
				request.email());
				
	String confirmationCode = ConfirmationCodeGenerator.generateUnique
			(reservations::existsByConfirmationCode);
	
	Reservation reservation = new Reservation(
			customer, request.partySize(),
			request.startTime(), request.seatingPreference(),
			confirmationCode
			);
	return reservations.save(reservation);
	}
	
	public Optional<Reservation> findByConfirmationCode(String code){
		return reservations.findByConfirmationCodeIgnoreCase(code);
		
	}
	
	@Transactional
	public Reservation modify(String confirmationCode, ReservationRequest request) {
		
		Reservation reservation = findByConfirmationCode(confirmationCode)
				.orElseThrow(() -> new RuntimeException("Reservation not found"));
				
				if(reservation.getAssignedTable() == null) {
					throw new RuntimeException("No table assigned to reservation");
				}
				
				var table = tables.findLockedByTableNumber(reservation.getAssignedTable().getTableNumber())
						.orElseThrow(() -> new IllegalStateException("Assigned table not found"));
				Long tableId = table.getId();
				
				LocalDateTime newEndTime= request.startTime().plus(RestaurantRules.RESERVATION_LENGTH);
				
				Long reservationId = reservation.getId();
					
				if(reservations.isTableFree(tableId, request.startTime(), newEndTime,reservationId) == false) {
					throw new RuntimeException("This table is not available");
				}
				
				reservation.setPartySize(request.partySize());
				reservation.setStartTime(request.startTime());
				reservation.setSeatingPreference(request.seatingPreference()); 
				
				return reservations.save(reservation);
	}
	
	public Reservation cancel(String confirmationCode) {
		
		Reservation reservation = findByConfirmationCode(confirmationCode)
				.orElseThrow(() -> new RuntimeException("Reservation not found"));
		
				reservation.cancel();
				
				return reservations.save(reservation);
		
	}
	
	
}
 