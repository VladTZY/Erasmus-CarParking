package com.parking.pricing;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Exported interface — allows the Reservation and Billing modules to price a parking session
 * without depending on internal pricing classes.
 */
public interface IPricingPolicy {
    PricingEstimateDTO estimate(UUID spaceId, int durationMinutes);
    BigDecimal calculate(UUID spaceId, int durationMinutes);
}
