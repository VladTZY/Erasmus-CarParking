package com.parking.zonemgmt.internal;

import com.parking.zonemgmt.ParkingSpace;
import com.parking.zonemgmt.ParkingZoneDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
class ZoneController {

    private final ZoneService zoneService;

    ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    // ── Zone endpoints ────────────────────────────────────────────────────────

    @GetMapping("/zones")
    List<ParkingZoneDTO> listZones() {
        return zoneService.listZones();
    }

    @PostMapping("/zones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    ParkingZoneDTO createZone(@RequestBody @Valid ZoneRequest req) {
        return zoneService.createZone(req.name(), req.address(), req.totalCapacity());
    }

    @PutMapping("/zones/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ParkingZoneDTO updateZone(@PathVariable UUID id, @RequestBody @Valid ZoneRequest req) {
        return zoneService.updateZone(id, req.name(), req.address(), req.totalCapacity());
    }

    @DeleteMapping("/zones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void deleteZone(@PathVariable UUID id) {
        zoneService.deleteZone(id);
    }

    @PatchMapping("/zones/{id}/map")
    @PreAuthorize("hasRole('ADMIN')")
    ParkingZoneDTO updateMapData(@PathVariable UUID id, @RequestBody MapUpdateRequest req) {
        return zoneService.updateMapData(id, req.latitude(), req.longitude(), req.boundary());
    }

    // ── Space endpoints ───────────────────────────────────────────────────────

    @GetMapping("/zones/{id}/spaces")
    List<ParkingSpace> listSpaces(@PathVariable UUID id) {
        return zoneService.listSpaces(id);
    }

    @GetMapping("/zones/{id}/spaces/available")
    List<ParkingSpace> listAvailableSpaces(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return zoneService.listAvailableSpaces(id, startTime, endTime);
    }

    @GetMapping("/zones/spaces/available")
    List<ParkingSpace> listAllAvailableSpaces(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return zoneService.listAllAvailableSpaces(startTime, endTime);
    }

    @PostMapping("/zones/{id}/spaces")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    ParkingSpace createSpace(@PathVariable UUID id, @RequestBody @Valid SpaceCreateRequest req) {
        return zoneService.createSpace(id, req.name(), req.type());
    }

    @PutMapping("/zones/{id}/spaces/{spaceId}")
    @PreAuthorize("hasRole('ADMIN')")
    ParkingSpace updateSpace(@PathVariable UUID id,
                             @PathVariable UUID spaceId,
                             @RequestBody @Valid SpaceUpdateRequest req) {
        return zoneService.updateSpace(spaceId, req.type(), req.state());
    }

    // ── Error handlers ────────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    // ── Request records ───────────────────────────────────────────────────────

    record ZoneRequest(
            @NotBlank String name,
            @NotBlank String address,
            @Positive int totalCapacity
    ) {}

    record SpaceCreateRequest(@NotBlank String name, @NotNull ParkingSpace.SpaceType type) {}

    record SpaceUpdateRequest(
            @NotNull ParkingSpace.SpaceType type,
            @NotNull ParkingSpace.SpaceState state
    ) {}

    record MapUpdateRequest(Double latitude, Double longitude, String boundary) {}
}
