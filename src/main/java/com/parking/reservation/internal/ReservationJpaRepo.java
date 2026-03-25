package com.parking.reservation.internal;

import com.parking.reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

interface ReservationJpaRepo extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByCitizenId(UUID citizenId);
    List<Reservation> findBySpaceIdAndStatus(UUID spaceId, ReservationStatus status);
    List<Reservation> findBySpaceIdAndStatusNot(UUID spaceId, ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.spaceId = :spaceId " +
           "AND r.status <> :excludeStatus " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    List<Reservation> findOverlapping(
            @Param("spaceId") UUID spaceId,
            @Param("excludeStatus") ReservationStatus excludeStatus,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
