package com.homecare.tracking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationUpdate {

    @NotNull
    private UUID bookingId;

    private double lat;

    private double lng;

    private double accuracy;

    private double heading;

    private double speedKmh;
}

