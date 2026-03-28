package com.homecare.admin.dto;

import com.homecare.booking.dto.BookingResponse;
import com.homecare.user.dto.HelperProfileDto;
import com.homecare.payment.dto.WalletResponse;
import com.homecare.review.dto.ReviewResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class HelperDetailResponse {
    private HelperProfileDto profile;
    private List<BookingResponse> recentBookings;
    private WalletResponse earnings;
    private BigDecimal totalEarnings;
    private int totalBookings;
    private List<ReviewResponse> recentReviews;
}

