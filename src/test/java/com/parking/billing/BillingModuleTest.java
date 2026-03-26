package com.parking.billing;

import com.parking.pricing.IPricingPolicy;
import com.parking.reservation.IReservationRepo;
import com.parking.reservation.ReservationCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Module-scoped test for the Billing module.
 * Covers the ReservationCreatedEvent → InvoiceGeneratedEvent flow.
 * MockPaymentGateway (billing.internal) is loaded automatically and always succeeds.
 * Charging invoice lifecycle is tested in BillingChargingListenerTest (billing.internal).
 */
@ApplicationModuleTest
class BillingModuleTest {

    @MockBean IPricingPolicy pricingPolicy;
    @MockBean IReservationRepo reservationRepo;
    @Autowired IInvoiceRepository invoiceRepository;
    @Autowired ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUpPricingMock() {
        when(pricingPolicy.calculate(any(UUID.class), anyInt()))
                .thenReturn(new BigDecimal("3.00"));
    }

    @Test
    void onReservationCreated_createsAndPaysReservationInvoice(Scenario scenario) {
        var reservationId = UUID.randomUUID();
        var spaceId = UUID.randomUUID();
        var citizenId = UUID.randomUUID();
        var event = new ReservationCreatedEvent(
                reservationId, spaceId, citizenId, new BigDecimal("3.00"), 60, false);

        scenario.stimulate(() -> eventPublisher.publishEvent(event))
                .andWaitForEventOfType(InvoiceGeneratedEvent.class)
                .toArriveAndVerify(e -> {
                    assertThat(e.citizenId()).isEqualTo(citizenId);
                    assertThat(e.reservationId()).isEqualTo(reservationId);
                    assertThat(e.amount()).isEqualByComparingTo(new BigDecimal("3.00"));

                    var invoices = invoiceRepository.findByCitizenId(citizenId);
                    assertThat(invoices).hasSize(1);
                    assertThat(invoices.get(0).status()).isEqualTo(InvoiceStatus.PAID);
                    assertThat(invoices.get(0).invoiceType()).isEqualTo(InvoiceType.RESERVATION);
                });
    }

    @Test
    void onReservationCreated_withCharging_createsOnlyOneReservationInvoice(Scenario scenario) {
        var reservationId = UUID.randomUUID();
        var citizenId = UUID.randomUUID();
        var event = new ReservationCreatedEvent(
                reservationId, UUID.randomUUID(), citizenId, new BigDecimal("2.00"), 120, true);

        // BillingService creates RESERVATION invoice; the charging invoice comes on ChargingStartedEvent
        scenario.stimulate(() -> eventPublisher.publishEvent(event))
                .andWaitForEventOfType(InvoiceGeneratedEvent.class)
                .toArriveAndVerify(e -> {
                    var invoices = invoiceRepository.findByCitizenId(citizenId);
                    assertThat(invoices).hasSize(1);
                    assertThat(invoices.get(0).invoiceType()).isEqualTo(InvoiceType.RESERVATION);
                    assertThat(invoices.get(0).status()).isEqualTo(InvoiceStatus.PAID);
                });
    }
}
