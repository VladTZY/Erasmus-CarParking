package com.parking.billing.internal;

import com.parking.billing.InvoiceDTO;
import com.parking.reservation.IReservationRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
class BillingController {

    private final InvoiceRepository invoiceRepository;
    private final IReservationRepo reservationRepo;

    BillingController(InvoiceRepository invoiceRepository, IReservationRepo reservationRepo) {
        this.invoiceRepository = invoiceRepository;
        this.reservationRepo = reservationRepo;
    }

    @GetMapping("/invoices/my")
    List<InvoiceDTO> getMy(Authentication auth) {
        UUID citizenId = (UUID) auth.getPrincipal();
        return invoiceRepository.findByCitizenId(citizenId).stream().map(this::enrich).toList();
    }

    @GetMapping("/invoices/{id}")
    ResponseEntity<InvoiceDTO> getById(@PathVariable UUID id) {
        return invoiceRepository.findById(id)
                .map(this::enrich)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasRole('ADMIN')")
    List<InvoiceDTO> getAll() {
        return invoiceRepository.findAll().stream().map(this::enrich).toList();
    }

    private InvoiceDTO enrich(InvoiceDTO dto) {
        return reservationRepo.findById(dto.reservationId())
                .map(r -> new InvoiceDTO(dto.id(), dto.reservationId(), r.spaceName(), r.zoneName(),
                        dto.amount(), dto.status(), dto.invoiceType(), dto.createdAt()))
                .orElse(dto);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<java.util.Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(404).body(java.util.Map.of("error", ex.getMessage()));
    }
}
