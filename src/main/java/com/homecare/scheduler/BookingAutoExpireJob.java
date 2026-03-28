package com.homecare.scheduler;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.PaymentStatus;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * One-shot Quartz job created when a PENDING_ASSIGNMENT booking is created.
 * Fires after {@code homecare.booking.auto-expire-minutes} (default 15).
 * If the booking is still PENDING_ASSIGNMENT: cancels with full refund and notifies customer.
 */
@Component
@Slf4j
public class BookingAutoExpireJob implements Job {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String bookingIdStr = context.getJobDetail().getJobDataMap().getString("bookingId");
        if (bookingIdStr == null) {
            log.warn("BookingAutoExpireJob fired without bookingId");
            return;
        }

        UUID bookingId = UUID.fromString(bookingIdStr);
        log.info("BookingAutoExpireJob fired for booking {}", bookingId);

        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            log.warn("Booking {} not found — skipping expiry", bookingId);
            return;
        }

        Booking booking = bookingOpt.get();

        // Only expire if still pending
        if (booking.getStatus() != BookingStatus.PENDING_ASSIGNMENT) {
            log.info("Booking {} is no longer PENDING_ASSIGNMENT (status={}), skipping expiry",
                    bookingId, booking.getStatus());
            return;
        }

        // Cancel the booking
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingRepository.save(booking);

        // Issue full refund
        try {
            paymentService.refundBooking(bookingId, BigDecimal.ONE);
        } catch (Exception e) {
            log.error("Failed to refund booking {} during auto-expire: {}", bookingId, e.getMessage());
        }

        // Notify customer
        notificationService.sendToUser(
                booking.getCustomer().getId(),
                NotificationType.BOOKING_CANCELLED,
                Map.of("bookingId", bookingId.toString(),
                       "reason", "No helper was available within the time limit. A full refund has been initiated."));

        log.info("Booking {} auto-expired and cancelled with full refund", bookingId);
    }
}

