package com.parking.billing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Exported interface — allows other modules (e.g. Notification) to query invoices
 * without depending on internal billing classes. Writes go through BillingService only.
 */
public interface IInvoiceRepository {
    Optional<InvoiceDTO> findById(UUID id);
    List<InvoiceDTO> findByCitizenId(UUID citizenId);
    List<InvoiceDTO> findAll();
}
