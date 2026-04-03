package com.homecare.dispute.repository;

import com.homecare.core.enums.DisputeStatus;
import com.homecare.core.enums.DisputeType;
import com.homecare.dispute.entity.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    boolean existsByBookingIdAndRaisedByIdAndStatusIn(UUID bookingId, UUID raisedById, List<DisputeStatus> statuses);

    Optional<Dispute> findByBookingId(UUID bookingId);

    Page<Dispute> findByRaisedByIdOrderByCreatedAtDesc(UUID raisedById, Pageable pageable);

    @Query(value = """
        SELECT d FROM Dispute d
        LEFT JOIN FETCH d.booking
        LEFT JOIN FETCH d.raisedBy
        LEFT JOIN FETCH d.assignedAdmin
        WHERE (:status IS NULL OR d.status = :status)
        AND (:type IS NULL OR d.type = :type)
        AND (:from IS NULL OR d.createdAt >= :from)
        AND (:to IS NULL OR d.createdAt <= :to)
        ORDER BY d.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(d) FROM Dispute d
        WHERE (:status IS NULL OR d.status = :status)
        AND (:type IS NULL OR d.type = :type)
        AND (:from IS NULL OR d.createdAt >= :from)
        AND (:to IS NULL OR d.createdAt <= :to)
        """)
    Page<Dispute> findAllWithFilters(@Param("status") DisputeStatus status,
                                     @Param("type") DisputeType type,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);
}

