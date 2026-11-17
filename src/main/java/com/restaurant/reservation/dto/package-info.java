/**
 * DTOs (Data Transfer Objects): what the React UI sends to the server and
 * what the server sends back. They only CARRY data. They are never saved.
 *
 * Owners: whoever builds the matching feature
 *   - Kodah:  ReservationRequest, ReservationResponse
 *   - Ali:    TableStatusUpdate, TableView
 *   - Keira:  WaitlistJoinRequest, WaitlistPositionResponse
 *   - Angela: notification DTOs
 *
 * Tip: write DTOs as Java records, with validation annotations, e.g.
 * <pre>
 * public record ReservationRequest(
 *         &#64;NotBlank String name,
 *         String phone,
 *         String email,
 *         &#64;Min(1) &#64;Max(12) int partySize,
 *         &#64;NotNull LocalDateTime startTime,
 *         &#64;NotNull SeatingPreference seatingPreference) { }
 * </pre>
 * Never send an entity (Reservation, DiningTable, ...) straight to the UI.
 * Copy what the UI needs into a response DTO instead.
 */
package com.restaurant.reservation.dto;
