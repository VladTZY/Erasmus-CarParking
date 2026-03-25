package com.parking.reservation;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Exported interface — allows the ZoneMgmt module to check whether a space
 * has any conflicting reservations in a given time window, without depending
 * on internal reservation classes.
 */
public interface IReservationAvailability {
    /**
     * Returns true if there is at least one non-cancelled reservation for the
     * given space that overlaps [startTime, endTime).
     */
    boolean hasOverlap(UUID spaceId, LocalDateTime startTime, LocalDateTime endTime);
}
