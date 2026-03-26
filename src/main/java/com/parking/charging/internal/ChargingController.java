package com.parking.charging.internal;

import com.parking.charging.ChargingSessionDTO;
import com.parking.reservation.IReservationRepo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
    private final OpenRouterService openRouterService;
    private final IReservationRepo reservationRepo;

    ChargingController(ChargingPointService chargingPointService, ChargingSessionRepo sessionRepo,
                       OpenRouterService openRouterService, IReservationRepo reservationRepo) {
        this.chargingPointService = chargingPointService;
        this.sessionRepo = sessionRepo;
        this.openRouterService = openRouterService;
        this.reservationRepo = reservationRepo;
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

    @PostMapping("/charging/sessions/scan-plate")
    ResponseEntity<?> scanPlate(@RequestBody @Valid ScanPlateRequest req) {
        var reservation = reservationRepo.findById(req.reservationId())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + req.reservationId()));

        if (!reservation.withCharging()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reservation does not include EV charging"));
        }

        String storedPlate = reservation.licensePlate();
        if (storedPlate == null || storedPlate.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No license plate registered for this reservation"));
        }

        String detectedPlate = openRouterService.extractLicensePlate(req.imageBase64());
        String normalizedStored = storedPlate.toUpperCase().replaceAll("[^A-Z0-9]", "");
        boolean matched = normalizedStored.equals(detectedPlate);
        org.slf4j.LoggerFactory.getLogger(ChargingController.class)
                .info("Plate check — stored: '{}', detected: '{}', matched: {}", normalizedStored, detectedPlate, matched);

        if (matched) {
            ChargingSessionDTO session = chargingPointService.startSession(req.reservationId());
            return ResponseEntity.ok(Map.of("matched", true, "detectedPlate", detectedPlate, "session", session));
        } else {
            return ResponseEntity.ok(Map.of("matched", false, "detectedPlate", detectedPlate));
        }
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

    record ScanPlateRequest(@NotNull UUID reservationId, @NotBlank String imageBase64) {}
}
