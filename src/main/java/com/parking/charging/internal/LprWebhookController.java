package com.parking.charging.internal;

import com.parking.charging.ChargingSessionDTO;
import com.parking.charging.ChargingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
class LprWebhookController {

    private final ChargingPointService chargingPointService;
    private final ChargingSessionRepo sessionRepo;

    LprWebhookController(ChargingPointService chargingPointService, ChargingSessionRepo sessionRepo) {
        this.chargingPointService = chargingPointService;
        this.sessionRepo = sessionRepo;
    }

    @PostMapping("/webhooks/lpr")
    ResponseEntity<?> handle(@RequestBody @Valid LprEvent event) {
        return switch (event.eventType()) {
            case ENTRY -> {
                var session = sessionRepo.findBySpaceIdAndStatus(event.spaceId(), ChargingStatus.PENDING)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No pending charging session for space: " + event.spaceId()));
                ChargingSessionDTO dto = chargingPointService.startSession(session.getReservationId());
                yield ResponseEntity.ok(dto);
            }
            case EXIT -> {
                var session = sessionRepo.findBySpaceIdAndStatus(event.spaceId(), ChargingStatus.ACTIVE)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No active charging session for space: " + event.spaceId()));
                ChargingSessionDTO dto = chargingPointService.stopSession(session.getId());
                yield ResponseEntity.ok(dto);
            }
        };
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
    }

    record LprEvent(
            @NotNull String licensePlate,
            @NotNull UUID spaceId,
            @NotNull EventType eventType
    ) {}

    enum EventType { ENTRY, EXIT }
}
