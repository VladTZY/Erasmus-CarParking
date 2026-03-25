package com.parking.charging;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargingPlanDTO(
        UUID spaceId,
        int requestedKwh,
        BigDecimal estimatedCost,
        String currency,
        String strategy
) {}
