package com.homecare.booking.dto;

import com.homecare.core.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private UUID helperId;
    private String helperName;

    private ServiceType serviceType;
    private BookingType bookingType;
    private BookingStatus status;

    private Instant scheduledAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;

    private String addressLine;
    private double latitude;
    private double longitude;

    private int durationHours;
    private String specialInstructions;

    private double basePrice;
    private double totalPrice;
    private PaymentStatus paymentStatus;
    private String paymentReference;

    private int rating;
    private String reviewText;

    private UUID subscriptionId;

    private Instant createdAt;
    private Instant updatedAt;
}

