package com.homecare.user.entity;

import com.homecare.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "helper_unavailable_dates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_helper_date",
                columnNames = {"helper_id", "date"}),
        indexes = @Index(name = "idx_unavailable_helper", columnList = "helper_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HelperUnavailableDate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helper_id", nullable = false)
    private User helper;

    @Column(nullable = false)
    private LocalDate date;

    private String reason;
}

