package com.parking.zonemgmt.internal;

import com.parking.zonemgmt.ParkingSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ParkingSpaceJpaRepo extends JpaRepository<ParkingSpace, UUID> {
    List<ParkingSpace> findByZoneId(UUID zoneId);
}
