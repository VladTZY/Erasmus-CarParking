package com.parking.reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationEstimateDTO(
        UUID spaceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int durationMinutes,
        BigDecimal estimatedFee,
        String currency,
        BigDecimal ratePerHour
) {}
