package com.parking.notification.internal;

import com.parking.billing.InvoiceGeneratedEvent;
import com.parking.reservation.ReservationCancelledEvent;
import com.parking.reservation.ReservationCreatedEvent;
import com.parking.usermgmt.UserRegisteredEvent;

/**
 * Value objects — one template per event type.
 * Each carries the subject/body rendering logic for that event.
 */
class NotificationTemplates {

    record UserRegistered(UserRegisteredEvent event) {
        String subject() {
            return "Welcome to SmartParking!";
        }

        String body() {
            return "Hello! Your account has been created with role %s. Start reserving parking spaces now."
                    .formatted(event.role());
        }
    }

    record ReservationCreated(ReservationCreatedEvent event) {
        String subject() {
            return "Reservation confirmed";
        }

        String body() {
            return "Your reservation has been confirmed for space %s. Duration: %d min. Estimated fee: €%s.%s"
                    .formatted(event.spaceId(), event.durationMinutes(), event.estimatedFee(),
                            event.withCharging() ? " EV charging included." : "");
        }
    }

    record ReservationCancelled(ReservationCancelledEvent event) {
        String subject() {
            return "Reservation cancelled";
        }

        String body() {
            return "Your reservation %s for space %s has been cancelled."
                    .formatted(event.reservationId(), event.spaceId());
        }
    }

    record InvoiceGenerated(InvoiceGeneratedEvent event) {
        String subject() {
            return "Invoice generated — €%s".formatted(event.amount());
        }

        String body() {
            return "An invoice of €%s has been generated for reservation %s (invoice id: %s)."
                    .formatted(event.amount(), event.reservationId(), event.invoiceId());
        }
    }
}
