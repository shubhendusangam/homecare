package com.homecare.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BookingAnalyticsResponse {

    private List<BookingDataPoint> data;

    @Data
    @Builder
    public static class BookingDataPoint {
        private String period;
        private long total;
        private long completed;
        private long cancelled;
        private long pending;
    }
}

