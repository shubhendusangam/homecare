package com.homecare.user.entity;

import com.homecare.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "helper_availability_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_helper_day_start",
                columnNames = {"helper_id", "day_of_week", "start_time"}),
        indexes = @Index(name = "idx_availability_helper", columnList = "helper_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HelperAvailabilitySlot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helper_id", nullable = false)
    private User helper;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    private boolean active = true;
}

