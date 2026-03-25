package com.parking.billing.internal;

import com.parking.billing.IPaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
class MockPaymentGateway implements IPaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    @Override
    public PaymentResult charge(UUID citizenId, BigDecimal amount, String description) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String transactionId = UUID.randomUUID().toString();
        log.info("Mock payment charged €{} for citizen {} — txn: {}", amount, citizenId, transactionId);
        return new PaymentResult(true, transactionId);
    }
}
