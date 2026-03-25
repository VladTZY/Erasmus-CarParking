package com.parking.zonemgmt;

import java.util.UUID;

/** Read model for a parking zone, including live availability count and map data. */
public record ParkingZoneDTO(
        UUID id,
        String name,
        String address,
        int totalCapacity,
        long availableCount,
        Double latitude,
        Double longitude,
        String boundary
) {}
