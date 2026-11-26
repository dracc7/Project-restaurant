
package com.restaurant.reservation.service;

import com.restaurant.reservation.dto.NotificationDTO;
import com.restaurant.reservation.model.Notification;
import com.restaurant.reservation.model.NotificationType;
import com.restaurant.reservation.model.Reservation;
import com.restaurant.reservation.model.WaitlistEntry;
import com.restaurant.reservation.repository.NotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Service for managing customer notifications
 * 
 * Other services may use this service to create notifications when
 * reservation or waitlist events occur
 * 
 * Notifications are associated with the relevant reservation or waitlist entry 
 * and saved through the NotificationRepository
 * 
 * Currently, notifications are stored as in-app notifications; this service does
 * not send external emails or text messages.
 * 
 * - Handles notification creation for reservations and waitlist entries 
 * - Retrieves notification history using a customer's phone number or email address
 * - Converts Notification objects into DTOs before returning them to other
 *   application layers */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** Creates a notification associated with a reservation
     * 
     * Other services should call this method when an event involving a 
     * reservation requires a notification */
    @Transactional
    public NotificationDTO createReservationNotification(
            Reservation reservation,
            String message,
            NotificationType type) {

    		// Validates the associated reservation
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null.");
        }

        // Creates notification
        Notification notification = new Notification(message, type, reservation);
        
        // Saves the notification to the repository
        Notification savedNotification = notificationRepository.save(notification);
        
        // Converts to DTO
        return convertToDTO(savedNotification);
    }

    /** Creates a notification associated with a waitlist entry
     * 
     * Other services should call this method when an event involving a waitlist
     * entry requires a notification */
    @Transactional
    public NotificationDTO createWaitlistNotification(WaitlistEntry waitlistEntry,
            String message, NotificationType type) {

    		// Validates the associated waitlist entry
        if (waitlistEntry == null) {
            throw new IllegalArgumentException("Waitlist entry cannot be null.");
        }

        // Creates notification
        Notification notification = new Notification(message, type, waitlistEntry);

        // Saves the notification to the repository
        Notification savedNotification = notificationRepository.save(notification);

        // Converts to DTO
        return convertToDTO(savedNotification);
    }

    /** Retrieves a customer's notification history using their associated phone 
     * number
     * 
     * This method may be called when the application needs to display
     * notifications associated with a customer's phone number */
    public List<NotificationDTO> getNotificationsByPhone(String phone) {

    		// Validates phone number
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be blank.");
        }

        // Retrieves list of notifications associated with the given phone number
        // in descending order based on time created
        List<Notification> notifications = notificationRepository
        			.findByRecipientPhoneOrderByCreatedAtDesc(phone.trim());

        // Converts notifications into DTOs
        return notifications.stream().map(this::convertToDTO).toList();
    }

    /** Retrieves a customer's notification history using their associated email
     * address
     * 
     * This method may be called when the application needs to display
     * notifications associated with a customer's email address */
    public List<NotificationDTO> getNotificationsByEmail(String email) {

    		// Validates email address
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be blank.");
        }

        // Retrieves list of notifications associated with the given email address
        // in descending order based on time created
        List<Notification> notifications = notificationRepository
        			.findByRecipientEmailOrderByCreatedAtDesc(email.trim());

        return notifications.stream().map(this::convertToDTO).toList();
    }

    /** Converts a Notification into a NotificationDTO */
    private NotificationDTO convertToDTO(Notification notification) {

        return new NotificationDTO(notification.getId(), notification.getMessage(),
                notification.getType(), notification.getCreatedAt());
    }
}

