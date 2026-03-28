package com.homecare.scheduler.entity;

import com.homecare.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Persisted daily statistics report — computed by {@code DailyReportJob}.
 */
@Entity
@Table(name = "daily_reports", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_report_date", columnNames = "report_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyReport extends BaseEntity {

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Builder.Default
    private int totalBookings = 0;

    @Builder.Default
    private int completedBookings = 0;

    @Builder.Default
    private int cancelledBookings = 0;

    @Builder.Default
    private int newCustomers = 0;

    @Builder.Default
    private int newHelpers = 0;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Builder.Default
    private double avgHelperRating = 0.0;
}

