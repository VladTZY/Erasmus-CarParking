package com.parking.zonemgmt;

import java.util.UUID;

/**
 * Published when a parking space transitions to a new state.
 * The Notification module listens for OCCUPIED transitions to log anomalies.
 */
public record SpaceStateChangedEvent(UUID spaceId, ParkingSpace.SpaceState newState) {}
