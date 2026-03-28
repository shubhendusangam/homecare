package com.homecare.booking.dto;

import com.homecare.core.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BookingStatusHistoryResponse {

    private UUID id;
    private UUID bookingId;
    private BookingStatus fromStatus;
    private BookingStatus toStatus;
    private String changedBy;
    private String reason;
    private Instant changedAt;
}

