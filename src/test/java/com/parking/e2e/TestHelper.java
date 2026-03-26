package com.parking.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared HTTP helpers for E2E flow tests.
 * Performs common operations and returns parsed IDs or tokens.
 */
class TestHelper {

    private final MockMvc mockMvc;
    private final ObjectMapper mapper;

    TestHelper(MockMvc mockMvc, ObjectMapper mapper) {
        this.mockMvc = mockMvc;
        this.mapper = mapper;
    }

    /** Register a user and return the JWT token. */
    String registerAndGetToken(String email, String password, String role) throws Exception {
        var body = mapper.writeValueAsString(Map.of(
                "email", email, "password", password, "role", role));
        var result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return parseToken(result);
    }

    /** Create a zone and return its UUID. */
    UUID createZone(String adminToken, String name, String address) throws Exception {
        var body = mapper.writeValueAsString(Map.of("name", name, "address", address));
        var result = mockMvc.perform(post("/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return parseId(result);
    }

    /** Create a space and return its UUID. */
    UUID createSpace(String adminToken, UUID zoneId, String name, String type) throws Exception {
        var body = mapper.writeValueAsString(Map.of("name", name, "type", type));
        var result = mockMvc.perform(post("/zones/" + zoneId + "/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return parseId(result);
    }

    /** Create a pricing rule for the zone. */
    void createPricingRule(String adminToken, UUID zoneId, String spaceType,
                           String ratePerHour) throws Exception {
        var body = mapper.writeValueAsString(Map.of(
                "zoneId", zoneId.toString(),
                "spaceType", spaceType,
                "ratePerHour", ratePerHour,
                "validFrom", "2020-01-01T00:00:00"
        ));
        mockMvc.perform(post("/pricing/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(body))
                .andExpect(status().isCreated());
    }

    /** Create a reservation and return its UUID. */
    UUID createReservation(String citizenToken, UUID spaceId, LocalDateTime start,
                           LocalDateTime end, boolean withCharging,
                           String licensePlate) throws Exception {
        var bodyMap = new java.util.HashMap<String, Object>();
        bodyMap.put("spaceId", spaceId.toString());
        bodyMap.put("startTime", start.toString());
        bodyMap.put("endTime", end.toString());
        bodyMap.put("withCharging", withCharging);
        if (licensePlate != null) bodyMap.put("licensePlate", licensePlate);

        var result = mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + citizenToken)
                        .content(mapper.writeValueAsString(bodyMap)))
                .andExpect(status().isCreated())
                .andReturn();
        return parseId(result);
    }

    /** Cancel a reservation. */
    void cancelReservation(String token, UUID reservationId) throws Exception {
        mockMvc.perform(delete("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    /** Start a charging session and return its UUID. */
    UUID startCharging(String token, UUID reservationId) throws Exception {
        var body = mapper.writeValueAsString(Map.of("reservationId", reservationId.toString()));
        var result = mockMvc.perform(post("/charging/sessions/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return parseId(result);
    }

    /** Stop a charging session. */
    JsonNode stopCharging(String token, UUID sessionId) throws Exception {
        var result = mockMvc.perform(post("/charging/sessions/" + sessionId + "/stop")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    // ── Parsing helpers ──────────────────────────────────────────────────────

    private String parseToken(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private UUID parseId(MvcResult result) throws Exception {
        return UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
