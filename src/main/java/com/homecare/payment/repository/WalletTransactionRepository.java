package com.homecare.payment.repository;

import com.homecare.payment.entity.WalletTransaction;
import com.homecare.payment.enums.TransactionStatus;
import com.homecare.payment.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    Page<WalletTransaction> findByWalletUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<WalletTransaction> findByBookingIdAndType(UUID bookingId, TransactionType type);

    Optional<WalletTransaction> findByBookingIdAndTypeAndStatus(UUID bookingId, TransactionType type, TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t " +
           "WHERE t.wallet.user.id = :userId AND t.type = :type AND t.status = 'SUCCESS'")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") UUID userId, @Param("type") TransactionType type);

    @Query("SELECT COUNT(t) FROM WalletTransaction t " +
           "WHERE t.wallet.user.id = :userId AND t.type = 'CREDIT_EARNING' AND t.status = 'SUCCESS'")
    long countCompletedJobsByUserId(@Param("userId") UUID userId);

    @Query("SELECT t FROM WalletTransaction t WHERE " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:from IS NULL OR t.createdAt >= :from) AND " +
           "(:to IS NULL OR t.createdAt <= :to) " +
           "ORDER BY t.createdAt DESC")
    Page<WalletTransaction> findAllWithFilters(@Param("type") TransactionType type,
                                                @Param("status") TransactionStatus status,
                                                @Param("from") Instant from,
                                                @Param("to") Instant to,
                                                Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t " +
           "WHERE t.type = 'DEBIT_BOOKING' AND t.status = 'SUCCESS' " +
           "AND t.createdAt >= :from AND t.createdAt <= :to")
    BigDecimal sumRevenueInPeriod(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(t) FROM WalletTransaction t " +
           "WHERE t.type = 'DEBIT_BOOKING' AND t.status = 'SUCCESS' " +
           "AND t.createdAt >= :from AND t.createdAt <= :to")
    long countTransactionsInPeriod(@Param("from") Instant from, @Param("to") Instant to);
}

