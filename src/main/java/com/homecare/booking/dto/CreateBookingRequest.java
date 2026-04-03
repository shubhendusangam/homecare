package com.homecare.booking.dto;

import com.homecare.core.enums.BookingType;
import com.homecare.core.enums.ServiceType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateBookingRequest {

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Booking type is required")
    private BookingType bookingType;

    private Instant scheduledAt;

    @NotBlank(message = "Address is required")
    private String addressLine;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be between 1 and 8 hours")
    @Max(value = 8, message = "Duration must be between 1 and 8 hours")
    private Integer durationHours;

    @Size(max = 1000, message = "Special instructions must be under 1000 characters")
    private String specialInstructions;

    /** Optional — if present, this booking is covered by the given subscription. */
    private UUID subscriptionId;

    /** Optional — if present, request this specific helper (must be in customer's favourites). */
    private UUID requestedHelperId;
}

