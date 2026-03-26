package com.parking.charging;

import java.util.UUID;

public record ChargingCompletedEvent(
        UUID sessionId,
        UUID reservationId,
        UUID citizenId,
        double energyKwh
) {}
