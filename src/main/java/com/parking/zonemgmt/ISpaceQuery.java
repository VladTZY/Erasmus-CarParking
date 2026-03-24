package com.parking.zonemgmt;

import java.util.Optional;
import java.util.UUID;

/**
 * Exported interface — allows other modules (e.g. Pricing) to look up space details
 * without depending on internal zone management classes.
 */
public interface ISpaceQuery {
    Optional<ParkingSpace> findSpace(UUID spaceId);
}
