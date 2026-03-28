package com.homecare.tracking.service;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.core.util.GeoUtils;
import com.homecare.tracking.dto.LocationBroadcast;
import com.homecare.tracking.dto.LocationUpdate;
import com.homecare.tracking.dto.StatusEvent;
import com.homecare.tracking.entity.LocationHistory;
import com.homecare.tracking.repository.LocationHistoryRepository;
import com.homecare.user.repository.HelperProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final LocationHistoryRepository locationHistoryRepository;
    private final BookingRepository bookingRepository;
    private final HelperProfileRepository helperProfileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final Set<BookingStatus> TRACKABLE_STATUSES =
            Set.of(BookingStatus.HELPER_EN_ROUTE, BookingStatus.IN_PROGRESS);

    private static final double DEFAULT_SPEED_KMH = 25.0;

    // ─── Process location update from helper ──────────────────────────

    @Transactional
    public void processLocationUpdate(UUID helperId, LocationUpdate update) {
        Booking booking = bookingRepository.findById(update.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", update.getBookingId()));

        // Validate helper ownership
        if (booking.getHelper() == null || !booking.getHelper().getId().equals(helperId)) {
            throw new BusinessException("You are not the assigned helper for this booking",
                    ErrorCode.TRACKING_NOT_ALLOWED);
        }

        // Validate booking is in a trackable status
        if (!TRACKABLE_STATUSES.contains(booking.getStatus())) {
            throw new BusinessException("Location tracking not allowed for status: " + booking.getStatus(),
                    ErrorCode.TRACKING_NOT_ALLOWED);
        }

        // Persist location history for audit trail
        LocationHistory history = LocationHistory.builder()
                .booking(booking)
                .helper(booking.getHelper())
                .latitude(update.getLat())
                .longitude(update.getLng())
                .accuracy(update.getAccuracy())
                .heading(update.getHeading())
                .speedKmh(update.getSpeedKmh())
                .recordedAt(Instant.now())
                .build();
        locationHistoryRepository.save(history);

        // Update helper profile with latest coordinates
        helperProfileRepository.findByUserId(helperId).ifPresent(profile -> {
            profile.setLatitude(update.getLat());
            profile.setLongitude(update.getLng());
            profile.setLastLocationUpdate(Instant.now());
            helperProfileRepository.save(profile);
        });

        // Compute ETA
        String eta = computeEta(update.getLat(), update.getLng(),
                booking.getLatitude(), booking.getLongitude(),
                update.getSpeedKmh());

        // Build broadcast DTO
        LocationBroadcast broadcast = LocationBroadcast.of(
                helperId, booking.getId(),
                update.getLat(), update.getLng(),
                update.getHeading(), update.getSpeedKmh(),
                eta);

        // Broadcast to subscribers on booking-specific topic
        messagingTemplate.convertAndSend(
                "/topic/booking/" + booking.getId() + "/location",
                broadcast);

        // Also broadcast to admin live feed
        messagingTemplate.convertAndSend("/topic/admin/bookings", broadcast);

        log.debug("Location update for booking {} from helper {}: [{}, {}] ETA={}",
                booking.getId(), helperId, update.getLat(), update.getLng(), eta);
    }

    // ─── Broadcast status change ──────────────────────────────────────

    public void broadcastStatusChange(UUID bookingId, BookingStatus newStatus, String message) {
        StatusEvent event = StatusEvent.of(bookingId, newStatus, message);
        messagingTemplate.convertAndSend(
                "/topic/booking/" + bookingId + "/status",
                event);
        log.debug("Status broadcast for booking {}: {}", bookingId, newStatus);
    }

    // ─── Get location history for a booking ───────────────────────────

    @Transactional(readOnly = true)
    public List<LocationHistory> getLocationHistory(UUID bookingId) {
        bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        return locationHistoryRepository.findByBookingIdOrderByRecordedAtAsc(bookingId);
    }

    // ─── Get latest location for a booking ────────────────────────────

    @Transactional(readOnly = true)
    public LocationBroadcast getLatestLocation(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        return locationHistoryRepository.findTopByBookingIdOrderByRecordedAtDesc(bookingId)
                .map(lh -> {
                    String eta = computeEta(lh.getLatitude(), lh.getLongitude(),
                            booking.getLatitude(), booking.getLongitude(),
                            lh.getSpeedKmh());
                    return LocationBroadcast.builder()
                            .helperId(lh.getHelper().getId())
                            .bookingId(bookingId)
                            .lat(lh.getLatitude())
                            .lng(lh.getLongitude())
                            .timestamp(lh.getRecordedAt())
                            .heading(lh.getHeading())
                            .speedKmh(lh.getSpeedKmh())
                            .etaMinutes(eta)
                            .build();
                })
                .orElse(null);
    }

    // ─── Cleanup old records (retain 30 days) ─────────────────────────

    @Transactional
    public int purgeOldRecords() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        int deleted = locationHistoryRepository.deleteOlderThan(cutoff);
        log.info("Purged {} location history records older than 30 days", deleted);
        return deleted;
    }

    // ─── ETA computation (Haversine straight-line stub) ───────────────

    public String computeEta(double helperLat, double helperLng,
                              double destLat, double destLng,
                              double speedKmh) {
        double distanceKm = GeoUtils.haversineDistance(helperLat, helperLng, destLat, destLng);
        double effectiveSpeed = speedKmh > 0 ? speedKmh : DEFAULT_SPEED_KMH;
        int minutes = (int) Math.ceil(distanceKm / effectiveSpeed * 60);
        if (minutes <= 0) return "< 1 min";
        return minutes + " min";
    }
}

