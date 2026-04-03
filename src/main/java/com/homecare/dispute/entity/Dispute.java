package com.homecare.dispute.entity;

import com.homecare.booking.entity.Booking;
import com.homecare.core.entity.BaseEntity;
import com.homecare.core.enums.*;
import com.homecare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "disputes", indexes = {
        @Index(name = "idx_dispute_booking", columnList = "booking_id"),
        @Index(name = "idx_dispute_status", columnList = "status"),
        @Index(name = "idx_dispute_raised_by", columnList = "raised_by_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by_id", nullable = false)
    private User raisedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeRaisedBy raisedByRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private DisputeResolution resolution;

    @Column(length = 2000)
    private String adminNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private User assignedAdmin;

    private Instant resolvedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;
}

