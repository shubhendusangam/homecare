package com.homecare.tracking.repository;

import com.homecare.tracking.entity.LocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationHistoryRepository extends JpaRepository<LocationHistory, UUID> {

    Optional<LocationHistory> findTopByBookingIdOrderByRecordedAtDesc(UUID bookingId);

    List<LocationHistory> findByBookingIdOrderByRecordedAtAsc(UUID bookingId);

    @Modifying
    @Query("DELETE FROM LocationHistory lh WHERE lh.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}

