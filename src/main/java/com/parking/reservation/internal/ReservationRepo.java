package com.parking.reservation.internal;

import com.parking.reservation.IReservationAvailability;
import com.parking.reservation.IReservationRepo;
import com.parking.reservation.ReservationDTO;
import com.parking.reservation.ReservationStatus;
import com.parking.zonemgmt.ISpaceQuery;
import com.parking.zonemgmt.IZoneQuery;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class ReservationRepo implements IReservationRepo, IReservationAvailability {

    private final ReservationJpaRepo jpa;
    private final ISpaceQuery spaceQuery;
    private final IZoneQuery zoneQuery;

    ReservationRepo(ReservationJpaRepo jpa, @Lazy ISpaceQuery spaceQuery, @Lazy IZoneQuery zoneQuery) {
        this.jpa = jpa;
        this.spaceQuery = spaceQuery;
        this.zoneQuery = zoneQuery;
    }

    // Package-private — for use by ReservationService only

    Reservation save(Reservation reservation) {
        return jpa.save(reservation);
    }

    Optional<Reservation> findEntityById(UUID id) {
        return jpa.findById(id);
    }

    List<Reservation> findEntitiesByCitizenId(UUID citizenId) {
        return jpa.findByCitizenId(citizenId);
    }

    List<Reservation> findEntitiesBySpaceIdAndStatus(UUID spaceId, ReservationStatus status) {
        return jpa.findBySpaceIdAndStatus(spaceId, status);
    }

    List<ReservationDTO> findScheduleBySpaceId(UUID spaceId) {
        return jpa.findBySpaceIdAndStatusNot(spaceId, ReservationStatus.CANCELLED)
                .stream().map(this::toDTO).toList();
    }

    @Override
    public boolean hasOverlap(UUID spaceId, LocalDateTime startTime, LocalDateTime endTime) {
        return !jpa.findOverlapping(spaceId, ReservationStatus.CANCELLED, startTime, endTime).isEmpty();
    }

    // IReservationRepo — public contract for other modules

    @Override
    public Optional<ReservationDTO> findById(UUID id) {
        return jpa.findById(id).map(this::toDTO);
    }

    @Override
    public List<ReservationDTO> findByCitizenId(UUID citizenId) {
        return jpa.findByCitizenId(citizenId).stream().map(this::toDTO).toList();
    }

    private ReservationDTO toDTO(Reservation r) {
        int duration = (int) ChronoUnit.MINUTES.between(r.getStartTime(), r.getEndTime());
        var space = spaceQuery.findSpace(r.getSpaceId());
        String spaceName = space.map(s -> s.getName()).orElse(null);
        String zoneName = space.flatMap(s -> zoneQuery.findZoneById(s.getZoneId()))
                .map(z -> z.name()).orElse(null);
        return new ReservationDTO(r.getId(), r.getSpaceId(), spaceName, zoneName, r.getCitizenId(),
                r.getStartTime(), r.getEndTime(), duration,
                r.getEstimatedFee(), r.isWithCharging(), r.getStatus());
    }
}
