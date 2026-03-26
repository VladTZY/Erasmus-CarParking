package com.parking.notification;

import com.parking.billing.InvoiceGeneratedEvent;
import com.parking.reservation.ReservationCancelledEvent;
import com.parking.reservation.ReservationCreatedEvent;
import com.parking.usermgmt.User;
import com.parking.usermgmt.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module-scoped test for the Notification module.
 * Verifies that each domain event results in a Notification record and a NotificationSentEvent.
 */
@ApplicationModuleTest
class NotificationModuleTest {

    @Autowired INotificationRepo notificationRepo;
    @Autowired ApplicationEventPublisher eventPublisher;

    @Test
    void onUserRegistered_savesWelcomeNotification(Scenario scenario) {
        var userId = UUID.randomUUID();
        var email = "welcome@test.com";

        scenario.stimulate(() -> eventPublisher.publishEvent(new UserRegisteredEvent(userId, email, User.Role.CITIZEN)))
                .andWaitForEventOfType(NotificationSentEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.recipientId()).isEqualTo(userId);
                    assertThat(event.eventType()).isEqualTo("UserRegisteredEvent");

                    var notifications = notificationRepo.findByRecipientId(userId);
                    assertThat(notifications).hasSize(1);
                    assertThat(notifications.get(0).subject()).isNotBlank();
                    assertThat(notifications.get(0).relatedEventType()).isEqualTo("UserRegisteredEvent");
                });
    }

    @Test
    void onReservationCreated_savesConfirmationNotification(Scenario scenario) {
        var citizenId = UUID.randomUUID();
        var reservationId = UUID.randomUUID();

        scenario.stimulate(() -> eventPublisher.publishEvent(new ReservationCreatedEvent(
                        reservationId, UUID.randomUUID(), citizenId, new BigDecimal("4.00"), 120, false)))
                .andWaitForEventOfType(NotificationSentEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.recipientId()).isEqualTo(citizenId);
                    assertThat(event.eventType()).isEqualTo("ReservationCreatedEvent");

                    var notifications = notificationRepo.findByRecipientId(citizenId);
                    assertThat(notifications).hasSize(1);
                    assertThat(notifications.get(0).relatedEventType()).isEqualTo("ReservationCreatedEvent");
                });
    }

    @Test
    void onReservationCancelled_savesCancellationNotification(Scenario scenario) {
        var citizenId = UUID.randomUUID();
        var reservationId = UUID.randomUUID();

        scenario.stimulate(() -> eventPublisher.publishEvent(
                        new ReservationCancelledEvent(reservationId, UUID.randomUUID(), citizenId)))
                .andWaitForEventOfType(NotificationSentEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.recipientId()).isEqualTo(citizenId);
                    assertThat(event.eventType()).isEqualTo("ReservationCancelledEvent");

                    var notifications = notificationRepo.findByRecipientId(citizenId);
                    assertThat(notifications).hasSize(1);
                    assertThat(notifications.get(0).relatedEventType()).isEqualTo("ReservationCancelledEvent");
                });
    }

    @Test
    void onInvoiceGenerated_savesInvoiceNotification(Scenario scenario) {
        var citizenId = UUID.randomUUID();
        var invoiceId = UUID.randomUUID();

        scenario.stimulate(() -> eventPublisher.publishEvent(new InvoiceGeneratedEvent(
                        invoiceId, UUID.randomUUID(), citizenId, new BigDecimal("5.00"))))
                .andWaitForEventOfType(NotificationSentEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.recipientId()).isEqualTo(citizenId);
                    assertThat(event.eventType()).isEqualTo("InvoiceGeneratedEvent");

                    var notifications = notificationRepo.findByRecipientId(citizenId);
                    assertThat(notifications).hasSize(1);
                    assertThat(notifications.get(0).relatedEventType()).isEqualTo("InvoiceGeneratedEvent");
                });
    }
}
