package com.parking.reservation.internal;

class SpaceNotAvailableException extends RuntimeException {
    SpaceNotAvailableException(java.util.UUID spaceId) {
        super("Space is not available: " + spaceId);
    }
}
