package com.parking.billing.internal;

import com.parking.billing.InvoiceStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices")
class Invoice {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID reservationId;

    @Column(nullable = false)
    private UUID citizenId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Invoice() {}

    Invoice(UUID id, UUID reservationId, UUID citizenId, BigDecimal amount,
            String description, InvoiceStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.citizenId = citizenId;
        this.amount = amount;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    UUID getReservationId() { return reservationId; }
    UUID getCitizenId() { return citizenId; }
    BigDecimal getAmount() { return amount; }
    String getDescription() { return description; }
    InvoiceStatus getStatus() { return status; }
    LocalDateTime getCreatedAt() { return createdAt; }

    void setStatus(InvoiceStatus status) { this.status = status; }
}
