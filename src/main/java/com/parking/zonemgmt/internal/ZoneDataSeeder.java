package com.parking.zonemgmt.internal;

import com.parking.zonemgmt.ParkingSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

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

        // Zone A — Westenhellweg (Dortmund city centre shopping area)
        var zoneA = zoneService.createZone("Westenhellweg", "Westenhellweg, 44137 Dortmund");
        createSpaces(zoneA.id(), "WH-", 1, 4, ParkingSpace.SpaceType.REGULAR);
        createSpaces(zoneA.id(), "WH-", 5, 2, ParkingSpace.SpaceType.EV);
        zoneService.updateMapData(zoneA.id(), 51.5136, 7.4653,
                "[[51.5143,7.4640],[51.5143,7.4666],[51.5129,7.4666],[51.5129,7.4640]]");

        // Zone B — Hauptbahnhof (Dortmund main train station)
        var zoneB = zoneService.createZone("Hauptbahnhof", "Königswall 15, 44137 Dortmund");
        createSpaces(zoneB.id(), "HBF-", 1, 3, ParkingSpace.SpaceType.REGULAR);
        createSpaces(zoneB.id(), "HBF-", 4, 2, ParkingSpace.SpaceType.EV);
        zoneService.updateMapData(zoneB.id(), 51.5178, 7.4590,
                "[[51.5185,7.4577],[51.5185,7.4603],[51.5171,7.4603],[51.5171,7.4577]]");

        // Zone C — Westfalenhallen (exhibition & events centre)
        var zoneC = zoneService.createZone("Westfalenhallen", "Rheinlanddamm 200, 44139 Dortmund");
        createSpaces(zoneC.id(), "WFH-", 1, 5, ParkingSpace.SpaceType.REGULAR);
        createSpaces(zoneC.id(), "WFH-", 6, 3, ParkingSpace.SpaceType.EV);
        zoneService.updateMapData(zoneC.id(), 51.5056, 7.4617,
                "[[51.5063,7.4603],[51.5063,7.4631],[51.5049,7.4631],[51.5049,7.4603]]");

        // Zone D — Phoenixsee (lakeside residential & commercial area)
        var zoneD = zoneService.createZone("Phoenixsee", "Phoenix-Seepromenade, 44263 Dortmund");
        createSpaces(zoneD.id(), "PHX-", 1, 2, ParkingSpace.SpaceType.REGULAR);
        createSpaces(zoneD.id(), "PHX-", 3, 2, ParkingSpace.SpaceType.EV);
        zoneService.updateMapData(zoneD.id(), 51.4833, 7.5167,
                "[[51.4840,7.5153],[51.4840,7.5181],[51.4826,7.5181],[51.4826,7.5153]]");

        log.info("[Seed] Zones seeded: Westenhellweg (6), Hauptbahnhof (5), Westfalenhallen (8), Phoenixsee (4)");
    }

    private void createSpaces(UUID zoneId, String prefix, int startIndex, int count, ParkingSpace.SpaceType type) {
        for (int i = 0; i < count; i++) {
            String name = prefix + (startIndex + i);
            var space = zoneService.createSpace(zoneId, name, type);
            log.info("[Seed]   {} — {} ({})", name, type, space.getId());
        }
    }
}
