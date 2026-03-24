package com.parking.reservation;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when a reservation is confirmed.
 * Placeholder — full implementation in Phase 5.
 */
public record ReservationCreatedEvent(
        UUID reservationId,
        UUID spaceId,
        UUID citizenId,
        BigDecimal estimatedFee,
        int durationMinutes,
        boolean withCharging
) {}
