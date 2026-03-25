package com.parking.pricing.internal;

import com.parking.zonemgmt.IZoneQuery;
import com.parking.zonemgmt.ParkingZoneDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(3)
@Profile("!test")
class PricingDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PricingDataSeeder.class);

    private static final LocalDateTime EPOCH = LocalDateTime.of(2020, 1, 1, 0, 0);

    private final PricingRuleService pricingRuleService;
    private final PricingRuleJpaRepo ruleRepo;
    private final IZoneQuery zoneQuery;

    PricingDataSeeder(PricingRuleService pricingRuleService,
                      PricingRuleJpaRepo ruleRepo,
                      IZoneQuery zoneQuery) {
        this.pricingRuleService = pricingRuleService;
        this.ruleRepo = ruleRepo;
        this.zoneQuery = zoneQuery;
    }

    @Override
    public void run(String... args) {
        if (!ruleRepo.findAll().isEmpty()) {
            log.info("[Seed] Pricing rules already present — skipping pricing seed");
            return;
        }

        List<ParkingZoneDTO> zones = zoneQuery.findAllZones();
        var zoneA = zones.stream().filter(z -> z.name().equals("City Centre")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Zone A not found — run ZoneDataSeeder first"));
        var zoneB = zones.stream().filter(z -> z.name().equals("Train Station")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Zone B not found — run ZoneDataSeeder first"));

        // Zone A
        var rA1 = pricingRuleService.createRule(zoneA.id(), PricingRule.SpaceType.REGULAR,
                new BigDecimal("2.00"), EPOCH, null);
        var rA2 = pricingRuleService.createRule(zoneA.id(), PricingRule.SpaceType.EV,
                new BigDecimal("3.50"), EPOCH, null);

        // Zone B
        var rB1 = pricingRuleService.createRule(zoneB.id(), PricingRule.SpaceType.REGULAR,
                new BigDecimal("1.50"), EPOCH, null);
        var rB2 = pricingRuleService.createRule(zoneB.id(), PricingRule.SpaceType.EV,
                new BigDecimal("2.80"), EPOCH, null);

        log.info("[Seed] Pricing rules created:");
        log.info("[Seed]   Zone A REGULAR €2.00/hr  id={}", rA1.getId());
        log.info("[Seed]   Zone A EV      €3.50/hr  id={}", rA2.getId());
        log.info("[Seed]   Zone B REGULAR €1.50/hr  id={}", rB1.getId());
        log.info("[Seed]   Zone B EV      €2.80/hr  id={}", rB2.getId());
    }
}
