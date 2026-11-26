**Using NotificationService**

NotificationService provides methods for creating in-app notifications and retrieving a customer's notification history.

Other services should inject NotificationService through their constructors rather than creating notification entities or accessing NotificationRepository directly.

```
private final NotificationService notificationService;
```

**Example Constructor:**

```
public ReservationService(ReservationRepository reservations,
	NotificationService notificationService) {
		this.reservations = reservations;
		this.notificationService = notificationService;
}
```


<br>

**Creating a reservation notification**

Call *createReservationNotification()* when a reservation event requires a notification, such as confirmation, modification, or cancellation.

```
notificationService.createReservationNotification(reservation, message, type);
```

**Parameters:**

- *reservation*: The saved Reservation associated with the notification.
- *message*: The message displayed to the customer.
- *type*: The category of notification (refer to *NotificationType*).

**Example Usage:**

```
public Reservation book(ReservationRequest request) {
	...
	// Save reservation to repository
	savedReservation = reservations.save(reservation);
	
	// Send notification after saving reservation to repository
	notificationService.createReservationNotification(
   		savedReservation, "Your reservation has been confirmed!",
   		NotificationType.RESERVATION_CONFIRMATION);
   		
   	return savedReservation;
}
```

<br>

**Creating a waitlist notification**

Call *createWaitlistNotification()* when a waitlist event requires a notification, such as when a table becomes available.

```
notificationService.createWaitlistNotification(waitlistEntry, message, type);
```

**Parameters:**

- *waitlistEntry*: The WaitlistEntry associated with the notification.
- *message*: The message displayed to the customer.
- *type*: The category of notification (refer to *NotificationType).

<br>

**Retrieving notification history**

Notifications may be retrieved using the customer's phone number or email address.

```
List<NotificationDTO> notifications =
        notificationService.getNotificationsByPhone(phone);
```
Or:

```
List<NotificationDTO> notifications =
        notificationService.getNotificationsByEmail(email);
```

Both methods return notification DTOs ordered from newest to oldest.

<br>

**Important notes**

- Pass the saved Reservation or WaitlistEntry entity to notification creation methods.
- Do not access NotificationRepository directly from other services.
- NotificationService handles creation, persistence, and conversion to NotificationDTO.
- In the current version, notifications are stored as in-app notifications; no external email or SMS messages are sent.
- Notification creation should occur only after the associated event has successfully completed.