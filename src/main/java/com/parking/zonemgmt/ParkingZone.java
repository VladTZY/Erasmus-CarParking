package com.parking.zonemgmt;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "parking_zones")
public class ParkingZone {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private int totalCapacity;

    protected ParkingZone() {}

    public ParkingZone(UUID id, String name, String address, int totalCapacity) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.totalCapacity = totalCapacity;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public int getTotalCapacity() { return totalCapacity; }

    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }
}
