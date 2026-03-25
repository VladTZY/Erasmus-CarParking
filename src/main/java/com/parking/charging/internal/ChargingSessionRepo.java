package com.parking.charging.internal;

import com.parking.charging.ChargingSessionDTO;
import com.parking.charging.ChargingStatus;
import com.parking.charging.IChargingSessionRepo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class ChargingSessionRepo implements IChargingSessionRepo {

    private final ChargingSessionJpaRepo jpa;

    ChargingSessionRepo(ChargingSessionJpaRepo jpa) {
        this.jpa = jpa;
    }

    // Package-private — for use by services in this module only

    ChargingSession save(ChargingSession session) {
        return jpa.save(session);
    }

    void delete(ChargingSession session) {
        jpa.delete(session);
    }

    Optional<ChargingSession> findEntityById(UUID id) {
        return jpa.findById(id);
    }

    Optional<ChargingSession> findByReservationIdAndStatus(UUID reservationId, ChargingStatus status) {
        return jpa.findByReservationIdAndStatus(reservationId, status);
    }

    Optional<ChargingSession> findBySpaceIdAndStatus(UUID spaceId, ChargingStatus status) {
        return jpa.findBySpaceIdAndStatus(spaceId, status);
    }

    // IChargingSessionRepo — public contract for other modules

    @Override
    public Optional<ChargingSessionDTO> findById(UUID id) {
        return jpa.findById(id).map(this::toDTO);
    }

    @Override
    public List<ChargingSessionDTO> findByCitizenId(UUID citizenId) {
        return jpa.findByCitizenId(citizenId).stream().map(this::toDTO).toList();
    }

    private ChargingSessionDTO toDTO(ChargingSession s) {
        return new ChargingSessionDTO(s.getId(), s.getReservationId(), s.getSpaceId(),
                s.getStatus(), s.getStartedAt(), s.getEnergyKwh());
    }
}
