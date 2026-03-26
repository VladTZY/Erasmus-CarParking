package com.parking.reservation.internal;

import com.parking.pricing.IPricingPolicy;
import com.parking.reservation.*;
import com.parking.zonemgmt.ISpaceQuery;
import com.parking.zonemgmt.ISpaceStateManager;
import com.parking.zonemgmt.IZoneAvailability;
import com.parking.zonemgmt.IZoneQuery;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
class ReservationService {

    private final ReservationRepo repo;
    private final IZoneAvailability zoneAvailability;
    private final ISpaceStateManager spaceStateManager;
    private final ISpaceQuery spaceQuery;
    private final IZoneQuery zoneQuery;
    private final IPricingPolicy pricingPolicy;
    private final ApplicationEventPublisher eventPublisher;

    ReservationService(ReservationRepo repo,
                       IZoneAvailability zoneAvailability,
                       ISpaceStateManager spaceStateManager,
                       @Lazy ISpaceQuery spaceQuery,
                       @Lazy IZoneQuery zoneQuery,
                       IPricingPolicy pricingPolicy,
                       ApplicationEventPublisher eventPublisher) {
        this.repo = repo;
        this.zoneAvailability = zoneAvailability;
        this.spaceStateManager = spaceStateManager;
        this.spaceQuery = spaceQuery;
        this.zoneQuery = zoneQuery;
        this.pricingPolicy = pricingPolicy;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    ReservationEstimateDTO getEstimate(UUID spaceId, LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        if (repo.hasOverlap(spaceId, startTime, endTime)) {
            throw new SpaceNotAvailableException(spaceId);
        }
        int durationMinutes = (int) ChronoUnit.MINUTES.between(startTime, endTime);
        var estimate = pricingPolicy.estimate(spaceId, durationMinutes);
        return new ReservationEstimateDTO(spaceId, startTime, endTime, durationMinutes,
                estimate.estimatedFee(), estimate.currency(), estimate.ratePerHour());
    }

    @Transactional
    ReservationDTO createReservation(UUID spaceId, UUID citizenId, LocalDateTime startTime,
                                     LocalDateTime endTime, boolean withCharging, String licensePlate) {
        validateTimeRange(startTime, endTime);
        // Acquire a pessimistic write lock on the space row first — serializes concurrent requests
        // for the same space and prevents double-booking races without changing the displayed state.
        spaceStateManager.lockSpace(spaceId);
        if (repo.hasOverlap(spaceId, startTime, endTime)) {
            throw new SpaceNotAvailableException(spaceId);
        }
        int durationMinutes = (int) ChronoUnit.MINUTES.between(startTime, endTime);
        var estimate = pricingPolicy.estimate(spaceId, durationMinutes);

        var reservation = new Reservation(
                UUID.randomUUID(), spaceId, citizenId, startTime, endTime,
                estimate.estimatedFee(), withCharging, licensePlate, ReservationStatus.CONFIRMED
        );
        reservation = repo.save(reservation);

        eventPublisher.publishEvent(new ReservationCreatedEvent(
                reservation.getId(), spaceId, citizenId,
                estimate.estimatedFee(), durationMinutes, withCharging
        ));

        return toDTO(reservation);
    }

    /** Returns all non-cancelled reservations for a space (for schedule display). */
    @Transactional(readOnly = true)
    List<ReservationDTO> getSchedule(UUID spaceId) {
        return repo.findScheduleBySpaceId(spaceId);
    }

    @Transactional
    void cancelReservation(UUID reservationId, UUID citizenId) {
        var reservation = repo.findEntityById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (!reservation.getCitizenId().equals(citizenId)) {
            throw new IllegalStateException("Not authorized to cancel this reservation");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        repo.save(reservation);

        eventPublisher.publishEvent(new ReservationCancelledEvent(
                reservationId, reservation.getSpaceId(), reservation.getCitizenId()
        ));
    }

    @Transactional(readOnly = true)
    ReservationDTO getById(UUID reservationId) {
        return repo.findEntityById(reservationId)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
    }

    @Transactional(readOnly = true)
    List<ReservationDTO> getByCitizenId(UUID citizenId) {
        return repo.findEntitiesByCitizenId(citizenId).stream().map(this::toDTO).toList();
    }

    /** Returns space IDs in the zone that have no overlapping reservations in [startTime, endTime]. */
    @Transactional(readOnly = true)
    List<ReservationEstimateDTO> searchAvailable(UUID zoneId, LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        int durationMinutes = (int) ChronoUnit.MINUTES.between(startTime, endTime);
        return zoneAvailability.findAllSpaceIds(zoneId).stream()
                .filter(spaceId -> !repo.hasOverlap(spaceId, startTime, endTime))
                .map(spaceId -> {
                    var estimate = pricingPolicy.estimate(spaceId, durationMinutes);
                    return new ReservationEstimateDTO(spaceId, startTime, endTime, durationMinutes,
                            estimate.estimatedFee(), estimate.currency(), estimate.ratePerHour());
                })
                .toList();
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidTimeRangeException("End time must be after start time");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new InvalidTimeRangeException("Start time must be in the future");
        }
    }

    private ReservationDTO toDTO(Reservation r) {
        int duration = (int) ChronoUnit.MINUTES.between(r.getStartTime(), r.getEndTime());
        var space = spaceQuery.findSpace(r.getSpaceId());
        String spaceName = space.map(s -> s.getName()).orElse(null);
        String zoneName = space.flatMap(s -> zoneQuery.findZoneById(s.getZoneId()))
                .map(z -> z.name()).orElse(null);
        return new ReservationDTO(r.getId(), r.getSpaceId(), spaceName, zoneName, r.getCitizenId(),
                r.getStartTime(), r.getEndTime(), duration,
                r.getEstimatedFee(), r.isWithCharging(), r.getStatus(), r.getLicensePlate());
    }
}
