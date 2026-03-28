package com.homecare.tracking.controller;

import com.homecare.tracking.dto.LocationBroadcast;
import com.homecare.tracking.dto.LocationUpdate;
import com.homecare.tracking.entity.LocationHistory;
import com.homecare.tracking.service.TrackingService;
import com.homecare.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TrackingController {

    private final TrackingService trackingService;

    // ─── STOMP: Helper publishes location update ──────────────────────

    @MessageMapping("/location/update")
    public void updateLocation(@Payload LocationUpdate update,
                               SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        if (attrs == null || attrs.get("userId") == null) {
            log.warn("Location update received without authenticated session");
            return;
        }

        UUID helperId = (UUID) attrs.get("userId");
        String role = (String) attrs.get("role");

        if (!"HELPER".equals(role)) {
            log.warn("Non-helper user {} attempted to send location update", helperId);
            return;
        }

        try {
            trackingService.processLocationUpdate(helperId, update);
        } catch (Exception e) {
            log.error("Error processing location update from helper {}: {}", helperId, e.getMessage());
        }
    }

    // ─── REST: Get latest location for a booking ──────────────────────

    @GetMapping("/api/v1/tracking/{bookingId}/latest")
    @ResponseBody
    public ResponseEntity<LocationBroadcast> getLatestLocation(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        LocationBroadcast latest = trackingService.getLatestLocation(bookingId);
        if (latest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latest);
    }

    // ─── REST: Get full location history for a booking ────────────────

    @GetMapping("/api/v1/tracking/{bookingId}/history")
    @ResponseBody
    public ResponseEntity<List<LocationHistory>> getLocationHistory(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<LocationHistory> history = trackingService.getLocationHistory(bookingId);
        return ResponseEntity.ok(history);
    }
}

