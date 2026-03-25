package com.parking.billing.internal;

import com.parking.billing.IPaymentGateway;
import com.parking.billing.InvoiceGeneratedEvent;
import com.parking.billing.InvoiceStatus;
import com.parking.pricing.IPricingPolicy;
import com.parking.reservation.ReservationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final InvoiceRepository invoiceRepository;
    private final IPricingPolicy pricingPolicy;
    private final IPaymentGateway paymentGateway;
    private final ApplicationEventPublisher eventPublisher;

    BillingService(InvoiceRepository invoiceRepository,
                   IPricingPolicy pricingPolicy,
                   IPaymentGateway paymentGateway,
                   ApplicationEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.pricingPolicy = pricingPolicy;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
    }

    @ApplicationModuleListener
    @Transactional
    public void on(ReservationCreatedEvent event) {
        var amount = pricingPolicy.calculate(event.spaceId(), event.durationMinutes());
        var description = "Parking reservation %s — %d min".formatted(event.reservationId(), event.durationMinutes());

        var invoice = new Invoice(
                UUID.randomUUID(), event.reservationId(), event.citizenId(),
                amount, description, InvoiceStatus.PENDING, LocalDateTime.now()
        );
        invoice = invoiceRepository.save(invoice);

        var result = paymentGateway.charge(event.citizenId(), amount, description);

        invoice.setStatus(result.success() ? InvoiceStatus.PAID : InvoiceStatus.FAILED);
        invoiceRepository.save(invoice);

        eventPublisher.publishEvent(new InvoiceGeneratedEvent(
                invoice.getId(), event.reservationId(), event.citizenId(), amount
        ));

        log.info("Invoice {} {} for reservation {}", invoice.getId(),
                invoice.getStatus(), event.reservationId());
    }
}
