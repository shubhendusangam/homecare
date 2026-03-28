package com.homecare.booking.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.core.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "booking_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus toStatus;

    private String changedBy;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private Instant changedAt;
}

