package com.parking.reservation;

import java.util.UUID;

/**
 * Published when a reservation is cancelled.
 * Placeholder — full implementation in Phase 5.
 */
public record ReservationCancelledEvent(
        UUID reservationId,
        UUID spaceId,
        UUID citizenId
) {}
