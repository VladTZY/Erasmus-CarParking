/**
 * Pricing module.
 *
 * <p>Manages tariff rules per zone and space type. Exports {@code IPricingPolicy}
 * for use by the Reservation module (price estimate) and the Billing module
 * (final fee calculation).
 * Published events: {@code PricingRuleChangedEvent}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Pricing"
)
package com.parking.pricing;
