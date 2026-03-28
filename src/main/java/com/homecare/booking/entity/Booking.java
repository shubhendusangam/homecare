package com.homecare.booking.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.core.enums.*;
import com.homecare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helper_id")
    private User helper;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING_ASSIGNMENT;

    private Instant scheduledAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;

    private String addressLine;
    private double latitude;
    private double longitude;

    @Column(nullable = false)
    private int durationHours;

    @Column(length = 1000)
    private String specialInstructions;

    @Column(nullable = false)
    private double basePrice;

    @Column(nullable = false)
    private double totalPrice;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private String paymentReference;

    @Builder.Default
    private int rating = 0;

    @Column(length = 2000)
    private String reviewText;
}

