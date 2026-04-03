package com.homecare.booking.service;

import com.homecare.booking.config.BookingConfig;
import com.homecare.booking.dto.*;
import com.homecare.booking.entity.Booking;
import com.homecare.booking.entity.BookingStatusHistory;
import com.homecare.booking.mapper.BookingResponseMapper;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.booking.repository.BookingStatusHistoryRepository;
import com.homecare.booking.statemachine.BookingStateMachine;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.*;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.core.logging.AuditEvent;
import com.homecare.core.util.GeoUtils;
import com.homecare.payment.service.PaymentService;
import com.homecare.scheduler.BookingAutoExpireJob;
import com.homecare.scheduler.BookingReminderJob;
import com.homecare.scheduler.ScheduledBookingTriggerJob;
import com.homecare.subscription.entity.CustomerSubscription;
import com.homecare.subscription.repository.CustomerSubscriptionRepository;
import com.homecare.subscription.service.SubscriptionService;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.entity.User;
import com.homecare.user.enums.HelperStatus;
import com.homecare.user.repository.FavouriteHelperRepository;
import com.homecare.user.repository.HelperProfileRepository;
import com.homecare.user.repository.UserRepository;
import com.homecare.user.service.FavouriteHelperService;
import com.homecare.user.service.HelperAvailabilityService;
import com.homecare.referral.service.ReferralService;
import com.homecare.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final HelperProfileRepository helperProfileRepository;
    private final BookingConfig bookingConfig;
    private final SimpMessagingTemplate messagingTemplate;
    private final Scheduler quartzScheduler;
    private final TrackingService trackingService;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentService paymentService;
    private final BookingResponseMapper bookingResponseMapper;
    private final BookingStateMachine stateMachine;
    private final SubscriptionService subscriptionService;
    private final CustomerSubscriptionRepository customerSubscriptionRepository;
    private final FavouriteHelperRepository favouriteHelperRepository;
    private final FavouriteHelperService favouriteHelperService;
    private final HelperAvailabilityService helperAvailabilityService;
    private final ReferralService referralService;

    // ─── Create Booking ───────────────────────────────────────────────

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, UUID customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", customerId));

        // Validate scheduled bookings
        if (request.getBookingType() == BookingType.SCHEDULED) {
            validateScheduledAt(request.getScheduledAt());
        }

        // Compute pricing
        double basePrice = bookingConfig.getBasePrice(request.getServiceType());
        double totalPrice = bookingConfig.computeTotalPrice(request.getServiceType(), request.getDurationHours());

        Booking booking = Booking.builder()
                .customer(customer)
                .serviceType(request.getServiceType())
                .bookingType(request.getBookingType())
                .status(BookingStatus.PENDING_ASSIGNMENT)
                .scheduledAt(request.getScheduledAt())
                .addressLine(request.getAddressLine())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .durationHours(request.getDurationHours())
                .specialInstructions(request.getSpecialInstructions())
                .basePrice(basePrice)
                .totalPrice(totalPrice)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        // If subscription-backed booking, validate and link
        if (request.getSubscriptionId() != null) {
            CustomerSubscription subscription = customerSubscriptionRepository
                    .findById(request.getSubscriptionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "CustomerSubscription", "id", request.getSubscriptionId()));

            if (subscription.getStatus() != com.homecare.core.enums.SubscriptionStatus.ACTIVE) {
                throw new BusinessException("Subscription is not active", ErrorCode.SUBSCRIPTION_CANNOT_BE_CANCELLED);
            }
            if (subscription.getSessionsUsedThisCycle() >= subscription.getPlan().getSessionsPerCycle()) {
                throw new BusinessException(
                        "All sessions for this subscription cycle have been used",
                        ErrorCode.SUBSCRIPTION_CANNOT_BE_CANCELLED);
            }
            booking.setSubscription(subscription);
            booking.setPaymentStatus(PaymentStatus.PAID); // Subscription covers payment
        }

        booking = bookingRepository.save(booking);
        recordStatusChange(booking, null, BookingStatus.PENDING_ASSIGNMENT, customerId.toString(), "Booking created");

        if (request.getRequestedHelperId() != null) {
            // Preferred helper — validate and assign directly, never silently fall through
            assignRequestedHelper(booking, customerId, request.getRequestedHelperId());
            booking = bookingRepository.save(booking);
        } else if (request.getBookingType() == BookingType.IMMEDIATE) {
            // Auto-assign nearest helper
            findAndAssignHelper(booking);
            booking = bookingRepository.save(booking);
        } else {
            // Schedule helper assignment 30 min before scheduledAt
            scheduleHelperAssignment(booking);
        }

        // Schedule auto-expire after configured minutes
        scheduleAutoExpire(booking);

        // Notify customer
        notifyCustomer(booking, "Your booking has been created successfully");

        log.info("Booking created: {} by customer {}", booking.getId(), customerId);
        eventPublisher.publishEvent(AuditEvent.of("BOOKING_CREATED", customerId,
                Map.of("bookingId", booking.getId(), "serviceType", booking.getServiceType().name(),
                       "bookingType", booking.getBookingType().name())));
        return toDto(booking);
    }

    // ─── Customer: Get own bookings ───────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getCustomerBookings(UUID customerId, Pageable pageable) {
        Page<BookingResponse> page = bookingRepository
                .findByCustomerIdWithParties(customerId, pageable)
                .map(this::toDto);
        return PagedResponse.from(page);
    }

    // ─── Get booking detail ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId, UUID userId) {
        Booking booking = findBooking(bookingId);
        // Allow access for customer, assigned helper, or admin
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        boolean isAdmin = user.getRole() == com.homecare.user.enums.Role.ADMIN;
        if (!isAdmin
                && !booking.getCustomer().getId().equals(userId)
                && (booking.getHelper() == null || !booking.getHelper().getId().equals(userId))) {
            throw new BusinessException("You do not have access to this booking", ErrorCode.FORBIDDEN);
        }
        return toDto(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingForAdmin(UUID bookingId) {
        return toDto(findBooking(bookingId));
    }

    // ─── Customer cancel ──────────────────────────────────────────────

    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID cancelledBy) {
        Booking booking = findBooking(bookingId);

        // Validate ownership
        if (!booking.getCustomer().getId().equals(cancelledBy)) {
            throw new BusinessException("You can only cancel your own bookings", ErrorCode.FORBIDDEN);
        }

        BookingStatus currentStatus = booking.getStatus();

        // Determine refund based on cancellation policy
        boolean paymentMade = booking.getPaymentStatus() == PaymentStatus.PAID
                || booking.getPaymentStatus() == PaymentStatus.HELD;
        switch (currentStatus) {
            case PENDING_ASSIGNMENT -> {
                if (paymentMade) {
                    booking.setPaymentStatus(PaymentStatus.REFUNDED);
                    issueRefund(booking, BigDecimal.ONE);
                    log.info("Booking {} cancelled from PENDING_ASSIGNMENT — full refund", bookingId);
                } else {
                    log.info("Booking {} cancelled from PENDING_ASSIGNMENT — no payment to refund", bookingId);
                }
            }
            case ASSIGNED -> {
                if (paymentMade) {
                    if (booking.getScheduledAt() != null
                            && Duration.between(Instant.now(), booking.getScheduledAt()).toMinutes() > 30) {
                        booking.setPaymentStatus(PaymentStatus.REFUNDED);
                        issueRefund(booking, BigDecimal.ONE);
                        log.info("Booking {} cancelled from ASSIGNED (>30 min) — full refund", bookingId);
                    } else {
                        booking.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
                        issueRefund(booking, new BigDecimal("0.50"));
                        log.info("Booking {} cancelled from ASSIGNED (<30 min) — 50% refund", bookingId);
                    }
                } else {
                    log.info("Booking {} cancelled from ASSIGNED — no payment to refund", bookingId);
                }
                // Restore helper status
                restoreHelperStatus(booking);
            }
            case HELPER_EN_ROUTE, IN_PROGRESS -> {
                throw new BusinessException(
                        "Booking cannot be cancelled at this stage. Please contact admin.",
                        ErrorCode.BOOKING_CANNOT_BE_CANCELLED);
            }
            default -> {
                throw new BusinessException(
                        "Booking cannot be cancelled in status: " + currentStatus,
                        ErrorCode.BOOKING_CANNOT_BE_CANCELLED);
            }
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        recordStatusChange(booking, oldStatus, BookingStatus.CANCELLED, cancelledBy.toString(), "Cancelled by customer");

        // Notify helper if assigned
        if (booking.getHelper() != null) {
            notifyHelper(booking, "Booking has been cancelled by the customer");
        }

        log.info("Booking {} cancelled by customer {}", bookingId, cancelledBy);
        return toDto(booking);
    }

    // ─── Admin cancel (force) ─────────────────────────────────────────

    @Transactional
    public BookingResponse adminCancelBooking(UUID bookingId, String reason, UUID adminId) {
        Booking booking = findBooking(bookingId);

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Cannot cancel a booking that is already " + booking.getStatus(),
                    ErrorCode.BOOKING_CANNOT_BE_CANCELLED);
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUNDED);

        // Restore helper
        restoreHelperStatus(booking);

        bookingRepository.save(booking);
        recordStatusChange(booking, oldStatus, BookingStatus.CANCELLED, adminId.toString(),
                "Admin force cancel: " + (reason != null ? reason : "No reason provided"));

        notifyCustomer(booking, "Your booking has been cancelled by admin");
        if (booking.getHelper() != null) {
            notifyHelper(booking, "Booking has been cancelled by admin");
        }

        log.info("Booking {} force-cancelled by admin {}", bookingId, adminId);
        return toDto(booking);
    }

    // ─── Admin assign ─────────────────────────────────────────────────

    @Transactional
    public BookingResponse adminAssignHelper(UUID bookingId, UUID helperId, UUID adminId) {
        Booking booking = findBooking(bookingId);
        stateMachine.validateTransition(booking.getStatus(), BookingStatus.ASSIGNED);

        User helperUser = userRepository.findById(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("Helper", "id", helperId));
        HelperProfile helperProfile = helperProfileRepository.findByUserId(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("HelperProfile", "userId", helperId));

        if (bookingRepository.hasActiveBooking(helperId)) {
            throw new BusinessException("Helper already has an active booking", ErrorCode.HELPER_NOT_AVAILABLE);
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setHelper(helperUser);
        booking.setStatus(BookingStatus.ASSIGNED);
        booking.setAcceptedAt(Instant.now());

        helperProfile.setStatus(HelperStatus.ON_JOB);
        helperProfile.setAvailable(false);
        helperProfileRepository.save(helperProfile);

        bookingRepository.save(booking);
        recordStatusChange(booking, oldStatus, BookingStatus.ASSIGNED, adminId.toString(),
                "Manually assigned by admin");

        // Cancel the auto-expire timer
        cancelAutoExpireJob(booking.getId());

        // Schedule reminder 30 min before booking
        scheduleBookingReminder(booking);

        notifyCustomer(booking, "A helper has been assigned to your booking");
        notifyHelper(booking, "You have been assigned to a new booking by admin");
        trackingService.broadcastStatusChange(bookingId, BookingStatus.ASSIGNED, "Helper assigned");

        log.info("Booking {} assigned to helper {} by admin {}", bookingId, helperId, adminId);
        return toDto(booking);
    }

    // ─── Admin: Get all bookings ──────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getAllBookings(BookingStatus status, ServiceType serviceType,
                                                         Instant from, Instant to, Pageable pageable) {
        Page<BookingResponse> page = bookingRepository
                .findAllWithFiltersAndParties(status, serviceType, from, to, pageable)
                .map(this::toDto);
        return PagedResponse.from(page);
    }

    // ─── Available helpers ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AvailableHelperResponse> findAvailableHelpers(ServiceType serviceType,
                                                               double lat, double lng,
                                                               Double radiusKm,
                                                               Instant scheduledAt) {
        double radius = radiusKm != null ? radiusKm : bookingConfig.getBooking().getAutoAssignRadiusKm();
        double[] bbox = GeoUtils.boundingBox(lat, lng, radius);

        List<HelperProfile> helpers = helperProfileRepository
                .findNearbyAvailableHelpers(serviceType, lat, lng, radius,
                        bbox[0], bbox[1], bbox[2], bbox[3]);

        Instant resolvedTime = scheduledAt != null ? scheduledAt : Instant.now();

        return helpers.stream()
                .filter(h -> !bookingRepository.hasActiveBooking(h.getUser().getId()))
                .filter(h -> helperAvailabilityService.isHelperAvailableAt(h.getUser().getId(), resolvedTime))
                .map(h -> toAvailableHelperDto(h, lat, lng))
                .toList();
    }

    // ─── Available time slots for a service + location + date ─────────

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(ServiceType serviceType,
                                                          double lat, double lng,
                                                          LocalDate date,
                                                          Double radiusKm) {
        double radius = radiusKm != null ? radiusKm : bookingConfig.getBooking().getAutoAssignRadiusKm();
        double[] bbox = GeoUtils.boundingBox(lat, lng, radius);

        List<HelperProfile> helpers = helperProfileRepository
                .findNearbyAvailableHelpers(serviceType, lat, lng, radius,
                        bbox[0], bbox[1], bbox[2], bbox[3]);

        // Pre-filter: remove helpers already on active bookings
        List<HelperProfile> freeHelpers = helpers.stream()
                .filter(h -> !bookingRepository.hasActiveBooking(h.getUser().getId()))
                .toList();

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        List<AvailableSlotResponse> slots = new java.util.ArrayList<>();

        for (int hour = 8; hour <= 20; hour++) {
            LocalTime slotTime = LocalTime.of(hour, 0);
            Instant slotInstant = date.atTime(slotTime).atZone(zone).toInstant();

            int count = (int) freeHelpers.stream()
                    .filter(h -> helperAvailabilityService.isHelperAvailableAt(h.getUser().getId(), slotInstant))
                    .count();

            slots.add(AvailableSlotResponse.builder()
                    .time(slotTime)
                    .availableHelperCount(count)
                    .build());
        }

        return slots;
    }

    // ─── Helper: Get assigned bookings ────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getHelperBookings(UUID helperId, Pageable pageable) {
        List<BookingStatus> statuses = List.of(
                BookingStatus.ASSIGNED, BookingStatus.HELPER_EN_ROUTE,
                BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED);
        Page<BookingResponse> page = bookingRepository
                .findByHelperIdWithParties(helperId, statuses, pageable)
                .map(this::toDto);
        return PagedResponse.from(page);
    }

    // ─── Helper: Get pending bookings ─────────────────────────────────

    @Transactional(readOnly = true)
    public List<BookingResponse> getPendingBookingsForHelper(UUID helperId) {
        HelperProfile profile = helperProfileRepository.findByUserId(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("HelperProfile", "userId", helperId));

        double lat = profile.getLatitude();
        double lng = profile.getLongitude();
        double radius = bookingConfig.getBooking().getAutoAssignRadiusKm();

        // Find PENDING_ASSIGNMENT bookings and filter by helper skills + proximity
        List<Booking> pending = bookingRepository.findByStatus(BookingStatus.PENDING_ASSIGNMENT);

        return pending.stream()
                .filter(b -> profile.getSkills().contains(b.getServiceType()))
                .filter(b -> GeoUtils.haversineDistance(lat, lng, b.getLatitude(), b.getLongitude()) <= radius)
                .map(this::toDto)
                .toList();
    }

    // ─── Helper: Accept booking ───────────────────────────────────────

    @Transactional
    public BookingResponse acceptBooking(UUID bookingId, UUID helperId) {
        Booking booking = findBooking(bookingId);
        stateMachine.validateTransition(booking.getStatus(), BookingStatus.ASSIGNED);

        User helperUser = userRepository.findById(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", helperId));
        HelperProfile helperProfile = helperProfileRepository.findByUserId(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("HelperProfile", "userId", helperId));

        if (bookingRepository.hasActiveBooking(helperId)) {
            throw new BusinessException("You already have an active booking", ErrorCode.BOOKING_CONFLICT);
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setHelper(helperUser);
        booking.setStatus(BookingStatus.ASSIGNED);
        booking.setAcceptedAt(Instant.now());

        helperProfile.setStatus(HelperStatus.ON_JOB);
        helperProfile.setAvailable(false);
        helperProfileRepository.save(helperProfile);

        bookingRepository.save(booking);
        recordStatusChange(booking, oldStatus, BookingStatus.ASSIGNED, helperId.toString(), "Accepted by helper");

        // Cancel the auto-expire timer
        cancelAutoExpireJob(booking.getId());

        // Schedule reminder 30 min before booking
        scheduleBookingReminder(booking);

        notifyCustomer(booking, "A helper has accepted your booking");

        log.info("Booking {} accepted by helper {}", bookingId, helperId);
        return toDto(booking);
    }

    // ─── Helper: Reject booking ───────────────────────────────────────

    @Transactional
    public BookingResponse rejectBooking(UUID bookingId, UUID helperId) {
        Booking booking = findBooking(bookingId);

        // Only allow rejection if the booking is ASSIGNED to this helper
        if (booking.getStatus() != BookingStatus.ASSIGNED) {
            throw new BusinessException("Cannot reject booking in status: " + booking.getStatus(),
                    ErrorCode.BOOKING_CONFLICT);
        }

        if (booking.getHelper() == null || !booking.getHelper().getId().equals(helperId)) {
            throw new BusinessException("This booking is not assigned to you", ErrorCode.FORBIDDEN);
        }

        BookingStatus oldStatus = booking.getStatus();

        // Restore helper status
        restoreHelperStatus(booking);

        // Remove helper assignment
        booking.setHelper(null);
        booking.setAcceptedAt(null);
        booking.setStatus(BookingStatus.PENDING_ASSIGNMENT);

        bookingRepository.save(booking);
        recordStatusChange(booking, oldStatus, BookingStatus.PENDING_ASSIGNMENT,
                helperId.toString(), "Rejected by helper — re-assigning");

        // Try to find next available helper
        findAndAssignHelper(booking);
        bookingRepository.save(booking);

        log.info("Booking {} rejected by helper {}, status: {}", bookingId, helperId, booking.getStatus());
        return toDto(booking);
    }

    // ─── Helper: Start travel ─────────────────────────────────────────

    @Transactional
    public BookingResponse startTravel(UUID bookingId, UUID helperId) {
        Booking booking = findBooking(bookingId);
        validateHelperOwnership(booking, helperId);
        stateMachine.validateTransition(booking.getStatus(), BookingStatus.HELPER_EN_ROUTE);

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.HELPER_EN_ROUTE);
        bookingRepository.save(booking);
        recordStatusChange(booking, oldStatus, BookingStatus.HELPER_EN_ROUTE, helperId.toString(), "Helper en route");

        notifyCustomer(booking, "Your helper is on the way!");
        trackingService.broadcastStatusChange(bookingId, BookingStatus.HELPER_EN_ROUTE, "Helper en route");

        log.info("Booking {} — helper {} started travel", bookingId, helperId);
        return toDto(booking);
    }

    // ─── Helper: Start job ────────────────────────────────────────────

    @Transactional
    public BookingResponse startJob(UUID bookingId, UUID helperId) {
        Booking booking = findBooking(bookingId);
        validateHelperOwnership(booking, helperId);
        stateMachine.validateTransition(booking.getStatus(), BookingStatus.IN_PROGRESS);

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setStartedAt(Instant.now());
        bookingRepository.save(booking);
        recordStatusChange(booking, oldStatus, BookingStatus.IN_PROGRESS, helperId.toString(), "Job started");

        notifyCustomer(booking, "Your helper has arrived and started working");
        trackingService.broadcastStatusChange(bookingId, BookingStatus.IN_PROGRESS, "Job started");

        log.info("Booking {} — helper {} started job", bookingId, helperId);
        return toDto(booking);
    }

    // ─── Helper: Complete job ─────────────────────────────────────────

    @Transactional
    public BookingResponse completeJob(UUID bookingId, UUID helperId) {
        Booking booking = findBooking(bookingId);
        validateHelperOwnership(booking, helperId);
        stateMachine.validateTransition(booking.getStatus(), BookingStatus.COMPLETED);

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        booking.setPaymentStatus(PaymentStatus.PAID);

        // Restore helper status and increment job count in a single fetch
        if (booking.getHelper() != null) {
            HelperProfile helperProfile = helperProfileRepository.findByUserId(helperId)
                    .orElseThrow(() -> new ResourceNotFoundException("HelperProfile", "userId", helperId));
            helperProfile.setStatus(HelperStatus.ONLINE);
            helperProfile.setAvailable(true);
            helperProfile.setTotalJobsCompleted(helperProfile.getTotalJobsCompleted() + 1);
            helperProfileRepository.save(helperProfile);
        }

        bookingRepository.save(booking);
        recordStatusChange(booking, oldStatus, BookingStatus.COMPLETED, helperId.toString(), "Job completed");

        // Release wallet hold — finalize customer deduction and credit helper
        // If subscription-backed, increment session used instead of normal wallet release
        if (booking.getSubscription() != null) {
            try {
                subscriptionService.incrementSessionUsed(booking.getSubscription().getId());
            } catch (Exception e) {
                log.error("Failed to increment subscription session for booking {}: {}", bookingId, e.getMessage());
            }
        } else {
            try {
                paymentService.releaseBookingPayment(bookingId);
            } catch (Exception e) {
                log.error("Failed to release wallet hold for booking {}: {}", bookingId, e.getMessage());
            }
        }

        notifyCustomer(booking, "Your booking has been completed. Please rate your experience!");
        trackingService.broadcastStatusChange(bookingId, BookingStatus.COMPLETED, "Job completed");

        // Increment favourite helper bookings-together counter
        if (booking.getHelper() != null) {
            try {
                favouriteHelperService.incrementBookingsTogether(booking.getCustomer(), booking.getHelper());
            } catch (Exception e) {
                log.warn("Failed to increment favourite bookings-together: {}", e.getMessage());
            }
        }

        // Check and issue referrer credit if this is the referee's first completed booking
        try {
            referralService.checkAndIssueReferrerCredit(booking.getCustomer());
        } catch (Exception e) {
            log.warn("Failed to check referral credit for customer {}: {}", booking.getCustomer().getId(), e.getMessage());
        }

        log.info("Booking {} completed by helper {}", bookingId, helperId);
        eventPublisher.publishEvent(AuditEvent.of("BOOKING_COMPLETED", helperId,
                Map.of("bookingId", bookingId)));
        return toDto(booking);
    }

    // ─── Find and assign helper ───────────────────────────────────────

    @Transactional
    public void findAndAssignHelper(Booking booking) {
        double radius = bookingConfig.getBooking().getAutoAssignRadiusKm();
        double[] bbox = GeoUtils.boundingBox(booking.getLatitude(), booking.getLongitude(), radius);
        List<HelperProfile> candidates = helperProfileRepository.findNearbyAvailableHelpers(
                booking.getServiceType(), booking.getLatitude(), booking.getLongitude(), radius,
                bbox[0], bbox[1], bbox[2], bbox[3]);

        // Filter out helpers with active bookings
        List<HelperProfile> availableCandidates = candidates.stream()
                .filter(h -> !bookingRepository.hasActiveBooking(h.getUser().getId()))
                .toList();

        // Filter by availability slot — only keep helpers whose schedule covers the booking time
        Instant resolvedTime = booking.getScheduledAt() != null ? booking.getScheduledAt() : Instant.now();
        availableCandidates = availableCandidates.stream()
                .filter(h -> helperAvailabilityService.isHelperAvailableAt(h.getUser().getId(), resolvedTime))
                .toList();

        // Step 1: check if any of the customer's favourite helpers are available nearby
        List<UUID> favouriteHelperIds = favouriteHelperRepository
                .findHelperIdsByCustomerId(booking.getCustomer().getId());

        Optional<HelperProfile> bestHelper = Optional.empty();
        if (!favouriteHelperIds.isEmpty()) {
            bestHelper = availableCandidates.stream()
                    .filter(h -> favouriteHelperIds.contains(h.getUser().getId()))
                    .findFirst();
        }

        // Step 2: fall back to normal haversine sort (first available)
        if (bestHelper.isEmpty()) {
            bestHelper = availableCandidates.stream().findFirst();
        }

        if (bestHelper.isPresent()) {
            HelperProfile helperProfile = bestHelper.get();
            User helperUser = helperProfile.getUser();

            booking.setHelper(helperUser);
            booking.setStatus(BookingStatus.ASSIGNED);
            booking.setAcceptedAt(Instant.now());

            helperProfile.setStatus(HelperStatus.ON_JOB);
            helperProfile.setAvailable(false);
            helperProfileRepository.save(helperProfile);

            recordStatusChange(booking, BookingStatus.PENDING_ASSIGNMENT, BookingStatus.ASSIGNED,
                    "system", "Auto-assigned to nearest helper");

            // Cancel the auto-expire timer
            cancelAutoExpireJob(booking.getId());

            // Schedule reminder 30 min before booking
            scheduleBookingReminder(booking);

            notifyHelper(booking, "New booking request assigned to you");
            notifyCustomer(booking, "A helper has been assigned to your booking");

            log.info("Booking {} auto-assigned to helper {}", booking.getId(), helperUser.getId());
        } else {
            log.info("No available helpers found for booking {}. Status remains PENDING_ASSIGNMENT",
                    booking.getId());
            notifyCustomer(booking, "We're looking for a helper near you. Please wait.");
        }
    }

    // ─── Assign requested (preferred) helper ────────────────────────

    private void assignRequestedHelper(Booking booking, UUID customerId, UUID requestedHelperId) {
        // Validate: requested helper is in customer's favourites
        if (!favouriteHelperRepository.existsByCustomerIdAndHelperId(customerId, requestedHelperId)) {
            throw new BusinessException(
                    "Requested helper is not in your favourites. Add them first.",
                    ErrorCode.VALIDATION_FAILED);
        }

        // Validate: user exists and has a helper profile
        User helperUser = userRepository.findById(requestedHelperId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", requestedHelperId));
        HelperProfile helperProfile = helperProfileRepository.findByUserId(requestedHelperId)
                .orElseThrow(() -> new ResourceNotFoundException("HelperProfile", "userId", requestedHelperId));

        // Validate: helper has required skill
        if (!helperProfile.getSkills().contains(booking.getServiceType())) {
            throw new BusinessException(
                    "Your preferred helper does not offer " + booking.getServiceType() + " service.",
                    ErrorCode.PREFERRED_HELPER_UNAVAILABLE);
        }

        // Validate: helper is ONLINE and not ON_JOB
        if (helperProfile.getStatus() != HelperStatus.ONLINE) {
            throw new BusinessException(
                    "Your preferred helper is not available (status: " + helperProfile.getStatus()
                            + "). Book with a different helper or remove preference.",
                    ErrorCode.PREFERRED_HELPER_UNAVAILABLE);
        }

        // Validate: helper's availability slot covers the booking time
        Instant bookingTime = booking.getScheduledAt() != null ? booking.getScheduledAt() : Instant.now();
        if (!helperAvailabilityService.isHelperAvailableAt(requestedHelperId, bookingTime)) {
            throw new BusinessException(
                    "Your preferred helper is not available at the requested time. " +
                            "Check their availability schedule.",
                    ErrorCode.HELPER_OUTSIDE_AVAILABILITY);
        }

        if (bookingRepository.hasActiveBooking(requestedHelperId)) {
            throw new BusinessException(
                    "Your preferred helper is currently on another job. Book with a different helper or remove preference.",
                    ErrorCode.PREFERRED_HELPER_UNAVAILABLE);
        }

        // Assign directly
        booking.setHelper(helperUser);
        booking.setStatus(BookingStatus.ASSIGNED);
        booking.setAcceptedAt(Instant.now());

        helperProfile.setStatus(HelperStatus.ON_JOB);
        helperProfile.setAvailable(false);
        helperProfileRepository.save(helperProfile);

        recordStatusChange(booking, BookingStatus.PENDING_ASSIGNMENT, BookingStatus.ASSIGNED,
                customerId.toString(), "Assigned to preferred helper");

        cancelAutoExpireJob(booking.getId());
        scheduleBookingReminder(booking);

        notifyHelper(booking, "A customer has requested you for a booking");
        notifyCustomer(booking, "Your preferred helper has been assigned to your booking");

        log.info("Booking {} assigned to preferred helper {}", booking.getId(), requestedHelperId);
    }


    // ─── Helpers ──────────────────────────────────────────────────────

    private void validateScheduledAt(Instant scheduledAt) {
        if (scheduledAt == null) {
            throw new BusinessException("scheduledAt is required for SCHEDULED bookings",
                    ErrorCode.VALIDATION_FAILED);
        }
        if (scheduledAt.isBefore(Instant.now().plus(Duration.ofMinutes(30)))) {
            throw new BusinessException("Scheduled time must be at least 30 minutes from now",
                    ErrorCode.VALIDATION_FAILED);
        }
        int maxDays = bookingConfig.getBooking().getMaxAdvanceScheduleDays();
        if (scheduledAt.isAfter(Instant.now().plus(Duration.ofDays(maxDays)))) {
            throw new BusinessException("Cannot schedule more than " + maxDays + " days in advance",
                    ErrorCode.VALIDATION_FAILED);
        }
    }

    private void validateHelperOwnership(Booking booking, UUID helperId) {
        if (booking.getHelper() == null || !booking.getHelper().getId().equals(helperId)) {
            throw new BusinessException("This booking is not assigned to you", ErrorCode.FORBIDDEN);
        }
    }

    private Booking findBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
    }

    private void restoreHelperStatus(Booking booking) {
        if (booking.getHelper() != null) {
            helperProfileRepository.findByUserId(booking.getHelper().getId()).ifPresent(profile -> {
                profile.setStatus(HelperStatus.ONLINE);
                profile.setAvailable(true);
                helperProfileRepository.save(profile);
            });
        }
    }

    private void issueRefund(Booking booking, BigDecimal refundPercentage) {
        try {
            paymentService.refundBooking(booking.getId(), refundPercentage);
        } catch (Exception e) {
            log.error("Failed to issue refund for booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    private void recordStatusChange(Booking booking, BookingStatus from, BookingStatus to,
                                     String changedBy, String reason) {
        BookingStatusHistory history = BookingStatusHistory.builder()
                .booking(booking)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .reason(reason)
                .changedAt(Instant.now())
                .build();
        statusHistoryRepository.save(history);
    }

    private void scheduleAutoExpire(Booking booking) {
        try {
            int expireMinutes = bookingConfig.getBooking().getAutoExpireMinutes();
            JobDetail jobDetail = JobBuilder.newJob(BookingAutoExpireJob.class)
                    .withIdentity("expire-booking-" + booking.getId(), "booking-expiry")
                    .usingJobData("bookingId", booking.getId().toString())
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("expire-trigger-" + booking.getId(), "booking-expiry")
                    .startAt(Date.from(Instant.now().plus(Duration.ofMinutes(expireMinutes))))
                    .build();

            quartzScheduler.scheduleJob(jobDetail, trigger);
            log.debug("Scheduled auto-expire for booking {} in {} minutes", booking.getId(), expireMinutes);
        } catch (SchedulerException e) {
            log.error("Failed to schedule auto-expire for booking {}", booking.getId(), e);
        }
    }

    /**
     * Cancels the auto-expire Quartz job when a helper accepts / is assigned to the booking.
     */
    public void cancelAutoExpireJob(UUID bookingId) {
        try {
            boolean deleted = quartzScheduler.deleteJob(
                    JobKey.jobKey("expire-booking-" + bookingId, "booking-expiry"));
            if (deleted) {
                log.debug("Auto-expire job cancelled for booking {}", bookingId);
            }
        } catch (SchedulerException e) {
            log.error("Failed to cancel auto-expire job for booking {}", bookingId, e);
        }
    }

    private void scheduleHelperAssignment(Booking booking) {
        try {
            Instant triggerTime = booking.getScheduledAt().minus(Duration.ofMinutes(30));
            if (triggerTime.isBefore(Instant.now())) {
                triggerTime = Instant.now().plusSeconds(5);
            }

            JobDetail jobDetail = JobBuilder.newJob(ScheduledBookingTriggerJob.class)
                    .withIdentity("assign-booking-" + booking.getId(), "booking-assignment")
                    .usingJobData("bookingId", booking.getId().toString())
                    .usingJobData("attempt", 1)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("assign-trigger-" + booking.getId(), "booking-assignment")
                    .startAt(Date.from(triggerTime))
                    .build();

            quartzScheduler.scheduleJob(jobDetail, trigger);
            log.debug("Scheduled helper assignment for booking {} at {}", booking.getId(), triggerTime);
        } catch (SchedulerException e) {
            log.error("Failed to schedule helper assignment for booking {}", booking.getId(), e);
        }
    }

    /**
     * Schedules a one-shot reminder 30 minutes before the booking start time.
     * Called when a booking transitions to ASSIGNED.
     */
    private void scheduleBookingReminder(Booking booking) {
        try {
            Instant reminderTime;
            if (booking.getScheduledAt() != null) {
                reminderTime = booking.getScheduledAt().minus(Duration.ofMinutes(30));
            } else {
                // For IMMEDIATE bookings, the "start" is roughly now, so send reminder shortly
                reminderTime = Instant.now().plusSeconds(10);
            }

            if (reminderTime.isBefore(Instant.now())) {
                reminderTime = Instant.now().plusSeconds(5);
            }

            JobDetail jobDetail = JobBuilder.newJob(BookingReminderJob.class)
                    .withIdentity("reminder-booking-" + booking.getId(), "booking-reminder")
                    .usingJobData("bookingId", booking.getId().toString())
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("reminder-trigger-" + booking.getId(), "booking-reminder")
                    .startAt(Date.from(reminderTime))
                    .build();

            quartzScheduler.scheduleJob(jobDetail, trigger);
            log.debug("Scheduled booking reminder for booking {} at {}", booking.getId(), reminderTime);
        } catch (SchedulerException e) {
            log.error("Failed to schedule reminder for booking {}", booking.getId(), e);
        }
    }

    private void notifyCustomer(Booking booking, String message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    booking.getCustomer().getId().toString(),
                    "/queue/bookings",
                    Map.of("bookingId", booking.getId().toString(),
                           "status", booking.getStatus().name(),
                           "message", message));
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to customer: {}", e.getMessage());
        }
    }

    private void notifyHelper(Booking booking, String message) {
        if (booking.getHelper() == null) return;
        try {
            messagingTemplate.convertAndSendToUser(
                    booking.getHelper().getId().toString(),
                    "/queue/bookings",
                    Map.of("bookingId", booking.getId().toString(),
                           "status", booking.getStatus().name(),
                           "message", message));
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to helper: {}", e.getMessage());
        }
    }


    // ─── DTO mapping ──────────────────────────────────────────────────

    private BookingResponse toDto(Booking booking) {
        return bookingResponseMapper.toDto(booking);
    }

    private AvailableHelperResponse toAvailableHelperDto(HelperProfile profile, double lat, double lng) {
        double distance = GeoUtils.haversineDistance(lat, lng, profile.getLatitude(), profile.getLongitude());
        return AvailableHelperResponse.builder()
                .helperId(profile.getUser().getId())
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .avatarUrl(profile.getUser().getAvatarUrl())
                .skills(profile.getSkills())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .distanceKm(Math.round(distance * 100.0) / 100.0)
                .rating(profile.getRating())
                .totalJobsCompleted(profile.getTotalJobsCompleted())
                .status(profile.getStatus())
                .build();
    }
}

