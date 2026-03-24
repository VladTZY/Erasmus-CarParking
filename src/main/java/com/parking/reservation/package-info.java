/**
 * Reservation module.
 *
 * <p>Citizens search available spaces with price previews and confirm reservations.
 * Depends on {@code IZoneAvailability} (Zone Management) and
 * {@code IPricingPolicy} (Pricing) via exported interfaces.
 * Published events: {@code ReservationCreatedEvent}, {@code ReservationCancelledEvent}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Reservation"
)
package com.parking.reservation;
