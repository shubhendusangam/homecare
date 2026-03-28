package com.homecare.tracking.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationBroadcast {

    private UUID helperId;
    private UUID bookingId;
    private double lat;
    private double lng;
    private Instant timestamp;
    private double heading;
    private double speedKmh;
    private String etaMinutes;

    public static LocationBroadcast of(UUID helperId, UUID bookingId,
                                       double lat, double lng,
                                       double heading, double speedKmh,
                                       String etaMinutes) {
        return LocationBroadcast.builder()
                .helperId(helperId)
                .bookingId(bookingId)
                .lat(lat)
                .lng(lng)
                .timestamp(Instant.now())
                .heading(heading)
                .speedKmh(speedKmh)
                .etaMinutes(etaMinutes)
                .build();
    }
}

