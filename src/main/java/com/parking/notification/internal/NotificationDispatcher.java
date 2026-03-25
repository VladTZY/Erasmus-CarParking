package com.parking.notification.internal;

import com.parking.billing.InvoiceGeneratedEvent;
import com.parking.reservation.ReservationCancelledEvent;
import com.parking.reservation.ReservationCreatedEvent;
import com.parking.usermgmt.UserRegisteredEvent;
import com.parking.zonemgmt.ParkingSpace;
import com.parking.zonemgmt.SpaceStateChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationService notificationService;

    NotificationDispatcher(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ApplicationModuleListener
    public void on(UserRegisteredEvent event) {
        var template = new NotificationTemplates.UserRegistered(event);
        notificationService.send(event.userId(), template.subject(), template.body(),
                "UserRegisteredEvent");
    }

    @ApplicationModuleListener
    public void on(ReservationCreatedEvent event) {
        var template = new NotificationTemplates.ReservationCreated(event);
        notificationService.send(event.citizenId(), template.subject(), template.body(),
                "ReservationCreatedEvent");
    }

    @ApplicationModuleListener
    public void on(ReservationCancelledEvent event) {
        var template = new NotificationTemplates.ReservationCancelled(event);
        notificationService.send(event.citizenId(), template.subject(), template.body(),
                "ReservationCancelledEvent");
    }

    @ApplicationModuleListener
    public void on(InvoiceGeneratedEvent event) {
        var template = new NotificationTemplates.InvoiceGenerated(event);
        notificationService.send(event.citizenId(), template.subject(), template.body(),
                "InvoiceGeneratedEvent");
    }

    @ApplicationModuleListener
    public void on(SpaceStateChangedEvent event) {
        if (event.newState() == ParkingSpace.SpaceState.OCCUPIED) {
            log.warn("ANOMALY — space {} is OCCUPIED without an active reservation", event.spaceId());
        }
    }
}
