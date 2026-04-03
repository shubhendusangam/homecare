package com.homecare.chat.repository;

import com.homecare.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByBookingIdOrderByCreatedAtDesc(UUID bookingId, Pageable pageable);

    long countByBookingIdAndSenderIdNotAndReadByRecipientFalse(UUID bookingId, UUID senderId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readByRecipient = true, m.readAt = :readAt " +
           "WHERE m.booking.id = :bookingId AND m.sender.id <> :readerId AND m.readByRecipient = false")
    int markAllAsReadForRecipient(@Param("bookingId") UUID bookingId,
                                 @Param("readerId") UUID readerId,
                                 @Param("readAt") Instant readAt);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}

