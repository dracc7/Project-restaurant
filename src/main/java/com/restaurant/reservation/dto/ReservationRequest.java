package com.restaurant.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;
import com.restaurant.reservation.model.SeatingPreference;

public record ReservationRequest (
	@NotBlank String name,
	String phone,
	String email,
	@Min(1) @Max(12) int partySize,
	@NotNull LocalDateTime startTime,
	@NotNull SeatingPreference seatingPreference
){}
	
