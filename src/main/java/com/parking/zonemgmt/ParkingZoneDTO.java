package com.parking.zonemgmt;

import java.util.UUID;

/** Read model for a parking zone, including live availability count. */
public record ParkingZoneDTO(UUID id, String name, String address, int totalCapacity, long availableCount) {}
