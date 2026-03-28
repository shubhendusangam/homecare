package com.homecare.tracking.dto;

import com.homecare.core.enums.BookingStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusEvent {

    private UUID bookingId;
    private String status;
    private String message;
    private Instant timestamp;

    public static StatusEvent of(UUID bookingId, BookingStatus status, String message) {
        return StatusEvent.builder()
                .bookingId(bookingId)
                .status(status.name())
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}

