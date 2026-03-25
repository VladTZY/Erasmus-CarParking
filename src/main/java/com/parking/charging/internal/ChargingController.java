package com.parking.charging.internal;

import com.parking.charging.ChargingSessionDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
class ChargingController {

    private final ChargingPointService chargingPointService;
    private final ChargingSessionRepo sessionRepo;

    ChargingController(ChargingPointService chargingPointService, ChargingSessionRepo sessionRepo) {
        this.chargingPointService = chargingPointService;
        this.sessionRepo = sessionRepo;
    }

    @PostMapping("/charging/sessions/start")
    @ResponseStatus(HttpStatus.CREATED)
    ChargingSessionDTO start(@RequestBody @Valid StartSessionRequest req) {
        return chargingPointService.startSession(req.reservationId());
    }

    @PostMapping("/charging/sessions/{id}/stop")
    ChargingSessionDTO stop(@PathVariable UUID id) {
        return chargingPointService.stopSession(id);
    }

    @GetMapping("/charging/sessions/my")
    List<ChargingSessionDTO> getMy(Authentication auth) {
        UUID citizenId = (UUID) auth.getPrincipal();
        return sessionRepo.findByCitizenId(citizenId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    record StartSessionRequest(@NotNull UUID reservationId) {}
}
