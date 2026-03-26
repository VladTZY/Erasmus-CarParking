package com.parking.usermgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Module-scoped test for the User Management module.
 * Verifies user registration publishes UserRegisteredEvent.
 */
@ApplicationModuleTest
@AutoConfigureMockMvc
class UserMgmtModuleTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    // Helper: wrap MockMvc call in Runnable (avoids checked-exception mismatch in Scenario.stimulate)
    private Runnable doRegister(String email, String password, String role) {
        return () -> {
            try {
                var body = mapper.writeValueAsString(Map.of(
                        "email", email, "password", password, "role", role));
                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.token").isString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Test
    void register_returns201WithTokenAndPublishesEvent(Scenario scenario) {
        var email = "citizen-module@test.com";

        scenario.stimulate(doRegister(email, "password123", "CITIZEN"))
                .andWaitForEventOfType(UserRegisteredEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.email()).isEqualTo(email);
                    assertThat(event.role()).isEqualTo(User.Role.CITIZEN);
                    assertThat(event.userId()).isNotNull();
                });
    }

    @Test
    void login_returnsTokenForExistingUser(Scenario scenario) throws Exception {
        var email = "admin-module@test.com";
        var password = "secret99";

        // Register first (Scenario ensures this is committed)
        scenario.stimulate(doRegister(email, password, "ADMIN"))
                .andWaitForEventOfType(UserRegisteredEvent.class)
                .toArrive();

        // Now test login
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void register_conflictOnDuplicateEmail(Scenario scenario) throws Exception {
        var email = "dup-module@test.com";

        // Register once
        scenario.stimulate(doRegister(email, "pass", "CITIZEN"))
                .andWaitForEventOfType(UserRegisteredEvent.class)
                .toArrive();

        // Second registration with the same email → 409
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", email, "password", "pass", "role", "CITIZEN"))))
                .andExpect(status().isConflict());
    }
}
