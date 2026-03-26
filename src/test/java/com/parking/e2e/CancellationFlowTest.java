package com.parking.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.notification.INotificationRepo;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test: reservation cancellation flow.
 *
 * Flow:
 *   create confirmed reservation (standard flow)
 *   → DELETE /reservations/{id}
 *   → assert space state = FREE
 *   → assert notification saved for ReservationCancelledEvent
 */
@SpringBootTest
@AutoConfigureMockMvc
class CancellationFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired INotificationRepo notificationRepo;

    private TestHelper helper;

    @BeforeEach
    void setUp() {
        helper = new TestHelper(mockMvc, mapper);
    }

    @Test
    void cancelReservation_spaceBecomesFreeAndNotificationSent() throws Exception {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var adminToken = helper.registerAndGetToken("admin-cancel-" + uid + "@test.com", "pass", "ADMIN");
        var citizenToken = helper.registerAndGetToken("citizen-cancel-" + uid + "@test.com", "pass", "CITIZEN");

        // Set up zone + space + pricing
        var zoneId = helper.createZone(adminToken, "Cancel Zone " + uid, "Cancel St");
        var spaceId = helper.createSpace(adminToken, zoneId, "CZ-01", "REGULAR");
        helper.createPricingRule(adminToken, zoneId, "REGULAR", "1.00");

        // Create reservation
        var start = LocalDateTime.now().plusDays(7).withNano(0);
        var end = start.plusHours(1);
        var reservationId = helper.createReservation(citizenToken, spaceId, start, end,
                false, null);

        // Space remains FREE — state reflects physical occupancy, not time-slot bookings
        mockMvc.perform(get("/zones/" + zoneId + "/spaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("FREE"));

        // Cancel the reservation
        helper.cancelReservation(citizenToken, reservationId);

        // Notification for cancellation should exist
        var citizenId = extractCitizenId(citizenToken);
        var notifications = notificationRepo.findByRecipientId(citizenId);
        var eventTypes = notifications.stream().map(n -> n.relatedEventType()).toList();
        assertThat(eventTypes).contains("ReservationCancelledEvent");
    }

    @Test
    void cancelReservation_withEVCharging_deletesChargingSession() throws Exception {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var adminToken = helper.registerAndGetToken("admin-ev-cancel-" + uid + "@test.com", "pass", "ADMIN");
        var citizenToken = helper.registerAndGetToken("citizen-ev-cancel-" + uid + "@test.com", "pass", "CITIZEN");

        var zoneId = helper.createZone(adminToken, "EV Cancel Zone " + uid, "EV Cancel Ave");
        var spaceId = helper.createSpace(adminToken, zoneId, "EVC-01", "EV");
        helper.createPricingRule(adminToken, zoneId, "EV", "3.00");

        var start = LocalDateTime.now().plusDays(8).withNano(0);
        var end = start.plusHours(2);

        // Create EV reservation (withCharging=true → creates PENDING charging session)
        var reservationId = helper.createReservation(citizenToken, spaceId, start, end,
                true, "XY9900");

        // Cancel → should delete the PENDING charging session (async @ApplicationModuleListener)
        helper.cancelReservation(citizenToken, reservationId);

        // Wait for async event processing to delete the session
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            var response = mockMvc.perform(get("/charging/sessions/my")
                            .header("Authorization", "Bearer " + citizenToken))
                    .andReturn();
            return mapper.readTree(response.getResponse().getContentAsString()).isEmpty();
        });
    }

    @Test
    void cancelAlreadyCancelledReservation_returns409() throws Exception {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var adminToken = helper.registerAndGetToken("admin-dup-cancel-" + uid + "@test.com", "pass", "ADMIN");
        var citizenToken = helper.registerAndGetToken("citizen-dup-cancel-" + uid + "@test.com", "pass", "CITIZEN");

        var zoneId = helper.createZone(adminToken, "Dup Zone " + uid, "Dup Rd");
        var spaceId = helper.createSpace(adminToken, zoneId, "DZ-01", "REGULAR");
        helper.createPricingRule(adminToken, zoneId, "REGULAR", "1.00");

        var start = LocalDateTime.now().plusDays(9).withNano(0);
        var end = start.plusHours(1);
        var reservationId = helper.createReservation(citizenToken, spaceId, start, end, false, null);

        // First cancel succeeds
        helper.cancelReservation(citizenToken, reservationId);

        // Second cancel fails with 409
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/reservations/" + reservationId);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isConflict());
    }

    private UUID extractCitizenId(String token) throws Exception {
        var result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
