package com.restaurant.reservation.service;

import com.restaurant.reservation.dto.NotificationDTO;
import com.restaurant.reservation.model.Customer;
import com.restaurant.reservation.model.Notification;
import com.restaurant.reservation.model.NotificationType;
import com.restaurant.reservation.model.Reservation;
import com.restaurant.reservation.model.SeatingPreference;
import com.restaurant.reservation.model.WaitlistEntry;
import com.restaurant.reservation.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    // ----------------------------------
    // | RESERVATION NOTIFICATION TESTS |
    // ----------------------------------
    
    @Test
    void createsReservationNotification() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101",
                "maria@example.com");

        Reservation reservation = new Reservation(customer, 4,
                LocalDateTime.of(2026, 12, 1, 18, 0),
                SeatingPreference.NO_PREFERENCE, "ABC123");

        String message = "Your reservation has been confirmed!";
        NotificationType type = NotificationType.RESERVATION_CONFIRMATION;

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDTO result =
                notificationService.createReservationNotification(reservation,
                        message, type);

        assertNotNull(result);
        assertEquals(message, result.message());
        assertEquals(type, result.type());
        assertNotNull(result.createdAt());

        verify(notificationRepository).save(any(Notification.class));
    }
    
    @Test
    void reservationNotificationContainsCorrectCustomerContact() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101",
                "maria@example.com");

        Reservation reservation = new Reservation(customer, 4,
                LocalDateTime.of(2026, 12, 1, 18, 0),
                SeatingPreference.NO_PREFERENCE, "ABC123");

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.createReservationNotification(reservation,
                "Your reservation has been confirmed!",
                NotificationType.RESERVATION_CONFIRMATION);

        verify(notificationRepository).save(captor.capture());

        Notification savedNotification = captor.getValue();

        assertEquals(customer.getPhone(), savedNotification.getRecipientPhone());

        assertEquals(customer.getEmail(), savedNotification.getRecipientEmail());
    }
    
    @Test
    void reservationNotificationIsAssociatedWithReservation() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101", 
        		"maria@example.com");

        Reservation reservation = new Reservation(customer, 4,
            LocalDateTime.of(2026, 12, 1, 18, 0),
            SeatingPreference.NO_PREFERENCE, "ABC123");

        ArgumentCaptor<Notification> captor =
            ArgumentCaptor.forClass(Notification.class);

        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.createReservationNotification(reservation,
            "Your reservation has been confirmed!",
            NotificationType.RESERVATION_CONFIRMATION);

        verify(notificationRepository).save(captor.capture());

        Notification savedNotification = captor.getValue();

        assertSame(reservation, savedNotification.getReservation());
        assertNull(savedNotification.getWaitlistEntry());
    }
    
    @Test
    void createReservationNotificationRejectsNullReservation() {
        assertThrows(IllegalArgumentException.class, () ->
            notificationService.createReservationNotification(null,
                "Your reservation has been confirmed!",
                NotificationType.RESERVATION_CONFIRMATION));

        verifyNoInteractions(notificationRepository);
    }
    
    @Test
    void createReservationNotificationRejectsBlankMessage() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101", 
        		"maria@example.com");

        Reservation reservation = new Reservation(customer, 4,
            LocalDateTime.of(2026, 12, 1, 18, 0),
            SeatingPreference.NO_PREFERENCE, "ABC123");

        assertThrows(IllegalArgumentException.class, () ->
            notificationService.createReservationNotification(reservation, "   ",
                NotificationType.RESERVATION_CONFIRMATION));

        verifyNoInteractions(notificationRepository);
    }
    
    @Test
    void createReservationNotificationRejectsNullType() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101", 
        		"maria@example.com");

        Reservation reservation = new Reservation(customer, 4,
            LocalDateTime.of(2026, 12, 1, 18, 0),
            SeatingPreference.NO_PREFERENCE, "ABC123");

        assertThrows(IllegalArgumentException.class, () ->
            notificationService.createReservationNotification(reservation,
                "Your reservation has been confirmed!", null));

        verifyNoInteractions(notificationRepository);
    }
    
    // -------------------------------
    // | WAITLIST NOTIFICATION TESTS |
    // -------------------------------
    
    @Test
    void createsWaitlistNotification() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101", 
        		"maria@example.com");

        WaitlistEntry waitlistEntry = new WaitlistEntry(customer, 4,
            SeatingPreference.NO_PREFERENCE);

        String message = "Your table is ready!";
        NotificationType type = NotificationType.TABLE_READY;

        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDTO result = notificationService.createWaitlistNotification(
            waitlistEntry, message, type);

        assertNotNull(result);
        assertEquals(message, result.message());
        assertEquals(type, result.type());
        assertNotNull(result.createdAt());

        verify(notificationRepository).save(any(Notification.class));
    }
    
    @Test
    void waitlistNotificationContainsCorrectCustomerContact() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101", 
        		"maria@example.com");

        WaitlistEntry waitlistEntry = new WaitlistEntry(customer, 4,
            SeatingPreference.NO_PREFERENCE);

        ArgumentCaptor<Notification> captor =
            ArgumentCaptor.forClass(Notification.class);

        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.createWaitlistNotification(waitlistEntry,
            "Your table is ready!", NotificationType.TABLE_READY);

        verify(notificationRepository).save(captor.capture());

        Notification savedNotification = captor.getValue();

        assertEquals(customer.getPhone(), savedNotification.getRecipientPhone());
        assertEquals(customer.getEmail(), savedNotification.getRecipientEmail());
    }
    
    @Test
    void waitlistNotificationIsAssociatedWithWaitlistEntry() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101", 
        		"maria@example.com");

        WaitlistEntry waitlistEntry = new WaitlistEntry(customer, 4,
            SeatingPreference.NO_PREFERENCE);

        ArgumentCaptor<Notification> captor =
            ArgumentCaptor.forClass(Notification.class);

        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.createWaitlistNotification(waitlistEntry,
            "Your table is ready!", NotificationType.TABLE_READY);

        verify(notificationRepository).save(captor.capture());

        Notification savedNotification = captor.getValue();

        assertSame(waitlistEntry, savedNotification.getWaitlistEntry());
        assertNull(savedNotification.getReservation());
    }
    
    @Test
    void createWaitlistNotificationRejectsNullWaitlistEntry() {
        assertThrows(IllegalArgumentException.class, () ->
        		notificationService.createWaitlistNotification(null,
                "Your table is ready!", NotificationType.TABLE_READY));

        verifyNoInteractions(notificationRepository);
    }
    
    @Test
    void createWaitlistNotificationRejectsBlankMessage() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101", 
        		"maria@example.com");

        WaitlistEntry waitlistEntry = new WaitlistEntry(customer, 4,
            SeatingPreference.NO_PREFERENCE);

        assertThrows(IllegalArgumentException.class, () ->
            notificationService.createWaitlistNotification(waitlistEntry, "   ",
                NotificationType.TABLE_READY));

        verifyNoInteractions(notificationRepository);
    }
    
    @Test
    void createWaitlistNotificationRejectsNullType() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101", 
        		"maria@example.com");

        WaitlistEntry waitlistEntry = new WaitlistEntry(customer, 4,
            SeatingPreference.NO_PREFERENCE);

        assertThrows(IllegalArgumentException.class, () ->
            notificationService.createWaitlistNotification(waitlistEntry,
                "Your table is ready!", null));

        verifyNoInteractions(notificationRepository);
    }
    
    // ----------------------------------------------
    // | NOTIFICATION HISTORY BY PHONE NUMBER TESTS |
    // ----------------------------------------------
    
    @Test
    void getNotificationsByPhoneReturnsNotifications() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101", 
        		"maria@example.com");

        Reservation reservation = new Reservation(customer, 4,
            LocalDateTime.of(2026, 12, 1, 18, 0),
            SeatingPreference.NO_PREFERENCE, "ABC123");

        Notification notification1 = new Notification(
            "Your reservation has been confirmed!",
            NotificationType.RESERVATION_CONFIRMATION, reservation);

        Notification notification2 = new Notification(
            "Reminder: Your reservation is coming up!",
            NotificationType.RESERVATION_REMINDER, reservation);

        List<Notification> notifications = List.of(notification1, notification2);

        when(notificationRepository.findByRecipientPhoneOrderByCreatedAtDesc(
            customer.getPhone())).thenReturn(notifications);

        List<NotificationDTO> result =
            notificationService.getNotificationsByPhone(customer.getPhone());

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals("Your reservation has been confirmed!", 
        		result.get(0).message());
        assertEquals(NotificationType.RESERVATION_CONFIRMATION,
            result.get(0).type());
        assertEquals(notification1.getCreatedAt(), result.get(0).createdAt());

        assertEquals("Reminder: Your reservation is coming up!", 
        		result.get(1).message());
        assertEquals(NotificationType.RESERVATION_REMINDER, result.get(1).type());
        assertEquals(notification2.getCreatedAt(), result.get(1).createdAt());

        verify(notificationRepository)
            .findByRecipientPhoneOrderByCreatedAtDesc(customer.getPhone());
    }
    
    @Test
    void getNotificationsByPhoneReturnsEmptyListWhenNoneExist() {
        String phone = "3105550101";

        when(notificationRepository.findByRecipientPhoneOrderByCreatedAtDesc(phone))
            .thenReturn(List.of());

        List<NotificationDTO> result =
            notificationService.getNotificationsByPhone(phone);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(notificationRepository)
            .findByRecipientPhoneOrderByCreatedAtDesc(phone);
    }
    
    // -----------------------------------------------
    // | NOTIFICATION HISTORY BY EMAIL ADDRESS TESTS |
    // -----------------------------------------------
    
    @Test
    void getNotificationsByEmailReturnsNotifications() {
        Customer customer = new Customer("Maria Lopez", "310-555-0101", 
        		"maria@example.com");

        Reservation reservation = new Reservation(customer, 4,
            LocalDateTime.of(2026, 12, 1, 18, 0),
            SeatingPreference.NO_PREFERENCE, "ABC123");

        Notification notification1 = new Notification(
            "Your reservation has been confirmed!",
            NotificationType.RESERVATION_CONFIRMATION, reservation);

        Notification notification2 = new Notification(
            "Reminder: Your reservation is coming up!",
            NotificationType.RESERVATION_REMINDER, reservation);

        List<Notification> notifications = List.of(notification1, notification2);

        when(notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(
            customer.getEmail())).thenReturn(notifications);

        List<NotificationDTO> result =
            notificationService.getNotificationsByEmail(customer.getEmail());

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals("Your reservation has been confirmed!", 
        		result.get(0).message());
        assertEquals(NotificationType.RESERVATION_CONFIRMATION,
            result.get(0).type());
        assertEquals(notification1.getCreatedAt(), result.get(0).createdAt());

        assertEquals("Reminder: Your reservation is coming up!", 
        		result.get(1).message());
        assertEquals(NotificationType.RESERVATION_REMINDER, result.get(1).type());
        assertEquals(notification2.getCreatedAt(), result.get(1).createdAt());

        verify(notificationRepository)
            .findByRecipientEmailOrderByCreatedAtDesc(customer.getEmail());
    }
    
    @Test
    void getNotificationsByEmailReturnsEmptyListWhenNoneExist() {
        String email = "maria@example.com";

        when(notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email))
            .thenReturn(List.of());

        List<NotificationDTO> result =
            notificationService.getNotificationsByEmail(email);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(notificationRepository)
            .findByRecipientEmailOrderByCreatedAtDesc(email);
    }
}
