package com.parking.zonemgmt.internal;

import com.parking.zonemgmt.ParkingSpace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ParkingSpaceJpaRepo extends JpaRepository<ParkingSpace, UUID> {
    List<ParkingSpace> findByZoneId(UUID zoneId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ParkingSpace s WHERE s.id = :id")
    Optional<ParkingSpace> findByIdForUpdate(@Param("id") UUID id);
}
