package com.homecare.user.repository;

import com.homecare.core.enums.ServiceType;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.enums.HelperStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HelperProfileRepository extends JpaRepository<HelperProfile, UUID> {

    Optional<HelperProfile> findByUserId(UUID userId);

    List<HelperProfile> findByStatusAndAvailableTrue(HelperStatus status);

    Page<HelperProfile> findByBackgroundVerifiedFalse(Pageable pageable);

    long countByStatus(HelperStatus status);

    long countByBackgroundVerifiedFalse();

    @Query("""
        SELECT h FROM HelperProfile h WHERE h.status = 'ONLINE'
        AND :serviceType MEMBER OF h.skills
        AND h.latitude BETWEEN :minLat AND :maxLat
        AND h.longitude BETWEEN :minLng AND :maxLng
        AND (6371 * acos(
            cos(radians(:lat)) * cos(radians(h.latitude))
            * cos(radians(h.longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(h.latitude))
        )) <= :radiusKm
        ORDER BY (6371 * acos(
            cos(radians(:lat)) * cos(radians(h.latitude))
            * cos(radians(h.longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(h.latitude))
        )) ASC, h.rating DESC
    """)
    List<HelperProfile> findNearbyAvailableHelpers(
            @Param("serviceType") ServiceType serviceType,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng);

    @Query("SELECT h FROM HelperProfile h WHERE h.status = :status " +
           "AND h.lastLocationUpdate IS NOT NULL AND h.lastLocationUpdate < :cutoff")
    List<HelperProfile> findByStatusAndLastLocationUpdateBefore(
            @Param("status") HelperStatus status,
            @Param("cutoff") Instant cutoff);
}

