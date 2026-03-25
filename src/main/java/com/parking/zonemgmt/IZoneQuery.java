package com.parking.zonemgmt;

import java.util.List;

/**
 * Exported interface — allows other modules (e.g. Pricing) to list zones
 * without depending on internal zone management classes.
 */
public interface IZoneQuery {
    List<ParkingZoneDTO> findAllZones();
}
