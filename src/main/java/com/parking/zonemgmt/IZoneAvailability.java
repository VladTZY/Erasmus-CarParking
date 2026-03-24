package com.parking.zonemgmt;

import java.util.List;
import java.util.UUID;

/**
 * Exported interface — allows the Reservation module to check space availability
 * without depending on internal zone management classes.
 */
public interface IZoneAvailability {
    boolean isSpaceAvailable(UUID spaceId);
    List<UUID> findAvailableSpaces(UUID zoneId);
}
