package com.parking.pricing.internal;

import com.parking.pricing.IPricingPolicy;
import com.parking.pricing.PricingEstimateDTO;
import com.parking.zonemgmt.ISpaceQuery;
import com.parking.zonemgmt.ParkingSpace;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
class PricingPolicyService implements IPricingPolicy {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("1.00");

    private final PricingRuleJpaRepo ruleRepo;
    private final ISpaceQuery spaceQuery;

    PricingPolicyService(PricingRuleJpaRepo ruleRepo, ISpaceQuery spaceQuery) {
        this.ruleRepo = ruleRepo;
        this.spaceQuery = spaceQuery;
    }

    @Override
    @Transactional(readOnly = true)
    public PricingEstimateDTO estimate(UUID spaceId, int durationMinutes) {
        var space = spaceQuery.findSpace(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found: " + spaceId));
        var rule = findApplicableRule(space);
        BigDecimal rate = rule != null ? rule.getRatePerHour() : DEFAULT_RATE;
        UUID ruleId = rule != null ? rule.getId() : null;
        BigDecimal fee = calculateFee(rate, durationMinutes);
        return new PricingEstimateDTO(fee, rate, ruleId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculate(UUID spaceId, int durationMinutes) {
        var space = spaceQuery.findSpace(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found: " + spaceId));
        var rule = findApplicableRule(space);
        BigDecimal rate = rule != null ? rule.getRatePerHour() : DEFAULT_RATE;
        return calculateFee(rate, durationMinutes);
    }

    private PricingRule findApplicableRule(ParkingSpace space) {
        LocalDateTime now = LocalDateTime.now();
        PricingRule.SpaceType targetType = space.getType() == ParkingSpace.SpaceType.EV
                ? PricingRule.SpaceType.EV
                : PricingRule.SpaceType.REGULAR;

        return ruleRepo.findAll().stream()
                .filter(r -> r.getZoneId().equals(space.getZoneId()))
                .filter(r -> r.getSpaceType() == targetType || r.getSpaceType() == PricingRule.SpaceType.ALL)
                .filter(r -> !r.getValidFrom().isAfter(now))
                .filter(r -> r.getValidTo() == null || !r.getValidTo().isBefore(now))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal calculateFee(BigDecimal ratePerHour, int durationMinutes) {
        return ratePerHour
                .multiply(BigDecimal.valueOf(durationMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
