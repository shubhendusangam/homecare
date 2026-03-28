package com.homecare.review.mapper;

import com.homecare.review.dto.ReviewResponse;
import com.homecare.review.entity.Review;
import org.springframework.stereotype.Component;

/**
 * Centralised mapper for {@link Review} → {@link ReviewResponse}.
 * Eliminates the duplicate {@code toDto / toReviewDto} methods
 * that existed in ReviewService and AdminService.
 */
@Component
public class ReviewResponseMapper {

    public ReviewResponse toDto(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBooking().getId())
                .customerId(review.getCustomer().getId())
                .customerName(review.getCustomer().getName())
                .helperId(review.getHelper().getId())
                .helperName(review.getHelper().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .published(review.isPublished())
                .flagged(review.isFlagged())
                .serviceType(review.getServiceType())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

