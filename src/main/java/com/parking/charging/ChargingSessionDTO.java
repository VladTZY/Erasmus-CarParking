package com.parking.charging;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChargingSessionDTO(
        UUID id,
        UUID reservationId,
        UUID spaceId,
        String spaceName,
        String zoneName,
        ChargingStatus status,
        LocalDateTime startedAt,
        Double energyKwh
) {}
