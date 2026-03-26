package com.parking.reservation.internal;

import com.parking.reservation.ReservationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservations")
class Reservation {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID spaceId;

    @Column(nullable = false)
    private UUID citizenId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedFee;

    @Column(nullable = false)
    private boolean withCharging;

    @Column
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    protected Reservation() {}

    Reservation(UUID id, UUID spaceId, UUID citizenId, LocalDateTime startTime, LocalDateTime endTime,
                BigDecimal estimatedFee, boolean withCharging, String licensePlate, ReservationStatus status) {
        this.id = id;
        this.spaceId = spaceId;
        this.citizenId = citizenId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.estimatedFee = estimatedFee;
        this.withCharging = withCharging;
        this.licensePlate = licensePlate;
        this.status = status;
    }

    UUID getId() { return id; }
    UUID getSpaceId() { return spaceId; }
    UUID getCitizenId() { return citizenId; }
    LocalDateTime getStartTime() { return startTime; }
    LocalDateTime getEndTime() { return endTime; }
    BigDecimal getEstimatedFee() { return estimatedFee; }
    boolean isWithCharging() { return withCharging; }
    String getLicensePlate() { return licensePlate; }
    ReservationStatus getStatus() { return status; }

    void setStatus(ReservationStatus status) { this.status = status; }
}
