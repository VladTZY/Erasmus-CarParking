package com.parking.zonemgmt.internal;

import com.parking.reservation.ReservationCancelledEvent;
import com.parking.reservation.ReservationCreatedEvent;
import com.parking.zonemgmt.ParkingSpace;
import com.parking.zonemgmt.SpaceStateChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
class SpaceStateService {

    private final ZoneService zoneService;
    private final ApplicationEventPublisher eventPublisher;

    SpaceStateService(ZoneService zoneService, ApplicationEventPublisher eventPublisher) {
        this.zoneService = zoneService;
        this.eventPublisher = eventPublisher;
    }

    @ApplicationModuleListener
    void on(ReservationCreatedEvent event) {
        zoneService.changeSpaceState(event.spaceId(), ParkingSpace.SpaceState.RESERVED);
        eventPublisher.publishEvent(new SpaceStateChangedEvent(event.spaceId(), ParkingSpace.SpaceState.RESERVED));
    }

    @ApplicationModuleListener
    void on(ReservationCancelledEvent event) {
        zoneService.changeSpaceState(event.spaceId(), ParkingSpace.SpaceState.FREE);
        eventPublisher.publishEvent(new SpaceStateChangedEvent(event.spaceId(), ParkingSpace.SpaceState.FREE));
    }
}
