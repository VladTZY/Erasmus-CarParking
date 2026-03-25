package com.parking.zonemgmt.internal;

import com.parking.zonemgmt.ParkingSpace;
import com.parking.zonemgmt.ParkingZone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal repository contract for ParkingZone and ParkingSpace aggregates.
 * Only used within the Zone Management module — other modules depend on IZoneAvailability.
 */
interface IParkingZoneRepo {
    Optional<ParkingZone> findZoneById(UUID id);
    List<ParkingZone> findAllZones();
    Optional<ParkingSpace> findSpaceById(UUID id);
    List<ParkingSpace> findAllSpaces();
    List<ParkingSpace> findSpacesByZoneId(UUID zoneId);
}
