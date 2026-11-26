package com.restaurant.reservation.dto;

import com.restaurant.reservation.model.NotificationType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        String message,
        NotificationType type,
        @NotNull LocalDateTime createdAt) {}