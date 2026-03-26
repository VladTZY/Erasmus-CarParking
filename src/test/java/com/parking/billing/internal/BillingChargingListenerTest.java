package com.parking.billing.internal;

import com.parking.billing.IInvoiceRepository;
import com.parking.billing.InvoiceStatus;
import com.parking.billing.InvoiceType;
import com.parking.charging.ChargingCompletedEvent;
import com.parking.charging.ChargingStartedEvent;
import com.parking.pricing.IPricingPolicy;
import com.parking.reservation.IReservationRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Internal billing module test covering the EV charging invoice lifecycle.
 * Placed in billing.internal to directly access InvoiceRepository (package-private).
 */
@ApplicationModuleTest
class BillingChargingListenerTest {

    @MockBean IPricingPolicy pricingPolicy;
    @MockBean IReservationRepo reservationRepo;

    @Autowired IInvoiceRepository publicInvoiceRepo;
    @Autowired ApplicationEventPublisher eventPublisher;

    @Test
    void onChargingStarted_createsPendingChargingInvoiceWithZeroAmount(Scenario scenario) {
        var sessionId = UUID.randomUUID();
        var reservationId = UUID.randomUUID();
        var spaceId = UUID.randomUUID();
        var citizenId = UUID.randomUUID();

        scenario.stimulate(() ->
                eventPublisher.publishEvent(new ChargingStartedEvent(sessionId, reservationId, spaceId, citizenId)))
            .andWaitForStateChange(() -> publicInvoiceRepo.findByCitizenId(citizenId), list -> !list.isEmpty())
            .andVerify(invoices -> {
                assertThat(invoices).hasSize(1);
                assertThat(invoices.get(0).status()).isEqualTo(InvoiceStatus.PENDING);
                assertThat(invoices.get(0).invoiceType()).isEqualTo(InvoiceType.CHARGING);
                assertThat(invoices.get(0).amount()).isEqualByComparingTo(BigDecimal.ZERO);
            });
    }

    @Test
    void onChargingCompleted_updatesInvoiceAmountAndMarksPaid(Scenario scenario) {
        var sessionId = UUID.randomUUID();
        var reservationId = UUID.randomUUID();
        var spaceId = UUID.randomUUID();
        var citizenId = UUID.randomUUID();
        double energyKwh = 10.0; // 10 kWh × €0.30 = €3.00

        // 1. Create PENDING invoice via ChargingStartedEvent
        scenario.stimulate(() ->
                eventPublisher.publishEvent(new ChargingStartedEvent(sessionId, reservationId, spaceId, citizenId)))
            .andWaitForStateChange(() -> publicInvoiceRepo.findByCitizenId(citizenId), list -> !list.isEmpty())
            .andVerify(invoices -> assertThat(invoices).hasSize(1));

        // 2. Complete the session — invoice should become PAID with correct amount
        scenario.stimulate(() ->
                eventPublisher.publishEvent(new ChargingCompletedEvent(sessionId, reservationId, citizenId, energyKwh)))
            .andWaitForStateChange(
                    () -> publicInvoiceRepo.findByCitizenId(citizenId),
                    invoices -> !invoices.isEmpty() && invoices.get(0).status() == InvoiceStatus.PAID)
            .andVerify(invoices -> {
                assertThat(invoices).hasSize(1);
                var invoice = invoices.get(0);
                assertThat(invoice.status()).isEqualTo(InvoiceStatus.PAID);
                assertThat(invoice.invoiceType()).isEqualTo(InvoiceType.CHARGING);
                assertThat(invoice.amount()).isEqualByComparingTo(new BigDecimal("3.00")); // 10 × €0.30
            });
    }

    @Test
    void onChargingCompleted_withNoMatchingInvoice_doesNotThrow(Scenario scenario) {
        var sessionId = UUID.randomUUID();
        var reservationId = UUID.randomUUID();
        var citizenId = UUID.randomUUID();

        // No ChargingStartedEvent was processed before — service should handle gracefully
        scenario.stimulate(() ->
                eventPublisher.publishEvent(new ChargingCompletedEvent(sessionId, reservationId, citizenId, 5.0)))
            .andWaitForStateChange(() -> publicInvoiceRepo.findByCitizenId(citizenId), List::isEmpty)
            .andVerify(invoices -> assertThat(invoices).isEmpty());
    }
}
