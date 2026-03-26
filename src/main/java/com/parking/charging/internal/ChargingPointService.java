package com.parking.charging.internal;

import com.parking.charging.ChargingCompletedEvent;
import com.parking.charging.ChargingSessionDTO;
import com.parking.charging.ChargingStartedEvent;
import com.parking.charging.ChargingStatus;
import com.parking.reservation.IReservationRepo;
import com.parking.zonemgmt.ISpaceQuery;
import com.parking.zonemgmt.IZoneQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
class ChargingPointService {

    private static final Logger log = LoggerFactory.getLogger(ChargingPointService.class);
    private static final int DEFAULT_REQUESTED_KWH = 22;

    private final ChargingSessionRepo sessionRepo;
    private final ChargingStrategyService strategyService;
    private final ApplicationEventPublisher eventPublisher;
    private final ISpaceQuery spaceQuery;
    private final IZoneQuery zoneQuery;
    private final IReservationRepo reservationRepo;

    ChargingPointService(ChargingSessionRepo sessionRepo,
                         ChargingStrategyService strategyService,
                         ApplicationEventPublisher eventPublisher,
                         @Lazy ISpaceQuery spaceQuery,
                         @Lazy IZoneQuery zoneQuery,
                         @Lazy IReservationRepo reservationRepo) {
        this.sessionRepo = sessionRepo;
        this.strategyService = strategyService;
        this.eventPublisher = eventPublisher;
        this.spaceQuery = spaceQuery;
        this.zoneQuery = zoneQuery;
        this.reservationRepo = reservationRepo;
    }

    @Transactional
    ChargingSessionDTO startSession(UUID reservationId) {
        var session = sessionRepo.findByReservationIdAndStatus(reservationId, ChargingStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No pending charging session for reservation: " + reservationId));

        session.setStatus(ChargingStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        session = sessionRepo.save(session);

        strategyService.calculateOptimalPlan(session.getSpaceId(), DEFAULT_REQUESTED_KWH);

        eventPublisher.publishEvent(new ChargingStartedEvent(
                session.getId(), session.getReservationId(), session.getSpaceId(), session.getCitizenId()
        ));

        log.info("Charging session {} started for reservation {}", session.getId(), reservationId);
        return toDTO(session);
    }

    @Transactional
    ChargingSessionDTO stopSession(UUID sessionId) {
        var session = sessionRepo.findEntityById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Charging session not found: " + sessionId));

        if (session.getStatus() != ChargingStatus.ACTIVE) {
            throw new IllegalStateException("Session is not active: " + sessionId);
        }

        double energyKwh = liveEnergyKwh(session);
        session.setStatus(ChargingStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setEnergyKwh(energyKwh);
        session = sessionRepo.save(session);

        eventPublisher.publishEvent(new ChargingCompletedEvent(
                session.getId(), session.getReservationId(), session.getCitizenId(), energyKwh
        ));

        log.info("Charging session {} completed — {} kWh delivered", session.getId(), energyKwh);
        return toDTO(session);
    }

    private ChargingSessionDTO toDTO(ChargingSession s) {
        var space = spaceQuery.findSpace(s.getSpaceId());
        String spaceName = space.map(sp -> sp.getName()).orElse(null);
        String zoneName = space.flatMap(sp -> zoneQuery.findZoneById(sp.getZoneId()))
                .map(z -> z.name()).orElse(null);
        String licensePlate = reservationRepo.findById(s.getReservationId())
                .map(r -> r.licensePlate()).orElse(null);
        return new ChargingSessionDTO(s.getId(), s.getReservationId(), s.getSpaceId(),
                spaceName, zoneName, s.getStatus(), s.getStartedAt(), liveEnergyKwh(s), licensePlate);
    }

    /** Returns live kWh for ACTIVE sessions (7.4 kW rate), or the stored value otherwise. */
    static Double liveEnergyKwh(ChargingSession s) {
        if (s.getStatus() == ChargingStatus.ACTIVE && s.getStartedAt() != null) {
            double hours = Duration.between(s.getStartedAt(), LocalDateTime.now()).toSeconds() / 3600.0;
            return Math.round(hours * 7.4 * 100.0) / 100.0;
        }
        return s.getEnergyKwh();
    }
}
