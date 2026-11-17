/**
 * CONTROLLERS: receive requests from the React UI (JSON over HTTP), check
 * the input, call a service, and send back a response DTO. They contain
 * no business rules and never use repositories directly.
 *
 * Owners
 *   - Kodah:  ReservationController  (/api/reservations)
 *   - Ali:    TableController        (/api/tables)
 *   - Keira:  WaitlistController     (/api/waitlist)
 *
 * Pattern:
 * <pre>
 * &#64;RestController
 * &#64;RequestMapping("/api/reservations")
 * public class ReservationController {
 *     private final ReservationService service;
 *     public ReservationController(ReservationService service) { this.service = service; }
 *
 *     &#64;PostMapping
 *     public ReservationResponse book(&#64;Valid &#64;RequestBody ReservationRequest request) {
 *         return ReservationResponse.from(service.book(request));
 *     }
 * }
 * </pre>
 */
package com.restaurant.reservation.controller;
