package com.homecare.review.repository;

import com.homecare.core.enums.ServiceType;
import com.homecare.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByBookingId(UUID bookingId);

    Page<Review> findByHelperIdAndPublishedTrueOrderByCreatedAtDesc(UUID helperId, Pageable pageable);

    Page<Review> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE " +
           "(:flagged IS NULL OR r.flagged = :flagged) AND " +
           "(:rating IS NULL OR r.rating = :rating) AND " +
           "(:serviceType IS NULL OR r.serviceType = :serviceType) " +
           "ORDER BY r.createdAt DESC")
    Page<Review> findAllWithFilters(@Param("flagged") Boolean flagged,
                                    @Param("rating") Integer rating,
                                    @Param("serviceType") ServiceType serviceType,
                                    Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.helper.id = :helperId AND r.published = true")
    Double findAverageRatingByHelperId(@Param("helperId") UUID helperId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.helper.id = :helperId AND r.published = true")
    int countByHelperIdPublished(@Param("helperId") UUID helperId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.createdAt >= :from AND r.createdAt < :to")
    Double findAverageRatingForDate(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.published = true")
    Double findOverallAverageRating();
}

