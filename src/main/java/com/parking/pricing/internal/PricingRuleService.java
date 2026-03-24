package com.parking.pricing.internal;

import com.parking.pricing.PricingRuleChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
class PricingRuleService {

    private final PricingRuleJpaRepo repo;
    private final ApplicationEventPublisher eventPublisher;

    PricingRuleService(PricingRuleJpaRepo repo, ApplicationEventPublisher eventPublisher) {
        this.repo = repo;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    List<PricingRule> listRules() {
        return repo.findAll();
    }

    @Transactional
    PricingRule createRule(UUID zoneId, PricingRule.SpaceType spaceType, BigDecimal ratePerHour,
                           LocalDateTime validFrom, LocalDateTime validTo) {
        var rule = new PricingRule(UUID.randomUUID(), zoneId, spaceType, ratePerHour, validFrom, validTo);
        rule = repo.save(rule);
        eventPublisher.publishEvent(new PricingRuleChangedEvent(rule.getId(), rule.getZoneId()));
        return rule;
    }

    @Transactional
    PricingRule updateRule(UUID id, UUID zoneId, PricingRule.SpaceType spaceType, BigDecimal ratePerHour,
                           LocalDateTime validFrom, LocalDateTime validTo) {
        var rule = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing rule not found: " + id));
        rule.setZoneId(zoneId);
        rule.setSpaceType(spaceType);
        rule.setRatePerHour(ratePerHour);
        rule.setValidFrom(validFrom);
        rule.setValidTo(validTo);
        rule = repo.save(rule);
        eventPublisher.publishEvent(new PricingRuleChangedEvent(rule.getId(), rule.getZoneId()));
        return rule;
    }

    @Transactional
    void deleteRule(UUID id) {
        var rule = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing rule not found: " + id));
        repo.deleteById(id);
        eventPublisher.publishEvent(new PricingRuleChangedEvent(id, rule.getZoneId()));
    }
}
