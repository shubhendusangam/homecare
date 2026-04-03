package com.homecare.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UnavailableDateResponse {

    private UUID id;
    private LocalDate date;
    private String reason;
}

