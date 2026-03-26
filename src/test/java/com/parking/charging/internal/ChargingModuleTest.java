package com.parking.charging.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.charging.ChargingStartedEvent;
import com.parking.charging.ChargingStatus;
import com.parking.charging.IChargingSessionRepo;
import com.parking.reservation.IReservationRepo;
import com.parking.reservation.ReservationCreatedEvent;
import com.parking.zonemgmt.ISpaceQuery;
import com.parking.zonemgmt.IZoneQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Module-scoped test for the Charging module.
 * Placed in charging.internal to access ChargingSessionJpaRepo (package-private).
 * Covers PENDING session creation from ReservationCreatedEvent and session start/stop.
 */
@ApplicationModuleTest
@AutoConfigureMockMvc
class ChargingModuleTest {

    @MockBean ISpaceQuery spaceQuery;
    @MockBean IZoneQuery zoneQuery;
    @MockBean IReservationRepo reservationRepo;

    @Autowired ChargingSessionJpaRepo jpaRepo;
    @Autowired IChargingSessionRepo publicSessionRepo;
    @Autowired ChargingPointService chargingPointService;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired ApplicationEventPublisher eventPublisher;

    @Test
    void onReservationCreatedWithCharging_createsPendingSession(Scenario scenario) {
        var reservationId = UUID.randomUUID();
        var spaceId = UUID.randomUUID();
        var citizenId = UUID.randomUUID();

        scenario.stimulate(() ->
                eventPublisher.publishEvent(new ReservationCreatedEvent(reservationId, spaceId, citizenId, BigDecimal.ONE, 60, true)))
        .andWaitForStateChange(
                () -> jpaRepo.findByReservationIdAndStatus(reservationId, ChargingStatus.PENDING))
        .andVerify(session -> {
            assertThat(session).isPresent();
            assertThat(session.get().getSpaceId()).isEqualTo(spaceId);
            assertThat(session.get().getCitizenId()).isEqualTo(citizenId);
            assertThat(session.get().getStatus()).isEqualTo(ChargingStatus.PENDING);
        });
    }

    @Test
    void onReservationCreatedWithoutCharging_doesNotCreateSession(Scenario scenario) {
        var reservationId = UUID.randomUUID();

        scenario.stimulate(() ->
                eventPublisher.publishEvent(new ReservationCreatedEvent(reservationId, UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.ONE, 60, false)))
        .andWaitForStateChange(
                () -> jpaRepo.findByReservationIdAndStatus(reservationId, ChargingStatus.PENDING),
                Optional::isEmpty)
        .andVerify(session -> assertThat(session).isEmpty());
    }

    @Test
    void startSession_transitionsToActiveAndPublishesEvent(Scenario scenario) {
        var reservationId = UUID.randomUUID();
        var spaceId = UUID.randomUUID();
        var citizenId = UUID.randomUUID();

        // stimulate(Runnable) — create PENDING session + start it in Scenario's committed transaction
        scenario.stimulate(() -> {
            var session = new ChargingSession(UUID.randomUUID(), reservationId, spaceId, citizenId,
                    ChargingStatus.PENDING);
            jpaRepo.save(session);
            chargingPointService.startSession(reservationId);
        })
        .andWaitForEventOfType(ChargingStartedEvent.class)
        .toArriveAndVerify(event -> {
            assertThat(event.reservationId()).isEqualTo(reservationId);
            assertThat(event.spaceId()).isEqualTo(spaceId);
            assertThat(event.citizenId()).isEqualTo(citizenId);
        });
    }

    @Test
    @Transactional
    void stopSession_transitionsToCompleted() {
        var reservationId = UUID.randomUUID();
        var spaceId = UUID.randomUUID();
        var citizenId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        // Create and start the session
        var session = new ChargingSession(sessionId, reservationId, spaceId, citizenId, ChargingStatus.PENDING);
        jpaRepo.save(session);
        chargingPointService.startSession(reservationId);

        // Stop it
        var result = chargingPointService.stopSession(sessionId);
        assertThat(result.status()).isEqualTo(ChargingStatus.COMPLETED);
        assertThat(result.energyKwh()).isNotNull();
        assertThat(result.energyKwh()).isGreaterThanOrEqualTo(0.0);

        var stopped = jpaRepo.findById(sessionId);
        assertThat(stopped).isPresent();
        assertThat(stopped.get().getStatus()).isEqualTo(ChargingStatus.COMPLETED);
    }

    @Test
    @Transactional
    void stopSession_whenNotActive_throws() {
        var sessionId = UUID.randomUUID();
        var reservationId = UUID.randomUUID();

        var session = new ChargingSession(sessionId, reservationId, UUID.randomUUID(),
                UUID.randomUUID(), ChargingStatus.PENDING);
        jpaRepo.save(session);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> chargingPointService.stopSession(sessionId));
    }

    @Test
    @WithMockUser
    void startSessionEndpoint_withNoPendingSession_returns404() throws Exception {
        var body = mapper.writeValueAsString(Map.of("reservationId", UUID.randomUUID().toString()));

        // No PENDING session in DB → service throws IllegalArgumentException → 404
        mockMvc.perform(post("/charging/sessions/start")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
