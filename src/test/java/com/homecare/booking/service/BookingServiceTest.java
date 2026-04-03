package com.homecare.booking.service;

import com.homecare.booking.config.BookingConfig;
import com.homecare.booking.dto.BookingResponse;
import com.homecare.booking.dto.CreateBookingRequest;
import com.homecare.booking.entity.Booking;
import com.homecare.booking.mapper.BookingResponseMapper;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.booking.repository.BookingStatusHistoryRepository;
import com.homecare.booking.statemachine.BookingStateMachine;
import com.homecare.core.enums.*;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.payment.service.PaymentService;
import com.homecare.tracking.service.TrackingService;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.entity.User;
import com.homecare.user.enums.HelperStatus;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.FavouriteHelperRepository;
import com.homecare.user.repository.HelperProfileRepository;
import com.homecare.user.repository.UserRepository;
import com.homecare.user.service.FavouriteHelperService;
import com.homecare.user.service.HelperAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService")
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingStatusHistoryRepository statusHistoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private HelperProfileRepository helperProfileRepository;
    @Mock private BookingConfig bookingConfig;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private Scheduler quartzScheduler;
    @Mock private TrackingService trackingService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PaymentService paymentService;
    @Mock private BookingResponseMapper bookingResponseMapper;
    @Mock private BookingStateMachine stateMachine;
    @Mock private com.homecare.subscription.service.SubscriptionService subscriptionService;
    @Mock private com.homecare.subscription.repository.CustomerSubscriptionRepository customerSubscriptionRepository;
    @Mock private FavouriteHelperRepository favouriteHelperRepository;
    @Mock private FavouriteHelperService favouriteHelperService;
    @Mock private HelperAvailabilityService helperAvailabilityService;

    @InjectMocks private BookingService bookingService;

    private User customer;
    private User helper;
    private HelperProfile helperProfile;
    private Booking booking;
    private BookingConfig.BookingSettings bookingSettings;

    @BeforeEach
    void setUp() {
        customer = User.builder().name("Customer").email("c@test.com").role(Role.CUSTOMER).build();
        customer.setId(UUID.randomUUID());

        helper = User.builder().name("Helper").email("h@test.com").role(Role.HELPER).build();
        helper.setId(UUID.randomUUID());

        helperProfile = HelperProfile.builder()
                .user(helper).skills(List.of(ServiceType.CLEANING))
                .status(HelperStatus.ONLINE).available(true)
                .latitude(28.6).longitude(77.2).rating(4.5).build();
        helperProfile.setId(UUID.randomUUID());

        booking = Booking.builder()
                .customer(customer).serviceType(ServiceType.CLEANING)
                .bookingType(BookingType.IMMEDIATE).status(BookingStatus.PENDING_ASSIGNMENT)
                .addressLine("123 Main St").latitude(28.61).longitude(77.21)
                .durationHours(2).basePrice(299).totalPrice(597)
                .paymentStatus(PaymentStatus.PENDING).build();
        booking.setId(UUID.randomUUID());

        bookingSettings = new BookingConfig.BookingSettings();
        bookingSettings.setAutoAssignRadiusKm(10);
        bookingSettings.setAutoExpireMinutes(15);
        bookingSettings.setMaxAdvanceScheduleDays(30);
    }

    // ─── cancelBooking ────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("BUG FIX 4: PENDING_ASSIGNMENT with PENDING payment → no refund issued")
        void pendingPayment_noRefund() {
            booking.setPaymentStatus(PaymentStatus.PENDING);

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());

            bookingService.cancelBooking(booking.getId(), customer.getId());

            verify(paymentService, never()).refundBooking(any(), any());
            assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        }

        @Test
        @DisplayName("PENDING_ASSIGNMENT with PAID payment → full refund")
        void paidPayment_fullRefund() {
            booking.setPaymentStatus(PaymentStatus.PAID);

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());

            bookingService.cancelBooking(booking.getId(), customer.getId());

            verify(paymentService).refundBooking(eq(booking.getId()), any());
            assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        }

        @Test
        @DisplayName("not the booking owner → throws FORBIDDEN")
        void notOwner() {
            UUID otherUserId = UUID.randomUUID();
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(BusinessException.class,
                    () -> bookingService.cancelBooking(booking.getId(), otherUserId));
        }

        @Test
        @DisplayName("IN_PROGRESS → cannot cancel")
        void inProgress_cannotCancel() {
            booking.setStatus(BookingStatus.IN_PROGRESS);
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(BusinessException.class,
                    () -> bookingService.cancelBooking(booking.getId(), customer.getId()));
        }

        @Test
        @DisplayName("ASSIGNED with scheduledAt >30 min → full refund")
        void assigned_moreThan30Min() {
            booking.setStatus(BookingStatus.ASSIGNED);
            booking.setHelper(helper);
            booking.setPaymentStatus(PaymentStatus.PAID);
            booking.setScheduledAt(Instant.now().plus(Duration.ofHours(2)));

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(helperProfileRepository.save(any())).thenReturn(helperProfile);
            when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());

            bookingService.cancelBooking(booking.getId(), customer.getId());

            assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        }
    }

    // ─── completeJob ──────────────────────────────────────────────────

    @Nested
    @DisplayName("completeJob")
    class CompleteJob {

        @Test
        @DisplayName("BUG FIX 2: completeJob releases wallet hold")
        void releasesWalletHold() {
            booking.setStatus(BookingStatus.IN_PROGRESS);
            booking.setHelper(helper);

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(helperProfileRepository.save(any())).thenReturn(helperProfile);
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());

            bookingService.completeJob(booking.getId(), helper.getId());

            verify(paymentService).releaseBookingPayment(booking.getId());
            assertEquals(BookingStatus.COMPLETED, booking.getStatus());
            assertEquals(PaymentStatus.PAID, booking.getPaymentStatus());
            assertEquals(HelperStatus.ONLINE, helperProfile.getStatus());
            assertTrue(helperProfile.isAvailable());
            assertEquals(1, helperProfile.getTotalJobsCompleted());
        }

        @Test
        @DisplayName("wrong helper → throws FORBIDDEN")
        void wrongHelper() {
            booking.setStatus(BookingStatus.IN_PROGRESS);
            booking.setHelper(helper);
            UUID wrongHelperId = UUID.randomUUID();

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(BusinessException.class,
                    () -> bookingService.completeJob(booking.getId(), wrongHelperId));
        }
    }

    // ─── rejectBooking ────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectBooking")
    class RejectBooking {

        @Test
        @DisplayName("BUG FIX 3: PENDING_ASSIGNMENT status → cannot reject (must be ASSIGNED)")
        void pendingAssignment_cannotReject() {
            booking.setStatus(BookingStatus.PENDING_ASSIGNMENT);
            booking.setHelper(null);

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(BusinessException.class,
                    () -> bookingService.rejectBooking(booking.getId(), helper.getId()));
        }

        @Test
        @DisplayName("ASSIGNED status with correct helper → rejection succeeds")
        void assignedStatus_correctHelper() {
            booking.setStatus(BookingStatus.ASSIGNED);
            booking.setHelper(helper);

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(helperProfileRepository.save(any())).thenReturn(helperProfile);
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(bookingConfig.getBooking()).thenReturn(bookingSettings);
            when(helperProfileRepository.findNearbyAvailableHelpers(any(), anyDouble(), anyDouble(), anyDouble(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of());
            when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());

            BookingResponse response = bookingService.rejectBooking(booking.getId(), helper.getId());

            assertEquals(BookingStatus.PENDING_ASSIGNMENT, booking.getStatus());
            assertNull(booking.getHelper());
            assertNull(booking.getAcceptedAt());
        }
    }

    // ─── acceptBooking ────────────────────────────────────────────────

    @Nested
    @DisplayName("acceptBooking")
    class AcceptBooking {

        @Test
        @DisplayName("happy path — assigns helper and changes status")
        void happyPath() throws Exception {
            booking.setStatus(BookingStatus.PENDING_ASSIGNMENT);

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(bookingRepository.hasActiveBooking(helper.getId())).thenReturn(false);
            when(helperProfileRepository.save(any())).thenReturn(helperProfile);
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());
            when(quartzScheduler.deleteJob(any())).thenReturn(true);

            bookingService.acceptBooking(booking.getId(), helper.getId());

            assertEquals(BookingStatus.ASSIGNED, booking.getStatus());
            assertEquals(helper, booking.getHelper());
            assertEquals(HelperStatus.ON_JOB, helperProfile.getStatus());
        }

        @Test
        @DisplayName("helper already has active booking → throws")
        void alreadyHasActiveBooking() {
            booking.setStatus(BookingStatus.PENDING_ASSIGNMENT);

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(bookingRepository.hasActiveBooking(helper.getId())).thenReturn(true);

            assertThrows(BusinessException.class,
                    () -> bookingService.acceptBooking(booking.getId(), helper.getId()));
        }
    }

    // ─── startTravel / startJob ───────────────────────────────────────

    @Test
    @DisplayName("startTravel — ASSIGNED → HELPER_EN_ROUTE")
    void startTravel() {
        booking.setStatus(BookingStatus.ASSIGNED);
        booking.setHelper(helper);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());

        bookingService.startTravel(booking.getId(), helper.getId());

        assertEquals(BookingStatus.HELPER_EN_ROUTE, booking.getStatus());
        verify(trackingService).broadcastStatusChange(booking.getId(), BookingStatus.HELPER_EN_ROUTE, "Helper en route");
    }

    @Test
    @DisplayName("startJob — HELPER_EN_ROUTE → IN_PROGRESS")
    void startJob() {
        booking.setStatus(BookingStatus.HELPER_EN_ROUTE);
        booking.setHelper(helper);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());

        bookingService.startJob(booking.getId(), helper.getId());

        assertEquals(BookingStatus.IN_PROGRESS, booking.getStatus());
        assertNotNull(booking.getStartedAt());
    }

    // ─── getBooking access control ────────────────────────────────────

    @Test
    @DisplayName("getBooking — customer can access own booking")
    void getBooking_customerAccess() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());

        assertDoesNotThrow(() -> bookingService.getBooking(booking.getId(), customer.getId()));
    }

    @Test
    @DisplayName("getBooking — non-owner non-helper non-admin → throws FORBIDDEN")
    void getBooking_nonOwner() {
        UUID randomUserId = UUID.randomUUID();
        User randomUser = User.builder().name("Random").email("r@test.com").role(Role.CUSTOMER).build();
        randomUser.setId(randomUserId);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findById(randomUserId)).thenReturn(Optional.of(randomUser));

        assertThrows(BusinessException.class,
                () -> bookingService.getBooking(booking.getId(), randomUserId));
    }

    @Test
    @DisplayName("getBooking — assigned helper can access")
    void getBooking_helperAccess() {
        booking.setHelper(helper);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));
        when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());

        assertDoesNotThrow(() -> bookingService.getBooking(booking.getId(), helper.getId()));
    }

    @Test
    @DisplayName("BUG FIX 12: getBooking — admin can access any booking")
    void getBooking_adminAccess() {
        User admin = User.builder().name("Admin").email("admin@test.com").role(Role.ADMIN).build();
        admin.setId(UUID.randomUUID());

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());

        assertDoesNotThrow(() -> bookingService.getBooking(booking.getId(), admin.getId()));
    }

    // ─── Booking not found ────────────────────────────────────────────

    @Test
    @DisplayName("non-existent booking → throws ResourceNotFoundException")
    void bookingNotFound() {
        UUID nonExistent = UUID.randomUUID();
        when(bookingRepository.findById(nonExistent)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.getBooking(nonExistent, customer.getId()));
    }

    // ─── Preferred helper (requestedHelperId) ──────────────────────

    @Nested
    @DisplayName("RequestedHelper")
    class RequestedHelper {

        @Test
        @DisplayName("createBooking with requestedHelperId assigns that helper if available")
        void requestedHelperHappyPath() {
            CreateBookingRequest request = new CreateBookingRequest();
            request.setServiceType(ServiceType.CLEANING);
            request.setBookingType(BookingType.IMMEDIATE);
            request.setAddressLine("123 Main St");
            request.setLatitude(28.61);
            request.setLongitude(77.21);
            request.setDurationHours(2);
            request.setRequestedHelperId(helper.getId());

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(bookingConfig.getBasePrice(ServiceType.CLEANING)).thenReturn(299.0);
            when(bookingConfig.computeTotalPrice(ServiceType.CLEANING, 2)).thenReturn(597.0);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                if (b.getId() == null) b.setId(UUID.randomUUID());
                return b;
            });
            when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(favouriteHelperRepository.existsByCustomerIdAndHelperId(customer.getId(), helper.getId()))
                    .thenReturn(true);
            when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(bookingRepository.hasActiveBooking(helper.getId())).thenReturn(false);
            when(helperProfileRepository.save(any())).thenReturn(helperProfile);
            when(bookingConfig.getBooking()).thenReturn(bookingSettings);
            when(helperAvailabilityService.isHelperAvailableAt(eq(helper.getId()), any()))
                    .thenReturn(true);
            when(bookingResponseMapper.toDto(any())).thenReturn(BookingResponse.builder().build());

            BookingResponse response = bookingService.createBooking(request, customer.getId());

            assertNotNull(response);
            verify(favouriteHelperRepository).existsByCustomerIdAndHelperId(customer.getId(), helper.getId());
        }

        @Test
        @DisplayName("createBooking with requestedHelperId who is OFFLINE → throws 422")
        void requestedHelperOffline() {
            helperProfile.setStatus(HelperStatus.OFFLINE);

            CreateBookingRequest request = new CreateBookingRequest();
            request.setServiceType(ServiceType.CLEANING);
            request.setBookingType(BookingType.IMMEDIATE);
            request.setAddressLine("123 Main St");
            request.setLatitude(28.61);
            request.setLongitude(77.21);
            request.setDurationHours(2);
            request.setRequestedHelperId(helper.getId());

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(bookingConfig.getBasePrice(ServiceType.CLEANING)).thenReturn(299.0);
            when(bookingConfig.computeTotalPrice(ServiceType.CLEANING, 2)).thenReturn(597.0);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                if (b.getId() == null) b.setId(UUID.randomUUID());
                return b;
            });
            when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(favouriteHelperRepository.existsByCustomerIdAndHelperId(customer.getId(), helper.getId()))
                    .thenReturn(true);
            when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.createBooking(request, customer.getId()));
            assertEquals(ErrorCode.PREFERRED_HELPER_UNAVAILABLE, ex.getErrorCode());
        }

        @Test
        @DisplayName("auto-assign checks favourites first before haversine sort")
        void autoAssignFavouritePriority() {
            User favHelper = User.builder().name("Fav Helper").email("fav@test.com").role(Role.HELPER).build();
            favHelper.setId(UUID.randomUUID());
            HelperProfile favProfile = HelperProfile.builder()
                    .user(favHelper).skills(List.of(ServiceType.CLEANING))
                    .status(HelperStatus.ONLINE).available(true)
                    .latitude(28.62).longitude(77.22).rating(4.0).build();
            favProfile.setId(UUID.randomUUID());

            // Non-favourite helper is closer (comes first in haversine sort)
            User closerHelper = User.builder().name("Closer Helper").email("closer@test.com").role(Role.HELPER).build();
            closerHelper.setId(UUID.randomUUID());
            HelperProfile closerProfile = HelperProfile.builder()
                    .user(closerHelper).skills(List.of(ServiceType.CLEANING))
                    .status(HelperStatus.ONLINE).available(true)
                    .latitude(28.605).longitude(77.205).rating(4.9).build();
            closerProfile.setId(UUID.randomUUID());

            when(bookingConfig.getBooking()).thenReturn(bookingSettings);
            when(helperProfileRepository.findNearbyAvailableHelpers(any(), anyDouble(), anyDouble(), anyDouble(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(closerProfile, favProfile)); // closer first
            when(bookingRepository.hasActiveBooking(any())).thenReturn(false);
            when(helperAvailabilityService.isHelperAvailableAt(any(), any()))
                    .thenReturn(true);
            when(favouriteHelperRepository.findHelperIdsByCustomerId(customer.getId()))
                    .thenReturn(List.of(favHelper.getId()));
            when(helperProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            bookingService.findAndAssignHelper(booking);

            // Favourite helper should be assigned, not the closer one
            assertEquals(favHelper, booking.getHelper());
            assertEquals(BookingStatus.ASSIGNED, booking.getStatus());
        }
    }

    // ─── Availability-filtered assignment ──────────────────────────────

    @Nested
    @DisplayName("AvailabilityFiltered")
    class AvailabilityFiltered {

        @Test
        @DisplayName("findAndAssignHelper only returns helpers available at booking time")
        void findAndAssignHelper_skipsHelperOutsideSlot() {
            when(bookingConfig.getBooking()).thenReturn(bookingSettings);
            when(helperProfileRepository.findNearbyAvailableHelpers(any(), anyDouble(), anyDouble(), anyDouble(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(helperProfile));
            when(bookingRepository.hasActiveBooking(helper.getId())).thenReturn(false);
            // Helper is outside their availability slot
            when(helperAvailabilityService.isHelperAvailableAt(eq(helper.getId()), any()))
                    .thenReturn(false);

            bookingService.findAndAssignHelper(booking);

            // No helper assigned because the only candidate was outside availability
            assertNull(booking.getHelper());
            assertEquals(BookingStatus.PENDING_ASSIGNMENT, booking.getStatus());
        }

        @Test
        @DisplayName("findAndAssignHelper assigns helper who is within availability slot")
        void findAndAssignHelper_assignsAvailableHelper() {
            when(bookingConfig.getBooking()).thenReturn(bookingSettings);
            when(helperProfileRepository.findNearbyAvailableHelpers(any(), anyDouble(), anyDouble(), anyDouble(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(helperProfile));
            when(bookingRepository.hasActiveBooking(helper.getId())).thenReturn(false);
            when(helperAvailabilityService.isHelperAvailableAt(eq(helper.getId()), any()))
                    .thenReturn(true);
            when(favouriteHelperRepository.findHelperIdsByCustomerId(customer.getId()))
                    .thenReturn(List.of());
            when(helperProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            bookingService.findAndAssignHelper(booking);

            assertEquals(helper, booking.getHelper());
            assertEquals(BookingStatus.ASSIGNED, booking.getStatus());
        }

        @Test
        @DisplayName("scheduled booking at 11 PM finds no helpers if no night slots")
        void scheduledBookingLateNight_noHelpers() {
            booking.setBookingType(BookingType.SCHEDULED);
            booking.setScheduledAt(Instant.now().plus(java.time.Duration.ofDays(1))); // tomorrow

            when(bookingConfig.getBooking()).thenReturn(bookingSettings);
            when(helperProfileRepository.findNearbyAvailableHelpers(any(), anyDouble(), anyDouble(), anyDouble(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(helperProfile));
            when(bookingRepository.hasActiveBooking(helper.getId())).thenReturn(false);
            when(helperAvailabilityService.isHelperAvailableAt(eq(helper.getId()),
                    eq(booking.getScheduledAt()))).thenReturn(false);

            bookingService.findAndAssignHelper(booking);

            assertNull(booking.getHelper());
            assertEquals(BookingStatus.PENDING_ASSIGNMENT, booking.getStatus());
        }

        @Test
        @DisplayName("assignRequestedHelper throws when helper's slot doesn't cover booking time")
        void requestedHelper_outsideAvailability() {
            Instant futureNight = Instant.now().plus(java.time.Duration.ofDays(1));

            CreateBookingRequest request = new CreateBookingRequest();
            request.setServiceType(ServiceType.CLEANING);
            request.setBookingType(BookingType.SCHEDULED);
            request.setScheduledAt(futureNight);
            request.setAddressLine("123 Main St");
            request.setLatitude(28.61);
            request.setLongitude(77.21);
            request.setDurationHours(2);
            request.setRequestedHelperId(helper.getId());

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(bookingConfig.getBasePrice(ServiceType.CLEANING)).thenReturn(299.0);
            when(bookingConfig.computeTotalPrice(ServiceType.CLEANING, 2)).thenReturn(597.0);
            when(bookingConfig.getBooking()).thenReturn(bookingSettings);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                if (b.getId() == null) b.setId(UUID.randomUUID());
                return b;
            });
            when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(favouriteHelperRepository.existsByCustomerIdAndHelperId(customer.getId(), helper.getId()))
                    .thenReturn(true);
            when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            // Helper is ONLINE but outside availability
            when(helperAvailabilityService.isHelperAvailableAt(eq(helper.getId()), any()))
                    .thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.createBooking(request, customer.getId()));
            assertEquals(ErrorCode.HELPER_OUTSIDE_AVAILABILITY, ex.getErrorCode());
        }
    }
}

