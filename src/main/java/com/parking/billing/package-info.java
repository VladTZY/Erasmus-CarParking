/**
 * Billing module.
 *
 * <p>Generates invoices and processes mock payments. Listens to
 * {@code ReservationCreatedEvent} and {@code ChargingStartedEvent}.
 * Depends on {@code IPricingPolicy} (Pricing) for final fee calculation.
 * Published events: {@code InvoiceGeneratedEvent}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Billing"
)
package com.parking.billing;
