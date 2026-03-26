package com.parking.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.zonemgmt.ISpaceQuery;
import com.parking.zonemgmt.IZoneQuery;
import com.parking.zonemgmt.ParkingSpace;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Module-scoped test for the Pricing module.
 * Verifies pricing rule creation and fee estimation.
 */
@ApplicationModuleTest
@AutoConfigureMockMvc
class PricingModuleTest {

    @MockBean ISpaceQuery spaceQuery;
    @MockBean IZoneQuery zoneQuery;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired IPricingPolicy pricingPolicy;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRule_returns201() throws Exception {
        var zoneId = UUID.randomUUID();
        var body = mapper.writeValueAsString(Map.of(
                "zoneId", zoneId.toString(),
                "spaceType", "REGULAR",
                "ratePerHour", "2.50",
                "validFrom", "2020-01-01T00:00:00"
        ));

        mockMvc.perform(post("/pricing/rules")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isString())
            .andExpect(jsonPath("$.ratePerHour").value(2.50))
            .andExpect(jsonPath("$.zoneId").value(zoneId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRule_andEstimate_returnCorrectFee() throws Exception {
        var zoneId = UUID.randomUUID();
        var spaceId = UUID.randomUUID();

        // Mock space lookup
        var space = new ParkingSpace(spaceId, zoneId, "A-01", ParkingSpace.SpaceType.REGULAR, ParkingSpace.SpaceState.FREE);
        when(spaceQuery.findSpace(spaceId)).thenReturn(Optional.of(space));

        // Create pricing rule: €3.00/h for REGULAR spaces in this zone
        var ruleBody = mapper.writeValueAsString(Map.of(
                "zoneId", zoneId.toString(),
                "spaceType", "REGULAR",
                "ratePerHour", "3.00",
                "validFrom", "2020-01-01T00:00:00"
        ));
        mockMvc.perform(post("/pricing/rules")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleBody))
            .andExpect(status().isCreated());

        // Estimate for 60 minutes should be €3.00
        mockMvc.perform(get("/pricing/estimate")
                .param("spaceId", spaceId.toString())
                .param("durationMinutes", "60"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estimatedFee").value(3.00))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.ratePerHour").value(3.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void estimate_usesDefaultRateWhenNoRuleExists() throws Exception {
        var spaceId = UUID.randomUUID();
        var zoneId = UUID.randomUUID();
        var space = new ParkingSpace(spaceId, zoneId, "B-01", ParkingSpace.SpaceType.REGULAR, ParkingSpace.SpaceState.FREE);
        when(spaceQuery.findSpace(spaceId)).thenReturn(Optional.of(space));

        // No pricing rule created → falls back to DEFAULT_RATE (€1.00/h)
        // 120 min = 2h × €1.00 = €2.00
        mockMvc.perform(get("/pricing/estimate")
                .param("spaceId", spaceId.toString())
                .param("durationMinutes", "120"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estimatedFee").value(2.00))
            .andExpect(jsonPath("$.ratePerHour").value(1.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void pricingPolicyInterface_estimateMatchesHttpEndpoint() throws Exception {
        var spaceId = UUID.randomUUID();
        var zoneId = UUID.randomUUID();
        var space = new ParkingSpace(spaceId, zoneId, "C-01", ParkingSpace.SpaceType.EV, ParkingSpace.SpaceState.FREE);
        when(spaceQuery.findSpace(spaceId)).thenReturn(Optional.of(space));

        // Create EV-specific rule: €4.00/h
        mockMvc.perform(post("/pricing/rules")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "zoneId", zoneId.toString(),
                        "spaceType", "EV",
                        "ratePerHour", "4.00",
                        "validFrom", "2020-01-01T00:00:00"
                ))))
            .andExpect(status().isCreated());

        // Use IPricingPolicy directly (exported interface)
        PricingEstimateDTO estimate = pricingPolicy.estimate(spaceId, 90);
        assertThat(estimate.estimatedFee()).isEqualByComparingTo(new BigDecimal("6.00")); // 1.5h × €4.00
        assertThat(estimate.currency()).isEqualTo("EUR");
        assertThat(estimate.ratePerHour()).isEqualByComparingTo(new BigDecimal("4.00"));
    }
}
