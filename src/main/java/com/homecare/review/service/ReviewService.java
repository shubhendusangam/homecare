package com.homecare.review.service;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.enums.ServiceType;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.core.logging.AuditEvent;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.review.dto.CreateReviewRequest;
import com.homecare.review.dto.ReviewResponse;
import com.homecare.review.entity.Review;
import com.homecare.review.mapper.ReviewResponseMapper;
import com.homecare.review.repository.ReviewRepository;
import com.homecare.user.repository.HelperProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final HelperProfileRepository helperProfileRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ReviewResponseMapper reviewResponseMapper;

    // ─── Submit Review ────────────────────────────────────────────────

    @Transactional
    public ReviewResponse submitReview(CreateReviewRequest request, UUID customerId) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        // Validate: customer owns the booking
        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You can only review your own bookings", ErrorCode.FORBIDDEN);
        }

        // Validate: booking must be COMPLETED
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException(
                    "Reviews can only be submitted for completed bookings. Current status: " + booking.getStatus(),
                    ErrorCode.REVIEW_NOT_ALLOWED);
        }

        // Validate: no duplicate review
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new BusinessException(
                    "A review has already been submitted for this booking",
                    ErrorCode.DUPLICATE_REVIEW);
        }

        // Validate: helper must be assigned
        if (booking.getHelper() == null) {
            throw new BusinessException("Cannot review a booking without an assigned helper",
                    ErrorCode.REVIEW_NOT_ALLOWED);
        }

        // Create review
        Review review = Review.builder()
                .booking(booking)
                .customer(booking.getCustomer())
                .helper(booking.getHelper())
                .rating(request.getRating())
                .comment(request.getComment())
                .published(true)
                .flagged(false)
                .serviceType(booking.getServiceType())
                .build();
        review = reviewRepository.save(review);

        // Update booking with rating and review text
        booking.setRating(request.getRating());
        booking.setReviewText(request.getComment());
        bookingRepository.save(booking);

        // Recompute helper rating
        recomputeHelperRating(booking.getHelper().getId());

        // Notify helper about the new review
        try {
            notificationService.sendToUser(booking.getHelper().getId(), NotificationType.NEW_REVIEW,
                    Map.of("rating", String.valueOf(request.getRating()),
                           "serviceType", booking.getServiceType().name(),
                           "bookingId", booking.getId().toString()));
        } catch (Exception e) {
            log.warn("Failed to send review notification to helper: {}", e.getMessage());
        }

        log.info("Review submitted: bookingId={}, helperId={}, rating={}",
                booking.getId(), booking.getHelper().getId(), request.getRating());
        eventPublisher.publishEvent(AuditEvent.of("REVIEW_SUBMITTED", customerId,
                Map.of("bookingId", booking.getId(), "helperId", booking.getHelper().getId(),
                       "rating", request.getRating())));

        return toDto(review);
    }

    // ─── Public: Helper Reviews ───────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getHelperReviews(UUID helperId, Pageable pageable) {
        var page = reviewRepository.findByHelperIdAndPublishedTrueOrderByCreatedAtDesc(helperId, pageable)
                .map(this::toDto);
        return PagedResponse.from(page);
    }

    // ─── Customer: Own Reviews ────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getCustomerReviews(UUID customerId, Pageable pageable) {
        var page = reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::toDto);
        return PagedResponse.from(page);
    }

    // ─── Admin: All Reviews ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getAllReviews(Boolean flagged, Integer rating,
                                                       ServiceType serviceType, Pageable pageable) {
        var page = reviewRepository.findAllWithFilters(flagged, rating, serviceType, pageable)
                .map(this::toDto);
        return PagedResponse.from(page);
    }

    // ─── Admin: Hide Review ───────────────────────────────────────────

    @Transactional
    public ReviewResponse hideReview(UUID reviewId) {
        Review review = findReview(reviewId);
        review.setPublished(false);
        reviewRepository.save(review);

        // Recompute helper rating (hidden reviews are excluded)
        recomputeHelperRating(review.getHelper().getId());

        log.info("Review {} hidden by admin", reviewId);
        return toDto(review);
    }

    // ─── Admin: Flag Review ───────────────────────────────────────────

    @Transactional
    public ReviewResponse flagReview(UUID reviewId) {
        Review review = findReview(reviewId);
        review.setFlagged(true);
        reviewRepository.save(review);

        log.info("Review {} flagged for moderation", reviewId);
        return toDto(review);
    }

    // ─── Rating Recomputation ─────────────────────────────────────────

    @Transactional
    public void recomputeHelperRating(UUID helperId) {
        Double avg = reviewRepository.findAverageRatingByHelperId(helperId);
        int count = reviewRepository.countByHelperIdPublished(helperId);

        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;

        helperProfileRepository.findByUserId(helperId).ifPresent(profile -> {
            profile.setRating(roundedAvg);
            // Don't overwrite totalJobsCompleted — that's managed by BookingService
            helperProfileRepository.save(profile);
        });

        log.debug("Recomputed helper rating: helperId={}, avg={}, count={}", helperId, roundedAvg, count);
    }

    // ─── Private Helpers ──────────────────────────────────────────────

    private Review findReview(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
    }

    private ReviewResponse toDto(Review review) {
        return reviewResponseMapper.toDto(review);
    }
}

