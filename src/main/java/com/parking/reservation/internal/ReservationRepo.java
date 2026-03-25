package com.parking.reservation.internal;

import com.parking.reservation.IReservationRepo;
import com.parking.reservation.ReservationDTO;
import com.parking.reservation.ReservationStatus;
import org.springframework.stereotype.Repository;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class ReservationRepo implements IReservationRepo {

    private final ReservationJpaRepo jpa;

    ReservationRepo(ReservationJpaRepo jpa) {
        this.jpa = jpa;
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
        return new ReservationDTO(r.getId(), r.getSpaceId(), r.getCitizenId(),
                r.getStartTime(), r.getEndTime(), duration,
                r.getEstimatedFee(), r.isWithCharging(), r.getStatus());
    }
}
