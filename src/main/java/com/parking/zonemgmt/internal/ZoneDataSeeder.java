package com.parking.zonemgmt.internal;

import com.parking.zonemgmt.ParkingSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@Profile("!test")
class ZoneDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ZoneDataSeeder.class);

    private final ZoneService zoneService;

    ZoneDataSeeder(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @Override
    public void run(String... args) {
        if (!zoneService.listZones().isEmpty()) {
            log.info("[Seed] Zones already present — skipping zone seed");
            return;
        }

        // Zone A — City Centre: 3 REGULAR + 2 EV
        var zoneA = zoneService.createZone("City Centre", "1 High Street", 5);
        createSpaces(zoneA.id(), 3, ParkingSpace.SpaceType.REGULAR);
        createSpaces(zoneA.id(), 2, ParkingSpace.SpaceType.EV);

        // Zone B — Train Station: 2 REGULAR + 2 EV
        var zoneB = zoneService.createZone("Train Station", "Central Station Plaza", 4);
        createSpaces(zoneB.id(), 2, ParkingSpace.SpaceType.REGULAR);
        createSpaces(zoneB.id(), 2, ParkingSpace.SpaceType.EV);

        log.info("[Seed] Zones created:");
        log.info("[Seed]   Zone A 'City Centre'   id={}", zoneA.id());
        log.info("[Seed]   Zone B 'Train Station' id={}", zoneB.id());
        log.info("[Seed]   Total spaces: 9 (5 in Zone A, 4 in Zone B) — all FREE");
    }

    private void createSpaces(java.util.UUID zoneId, int count, ParkingSpace.SpaceType type) {
        for (int i = 0; i < count; i++) {
            var space = zoneService.createSpace(zoneId, type);
            log.info("[Seed]     space id={} type={} zoneId={}", space.getId(), type, zoneId);
        }
    }
}
