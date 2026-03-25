package com.parking.charging.internal;

import com.parking.charging.ChargingPlanDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
class ChargingStrategyService {

    private static final BigDecimal RATE_PER_KWH = new BigDecimal("0.30");

    ChargingPlanDTO calculateOptimalPlan(UUID spaceId, int requestedKwh) {
        var estimatedCost = RATE_PER_KWH.multiply(BigDecimal.valueOf(requestedKwh))
                .setScale(2, RoundingMode.HALF_UP);
        return new ChargingPlanDTO(spaceId, requestedKwh, estimatedCost, "EUR", "IMMEDIATE_FULL_SPEED");
    }
}
