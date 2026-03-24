package com.parking.zonemgmt.internal;

import com.parking.zonemgmt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class ZoneService implements IZoneAvailability, ISpaceQuery {

    private final ParkingZoneRepo repo;

    ZoneService(ParkingZoneRepo repo) {
        this.repo = repo;
    }

    // ── IZoneAvailability ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isSpaceAvailable(UUID spaceId) {
        return repo.findSpaceById(spaceId)
                .map(s -> s.getState() == ParkingSpace.SpaceState.FREE)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findAvailableSpaces(UUID zoneId) {
        return repo.findSpacesByZoneId(zoneId).stream()
                .filter(s -> s.getState() == ParkingSpace.SpaceState.FREE)
                .map(ParkingSpace::getId)
                .toList();
    }

    // ── ISpaceQuery ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingSpace> findSpace(UUID spaceId) {
        return repo.findSpaceById(spaceId);
    }

    // ── Zone CRUD ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    List<ParkingZoneDTO> listZones() {
        return repo.findAllZones().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    ParkingZoneDTO getZone(UUID id) {
        return repo.findZoneById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));
    }

    @Transactional
    ParkingZoneDTO createZone(String name, String address, int totalCapacity) {
        var zone = new ParkingZone(UUID.randomUUID(), name, address, totalCapacity);
        return toDTO(repo.saveZone(zone));
    }

    @Transactional
    ParkingZoneDTO updateZone(UUID id, String name, String address, int totalCapacity) {
        var zone = repo.findZoneById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));
        zone.setName(name);
        zone.setAddress(address);
        zone.setTotalCapacity(totalCapacity);
        return toDTO(repo.saveZone(zone));
    }

    @Transactional
    void deleteZone(UUID id) {
        repo.deleteZone(id);
    }

    // ── Space CRUD ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    List<ParkingSpace> listSpaces(UUID zoneId) {
        return repo.findSpacesByZoneId(zoneId);
    }

    @Transactional
    ParkingSpace createSpace(UUID zoneId, ParkingSpace.SpaceType type) {
        repo.findZoneById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + zoneId));
        var space = new ParkingSpace(UUID.randomUUID(), zoneId, type, ParkingSpace.SpaceState.FREE);
        return repo.saveSpace(space);
    }

    @Transactional
    ParkingSpace updateSpace(UUID spaceId, ParkingSpace.SpaceType type, ParkingSpace.SpaceState state) {
        var space = repo.findSpaceById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found: " + spaceId));
        space.setType(type);
        space.setState(state);
        return repo.saveSpace(space);
    }

    @Transactional
    void changeSpaceState(UUID spaceId, ParkingSpace.SpaceState newState) {
        var space = repo.findSpaceById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found: " + spaceId));
        space.setState(newState);
        repo.saveSpace(space);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private ParkingZoneDTO toDTO(ParkingZone zone) {
        long available = repo.findSpacesByZoneId(zone.getId()).stream()
                .filter(s -> s.getState() == ParkingSpace.SpaceState.FREE)
                .count();
        return new ParkingZoneDTO(zone.getId(), zone.getName(), zone.getAddress(), zone.getTotalCapacity(), available);
    }
}
