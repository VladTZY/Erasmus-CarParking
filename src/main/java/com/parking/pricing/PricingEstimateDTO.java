package com.parking.pricing;

import java.math.BigDecimal;
import java.util.UUID;

/** Read model returned to callers requesting a price estimate. */
public record PricingEstimateDTO(
        BigDecimal estimatedFee,
        String currency,
        BigDecimal ratePerHour,
        UUID appliedRuleId
) {
    public PricingEstimateDTO(BigDecimal estimatedFee, BigDecimal ratePerHour, UUID appliedRuleId) {
        this(estimatedFee, "EUR", ratePerHour, appliedRuleId);
    }
}
