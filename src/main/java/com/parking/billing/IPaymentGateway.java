package com.parking.billing;

import java.math.BigDecimal;
import java.util.UUID;

public interface IPaymentGateway {
    PaymentResult charge(UUID citizenId, BigDecimal amount, String description);

    record PaymentResult(boolean success, String transactionId) {}
}
