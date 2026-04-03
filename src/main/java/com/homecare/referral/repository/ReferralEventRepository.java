package com.homecare.referral.repository;

import com.homecare.core.enums.ReferralStatus;
import com.homecare.referral.entity.ReferralEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ReferralEventRepository extends JpaRepository<ReferralEvent, UUID> {

    Optional<ReferralEvent> findByRefereeId(UUID refereeId);

    @Query(value = """
        SELECT e FROM ReferralEvent e
        LEFT JOIN FETCH e.referrer
        LEFT JOIN FETCH e.referee
        WHERE e.referrer.id = :referrerId
        ORDER BY e.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(e) FROM ReferralEvent e
        WHERE e.referrer.id = :referrerId
        """)
    Page<ReferralEvent> findByReferrerIdOrderByCreatedAtDesc(@Param("referrerId") UUID referrerId, Pageable pageable);

    long countByReferrerId(UUID referrerId);

    long countByReferrerIdAndStatus(UUID referrerId, ReferralStatus status);

    @Query("SELECT COALESCE(SUM(e.referrerCredit), 0) FROM ReferralEvent e " +
           "WHERE e.referrer.id = :referrerId AND e.referrerCreditIssued = true")
    BigDecimal sumReferrerCreditsEarned(@Param("referrerId") UUID referrerId);

    // ─── Admin queries ────────────────────────────────────────────────

    @Query(value = """
        SELECT e FROM ReferralEvent e
        LEFT JOIN FETCH e.referrer
        LEFT JOIN FETCH e.referee
        WHERE (:status IS NULL OR e.status = :status)
        AND (:from IS NULL OR e.createdAt >= :from)
        AND (:to IS NULL OR e.createdAt <= :to)
        ORDER BY e.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(e) FROM ReferralEvent e
        WHERE (:status IS NULL OR e.status = :status)
        AND (:from IS NULL OR e.createdAt >= :from)
        AND (:to IS NULL OR e.createdAt <= :to)
        """)
    Page<ReferralEvent> findAllWithFilters(@Param("status") ReferralStatus status,
                                           @Param("from") Instant from,
                                           @Param("to") Instant to,
                                           Pageable pageable);

    long countByStatus(ReferralStatus status);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.referrerCreditIssued = true THEN e.referrerCredit ELSE 0 END), 0) " +
           "+ COALESCE(SUM(CASE WHEN e.refereeCreditIssued = true THEN e.refereeCredit ELSE 0 END), 0) " +
           "FROM ReferralEvent e")
    BigDecimal sumAllCreditsIssued();
}

