/**
 * SERVICES: the business logic layer. Each service uses the repositories
 * to read and save data; services never write SQL themselves.
 *
 * Owners
 *   - Kodah:  ReservationService   (book, modify, cancel, availability checker, validation)
 *   - Ali:    TableService         (table status, seating assignment, double-booking prevention)
 *   - Keira:  WaitlistService      (join, queue organizer, position, estimated wait, removal)
 *   - Angela: NotificationService  (confirmations, reminders, table-ready alerts, history)
 *
 * Pattern (see README for a full example):
 * <pre>
 * &#64;Service
 * public class ReservationService {
 *     private final ReservationRepository reservations;   // Spring passes this in
 *     public ReservationService(ReservationRepository reservations) {
 *         this.reservations = reservations;
 *     }
 *     ...
 * }
 * </pre>
 * Put &#64;Transactional on methods that change more than one thing at once
 * (e.g. cancel a reservation AND update the table), so it's all-or-nothing.
 */
package com.restaurant.reservation.service;
