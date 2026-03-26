package com.parking.billing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceDTO(
        UUID id,
        UUID reservationId,
        String spaceName,
        String zoneName,
        BigDecimal amount,
        InvoiceStatus status,
        LocalDateTime createdAt
) {}
