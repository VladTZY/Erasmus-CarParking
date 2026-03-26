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

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String boundary;

    protected ParkingZone() {}

    public ParkingZone(UUID id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getBoundary() { return boundary; }

    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setBoundary(String boundary) { this.boundary = boundary; }
}
