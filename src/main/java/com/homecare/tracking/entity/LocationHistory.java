package com.homecare.tracking.entity;

import com.homecare.booking.entity.Booking;
import com.homecare.core.entity.BaseEntity;
import com.homecare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "location_history", indexes = {
        @Index(name = "idx_location_booking_time", columnList = "booking_id, recorded_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helper_id", nullable = false)
    private User helper;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private double accuracy;

    private double heading;

    private double speedKmh;

    @Column(nullable = false)
    private Instant recordedAt;
}

