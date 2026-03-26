package com.parking.zonemgmt;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Exported interface — owned by the ZoneMgmt module.
 * Implemented by the Reservation module to allow the ZoneMgmt module to check
 * whether a space has any conflicting reservations in a given time window,
 * without creating a reverse dependency.
 */
public interface IReservationAvailability {
    /**
     * Returns true if there is at least one non-cancelled reservation for the
     * given space that overlaps [startTime, endTime).
     */
    boolean hasOverlap(UUID spaceId, LocalDateTime startTime, LocalDateTime endTime);
}
