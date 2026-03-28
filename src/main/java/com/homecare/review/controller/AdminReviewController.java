package com.homecare.review.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.ServiceType;
import com.homecare.review.dto.ReviewResponse;
import com.homecare.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    /**
     * GET /api/v1/admin/reviews — all reviews (filter: rating, flagged, serviceType)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getAllReviews(
            @RequestParam(required = false) Boolean flagged,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ReviewResponse> response = reviewService.getAllReviews(
                flagged, rating, serviceType, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * PATCH /api/v1/admin/reviews/{id}/hide — set published=false
     */
    @PatchMapping("/{id}/hide")
    public ResponseEntity<ApiResponse<ReviewResponse>> hideReview(@PathVariable UUID id) {
        ReviewResponse response = reviewService.hideReview(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Review hidden successfully"));
    }

    /**
     * PATCH /api/v1/admin/reviews/{id}/flag — set flagged=true for moderation
     */
    @PatchMapping("/{id}/flag")
    public ResponseEntity<ApiResponse<ReviewResponse>> flagReview(@PathVariable UUID id) {
        ReviewResponse response = reviewService.flagReview(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Review flagged for moderation"));
    }
}

