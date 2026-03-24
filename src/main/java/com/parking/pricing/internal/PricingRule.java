package com.parking.pricing.internal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pricing_rules")
class PricingRule {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID zoneId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceType spaceType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerHour;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column
    private LocalDateTime validTo;

    public enum SpaceType { REGULAR, EV, ALL }

    protected PricingRule() {}

    PricingRule(UUID id, UUID zoneId, SpaceType spaceType, BigDecimal ratePerHour,
                LocalDateTime validFrom, LocalDateTime validTo) {
        this.id = id;
        this.zoneId = zoneId;
        this.spaceType = spaceType;
        this.ratePerHour = ratePerHour;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    UUID getId() { return id; }
    UUID getZoneId() { return zoneId; }
    SpaceType getSpaceType() { return spaceType; }
    BigDecimal getRatePerHour() { return ratePerHour; }
    LocalDateTime getValidFrom() { return validFrom; }
    LocalDateTime getValidTo() { return validTo; }

    void setZoneId(UUID zoneId) { this.zoneId = zoneId; }
    void setSpaceType(SpaceType spaceType) { this.spaceType = spaceType; }
    void setRatePerHour(BigDecimal ratePerHour) { this.ratePerHour = ratePerHour; }
    void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
}
