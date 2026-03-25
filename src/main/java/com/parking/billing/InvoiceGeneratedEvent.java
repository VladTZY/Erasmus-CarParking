package com.parking.billing;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceGeneratedEvent(
        UUID invoiceId,
        UUID reservationId,
        UUID citizenId,
        BigDecimal amount
) {}
