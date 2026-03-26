package com.parking.zonemgmt.internal;

import com.parking.zonemgmt.ParkingSpace;
import com.parking.zonemgmt.ParkingZone;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of {@link IParkingZoneRepo}.
 * Package-private — callers outside this module use the interface.
 */
@Repository
class ParkingZoneRepo implements IParkingZoneRepo {

    private final ParkingZoneJpaRepo zoneJpa;
    private final ParkingSpaceJpaRepo spaceJpa;

    ParkingZoneRepo(ParkingZoneJpaRepo zoneJpa, ParkingSpaceJpaRepo spaceJpa) {
        this.zoneJpa = zoneJpa;
        this.spaceJpa = spaceJpa;
    }

    @Override
    public Optional<ParkingZone> findZoneById(UUID id) {
        return zoneJpa.findById(id);
    }

    @Override
    public List<ParkingZone> findAllZones() {
        return zoneJpa.findAll();
    }

    @Override
    public Optional<ParkingSpace> findSpaceById(UUID id) {
        return spaceJpa.findById(id);
    }

    @Override
    public Optional<ParkingSpace> findSpaceByIdForUpdate(UUID id) {
        return spaceJpa.findByIdForUpdate(id);
    }

    @Override
    public List<ParkingSpace> findAllSpaces() {
        return spaceJpa.findAll();
    }

    @Override
    public List<ParkingSpace> findSpacesByZoneId(UUID zoneId) {
        return spaceJpa.findByZoneId(zoneId);
    }

    ParkingZone saveZone(ParkingZone zone) {
        return zoneJpa.save(zone);
    }

    void deleteZone(UUID id) {
        zoneJpa.deleteById(id);
    }

    ParkingSpace saveSpace(ParkingSpace space) {
        return spaceJpa.save(space);
    }

    void deleteSpace(UUID id) {
        spaceJpa.deleteById(id);
    }
}
