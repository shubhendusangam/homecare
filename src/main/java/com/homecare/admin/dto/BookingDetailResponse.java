package com.homecare.admin.dto;

import com.homecare.booking.dto.BookingResponse;
import com.homecare.booking.dto.BookingStatusHistoryResponse;
import com.homecare.tracking.dto.LocationBroadcast;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BookingDetailResponse {
    private BookingResponse booking;
    private List<BookingStatusHistoryResponse> statusHistory;
    private List<LocationBroadcast> locationTrail;
}

