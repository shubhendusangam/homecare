package com.homecare.admin.service;

import com.homecare.admin.dto.*;
import com.homecare.admin.entity.ServiceConfig;
import com.homecare.admin.repository.ServiceConfigRepository;
import com.homecare.booking.dto.BookingResponse;
import com.homecare.booking.dto.BookingStatusHistoryResponse;
import com.homecare.booking.entity.Booking;
import com.homecare.booking.entity.BookingStatusHistory;
import com.homecare.booking.mapper.BookingResponseMapper;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.booking.repository.BookingStatusHistoryRepository;
import com.homecare.booking.service.BookingService;
import com.homecare.core.enums.*;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.payment.dto.WalletResponse;
import com.homecare.payment.enums.TransactionType;
import com.homecare.payment.repository.WalletTransactionRepository;
import com.homecare.payment.service.PaymentService;
import com.homecare.payment.service.WalletService;
import com.homecare.review.dto.ReviewResponse;
import com.homecare.review.entity.Review;
import com.homecare.review.mapper.ReviewResponseMapper;
import com.homecare.review.repository.ReviewRepository;
import com.homecare.tracking.entity.LocationHistory;
import com.homecare.tracking.repository.LocationHistoryRepository;
import com.homecare.user.dto.CustomerProfileDto;
import com.homecare.user.dto.HelperProfileDto;
import com.homecare.user.entity.CustomerProfile;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.entity.User;
import com.homecare.user.enums.HelperStatus;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.CustomerProfileRepository;
import com.homecare.user.repository.HelperProfileRepository;
import com.homecare.user.repository.RefreshTokenRepository;
import com.homecare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final HelperProfileRepository helperProfileRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ReviewRepository reviewRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletService walletService;
    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final NotificationService notificationService;
    private final LocationHistoryRepository locationHistoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ServiceConfigRepository serviceConfigRepository;
    private final ServiceConfigCache serviceConfigCache;
    private final BannedUserStore bannedUserStore;
    private final BookingResponseMapper bookingResponseMapper;
    private final ReviewResponseMapper reviewResponseMapper;

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.15");

    // ═══════════════════════════════════════════════════════════════════
    //  DASHBOARD
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        Instant dayStart = today.atStartOfDay(ZONE).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZONE).toInstant();

        int todayBookings = (int) bookingRepository.countByCreatedAtBetween(dayStart, dayEnd);

        int activeBookings = (int) bookingRepository.countByStatusIn(
                List.of(BookingStatus.ASSIGNED, BookingStatus.HELPER_EN_ROUTE, BookingStatus.IN_PROGRESS));

        int onlineHelpers = (int) helperProfileRepository.countByStatus(HelperStatus.ONLINE);

        BigDecimal todayRevenue = walletTransactionRepository.sumRevenueInPeriod(dayStart, dayEnd);
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;

        int pendingVerifications = (int) helperProfileRepository.countByBackgroundVerifiedFalse();

        Double avgRatingVal = reviewRepository.findOverallAverageRating();
        double avgRating = avgRatingVal != null ? avgRatingVal : 0.0;

        // Bookings by service type
        Map<String, Integer> bookingsByService = new LinkedHashMap<>();
        for (ServiceType st : ServiceType.values()) {
            long count = bookingRepository.countByServiceTypeAndCreatedAtBetween(st, dayStart, dayEnd);
            bookingsByService.put(st.name(), (int) count);
        }

        // Revenue chart — last 7 days
        List<DashboardResponse.RevenueDataPoint> revenueChart = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Instant from = date.atStartOfDay(ZONE).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(ZONE).toInstant();
            BigDecimal rev = walletTransactionRepository.sumRevenueInPeriod(from, to);
            revenueChart.add(DashboardResponse.RevenueDataPoint.builder()
                    .date(date.format(fmt))
                    .revenue(rev != null ? rev : BigDecimal.ZERO)
                    .build());
        }

        return DashboardResponse.builder()
                .todayBookings(todayBookings)
                .activeBookings(activeBookings)
                .onlineHelpers(onlineHelpers)
                .todayRevenue(todayRevenue)
                .pendingVerifications(pendingVerifications)
                .avgRating(avgRating)
                .bookingsByService(bookingsByService)
                .revenueChart(revenueChart)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ANALYTICS — BOOKINGS
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public BookingAnalyticsResponse getBookingAnalytics(Instant from, Instant to, String groupBy) {
        if (from == null) from = Instant.now().minus(30, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();
        if (groupBy == null) groupBy = "DAY";

        List<BookingAnalyticsResponse.BookingDataPoint> data = new ArrayList<>();
        LocalDate startDate = from.atZone(ZONE).toLocalDate();
        LocalDate endDate = to.atZone(ZONE).toLocalDate();

        switch (groupBy.toUpperCase()) {
            case "WEEK" -> {
                LocalDate weekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                while (!weekStart.isAfter(endDate)) {
                    LocalDate weekEnd = weekStart.plusWeeks(1);
                    data.add(buildBookingDataPoint(
                            "W" + weekStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            weekStart, weekEnd));
                    weekStart = weekEnd;
                }
            }
            case "MONTH" -> {
                LocalDate monthStart = startDate.withDayOfMonth(1);
                while (!monthStart.isAfter(endDate)) {
                    LocalDate monthEnd = monthStart.plusMonths(1);
                    data.add(buildBookingDataPoint(
                            monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                            monthStart, monthEnd));
                    monthStart = monthEnd;
                }
            }
            default -> { // DAY
                LocalDate current = startDate;
                while (!current.isAfter(endDate)) {
                    data.add(buildBookingDataPoint(
                            current.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            current, current.plusDays(1)));
                    current = current.plusDays(1);
                }
            }
        }

        return BookingAnalyticsResponse.builder().data(data).build();
    }

    private BookingAnalyticsResponse.BookingDataPoint buildBookingDataPoint(
            String period, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZONE).toInstant();
        Instant end = to.atStartOfDay(ZONE).toInstant();
        return BookingAnalyticsResponse.BookingDataPoint.builder()
                .period(period)
                .total(bookingRepository.countByCreatedAtBetween(start, end))
                .completed(bookingRepository.countByStatusAndCreatedAtBetween(BookingStatus.COMPLETED, start, end))
                .cancelled(bookingRepository.countByStatusAndCreatedAtBetween(BookingStatus.CANCELLED, start, end))
                .pending(bookingRepository.countByStatusAndCreatedAtBetween(BookingStatus.PENDING_ASSIGNMENT, start, end))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ANALYTICS — REVENUE
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public RevenueAnalyticsResponse getRevenueAnalytics(Instant from, Instant to) {
        if (from == null) from = Instant.now().minus(30, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();

        BigDecimal totalRevenue = walletTransactionRepository.sumRevenueInPeriod(from, to);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        BigDecimal platformFee = totalRevenue.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);

        // Daily breakdown
        List<DashboardResponse.RevenueDataPoint> data = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = from.atZone(ZONE).toLocalDate();
        LocalDate endDate = to.atZone(ZONE).toLocalDate();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Instant dayStart = current.atStartOfDay(ZONE).toInstant();
            Instant dayEnd = current.plusDays(1).atStartOfDay(ZONE).toInstant();
            BigDecimal rev = walletTransactionRepository.sumRevenueInPeriod(dayStart, dayEnd);
            data.add(DashboardResponse.RevenueDataPoint.builder()
                    .date(current.format(fmt))
                    .revenue(rev != null ? rev : BigDecimal.ZERO)
                    .build());
            current = current.plusDays(1);
        }

        return RevenueAnalyticsResponse.builder()
                .totalRevenue(totalRevenue)
                .platformFee(platformFee)
                .data(data)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ANALYTICS — HELPERS
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<HelperAnalyticsItem> getHelperAnalytics(String metric, int limit) {
        List<HelperProfile> allHelpers = helperProfileRepository.findAll();

        return switch (metric != null ? metric : "topRated") {
            case "mostBooked" -> allHelpers.stream()
                    .sorted(Comparator.comparingInt(HelperProfile::getTotalJobsCompleted).reversed())
                    .limit(limit)
                    .map(this::toHelperAnalyticsItem)
                    .toList();
            case "highestEarning" -> allHelpers.stream()
                    .map(hp -> {
                        BigDecimal earnings = walletTransactionRepository.sumAmountByUserIdAndType(
                                hp.getUser().getId(), TransactionType.CREDIT_EARNING);
                        return Map.entry(hp, earnings != null ? earnings : BigDecimal.ZERO);
                    })
                    .sorted(Map.Entry.<HelperProfile, BigDecimal>comparingByValue().reversed())
                    .limit(limit)
                    .map(e -> toHelperAnalyticsItemWithEarnings(e.getKey(), e.getValue()))
                    .toList();
            default -> // topRated
                    allHelpers.stream()
                            .sorted(Comparator.comparingDouble(HelperProfile::getRating).reversed())
                            .limit(limit)
                            .map(this::toHelperAnalyticsItem)
                            .toList();
        };
    }

    private HelperAnalyticsItem toHelperAnalyticsItem(HelperProfile hp) {
        BigDecimal earnings = walletTransactionRepository.sumAmountByUserIdAndType(
                hp.getUser().getId(), TransactionType.CREDIT_EARNING);
        return HelperAnalyticsItem.builder()
                .helperId(hp.getId())
                .userId(hp.getUser().getId())
                .name(hp.getUser().getName())
                .rating(hp.getRating())
                .totalJobsCompleted(hp.getTotalJobsCompleted())
                .totalEarnings(earnings != null ? earnings : BigDecimal.ZERO)
                .build();
    }

    private HelperAnalyticsItem toHelperAnalyticsItemWithEarnings(HelperProfile hp, BigDecimal earnings) {
        return HelperAnalyticsItem.builder()
                .helperId(hp.getId())
                .userId(hp.getUser().getId())
                .name(hp.getUser().getName())
                .rating(hp.getRating())
                .totalJobsCompleted(hp.getTotalJobsCompleted())
                .totalEarnings(earnings)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ANALYTICS — CUSTOMERS
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<CustomerAnalyticsItem> getCustomerAnalytics(String metric, int limit) {
        Page<User> customers = userRepository.findAllWithFilters(Role.CUSTOMER, true, null,
                PageRequest.of(0, 1000));

        return switch (metric != null ? metric : "mostActive") {
            case "highestSpend" -> customers.getContent().stream()
                    .map(u -> {
                        BigDecimal spent = walletTransactionRepository.sumAmountByUserIdAndType(
                                u.getId(), TransactionType.DEBIT_BOOKING);
                        int total = (int) bookingRepository.countByCustomerId(u.getId());
                        return CustomerAnalyticsItem.builder()
                                .userId(u.getId())
                                .name(u.getName())
                                .email(u.getEmail())
                                .totalBookings(total)
                                .totalSpent(spent != null ? spent : BigDecimal.ZERO)
                                .build();
                    })
                    .sorted(Comparator.comparing(CustomerAnalyticsItem::getTotalSpent).reversed())
                    .limit(limit)
                    .toList();
            default -> // mostActive
                    customers.getContent().stream()
                            .map(u -> {
                                int total = (int) bookingRepository.countByCustomerId(u.getId());
                                BigDecimal spent = walletTransactionRepository.sumAmountByUserIdAndType(
                                        u.getId(), TransactionType.DEBIT_BOOKING);
                                return CustomerAnalyticsItem.builder()
                                        .userId(u.getId())
                                        .name(u.getName())
                                        .email(u.getEmail())
                                        .totalBookings(total)
                                        .totalSpent(spent != null ? spent : BigDecimal.ZERO)
                                        .build();
                            })
                            .sorted(Comparator.comparingInt(CustomerAnalyticsItem::getTotalBookings).reversed())
                            .limit(limit)
                            .toList();
        };
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SERVICE CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ServiceConfigDto> getAllServiceConfigs() {
        return serviceConfigCache.getAll().stream()
                .map(this::toServiceConfigDto)
                .toList();
    }

    @Transactional
    public ServiceConfigDto updateServiceConfig(ServiceType serviceType, UpdateServiceConfigRequest request) {
        ServiceConfig config = serviceConfigRepository.findByServiceType(serviceType)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceConfig", "serviceType", serviceType));

        if (request.getBasePrice() != null) config.setBasePrice(request.getBasePrice());
        if (request.getPerHourPrice() != null) config.setPerHourPrice(request.getPerHourPrice());
        if (request.getActive() != null) config.setActive(request.getActive());

        serviceConfigRepository.save(config);
        serviceConfigCache.refresh(serviceType);

        log.info("Service config updated: {} — basePrice={}, perHourPrice={}, active={}",
                serviceType, config.getBasePrice(), config.getPerHourPrice(), config.isActive());

        return toServiceConfigDto(config);
    }

    private ServiceConfigDto toServiceConfigDto(ServiceConfig sc) {
        return ServiceConfigDto.builder()
                .id(sc.getId())
                .serviceType(sc.getServiceType())
                .name(sc.getName())
                .basePrice(sc.getBasePrice())
                .perHourPrice(sc.getPerHourPrice())
                .icon(sc.getIcon())
                .active(sc.isActive())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CUSTOMER MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        CustomerProfile cp = customerProfileRepository.findByUserId(userId).orElse(null);

        CustomerProfileDto profileDto = CustomerProfileDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .addressLine(cp != null ? cp.getAddressLine() : null)
                .city(cp != null ? cp.getCity() : null)
                .state(cp != null ? cp.getState() : null)
                .pincode(cp != null ? cp.getPincode() : null)
                .latitude(cp != null ? cp.getLatitude() : 0)
                .longitude(cp != null ? cp.getLongitude() : 0)
                .preferredLanguage(cp != null ? cp.getPreferredLanguage() : null)
                .build();

        Page<Booking> bookings = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, 10));
        List<BookingResponse> recentBookings = bookings.getContent().stream()
                .map(this::toBookingDto)
                .toList();

        WalletResponse wallet = null;
        try {
            wallet = walletService.getWalletBalance(userId);
        } catch (Exception e) {
            log.debug("No wallet found for customer {}", userId);
        }

        int totalBookings = (int) bookingRepository.countByCustomerId(userId);

        return CustomerDetailResponse.builder()
                .profile(profileDto)
                .recentBookings(recentBookings)
                .wallet(wallet)
                .totalBookings(totalBookings)
                .build();
    }

    @Transactional
    public void banCustomer(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setActive(false);
        userRepository.save(user);

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllByUserId(userId);

        // Add to banned set so JWT filter rejects existing access tokens
        bannedUserStore.ban(userId);

        log.info("Customer {} banned by admin", userId);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPER MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public HelperDetailResponse getHelperDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        HelperProfile hp = helperProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("HelperProfile", "userId", userId));

        HelperProfileDto profileDto = HelperProfileDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .skills(hp.getSkills())
                .latitude(hp.getLatitude())
                .longitude(hp.getLongitude())
                .city(hp.getCity())
                .pincode(hp.getPincode())
                .available(hp.isAvailable())
                .backgroundVerified(hp.isBackgroundVerified())
                .idProofUrl(hp.getIdProofUrl())
                .rating(hp.getRating())
                .totalJobsCompleted(hp.getTotalJobsCompleted())
                .status(hp.getStatus())
                .build();

        List<BookingStatus> statuses = List.of(
                BookingStatus.ASSIGNED, BookingStatus.HELPER_EN_ROUTE,
                BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED);
        Page<Booking> bookings = bookingRepository.findByHelperIdAndStatusInOrderByCreatedAtDesc(
                userId, statuses, PageRequest.of(0, 10));
        List<BookingResponse> recentBookings = bookings.getContent().stream()
                .map(this::toBookingDto)
                .toList();

        WalletResponse earnings = null;
        try {
            earnings = walletService.getWalletBalance(userId);
        } catch (Exception e) {
            log.debug("No wallet found for helper {}", userId);
        }

        BigDecimal totalEarnings = walletTransactionRepository.sumAmountByUserIdAndType(
                userId, TransactionType.CREDIT_EARNING);

        Page<Review> reviews = reviewRepository.findByHelperIdAndPublishedTrueOrderByCreatedAtDesc(
                userId, PageRequest.of(0, 5));
        List<ReviewResponse> recentReviews = reviews.getContent().stream()
                .map(this::toReviewDto)
                .toList();

        int totalBookings = (int) bookingRepository.countByHelperId(userId);

        return HelperDetailResponse.builder()
                .profile(profileDto)
                .recentBookings(recentBookings)
                .earnings(earnings)
                .totalEarnings(totalEarnings != null ? totalEarnings : BigDecimal.ZERO)
                .totalBookings(totalBookings)
                .recentReviews(recentReviews)
                .build();
    }

    @Transactional
    public void suspendHelper(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setActive(false);
        userRepository.save(user);

        helperProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setStatus(HelperStatus.OFFLINE);
            profile.setAvailable(false);
            helperProfileRepository.save(profile);
        });

        refreshTokenRepository.revokeAllByUserId(userId);
        bannedUserStore.ban(userId);

        log.info("Helper {} suspended by admin", userId);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  BOOKING MANAGEMENT — DETAIL + ACTIONS
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        BookingResponse bookingDto = toBookingDto(booking);

        List<BookingStatusHistoryResponse> history = statusHistoryRepository
                .findByBookingIdOrderByChangedAtDesc(bookingId).stream()
                .map(h -> BookingStatusHistoryResponse.builder()
                        .id(h.getId())
                        .bookingId(bookingId)
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .changedBy(h.getChangedBy())
                        .reason(h.getReason())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();

        List<LocationHistory> locationHistory = locationHistoryRepository
                .findByBookingIdOrderByRecordedAtAsc(bookingId);

        // For the location trail, just convert to a simple representation
        var locationTrail = locationHistory.stream()
                .map(lh -> com.homecare.tracking.dto.LocationBroadcast.builder()
                        .helperId(lh.getHelper().getId())
                        .bookingId(bookingId)
                        .lat(lh.getLatitude())
                        .lng(lh.getLongitude())
                        .timestamp(lh.getRecordedAt())
                        .heading(lh.getHeading())
                        .speedKmh(lh.getSpeedKmh())
                        .build())
                .toList();

        return BookingDetailResponse.builder()
                .booking(bookingDto)
                .statusHistory(history)
                .locationTrail(locationTrail)
                .build();
    }

    @Transactional
    public BookingResponse reassignBooking(UUID bookingId, UUID newHelperId, UUID adminId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // Restore old helper if any
        if (booking.getHelper() != null) {
            helperProfileRepository.findByUserId(booking.getHelper().getId()).ifPresent(profile -> {
                profile.setStatus(HelperStatus.ONLINE);
                profile.setAvailable(true);
                helperProfileRepository.save(profile);
            });
        }

        // Reset booking to PENDING_ASSIGNMENT and then re-assign
        booking.setHelper(null);
        booking.setAcceptedAt(null);
        booking.setStatus(BookingStatus.PENDING_ASSIGNMENT);
        bookingRepository.save(booking);

        return bookingService.adminAssignHelper(bookingId, newHelperId, adminId);
    }

    @Transactional
    public BookingResponse forceCompleteBooking(UUID bookingId, UUID adminId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Booking is already completed", ErrorCode.BOOKING_CONFLICT);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Cannot complete a cancelled booking", ErrorCode.BOOKING_CONFLICT);
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        booking.setPaymentStatus(PaymentStatus.PAID);

        // Restore helper
        if (booking.getHelper() != null) {
            helperProfileRepository.findByUserId(booking.getHelper().getId()).ifPresent(profile -> {
                profile.setStatus(HelperStatus.ONLINE);
                profile.setAvailable(true);
                helperProfileRepository.save(profile);
            });
        }

        bookingRepository.save(booking);

        // Record status change
        BookingStatusHistory history = BookingStatusHistory.builder()
                .booking(booking)
                .fromStatus(oldStatus)
                .toStatus(BookingStatus.COMPLETED)
                .changedBy(adminId.toString())
                .reason("Force-completed by admin (dispute resolution)")
                .changedAt(Instant.now())
                .build();
        statusHistoryRepository.save(history);

        // Send review prompt to customer
        notificationService.sendToUser(
                booking.getCustomer().getId(),
                NotificationType.BOOKING_COMPLETED,
                Map.of("bookingId", bookingId.toString(),
                       "message", "Your booking has been completed. Please rate your experience!"));

        log.info("Booking {} force-completed by admin {}", bookingId, adminId);
        return toBookingDto(booking);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DTO MAPPERS
    // ═══════════════════════════════════════════════════════════════════

    private BookingResponse toBookingDto(Booking booking) {
        return bookingResponseMapper.toDto(booking);
    }

    private ReviewResponse toReviewDto(Review review) {
        return reviewResponseMapper.toDto(review);
    }
}

