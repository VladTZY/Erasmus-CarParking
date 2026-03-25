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
        var westenhellweg = zones.stream().filter(z -> z.name().equals("Westenhellweg")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Westenhellweg zone not found — run ZoneDataSeeder first"));
        var hauptbahnhof = zones.stream().filter(z -> z.name().equals("Hauptbahnhof")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Hauptbahnhof zone not found — run ZoneDataSeeder first"));
        var westfalenhallen = zones.stream().filter(z -> z.name().equals("Westfalenhallen")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Westfalenhallen zone not found — run ZoneDataSeeder first"));
        var phoenixsee = zones.stream().filter(z -> z.name().equals("Phoenixsee")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Phoenixsee zone not found — run ZoneDataSeeder first"));

        // Westenhellweg — city centre premium rates
        pricingRuleService.createRule(westenhellweg.id(), PricingRule.SpaceType.REGULAR,
                new BigDecimal("2.50"), EPOCH, null);
        pricingRuleService.createRule(westenhellweg.id(), PricingRule.SpaceType.EV,
                new BigDecimal("4.00"), EPOCH, null);

        // Hauptbahnhof — standard station rates
        pricingRuleService.createRule(hauptbahnhof.id(), PricingRule.SpaceType.REGULAR,
                new BigDecimal("2.00"), EPOCH, null);
        pricingRuleService.createRule(hauptbahnhof.id(), PricingRule.SpaceType.EV,
                new BigDecimal("3.50"), EPOCH, null);

        // Westfalenhallen — event venue rates
        pricingRuleService.createRule(westfalenhallen.id(), PricingRule.SpaceType.REGULAR,
                new BigDecimal("1.80"), EPOCH, null);
        pricingRuleService.createRule(westfalenhallen.id(), PricingRule.SpaceType.EV,
                new BigDecimal("3.00"), EPOCH, null);

        // Phoenixsee — residential/leisure rates
        pricingRuleService.createRule(phoenixsee.id(), PricingRule.SpaceType.REGULAR,
                new BigDecimal("1.50"), EPOCH, null);
        pricingRuleService.createRule(phoenixsee.id(), PricingRule.SpaceType.EV,
                new BigDecimal("2.80"), EPOCH, null);

        log.info("[Seed] Pricing rules created for Westenhellweg, Hauptbahnhof, Westfalenhallen, Phoenixsee");
    }
}
