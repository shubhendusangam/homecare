package com.homecare.review.service;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.enums.ServiceType;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.notification.service.NotificationService;
import com.homecare.review.dto.CreateReviewRequest;
import com.homecare.review.dto.ReviewResponse;
import com.homecare.review.entity.Review;
import com.homecare.review.mapper.ReviewResponseMapper;
import com.homecare.review.repository.ReviewRepository;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.HelperProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private HelperProfileRepository helperProfileRepository;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ReviewResponseMapper reviewResponseMapper;

    @InjectMocks private ReviewService reviewService;

    private User customer;
    private User helper;
    private HelperProfile helperProfile;
    private Booking booking;
    private CreateReviewRequest request;

    @BeforeEach
    void setUp() {
        customer = User.builder().name("Customer").email("c@test.com").role(Role.CUSTOMER).build();
        customer.setId(UUID.randomUUID());

        helper = User.builder().name("Helper").email("h@test.com").role(Role.HELPER).build();
        helper.setId(UUID.randomUUID());

        helperProfile = HelperProfile.builder()
                .user(helper).rating(4.0).totalJobsCompleted(5).build();
        helperProfile.setId(UUID.randomUUID());

        booking = Booking.builder()
                .customer(customer).helper(helper)
                .serviceType(ServiceType.CLEANING).status(BookingStatus.COMPLETED).build();
        booking.setId(UUID.randomUUID());

        request = new CreateReviewRequest();
        request.setBookingId(booking.getId());
        request.setRating(5);
        request.setComment("Excellent service!");
    }

    @Nested
    @DisplayName("submitReview")
    class SubmitReview {

        @Test
        @DisplayName("happy path — creates review and recomputes rating")
        void happyPath() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(reviewRepository.existsByBookingId(booking.getId())).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
                Review r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(bookingRepository.save(any())).thenReturn(booking);
            when(reviewRepository.findAverageRatingByHelperId(helper.getId())).thenReturn(4.5);
            when(reviewRepository.countByHelperIdPublished(helper.getId())).thenReturn(6);
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(helperProfileRepository.save(any())).thenReturn(helperProfile);
            when(reviewResponseMapper.toDto(any())).thenReturn(
                    ReviewResponse.builder().id(UUID.randomUUID()).rating(5).build());

            ReviewResponse response = reviewService.submitReview(request, customer.getId());

            assertNotNull(response);
            verify(reviewRepository).save(any(Review.class));
            verify(bookingRepository).save(booking);
            assertEquals(5, booking.getRating());
            assertEquals("Excellent service!", booking.getReviewText());
            // Helper rating recomputed
            assertEquals(4.5, helperProfile.getRating());
        }

        @Test
        @DisplayName("not the booking owner → throws FORBIDDEN")
        void notOwner() {
            UUID otherUser = UUID.randomUUID();
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.submitReview(request, otherUser));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("booking not COMPLETED → throws REVIEW_NOT_ALLOWED")
        void notCompleted() {
            booking.setStatus(BookingStatus.ASSIGNED);
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.submitReview(request, customer.getId()));
            assertEquals(ErrorCode.REVIEW_NOT_ALLOWED, ex.getErrorCode());
        }

        @Test
        @DisplayName("duplicate review → throws DUPLICATE_REVIEW")
        void duplicateReview() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(reviewRepository.existsByBookingId(booking.getId())).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.submitReview(request, customer.getId()));
            assertEquals(ErrorCode.DUPLICATE_REVIEW, ex.getErrorCode());
        }

        @Test
        @DisplayName("no helper assigned → throws REVIEW_NOT_ALLOWED")
        void noHelper() {
            booking.setHelper(null);
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(reviewRepository.existsByBookingId(booking.getId())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.submitReview(request, customer.getId()));
            assertEquals(ErrorCode.REVIEW_NOT_ALLOWED, ex.getErrorCode());
        }

        @Test
        @DisplayName("booking not found → throws")
        void bookingNotFound() {
            when(bookingRepository.findById(any())).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> reviewService.submitReview(request, customer.getId()));
        }
    }

    @Nested
    @DisplayName("hideReview")
    class HideReview {

        @Test
        @DisplayName("sets published=false and recomputes rating")
        void happyPath() {
            Review review = Review.builder()
                    .booking(booking).customer(customer).helper(helper)
                    .rating(5).comment("Great!").published(true).flagged(false)
                    .serviceType(ServiceType.CLEANING).build();
            review.setId(UUID.randomUUID());

            when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
            when(reviewRepository.save(any())).thenReturn(review);
            when(reviewRepository.findAverageRatingByHelperId(helper.getId())).thenReturn(4.0);
            when(reviewRepository.countByHelperIdPublished(helper.getId())).thenReturn(4);
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(helperProfileRepository.save(any())).thenReturn(helperProfile);
            when(reviewResponseMapper.toDto(any())).thenReturn(
                    ReviewResponse.builder().id(review.getId()).published(false).build());

            ReviewResponse response = reviewService.hideReview(review.getId());

            assertFalse(review.isPublished());
            // Rating recomputed to 4.0
            assertEquals(4.0, helperProfile.getRating());
        }
    }

    @Nested
    @DisplayName("flagReview")
    class FlagReview {

        @Test
        @DisplayName("sets flagged=true")
        void happyPath() {
            Review review = Review.builder()
                    .booking(booking).customer(customer).helper(helper)
                    .rating(1).comment("Terrible").published(true).flagged(false)
                    .serviceType(ServiceType.CLEANING).build();
            review.setId(UUID.randomUUID());

            when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
            when(reviewRepository.save(any())).thenReturn(review);
            when(reviewResponseMapper.toDto(any())).thenReturn(
                    ReviewResponse.builder().id(review.getId()).flagged(true).build());

            reviewService.flagReview(review.getId());

            assertTrue(review.isFlagged());
        }
    }

    @Nested
    @DisplayName("recomputeHelperRating")
    class RecomputeHelperRating {

        @Test
        @DisplayName("no reviews → rating set to 0.0")
        void noReviews() {
            when(reviewRepository.findAverageRatingByHelperId(helper.getId())).thenReturn(null);
            when(reviewRepository.countByHelperIdPublished(helper.getId())).thenReturn(0);
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(helperProfileRepository.save(any())).thenReturn(helperProfile);

            reviewService.recomputeHelperRating(helper.getId());

            assertEquals(0.0, helperProfile.getRating());
        }

        @Test
        @DisplayName("rating is rounded to 1 decimal place")
        void ratingRounded() {
            when(reviewRepository.findAverageRatingByHelperId(helper.getId())).thenReturn(4.333333);
            when(reviewRepository.countByHelperIdPublished(helper.getId())).thenReturn(3);
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(helperProfileRepository.save(any())).thenReturn(helperProfile);

            reviewService.recomputeHelperRating(helper.getId());

            assertEquals(4.3, helperProfile.getRating());
        }
    }
}

