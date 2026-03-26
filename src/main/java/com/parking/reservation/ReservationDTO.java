package com.parking.reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationDTO(
        UUID id,
        UUID spaceId,
        String spaceName,
        String zoneName,
        UUID citizenId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int durationMinutes,
        BigDecimal estimatedFee,
        boolean withCharging,
        ReservationStatus status,
        String licensePlate
) {}
