package com.homecare.booking.mapper;

import com.homecare.booking.dto.BookingResponse;
import com.homecare.booking.entity.Booking;
import org.springframework.stereotype.Component;

/**
 * Centralised mapper for {@link Booking} → {@link BookingResponse}.
 * Eliminates the duplicate {@code toDto / toBookingDto} methods
 * that existed in BookingService and AdminService.
 */
@Component
public class BookingResponseMapper {

    public BookingResponse toDto(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .customerId(booking.getCustomer().getId())
                .customerName(booking.getCustomer().getName())
                .helperId(booking.getHelper() != null ? booking.getHelper().getId() : null)
                .helperName(booking.getHelper() != null ? booking.getHelper().getName() : null)
                .serviceType(booking.getServiceType())
                .bookingType(booking.getBookingType())
                .status(booking.getStatus())
                .scheduledAt(booking.getScheduledAt())
                .acceptedAt(booking.getAcceptedAt())
                .startedAt(booking.getStartedAt())
                .completedAt(booking.getCompletedAt())
                .addressLine(booking.getAddressLine())
                .latitude(booking.getLatitude())
                .longitude(booking.getLongitude())
                .durationHours(booking.getDurationHours())
                .specialInstructions(booking.getSpecialInstructions())
                .basePrice(booking.getBasePrice())
                .totalPrice(booking.getTotalPrice())
                .paymentStatus(booking.getPaymentStatus())
                .paymentReference(booking.getPaymentReference())
                .rating(booking.getRating())
                .reviewText(booking.getReviewText())
                .subscriptionId(booking.getSubscription() != null ? booking.getSubscription().getId() : null)
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}

