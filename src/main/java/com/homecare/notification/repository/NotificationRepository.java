package com.homecare.notification.repository;

import com.homecare.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
           "ORDER BY n.read ASC, n.createdAt DESC")
    Page<Notification> findByUserIdOrderByReadAscCreatedAtDesc(
            @Param("userId") UUID userId, Pageable pageable);

    long countByUserIdAndReadFalse(UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now " +
           "WHERE n.user.id = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") UUID userId, @Param("now") Instant now);
}

