/**
 * Zone Management module.
 *
 * <p>Manages parking zones and spaces. Exports {@code IZoneAvailability} for use
 * by the Reservation module. Listens to {@code ReservationCreatedEvent} and
 * {@code ReservationCancelledEvent} to keep space states consistent.
 * Published events: {@code SpaceStateChangedEvent}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Zone Management"
)
package com.parking.zonemgmt;
