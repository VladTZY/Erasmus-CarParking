package com.parking.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.billing.IInvoiceRepository;
import com.parking.billing.InvoiceStatus;
import com.parking.billing.InvoiceType;
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
 * End-to-end test: complete reservation flow with billing and notifications.
 *
 * Flow:
 *   register admin + citizen
 *   → create zone + EV space + pricing rule (as admin)
 *   → search available spaces (optional)
 *   → create reservation (as citizen)
 *   → assert space stays FREE (state = physical occupancy only)
 *   → assert RESERVATION invoice is PAID
 *   → assert notification exists for ReservationCreatedEvent and InvoiceGeneratedEvent
 */
@SpringBootTest
@AutoConfigureMockMvc
class FullReservationFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired IInvoiceRepository invoiceRepository;
    @Autowired INotificationRepo notificationRepo;

    private TestHelper helper;

    @BeforeEach
    void setUp() {
        helper = new TestHelper(mockMvc, mapper);
    }

    @Test
    void fullReservationFlow_invoicePaidAndNotificationsCreated() throws Exception {
        // ── 1. Register admin and citizen ────────────────────────────────────
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var adminToken = helper.registerAndGetToken("admin-" + uid + "@test.com", "pass", "ADMIN");
        var citizenToken = helper.registerAndGetToken("citizen-" + uid + "@test.com", "pass", "CITIZEN");

        // ── 2. Create infrastructure (as admin) ──────────────────────────────
        var zoneId = helper.createZone(adminToken, "City Centre " + uid, "1 Market Square");
        var spaceId = helper.createSpace(adminToken, zoneId, "CC-01", "EV");
        helper.createPricingRule(adminToken, zoneId, "ALL", "2.00"); // €2.00/h

        // ── 3. Search available spaces ───────────────────────────────────────
        var start = LocalDateTime.now().plusDays(5).withNano(0);
        var end = start.plusHours(2);

        mockMvc.perform(get("/reservations/search")
                        .header("Authorization", "Bearer " + citizenToken)
                        .param("zoneId", zoneId.toString())
                        .param("startTime", start.toString())
                        .param("endTime", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spaceId").value(spaceId.toString()))
                .andExpect(jsonPath("$[0].estimatedFee").value(4.00)); // 2h × €2.00

        // ── 4. Create reservation ────────────────────────────────────────────
        var reservationId = helper.createReservation(citizenToken, spaceId, start, end,
                false, null);
        assertThat(reservationId).isNotNull();

        // ── 5. Space remains FREE (state reflects physical occupancy, not time-slot bookings) ─
        mockMvc.perform(get("/zones/" + zoneId + "/spaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("FREE"));

        var citizenId = extractCitizenId(citizenToken);

        // ── 6. Reservation invoice is PAID ───────────────────────────────────
        // BillingService.on(ReservationCreatedEvent) fires async after commit — wait for it
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() ->
                invoiceRepository.findByCitizenId(citizenId).stream()
                        .anyMatch(i -> i.invoiceType() == InvoiceType.RESERVATION
                                && i.status() == InvoiceStatus.PAID));

        var invoices = invoiceRepository.findByCitizenId(citizenId);
        var reservationInvoice = invoices.stream()
                .filter(i -> i.invoiceType() == InvoiceType.RESERVATION)
                .findFirst();

        assertThat(reservationInvoice).isPresent();
        assertThat(reservationInvoice.get().status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(reservationInvoice.get().reservationId()).isEqualTo(reservationId);

        // ── 7. Notifications exist ───────────────────────────────────────────
        // Notifications are also async — wait for both ReservationCreatedEvent and InvoiceGeneratedEvent
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            var types = notificationRepo.findByRecipientId(citizenId).stream()
                    .map(n -> n.relatedEventType()).toList();
            return types.contains("ReservationCreatedEvent") && types.contains("InvoiceGeneratedEvent");
        });

        var notifications = notificationRepo.findByRecipientId(citizenId);
        var eventTypes = notifications.stream()
                .map(n -> n.relatedEventType())
                .toList();
        assertThat(eventTypes).contains("ReservationCreatedEvent", "InvoiceGeneratedEvent");
    }

    @Test
    void searchAvailableSpaces_afterReservation_excludesReservedSlot() throws Exception {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var adminToken = helper.registerAndGetToken("admin2-" + uid + "@test.com", "pass", "ADMIN");
        var citizenToken = helper.registerAndGetToken("citizen2-" + uid + "@test.com", "pass", "CITIZEN");

        var zoneId = helper.createZone(adminToken, "Downtown " + uid, "2 Central Ave");
        var spaceId = helper.createSpace(adminToken, zoneId, "DT-01", "REGULAR");
        helper.createPricingRule(adminToken, zoneId, "REGULAR", "1.00");

        var start = LocalDateTime.now().plusDays(10).withNano(0);
        var end = start.plusHours(1);

        // Reserve the space
        helper.createReservation(citizenToken, spaceId, start, end, false, null);

        // Searching the same window should return 0 available spaces (overlap)
        mockMvc.perform(get("/reservations/search")
                        .header("Authorization", "Bearer " + citizenToken)
                        .param("zoneId", zoneId.toString())
                        .param("startTime", start.toString())
                        .param("endTime", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UUID extractCitizenId(String token) throws Exception {
        // Decode from /users/me endpoint
        var result = mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
