package com.parking.reservation.internal;

import com.parking.reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ReservationJpaRepo extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByCitizenId(UUID citizenId);
    List<Reservation> findBySpaceIdAndStatus(UUID spaceId, ReservationStatus status);
}
