package com.homecare.scheduler;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.PaymentStatus;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Fires 30 minutes before a SCHEDULED booking's {@code scheduledAt}.
 * Calls {@code BookingService.findAndAssignHelper(booking)}.
 * <ul>
 *   <li>Attempt 1 (T-30 min) — if no helper: reschedule at T-15 min</li>
 *   <li>Attempt 2 (T-15 min) — if no helper: reschedule at T-0</li>
 *   <li>Attempt 3 (T-0)      — if still none: cancel with full refund</li>
 * </ul>
 */
@Component
@Slf4j
public class ScheduledBookingTriggerJob implements Job {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private com.homecare.booking.service.BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private Scheduler quartzScheduler;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getJobDetail().getJobDataMap();
        String bookingIdStr = data.getString("bookingId");
        int attempt = data.containsKey("attempt") ? data.getInt("attempt") : 1;

        if (bookingIdStr == null) {
            log.warn("ScheduledBookingTriggerJob fired without bookingId");
            return;
        }

        UUID bookingId = UUID.fromString(bookingIdStr);
        log.info("ScheduledBookingTriggerJob — booking={}, attempt={}", bookingId, attempt);

        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            log.warn("Booking {} not found — skipping", bookingId);
            return;
        }

        Booking booking = bookingOpt.get();
        if (booking.getStatus() != BookingStatus.PENDING_ASSIGNMENT) {
            log.info("Booking {} is no longer PENDING_ASSIGNMENT (status={}), skipping",
                    bookingId, booking.getStatus());
            return;
        }

        // Attempt to find and assign a helper
        bookingService.findAndAssignHelper(booking);
        bookingRepository.save(booking);

        // If helper was assigned, we're done
        if (booking.getStatus() == BookingStatus.ASSIGNED) {
            log.info("Booking {} assigned on attempt {}", bookingId, attempt);
            return;
        }

        // No helper found — decide next action based on attempt
        if (attempt < 3) {
            rescheduleNextAttempt(booking, attempt);
        } else {
            // Final attempt failed — cancel with full refund
            log.info("Booking {} — all 3 attempts exhausted, cancelling with full refund", bookingId);
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
            bookingRepository.save(booking);

            try {
                paymentService.refundBooking(bookingId, BigDecimal.ONE);
            } catch (Exception e) {
                log.error("Failed to refund booking {} during auto-cancel: {}", bookingId, e.getMessage());
            }

            notificationService.sendToUser(
                    booking.getCustomer().getId(),
                    NotificationType.BOOKING_CANCELLED,
                    Map.of("bookingId", bookingId.toString(),
                           "reason", "No helper available after multiple attempts. Full refund initiated."));
        }
    }

    private void rescheduleNextAttempt(Booking booking, int currentAttempt) {
        int nextAttempt = currentAttempt + 1;
        // Attempt 2 fires at T-15 min, attempt 3 fires at T-0
        int minutesBefore = (nextAttempt == 2) ? 15 : 0;
        Instant triggerTime = booking.getScheduledAt().minus(Duration.ofMinutes(minutesBefore));

        if (triggerTime.isBefore(Instant.now())) {
            triggerTime = Instant.now().plusSeconds(5);
        }

        try {
            JobDetail jobDetail = JobBuilder.newJob(ScheduledBookingTriggerJob.class)
                    .withIdentity("assign-booking-" + booking.getId() + "-attempt-" + nextAttempt,
                            "booking-assignment")
                    .usingJobData("bookingId", booking.getId().toString())
                    .usingJobData("attempt", nextAttempt)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("assign-trigger-" + booking.getId() + "-attempt-" + nextAttempt,
                            "booking-assignment")
                    .startAt(Date.from(triggerTime))
                    .build();

            quartzScheduler.scheduleJob(jobDetail, trigger);
            log.info("Rescheduled booking {} assignment — attempt {} at {}",
                    booking.getId(), nextAttempt, triggerTime);
        } catch (SchedulerException e) {
            log.error("Failed to reschedule booking {} assignment (attempt {}): {}",
                    booking.getId(), nextAttempt, e.getMessage());
        }
    }
}

