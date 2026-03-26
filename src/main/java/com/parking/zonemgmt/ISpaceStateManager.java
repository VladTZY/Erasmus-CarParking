package com.parking.zonemgmt;

import java.util.UUID;

/**
 * Exported interface — allows the Reservation module to update space states
 * without depending on internal zone management classes.
 */
public interface ISpaceStateManager {
    /**
     * Acquires a pessimistic write lock on the space row for the duration of the
     * current transaction. Used to serialise concurrent reservation creation for
     * the same space without changing the space's displayed state.
     */
    void lockSpace(UUID spaceId);
    void markReserved(UUID spaceId);
    void markFree(UUID spaceId);
}
