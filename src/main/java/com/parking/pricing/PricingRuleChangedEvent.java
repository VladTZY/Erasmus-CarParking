package com.parking.pricing;

import java.util.UUID;

/**
 * Published when a pricing rule is created, updated, or deleted.
 */
public record PricingRuleChangedEvent(UUID ruleId, UUID zoneId) {}
