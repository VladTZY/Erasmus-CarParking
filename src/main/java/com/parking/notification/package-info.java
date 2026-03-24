/**
 * Notification module.
 *
 * <p>Sends email and push notifications for all key domain events.
 * Listens to: {@code UserRegisteredEvent}, {@code ReservationCreatedEvent},
 * {@code ReservationCancelledEvent}, {@code InvoiceGeneratedEvent},
 * {@code SpaceStateChangedEvent}.
 * Published events: {@code NotificationSentEvent} (audit log).
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notification"
)
package com.parking.notification;
