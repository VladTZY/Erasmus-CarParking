package com.parking.charging.internal;

import com.parking.charging.ChargingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ChargingSessionJpaRepo extends JpaRepository<ChargingSession, UUID> {
    Optional<ChargingSession> findByReservationIdAndStatus(UUID reservationId, ChargingStatus status);
    Optional<ChargingSession> findBySpaceIdAndStatus(UUID spaceId, ChargingStatus status);
    List<ChargingSession> findByCitizenId(UUID citizenId);
}
