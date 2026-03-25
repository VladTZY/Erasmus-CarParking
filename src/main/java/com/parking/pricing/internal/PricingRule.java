package com.parking.pricing.internal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pricing_rules")
public class PricingRule {

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

    public UUID getId() { return id; }
    public UUID getZoneId() { return zoneId; }
    public SpaceType getSpaceType() { return spaceType; }
    public BigDecimal getRatePerHour() { return ratePerHour; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public LocalDateTime getValidTo() { return validTo; }

    public void setZoneId(UUID zoneId) { this.zoneId = zoneId; }
    public void setSpaceType(SpaceType spaceType) { this.spaceType = spaceType; }
    public void setRatePerHour(BigDecimal ratePerHour) { this.ratePerHour = ratePerHour; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
}
