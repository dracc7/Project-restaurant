package com.restaurant.reservation.repository;

import com.restaurant.reservation.model.Notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for accessing and managing customer notifications
 * - Provides methods for retrieving notification history using a customer's
 *   associated phone number or email address */
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    /** Finds all notifications associated with a phone number */
    List<Notification> findByRecipientPhoneOrderByCreatedAtDesc(
            String recipientPhone
    );

    /** Finds all notifications associated with an email address */
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(
            String recipientEmail
    );
    
}
