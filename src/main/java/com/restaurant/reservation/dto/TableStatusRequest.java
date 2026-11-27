package com.restaurant.reservation.dto;

import com.restaurant.reservation.model.TableStatus;
import jakarta.validation.constraints.NotNull;

public record TableStatusRequest(@NotNull TableStatus status) {
}
