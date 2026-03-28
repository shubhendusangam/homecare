package com.homecare.scheduler;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.enums.BookingStatus;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * One-shot Quartz job that fires 30 minutes before any booking start.
 * Sends {@code BOOKING_REMINDER} notification to both the customer and helper.
 * Created when a booking transitions to ASSIGNED.
 */
@Component
@Slf4j
public class BookingReminderJob implements Job {

    private static final Set<BookingStatus> ACTIVE_STATUSES = Set.of(
            BookingStatus.ASSIGNED, BookingStatus.HELPER_EN_ROUTE, BookingStatus.IN_PROGRESS);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String bookingIdStr = context.getJobDetail().getJobDataMap().getString("bookingId");
        if (bookingIdStr == null) {
            log.warn("BookingReminderJob fired without bookingId");
            return;
        }

        UUID bookingId = UUID.fromString(bookingIdStr);
        log.info("BookingReminderJob fired for booking {}", bookingId);

        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            log.warn("Booking {} not found — skipping reminder", bookingId);
            return;
        }

        Booking booking = bookingOpt.get();

        // Only send reminder for active bookings
        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            log.info("Booking {} is in status {} — skipping reminder", bookingId, booking.getStatus());
            return;
        }

        Map<String, String> vars = Map.of(
                "bookingId", bookingId.toString(),
                "serviceType", booking.getServiceType().name(),
                "scheduledAt", booking.getScheduledAt() != null ? booking.getScheduledAt().toString() : "soon");

        // Notify customer
        notificationService.sendToUser(
                booking.getCustomer().getId(),
                NotificationType.BOOKING_REMINDER,
                vars);

        // Notify helper (if assigned)
        if (booking.getHelper() != null) {
            notificationService.sendToUser(
                    booking.getHelper().getId(),
                    NotificationType.BOOKING_REMINDER,
                    vars);
        }

        log.info("Booking reminder sent for booking {}", bookingId);
    }
}

