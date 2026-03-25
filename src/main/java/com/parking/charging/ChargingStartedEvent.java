package com.parking.charging;

import java.util.UUID;

public record ChargingStartedEvent(
        UUID sessionId,
        UUID reservationId,
        UUID spaceId,
        UUID citizenId
) {}
