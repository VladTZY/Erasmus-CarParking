/**
 * Charging module.
 *
 * <p>Manages EV charging sessions alongside parking reservations.
 * Listens to {@code ReservationCreatedEvent} (creates pending session when
 * {@code withCharging=true}) and {@code ReservationCancelledEvent} (releases slot).
 * Published events: {@code ChargingStartedEvent}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Charging"
)
package com.parking.charging;
