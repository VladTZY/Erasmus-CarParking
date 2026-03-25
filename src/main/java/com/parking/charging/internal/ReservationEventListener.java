package com.parking.charging.internal;

import com.parking.charging.ChargingStatus;
import com.parking.reservation.ReservationCancelledEvent;
import com.parking.reservation.ReservationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class ReservationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventListener.class);

    private final ChargingSessionRepo sessionRepo;

    ReservationEventListener(ChargingSessionRepo sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    @ApplicationModuleListener
    public void on(ReservationCreatedEvent event) {
        if (!event.withCharging()) {
            return;
        }
        var session = new ChargingSession(
                UUID.randomUUID(), event.reservationId(), event.spaceId(),
                event.citizenId(), ChargingStatus.PENDING
        );
        sessionRepo.save(session);
        log.info("Charging session {} created (PENDING) for reservation {}", session.getId(), event.reservationId());
    }

    @ApplicationModuleListener
    public void on(ReservationCancelledEvent event) {
        sessionRepo.findByReservationIdAndStatus(event.reservationId(), ChargingStatus.PENDING)
                .ifPresent(session -> {
                    sessionRepo.delete(session);
                    log.info("Charging session {} deleted — reservation {} cancelled", session.getId(), event.reservationId());
                });
    }
}
