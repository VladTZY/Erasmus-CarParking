package com.parking.charging.internal;

import com.parking.charging.ChargingSessionDTO;
import com.parking.charging.ChargingStartedEvent;
import com.parking.charging.ChargingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
class ChargingPointService {

    private static final Logger log = LoggerFactory.getLogger(ChargingPointService.class);
    private static final Random RANDOM = new Random();
    private static final int DEFAULT_REQUESTED_KWH = 22;

    private final ChargingSessionRepo sessionRepo;
    private final ChargingStrategyService strategyService;
    private final ApplicationEventPublisher eventPublisher;

    ChargingPointService(ChargingSessionRepo sessionRepo,
                         ChargingStrategyService strategyService,
                         ApplicationEventPublisher eventPublisher) {
        this.sessionRepo = sessionRepo;
        this.strategyService = strategyService;
        this.eventPublisher = eventPublisher;
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

        double energyKwh = 5 + RANDOM.nextInt(36); // 5–40 kWh
        session.setStatus(ChargingStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setEnergyKwh(energyKwh);
        session = sessionRepo.save(session);

        log.info("Charging session {} completed — {} kWh delivered", session.getId(), energyKwh);
        return toDTO(session);
    }

    private ChargingSessionDTO toDTO(ChargingSession s) {
        return new ChargingSessionDTO(s.getId(), s.getReservationId(), s.getSpaceId(),
                s.getStatus(), s.getStartedAt(), s.getEnergyKwh());
    }
}
