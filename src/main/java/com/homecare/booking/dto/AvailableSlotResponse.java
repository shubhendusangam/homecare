package com.homecare.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class AvailableSlotResponse {

    private LocalTime time;
    private int availableHelperCount;
}

