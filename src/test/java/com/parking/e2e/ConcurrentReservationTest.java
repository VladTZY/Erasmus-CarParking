package com.parking.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Concurrent reservation test.
 *
 * 10 threads simultaneously try to reserve the same space in the same time slot.
 * Exactly 1 should succeed (HTTP 201); all others must receive HTTP 409 Conflict.
 * Space state remains FREE (physical occupancy only — not changed by time-slot reservations).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext // Ensures clean DB state — concurrent writes may leave partial state
class ConcurrentReservationTest {

    private static final int THREAD_COUNT = 10;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    private TestHelper helper;
    private String adminToken;
    private String[] citizenTokens;
    private UUID zoneId;
    private UUID spaceId;

    @BeforeEach
    void setUp() throws Exception {
        helper = new TestHelper(mockMvc, mapper);

        var uid = UUID.randomUUID().toString().substring(0, 8);
        adminToken = helper.registerAndGetToken("admin-cc-" + uid + "@test.com", "pass", "ADMIN");

        zoneId = helper.createZone(adminToken, "Concurrent Zone " + uid, "Race Condition Ave");
        spaceId = helper.createSpace(adminToken, zoneId, "CC-01", "REGULAR");
        helper.createPricingRule(adminToken, zoneId, "REGULAR", "1.00");

        // Register 10 citizens (one per thread)
        citizenTokens = new String[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            citizenTokens[i] = helper.registerAndGetToken(
                    "cc-citizen-" + i + "-" + uid + "@test.com", "pass", "CITIZEN");
        }
    }

    @Test
    void tenConcurrentReservations_exactlyOneSucceeds() throws Exception {
        var start = LocalDateTime.now().plusDays(20).withNano(0);
        var end = start.plusHours(1);

        var successCount = new AtomicInteger(0);
        var conflictCount = new AtomicInteger(0);
        var otherErrorCount = new AtomicInteger(0);

        var latch = new CountDownLatch(THREAD_COUNT);
        var startSignal = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(THREAD_COUNT);
        var errors = new ArrayList<Throwable>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            final String citizenToken = citizenTokens[i];

            pool.submit(() -> {
                try {
                    startSignal.await(); // all threads wait until released together
                    var body = mapper.writeValueAsString(Map.of(
                            "spaceId", spaceId.toString(),
                            "startTime", start.toString(),
                            "endTime", end.toString(),
                            "withCharging", false
                    ));

                    var result = mockMvc.perform(post("/reservations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + citizenToken)
                                    .content(body))
                            .andReturn();

                    int statusCode = result.getResponse().getStatus();
                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 409) {
                        conflictCount.incrementAndGet();
                    } else {
                        otherErrorCount.incrementAndGet();
                    }
                } catch (Throwable t) {
                    synchronized (errors) { errors.add(t); }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Release all threads simultaneously to maximise contention
        startSignal.countDown();
        latch.await();
        pool.shutdown();

        // ── Assertions ───────────────────────────────────────────────────────
        assertThat(errors).isEmpty();
        assertThat(otherErrorCount.get()).isZero();
        assertThat(successCount.get())
                .as("Exactly one reservation should succeed")
                .isEqualTo(1);
        assertThat(conflictCount.get())
                .as("All other %d attempts should receive 409 Conflict", THREAD_COUNT - 1)
                .isEqualTo(THREAD_COUNT - 1);

        // Space remains FREE — state reflects physical occupancy, not time-slot bookings
        mockMvc.perform(get("/zones/" + zoneId + "/spaces"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$[0].state").value("FREE"));
    }

    @Test
    void concurrentDifferentSlots_allSucceed() throws Exception {
        // 10 threads each booking a DIFFERENT 1-hour slot → all should succeed
        var baseStart = LocalDateTime.now().plusDays(25).withNano(0);
        var successCount = new AtomicInteger(0);
        var latch = new CountDownLatch(THREAD_COUNT);
        var startSignal = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int slotIndex = i;
            final String citizenToken = citizenTokens[i];
            var slotStart = baseStart.plusDays(slotIndex); // different day per thread
            var slotEnd = slotStart.plusHours(1);

            pool.submit(() -> {
                try {
                    startSignal.await();
                    var body = mapper.writeValueAsString(Map.of(
                            "spaceId", spaceId.toString(),
                            "startTime", slotStart.toString(),
                            "endTime", slotEnd.toString(),
                            "withCharging", false
                    ));
                    var result = mockMvc.perform(post("/reservations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + citizenToken)
                                    .content(body))
                            .andReturn();
                    if (result.getResponse().getStatus() == 201) successCount.incrementAndGet();
                } catch (Throwable ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        startSignal.countDown();
        latch.await();
        pool.shutdown();

        assertThat(successCount.get())
                .as("All 10 reservations for non-overlapping slots should succeed")
                .isEqualTo(THREAD_COUNT);
    }
}
