package com.parking.zonemgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Module-scoped test for the Zone Management module.
 * Verifies zone/space creation and the IZoneAvailability interface.
 */
@ApplicationModuleTest
@AutoConfigureMockMvc
class ZoneMgmtModuleTest {

    @MockBean IReservationAvailability reservationAvailability;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired IZoneAvailability zoneAvailability;
    @Autowired IZoneQuery zoneQuery;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createZone_returns201AndListable() throws Exception {
        var response = mockMvc.perform(post("/zones")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "name", "Zone A", "address", "1 Main St"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isString())
            .andExpect(jsonPath("$.name").value("Zone A"))
            .andReturn();

        var zoneId = UUID.fromString(
                mapper.readTree(response.getResponse().getContentAsString()).get("id").asText());

        var zone = zoneQuery.findZoneById(zoneId);
        assertThat(zone).isPresent();
        assertThat(zone.get().name()).isEqualTo("Zone A");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSpace_isReportedAvailable() throws Exception {
        // Create zone first
        var zoneResponse = mockMvc.perform(post("/zones")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "name", "Zone B", "address", "2 Park Ave"))))
            .andExpect(status().isCreated())
            .andReturn();
        var zoneId = UUID.fromString(
                mapper.readTree(zoneResponse.getResponse().getContentAsString()).get("id").asText());

        // Create space in the zone
        var spaceResponse = mockMvc.perform(post("/zones/" + zoneId + "/spaces")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("name", "B-01", "type", "EV"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.state").value("FREE"))
            .andReturn();
        var spaceId = UUID.fromString(
                mapper.readTree(spaceResponse.getResponse().getContentAsString()).get("id").asText());

        // Verify via IZoneAvailability
        assertThat(zoneAvailability.isSpaceAvailable(spaceId)).isTrue();
        assertThat(zoneAvailability.findAllSpaceIds(zoneId)).contains(spaceId);
        assertThat(zoneAvailability.findAvailableSpaces(zoneId)).contains(spaceId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listSpaces_returnsCreatedSpaces() throws Exception {
        // Create zone + space
        var zoneResponse = mockMvc.perform(post("/zones")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "name", "Zone C", "address", "3 Oak Rd"))))
            .andExpect(status().isCreated())
            .andReturn();
        var zoneId = mapper.readTree(zoneResponse.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/zones/" + zoneId + "/spaces")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("name", "C-01", "type", "REGULAR"))))
            .andExpect(status().isCreated());

        // GET /zones/{id}/spaces returns the space
        mockMvc.perform(get("/zones/" + zoneId + "/spaces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("C-01"))
            .andExpect(jsonPath("$[0].type").value("REGULAR"))
            .andExpect(jsonPath("$[0].state").value("FREE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void availableSpacesEndpoint_filtersReservedByReservationAvailability() throws Exception {
        when(reservationAvailability.hasOverlap(any(), any(), any())).thenReturn(false);

        var zoneResponse = mockMvc.perform(post("/zones")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("name", "Zone D", "address", "4 Elm St"))))
            .andExpect(status().isCreated())
            .andReturn();
        var zoneId = mapper.readTree(zoneResponse.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/zones/" + zoneId + "/spaces")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("name", "D-01", "type", "REGULAR"))))
            .andExpect(status().isCreated());

        // Available spaces with no overlap should return the space
        mockMvc.perform(get("/zones/" + zoneId + "/spaces/available")
                .param("startTime", "2030-06-01T10:00:00")
                .param("endTime", "2030-06-01T12:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("D-01"));
    }
}
