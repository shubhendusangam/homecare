package com.homecare.admin.dto;

import com.homecare.booking.dto.BookingResponse;
import com.homecare.booking.dto.BookingStatusHistoryResponse;
import com.homecare.user.dto.CustomerProfileDto;
import com.homecare.payment.dto.WalletResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerDetailResponse {
    private CustomerProfileDto profile;
    private List<BookingResponse> recentBookings;
    private WalletResponse wallet;
    private int totalBookings;
}

