package com.parking.zonemgmt.internal;

import com.parking.zonemgmt.ParkingZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ParkingZoneJpaRepo extends JpaRepository<ParkingZone, UUID> {}
