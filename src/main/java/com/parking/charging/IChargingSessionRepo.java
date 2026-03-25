package com.parking.charging;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Exported interface — allows other modules (e.g. Notification) to query charging sessions
 * without depending on internal charging classes.
 */
public interface IChargingSessionRepo {
    Optional<ChargingSessionDTO> findById(UUID id);
    List<ChargingSessionDTO> findByCitizenId(UUID citizenId);
}
