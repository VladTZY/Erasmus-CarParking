package com.parking.zonemgmt.internal;

import com.parking.reservation.IReservationAvailability;
import com.parking.zonemgmt.*;
import org.springframework.context.ApplicationEventPublisher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class ZoneService implements IZoneAvailability, ISpaceQuery, ISpaceStateManager, IZoneQuery {

    private final ParkingZoneRepo repo;
    private final ApplicationEventPublisher eventPublisher;
    private final IReservationAvailability reservationAvailability;

    ZoneService(ParkingZoneRepo repo, ApplicationEventPublisher eventPublisher,
                IReservationAvailability reservationAvailability) {
        this.repo = repo;
        this.eventPublisher = eventPublisher;
        this.reservationAvailability = reservationAvailability;
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

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findAllSpaceIds(UUID zoneId) {
        return repo.findSpacesByZoneId(zoneId).stream()
                .map(ParkingSpace::getId)
                .toList();
    }

    // ── ISpaceQuery ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingSpace> findSpace(UUID spaceId) {
        return repo.findSpaceById(spaceId);
    }

    // ── ISpaceStateManager ────────────────────────────────────────────────────

    @Override
    @Transactional
    public void markReserved(UUID spaceId) {
        changeSpaceState(spaceId, ParkingSpace.SpaceState.RESERVED);
        eventPublisher.publishEvent(new SpaceStateChangedEvent(spaceId, ParkingSpace.SpaceState.RESERVED));
    }

    @Override
    @Transactional
    public void markFree(UUID spaceId) {
        changeSpaceState(spaceId, ParkingSpace.SpaceState.FREE);
        eventPublisher.publishEvent(new SpaceStateChangedEvent(spaceId, ParkingSpace.SpaceState.FREE));
    }

    // ── Zone CRUD ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ParkingZoneDTO> findAllZones() {
        return repo.findAllZones().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingZoneDTO> findZoneById(UUID zoneId) {
        return repo.findZoneById(zoneId).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    List<ParkingZoneDTO> listZones() {
        return findAllZones();
    }

    @Transactional(readOnly = true)
    ParkingZoneDTO getZone(UUID id) {
        return repo.findZoneById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));
    }

    @Transactional
    ParkingZoneDTO createZone(String name, String address) {
        var zone = new ParkingZone(UUID.randomUUID(), name, address);
        return toDTO(repo.saveZone(zone));
    }

    @Transactional
    ParkingZoneDTO updateZone(UUID id, String name, String address) {
        var zone = repo.findZoneById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));
        zone.setName(name);
        zone.setAddress(address);
        return toDTO(repo.saveZone(zone));
    }

    @Transactional
    void deleteZone(UUID id) {
        repo.deleteZone(id);
    }

    @Transactional
    ParkingZoneDTO updateMapData(UUID id, Double latitude, Double longitude, String boundary) {
        var zone = repo.findZoneById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));
        zone.setLatitude(latitude);
        zone.setLongitude(longitude);
        zone.setBoundary(boundary);
        return toDTO(repo.saveZone(zone));
    }

    // ── Space CRUD ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    List<ParkingSpace> listSpaces(UUID zoneId) {
        return repo.findSpacesByZoneId(zoneId);
    }

    @Transactional(readOnly = true)
    List<ParkingSpace> listAvailableSpaces(UUID zoneId, LocalDateTime startTime, LocalDateTime endTime) {
        return repo.findSpacesByZoneId(zoneId).stream()
                .filter(s -> !reservationAvailability.hasOverlap(s.getId(), startTime, endTime))
                .toList();
    }

    @Transactional(readOnly = true)
    List<ParkingSpace> listAllAvailableSpaces(LocalDateTime startTime, LocalDateTime endTime) {
        return repo.findAllSpaces().stream()
                .filter(s -> !reservationAvailability.hasOverlap(s.getId(), startTime, endTime))
                .toList();
    }

    @Transactional
    ParkingSpace createSpace(UUID zoneId, String name, ParkingSpace.SpaceType type) {
        repo.findZoneById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + zoneId));
        var space = new ParkingSpace(UUID.randomUUID(), zoneId, name, type, ParkingSpace.SpaceState.FREE);
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
        List<ParkingSpace> spaces = repo.findSpacesByZoneId(zone.getId());
        long total = spaces.size();
        long available = spaces.stream()
                .filter(s -> s.getState() == ParkingSpace.SpaceState.FREE)
                .count();
        return new ParkingZoneDTO(
                zone.getId(), zone.getName(), zone.getAddress(), total, available,
                zone.getLatitude(), zone.getLongitude(), zone.getBoundary()
        );
    }
}
