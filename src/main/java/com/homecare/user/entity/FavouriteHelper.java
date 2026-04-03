package com.homecare.user.entity;

import com.homecare.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "favourite_helpers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_favourite_customer_helper", columnNames = {"customer_id", "helper_id"})
}, indexes = {
        @Index(name = "idx_favourite_customer", columnList = "customer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavouriteHelper extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helper_id", nullable = false)
    private User helper;

    @Column(length = 200)
    private String nickname;

    @Column(length = 1000)
    private String notes;

    @Builder.Default
    private int totalBookingsTogether = 0;

    private Instant lastBookedAt;
}

