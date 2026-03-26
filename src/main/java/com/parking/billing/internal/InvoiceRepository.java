package com.parking.billing.internal;

import com.parking.billing.IInvoiceRepository;
import com.parking.billing.InvoiceDTO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class InvoiceRepository implements IInvoiceRepository {

    private final InvoiceJpaRepo jpa;

    InvoiceRepository(InvoiceJpaRepo jpa) {
        this.jpa = jpa;
    }

    // Package-private — for use by BillingService only
    Invoice save(Invoice invoice) {
        return jpa.save(invoice);
    }

    Optional<Invoice> findEntityById(UUID id) {
        return jpa.findById(id);
    }

    // IInvoiceRepository — public contract for other modules

    @Override
    public Optional<InvoiceDTO> findById(UUID id) {
        return jpa.findById(id).map(this::toDTO);
    }

    @Override
    public List<InvoiceDTO> findByCitizenId(UUID citizenId) {
        return jpa.findByCitizenId(citizenId).stream().map(this::toDTO).toList();
    }

    @Override
    public List<InvoiceDTO> findAll() {
        return jpa.findAll().stream().map(this::toDTO).toList();
    }

    Optional<Invoice> findPendingChargingInvoice(UUID reservationId) {
        return jpa.findByReservationId(reservationId).stream()
                .filter(i -> i.getInvoiceType() == com.parking.billing.InvoiceType.CHARGING
                        && i.getStatus() == com.parking.billing.InvoiceStatus.PENDING)
                .findFirst();
    }

    private InvoiceDTO toDTO(Invoice i) {
        return new InvoiceDTO(i.getId(), i.getReservationId(), null, null,
                i.getAmount(), i.getStatus(), i.getInvoiceType(), i.getCreatedAt());
    }
}
