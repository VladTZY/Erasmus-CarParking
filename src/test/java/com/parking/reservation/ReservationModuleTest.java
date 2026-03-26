package com.parking.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.pricing.IPricingPolicy;
import com.parking.pricing.PricingEstimateDTO;
import com.parking.zonemgmt.ISpaceQuery;
import com.parking.zonemgmt.ISpaceStateManager;
import com.parking.zonemgmt.IZoneAvailability;
import com.parking.zonemgmt.IZoneQuery;
import com.parking.zonemgmt.ParkingSpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Module-scoped test for the Reservation module.
 * Mocks all cross-module dependencies (zonemgmt, pricing).
 */
@ApplicationModuleTest
@AutoConfigureMockMvc
class ReservationModuleTest {

    @MockBean IZoneAvailability zoneAvailability;
    @MockBean ISpaceStateManager spaceStateManager;
    @MockBean ISpaceQuery spaceQuery;
    @MockBean IZoneQuery zoneQuery;
    @MockBean IPricingPolicy pricingPolicy;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    private UUID spaceId;
    private UUID zoneId;

    @BeforeEach
    void setUpMocks() {
        spaceId = UUID.randomUUID();
        zoneId = UUID.randomUUID();

        var space = new ParkingSpace(spaceId, zoneId, "A-01",
                ParkingSpace.SpaceType.REGULAR, ParkingSpace.SpaceState.FREE);
        when(spaceQuery.findSpace(spaceId)).thenReturn(Optional.of(space));
        when(zoneAvailability.isSpaceAvailable(spaceId)).thenReturn(true);
        when(zoneAvailability.findAllSpaceIds(zoneId)).thenReturn(List.of(spaceId));

        var estimate = new PricingEstimateDTO(new BigDecimal("2.00"), new BigDecimal("2.00"), null);
        when(pricingPolicy.estimate(any(UUID.class), anyInt())).thenReturn(estimate);
        when(pricingPolicy.calculate(any(UUID.class), anyInt())).thenReturn(new BigDecimal("2.00"));
    }

    /** Helper: wrap MockMvc POST as a Runnable for Scenario.stimulate(). */
    private Runnable doCreateReservation(UUID space, LocalDateTime start, LocalDateTime end,
                                         boolean withCharging) {
        return () -> {
            try {
                var body = mapper.writeValueAsString(Map.of(
                        "spaceId", space.toString(),
                        "startTime", start.toString(),
                        "endTime", end.toString(),
                        "withCharging", withCharging
                ));
                mockMvc.perform(post("/reservations")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("CONFIRMED"))
                        .andExpect(jsonPath("$.estimatedFee").value(2.00));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Test
    @WithMockUser(username = "3f2c4e5d-6a7b-8c9d-0e1f-2a3b4c5d6e7f", roles = "CITIZEN")
    void createReservation_returns201AndPublishesEvent(Scenario scenario) {
        var start = LocalDateTime.now().plusDays(1).withNano(0);
        var end = start.plusHours(2);

        scenario.stimulate(doCreateReservation(spaceId, start, end, false))
                .andWaitForEventOfType(ReservationCreatedEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.spaceId()).isEqualTo(spaceId);
                    assertThat(event.durationMinutes()).isEqualTo(120);
                    assertThat(event.withCharging()).isFalse();
                    assertThat(event.estimatedFee()).isEqualByComparingTo(new BigDecimal("2.00"));
                });
    }

    @Test
    @WithMockUser(username = "3f2c4e5d-6a7b-8c9d-0e1f-2a3b4c5d6e7f", roles = "CITIZEN")
    void createReservation_withCharging_publishesEventWithFlag(Scenario scenario) {
        var start = LocalDateTime.now().plusDays(2).withNano(0);
        var end = start.plusHours(1);

        scenario.stimulate(doCreateReservation(spaceId, start, end, true))
                .andWaitForEventOfType(ReservationCreatedEvent.class)
                .toArriveAndVerify(event -> assertThat(event.withCharging()).isTrue());
    }

    @Test
    @WithMockUser(username = "3f2c4e5d-6a7b-8c9d-0e1f-2a3b4c5d6e7f", roles = "CITIZEN")
    void createReservation_overlapping_returns409(Scenario scenario) {
        var start = LocalDateTime.now().plusDays(3).withNano(0);
        var end = start.plusHours(2);

        // First reservation succeeds
        scenario.stimulate(doCreateReservation(spaceId, start, end, false))
                .andWaitForEventOfType(ReservationCreatedEvent.class)
                .toArrive();

        // Second reservation for the same slot conflicts
        try {
            mockMvc.perform(post("/reservations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of(
                                    "spaceId", spaceId.toString(),
                                    "startTime", start.toString(),
                                    "endTime", end.toString(),
                                    "withCharging", false
                            ))))
                    .andExpect(status().isConflict());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @WithMockUser(username = "3f2c4e5d-6a7b-8c9d-0e1f-2a3b4c5d6e7f", roles = "CITIZEN")
    void createReservation_pastStartTime_returns400() throws Exception {
        mockMvc.perform(post("/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "spaceId", spaceId.toString(),
                                "startTime", "2020-01-01T10:00:00",
                                "endTime", "2020-01-01T12:00:00",
                                "withCharging", false
                        ))))
                .andExpect(status().isBadRequest());
    }
}
