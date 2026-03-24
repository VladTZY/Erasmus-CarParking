package com.parking.zonemgmt;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "parking_spaces")
public class ParkingSpace {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID zoneId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceState state;

    public enum SpaceType { REGULAR, EV }
    public enum SpaceState { FREE, RESERVED, OCCUPIED }

    protected ParkingSpace() {}

    public ParkingSpace(UUID id, UUID zoneId, SpaceType type, SpaceState state) {
        this.id = id;
        this.zoneId = zoneId;
        this.type = type;
        this.state = state;
    }

    public UUID getId() { return id; }
    public UUID getZoneId() { return zoneId; }
    public SpaceType getType() { return type; }
    public SpaceState getState() { return state; }

    public void setState(SpaceState state) { this.state = state; }
    public void setType(SpaceType type) { this.type = type; }
}
