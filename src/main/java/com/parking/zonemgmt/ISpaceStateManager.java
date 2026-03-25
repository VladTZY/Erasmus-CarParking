package com.parking.zonemgmt;

import java.util.UUID;

/**
 * Exported interface — allows the Reservation module to update space states
 * without depending on internal zone management classes.
 */
public interface ISpaceStateManager {
    void markReserved(UUID spaceId);
    void markFree(UUID spaceId);
}
