package com.parking.reservation.internal;

import com.parking.reservation.ReservationDTO;
import com.parking.reservation.ReservationEstimateDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
class ReservationController {

    private final ReservationService reservationService;

    ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/reservations/search")
    List<ReservationEstimateDTO> search(
            @RequestParam UUID zoneId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return reservationService.searchAvailable(zoneId, startTime, endTime);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    ReservationDTO create(@RequestBody @Valid CreateReservationRequest req, Authentication auth) {
        UUID citizenId = (UUID) auth.getPrincipal();
        return reservationService.createReservation(req.spaceId(), citizenId,
                req.startTime(), req.endTime(), req.withCharging());
    }

    @GetMapping("/reservations/{id}")
    ReservationDTO getById(@PathVariable UUID id) {
        return reservationService.getById(id);
    }

    @GetMapping("/reservations/space/{spaceId}")
    List<ReservationDTO> getSchedule(@PathVariable UUID spaceId) {
        return reservationService.getSchedule(spaceId);
    }

    @GetMapping("/reservations/my")
    List<ReservationDTO> getMy(Authentication auth) {
        UUID citizenId = (UUID) auth.getPrincipal();
        return reservationService.getByCitizenId(citizenId);
    }

    @DeleteMapping("/reservations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID id, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        // Admin can cancel any reservation — resolve the owner's citizenId so the service's
        // ownership check is satisfied without leaking admin-role logic into the service layer.
        UUID effectiveCitizenId = isAdmin
                ? reservationService.getById(id).citizenId()
                : userId;
        reservationService.cancelReservation(id, effectiveCitizenId);
    }

    @ExceptionHandler(SpaceNotAvailableException.class)
    ResponseEntity<Map<String, String>> handleNotAvailable(SpaceNotAvailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTimeRangeException.class)
    ResponseEntity<Map<String, String>> handleInvalidTime(InvalidTimeRangeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    record CreateReservationRequest(
            @NotNull UUID spaceId,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime,
            boolean withCharging
    ) {}
}
