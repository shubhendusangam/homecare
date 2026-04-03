package com.homecare.booking.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.core.enums.*;
import com.homecare.subscription.entity.CustomerSubscription;
import com.homecare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_customer_status", columnList = "customer_id, status"),
        @Index(name = "idx_bookings_helper_status", columnList = "helper_id, status"),
        @Index(name = "idx_bookings_status_created", columnList = "status, created_at"),
        @Index(name = "idx_bookings_scheduled_at", columnList = "scheduled_at")
})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private CustomerSubscription subscription;
}

