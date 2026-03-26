package com.parking.billing.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface InvoiceJpaRepo extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByCitizenId(UUID citizenId);
    List<Invoice> findByReservationId(UUID reservationId);
}
