package com.homecare.scheduler;

import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.enums.BookingStatus;
import com.homecare.notification.email.EmailService;
import com.homecare.notification.service.NotificationService;
import com.homecare.payment.repository.WalletTransactionRepository;
import com.homecare.review.repository.ReviewRepository;
import com.homecare.scheduler.entity.DailyReport;
import com.homecare.scheduler.repository.DailyReportRepository;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fires every day at 11:55 PM.
 * Computes daily statistics and persists them to {@code daily_reports}.
 * Sends a summary email to all ADMIN users.
 */
@Component
@Slf4j
public class DailyReportJob implements Job {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private DailyReportRepository dailyReportRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.15");

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LocalDate today = LocalDate.now();
        log.info("DailyReportJob — generating report for {}", today);

        // Compute date boundaries for today
        ZoneId zone = ZoneId.systemDefault();
        Instant dayStart = today.atStartOfDay(zone).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();

        // Booking counts
        long totalBookings = bookingRepository.countByCreatedAtBetween(dayStart, dayEnd);
        long completedBookings = bookingRepository.countByStatusAndCreatedAtBetween(
                BookingStatus.COMPLETED, dayStart, dayEnd);
        long cancelledBookings = bookingRepository.countByStatusAndCreatedAtBetween(
                BookingStatus.CANCELLED, dayStart, dayEnd);

        // New registrations
        long newCustomers = userRepository.countByRoleAndCreatedAtBetween(Role.CUSTOMER, dayStart, dayEnd);
        long newHelpers = userRepository.countByRoleAndCreatedAtBetween(Role.HELPER, dayStart, dayEnd);

        // Revenue (sum of successful DEBIT_BOOKING transactions today)
        BigDecimal totalRevenue = walletTransactionRepository.sumRevenueInPeriod(dayStart, dayEnd);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        BigDecimal platformFee = totalRevenue.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);

        // Average rating across all reviews created today
        Double avgRating = reviewRepository.findAverageRatingForDate(dayStart, dayEnd);
        double avgHelperRating = avgRating != null ? avgRating : 0.0;

        // Persist or update report
        DailyReport report = dailyReportRepository.findByReportDate(today)
                .orElse(DailyReport.builder().reportDate(today).build());

        report.setTotalBookings((int) totalBookings);
        report.setCompletedBookings((int) completedBookings);
        report.setCancelledBookings((int) cancelledBookings);
        report.setNewCustomers((int) newCustomers);
        report.setNewHelpers((int) newHelpers);
        report.setTotalRevenue(totalRevenue);
        report.setPlatformFee(platformFee);
        report.setAvgHelperRating(avgHelperRating);

        dailyReportRepository.save(report);
        log.info("DailyReport persisted for {} — bookings={}, completed={}, cancelled={}, revenue={}",
                today, totalBookings, completedBookings, cancelledBookings, totalRevenue);

        // Send admin alert notification
        String summaryBody = String.format(
                "Daily Report (%s): %d bookings, %d completed, %d cancelled, ₹%s revenue, %.1f avg rating",
                today, totalBookings, completedBookings, cancelledBookings,
                totalRevenue.toPlainString(), avgHelperRating);

        notificationService.sendAdminAlert("Daily Report — " + today, summaryBody);

        // Send email to all admins
        sendReportEmailToAdmins(report);
    }

    private void sendReportEmailToAdmins(DailyReport report) {
        List<User> admins = userRepository.findAllWithFilters(
                Role.ADMIN, true, null, PageRequest.of(0, 100)).getContent();

        Map<String, Object> emailVars = new HashMap<>();
        emailVars.put("reportDate", report.getReportDate().toString());
        emailVars.put("totalBookings", report.getTotalBookings());
        emailVars.put("completedBookings", report.getCompletedBookings());
        emailVars.put("cancelledBookings", report.getCancelledBookings());
        emailVars.put("newCustomers", report.getNewCustomers());
        emailVars.put("newHelpers", report.getNewHelpers());
        emailVars.put("totalRevenue", report.getTotalRevenue().toPlainString());
        emailVars.put("platformFee", report.getPlatformFee().toPlainString());
        emailVars.put("avgHelperRating", String.format("%.1f", report.getAvgHelperRating()));

        for (User admin : admins) {
            try {
                emailVars.put("userName", admin.getName() != null ? admin.getName() : "Admin");
                emailService.sendHtmlEmail(
                        admin.getEmail(),
                        "HomeCare Daily Report — " + report.getReportDate(),
                        "daily-report",
                        emailVars);
            } catch (Exception e) {
                log.error("Failed to send daily report email to admin {}: {}", admin.getId(), e.getMessage());
            }
        }
    }
}

