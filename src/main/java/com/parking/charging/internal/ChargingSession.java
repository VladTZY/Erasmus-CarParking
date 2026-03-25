package com.parking.charging.internal;

import com.parking.charging.ChargingStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "charging_sessions")
class ChargingSession {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID reservationId;

    @Column(nullable = false)
    private UUID spaceId;

    @Column(nullable = false)
    private UUID citizenId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargingStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Double energyKwh;

    protected ChargingSession() {}

    ChargingSession(UUID id, UUID reservationId, UUID spaceId, UUID citizenId, ChargingStatus status) {
        this.id = id;
        this.reservationId = reservationId;
        this.spaceId = spaceId;
        this.citizenId = citizenId;
        this.status = status;
    }

    UUID getId() { return id; }
    UUID getReservationId() { return reservationId; }
    UUID getSpaceId() { return spaceId; }
    UUID getCitizenId() { return citizenId; }
    ChargingStatus getStatus() { return status; }
    LocalDateTime getStartedAt() { return startedAt; }
    LocalDateTime getCompletedAt() { return completedAt; }
    Double getEnergyKwh() { return energyKwh; }

    void setStatus(ChargingStatus status) { this.status = status; }
    void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    void setEnergyKwh(Double energyKwh) { this.energyKwh = energyKwh; }
}
