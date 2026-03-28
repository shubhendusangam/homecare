package com.homecare.review.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.review.dto.CreateReviewRequest;
import com.homecare.review.dto.ReviewResponse;
import com.homecare.review.service.ReviewService;
import com.homecare.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * POST /api/v1/reviews — submit review after COMPLETED booking
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.submitReview(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * GET /api/v1/reviews/helpers/{helperId} — public helper reviews (paginated)
     */
    @GetMapping("/helpers/{helperId}")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getHelperReviews(
            @PathVariable UUID helperId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<ReviewResponse> response = reviewService.getHelperReviews(
                helperId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * GET /api/v1/reviews/me — customer's own reviews
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<ReviewResponse> response = reviewService.getCustomerReviews(
                principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

