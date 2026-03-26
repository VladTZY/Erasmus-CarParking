package com.parking.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.billing.IInvoiceRepository;
import com.parking.billing.InvoiceStatus;
import com.parking.billing.InvoiceType;
import com.parking.charging.ChargingStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test: complete EV charging session lifecycle.
 *
 * Flow:
 *   create EV reservation (withCharging=true)
 *   → assert PENDING charging session exists
 *   → POST /charging/sessions/start
 *   → assert session ACTIVE
 *   → POST /charging/sessions/{id}/stop
 *   → assert session COMPLETED with energyKwh
 *   → assert charging invoice is PAID with correct amount (kWh × €0.30)
 */
@SpringBootTest
@AutoConfigureMockMvc
class EVChargingFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired IInvoiceRepository invoiceRepository;

    private TestHelper helper;

    @BeforeEach
    void setUp() {
        helper = new TestHelper(mockMvc, mapper);
    }

    @Test
    void evChargingFlow_sessionCompletedAndInvoicePaid() throws Exception {
        // ── 1. Set up: admin infra + citizen ────────────────────────────────
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var adminToken = helper.registerAndGetToken("admin-ev-" + uid + "@test.com", "pass", "ADMIN");
        var citizenToken = helper.registerAndGetToken("citizen-ev-" + uid + "@test.com", "pass", "CITIZEN");

        var zoneId = helper.createZone(adminToken, "EV Zone " + uid, "EV Street 1");
        var spaceId = helper.createSpace(adminToken, zoneId, "EV-01", "EV");
        helper.createPricingRule(adminToken, zoneId, "EV", "3.00");

        // ── 2. Create EV reservation ─────────────────────────────────────────
        var start = LocalDateTime.now().plusDays(14).withNano(0);
        var end = start.plusHours(2);
        var reservationId = helper.createReservation(citizenToken, spaceId, start, end,
                true, "AB1234");
        assertThat(reservationId).isNotNull();

        // ── 3. PENDING charging session exists after reservation ─────────────
        mockMvc.perform(get("/charging/sessions/my")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].licensePlate").value("AB1234"));

        // ── 4. Start charging session ────────────────────────────────────────
        var sessionId = helper.startCharging(citizenToken, reservationId);
        assertThat(sessionId).isNotNull();

        // Session is now ACTIVE
        mockMvc.perform(get("/charging/sessions/my")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].startedAt").isString());

        // Charging invoice created as PENDING (amount = 0) — created async after startCharging
        var citizenId = extractCitizenId(citizenToken);
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() ->
                invoiceRepository.findByCitizenId(citizenId).stream()
                        .anyMatch(i -> i.invoiceType() == InvoiceType.CHARGING
                                && i.status() == InvoiceStatus.PENDING));
        var invoicesAfterStart = invoiceRepository.findByCitizenId(citizenId);
        var chargingInvoiceAfterStart = invoicesAfterStart.stream()
                .filter(i -> i.invoiceType() == InvoiceType.CHARGING)
                .findFirst();
        assertThat(chargingInvoiceAfterStart).isPresent();
        assertThat(chargingInvoiceAfterStart.get().status()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(chargingInvoiceAfterStart.get().amount()).isEqualByComparingTo(BigDecimal.ZERO);

        // ── 5. Stop charging session ─────────────────────────────────────────
        var stopResponse = helper.stopCharging(citizenToken, sessionId);
        assertThat(stopResponse.get("status").asText()).isEqualTo("COMPLETED");
        var energyKwh = stopResponse.get("energyKwh").asDouble();
        assertThat(energyKwh).isGreaterThanOrEqualTo(0.0);

        // Session is COMPLETED
        mockMvc.perform(get("/charging/sessions/my")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].energyKwh").isNumber());

        // ── 6. Charging invoice is PAID ──────────────────────────────────────
        // BillingService.on(ChargingCompletedEvent) fires async after stop commit — wait for it
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() ->
                invoiceRepository.findByCitizenId(citizenId).stream()
                        .anyMatch(i -> i.invoiceType() == InvoiceType.CHARGING
                                && i.status() == InvoiceStatus.PAID));
        var invoicesAfterStop = invoiceRepository.findByCitizenId(citizenId);
        var chargingInvoice = invoicesAfterStop.stream()
                .filter(i -> i.invoiceType() == InvoiceType.CHARGING)
                .findFirst();

        assertThat(chargingInvoice).isPresent();
        assertThat(chargingInvoice.get().status()).isEqualTo(InvoiceStatus.PAID);

        // Amount = energyKwh × €0.30 (rounded to 2dp)
        var expectedAmount = BigDecimal.valueOf(energyKwh)
                .multiply(new BigDecimal("0.30"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(chargingInvoice.get().amount()).isEqualByComparingTo(expectedAmount);
    }

    @Test
    void startCharging_noPendingSession_returns404() throws Exception {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var adminToken = helper.registerAndGetToken("admin-noev-" + uid + "@test.com", "pass", "ADMIN");
        var citizenToken = helper.registerAndGetToken("citizen-noev-" + uid + "@test.com", "pass", "CITIZEN");

        var zoneId = helper.createZone(adminToken, "No EV Zone " + uid, "Plain St");
        var spaceId = helper.createSpace(adminToken, zoneId, "P-01", "REGULAR");
        helper.createPricingRule(adminToken, zoneId, "REGULAR", "1.00");

        // Create reservation WITHOUT charging → no PENDING charging session
        var start = LocalDateTime.now().plusDays(15).withNano(0);
        var reservationId = helper.createReservation(citizenToken, spaceId, start,
                start.plusHours(1), false, null);

        // Attempting to start charging fails: no PENDING session found
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/charging/sessions/start")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                java.util.Map.of("reservationId", reservationId.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void stopCharging_alreadyCompletedSession_returns409() throws Exception {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var adminToken = helper.registerAndGetToken("admin-stop2-" + uid + "@test.com", "pass", "ADMIN");
        var citizenToken = helper.registerAndGetToken("citizen-stop2-" + uid + "@test.com", "pass", "CITIZEN");

        var zoneId = helper.createZone(adminToken, "Stop Zone " + uid, "Stop Rd");
        var spaceId = helper.createSpace(adminToken, zoneId, "S-01", "EV");
        helper.createPricingRule(adminToken, zoneId, "EV", "2.00");

        var start = LocalDateTime.now().plusDays(16).withNano(0);
        var reservationId = helper.createReservation(citizenToken, spaceId, start,
                start.plusHours(1), true, "ZZ0000");

        var sessionId = helper.startCharging(citizenToken, reservationId);
        helper.stopCharging(citizenToken, sessionId);

        // Second stop should fail
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/charging/sessions/" + sessionId + "/stop")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isConflict());
    }

    private UUID extractCitizenId(String token) throws Exception {
        var result = mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
