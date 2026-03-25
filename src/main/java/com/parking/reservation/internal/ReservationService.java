package com.parking.reservation.internal;

import com.parking.pricing.IPricingPolicy;
import com.parking.reservation.*;
import com.parking.zonemgmt.ISpaceStateManager;
import com.parking.zonemgmt.IZoneAvailability;
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
    private final IPricingPolicy pricingPolicy;
    private final ApplicationEventPublisher eventPublisher;

    ReservationService(ReservationRepo repo,
                       IZoneAvailability zoneAvailability,
                       ISpaceStateManager spaceStateManager,
                       IPricingPolicy pricingPolicy,
                       ApplicationEventPublisher eventPublisher) {
        this.repo = repo;
        this.zoneAvailability = zoneAvailability;
        this.spaceStateManager = spaceStateManager;
        this.pricingPolicy = pricingPolicy;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    ReservationEstimateDTO getEstimate(UUID spaceId, LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        if (!zoneAvailability.isSpaceAvailable(spaceId)) {
            throw new SpaceNotAvailableException(spaceId);
        }
        int durationMinutes = (int) ChronoUnit.MINUTES.between(startTime, endTime);
        var estimate = pricingPolicy.estimate(spaceId, durationMinutes);
        return new ReservationEstimateDTO(spaceId, startTime, endTime, durationMinutes,
                estimate.estimatedFee(), estimate.currency(), estimate.ratePerHour());
    }

    @Transactional
    ReservationDTO createReservation(UUID spaceId, UUID citizenId, LocalDateTime startTime,
                                     LocalDateTime endTime, boolean withCharging) {
        validateTimeRange(startTime, endTime);
        if (!zoneAvailability.isSpaceAvailable(spaceId)) {
            throw new SpaceNotAvailableException(spaceId);
        }
        int durationMinutes = (int) ChronoUnit.MINUTES.between(startTime, endTime);
        var estimate = pricingPolicy.estimate(spaceId, durationMinutes);

        var reservation = new Reservation(
                UUID.randomUUID(), spaceId, citizenId, startTime, endTime,
                estimate.estimatedFee(), withCharging, ReservationStatus.CONFIRMED
        );
        reservation = repo.save(reservation);

        spaceStateManager.markReserved(spaceId);

        eventPublisher.publishEvent(new ReservationCreatedEvent(
                reservation.getId(), spaceId, citizenId,
                estimate.estimatedFee(), durationMinutes, withCharging
        ));

        return toDTO(reservation);
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

        spaceStateManager.markFree(reservation.getSpaceId());

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

    @Transactional(readOnly = true)
    List<ReservationEstimateDTO> searchAvailable(UUID zoneId, LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        int durationMinutes = (int) ChronoUnit.MINUTES.between(startTime, endTime);
        return zoneAvailability.findAvailableSpaces(zoneId).stream()
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
        return new ReservationDTO(r.getId(), r.getSpaceId(), r.getCitizenId(),
                r.getStartTime(), r.getEndTime(), duration,
                r.getEstimatedFee(), r.isWithCharging(), r.getStatus());
    }
}
