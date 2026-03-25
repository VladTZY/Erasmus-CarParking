package com.parking.reservation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Exported interface — allows other modules (e.g. Notification) to query reservations
 * without depending on internal reservation classes. Writes go through ReservationService only.
 */
public interface IReservationRepo {
    Optional<ReservationDTO> findById(UUID id);
    List<ReservationDTO> findByCitizenId(UUID citizenId);
}
