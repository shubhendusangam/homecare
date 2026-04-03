package com.homecare.booking.repository;

import com.homecare.booking.entity.Booking;
import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @Query(value = """
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.customer
        LEFT JOIN FETCH b.helper
        WHERE b.customer.id = :customerId
        ORDER BY b.createdAt DESC
        """,
        countQuery = "SELECT COUNT(b) FROM Booking b WHERE b.customer.id = :customerId")
    Page<Booking> findByCustomerIdWithParties(@Param("customerId") UUID customerId, Pageable pageable);

    Page<Booking> findByHelperIdAndStatusInOrderByCreatedAtDesc(UUID helperId, List<BookingStatus> statuses, Pageable pageable);

    @Query(value = """
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.customer
        LEFT JOIN FETCH b.helper
        WHERE b.helper.id = :helperId AND b.status IN :statuses
        ORDER BY b.createdAt DESC
        """,
        countQuery = "SELECT COUNT(b) FROM Booking b WHERE b.helper.id = :helperId AND b.status IN :statuses")
    Page<Booking> findByHelperIdWithParties(@Param("helperId") UUID helperId,
                                           @Param("statuses") List<BookingStatus> statuses,
                                           Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.helper.id = :helperId " +
           "AND b.status IN :statuses ORDER BY b.createdAt DESC")
    List<Booking> findActiveBookingsByHelper(@Param("helperId") UUID helperId,
                                            @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.status = :status ORDER BY b.createdAt ASC")
    List<Booking> findByStatus(@Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING_ASSIGNMENT' " +
           "AND b.createdAt <= :cutoff ORDER BY b.createdAt ASC")
    List<Booking> findExpiredPendingBookings(@Param("cutoff") Instant cutoff);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING_ASSIGNMENT' " +
           "AND b.bookingType = 'SCHEDULED' " +
           "AND b.scheduledAt <= :threshold ORDER BY b.scheduledAt ASC")
    List<Booking> findScheduledBookingsReadyForAssignment(@Param("threshold") Instant threshold);

    @Query("SELECT b FROM Booking b WHERE " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:serviceType IS NULL OR b.serviceType = :serviceType) AND " +
           "(:from IS NULL OR b.createdAt >= :from) AND " +
           "(:to IS NULL OR b.createdAt <= :to) " +
           "ORDER BY b.createdAt DESC")
    Page<Booking> findAllWithFilters(@Param("status") BookingStatus status,
                                     @Param("serviceType") ServiceType serviceType,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);

    @Query(value = """
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.customer
        LEFT JOIN FETCH b.helper
        WHERE (:status IS NULL OR b.status = :status)
        AND (:serviceType IS NULL OR b.serviceType = :serviceType)
        AND (:from IS NULL OR b.createdAt >= :from)
        AND (:to IS NULL OR b.createdAt <= :to)
        ORDER BY b.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(b) FROM Booking b
        WHERE (:status IS NULL OR b.status = :status)
        AND (:serviceType IS NULL OR b.serviceType = :serviceType)
        AND (:from IS NULL OR b.createdAt >= :from)
        AND (:to IS NULL OR b.createdAt <= :to)
        """)
    Page<Booking> findAllWithFiltersAndParties(@Param("status") BookingStatus status,
                                               @Param("serviceType") ServiceType serviceType,
                                               @Param("from") Instant from,
                                               @Param("to") Instant to,
                                               Pageable pageable);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.helper.id = :helperId " +
           "AND b.status IN ('ASSIGNED', 'IN_PROGRESS', 'HELPER_EN_ROUTE')")
    boolean hasActiveBooking(@Param("helperId") UUID helperId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.createdAt >= :from AND b.createdAt < :to")
    long countByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status " +
           "AND b.createdAt >= :from AND b.createdAt < :to")
    long countByStatusAndCreatedAtBetween(@Param("status") BookingStatus status,
                                          @Param("from") Instant from,
                                          @Param("to") Instant to);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.serviceType = :serviceType " +
           "AND b.createdAt >= :from AND b.createdAt < :to")
    long countByServiceTypeAndCreatedAtBetween(@Param("serviceType") ServiceType serviceType,
                                                @Param("from") Instant from,
                                                @Param("to") Instant to);

    long countByCustomerId(UUID customerId);

    long countByHelperId(UUID helperId);
}

