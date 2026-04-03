package com.homecare.dispute.service;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.enums.*;
import com.homecare.core.exception.BusinessException;
import com.homecare.dispute.dto.*;
import com.homecare.dispute.entity.Dispute;
import com.homecare.dispute.entity.DisputeEvidence;
import com.homecare.dispute.mapper.DisputeResponseMapper;
import com.homecare.dispute.repository.DisputeEvidenceRepository;
import com.homecare.dispute.repository.DisputeRepository;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.payment.service.PaymentService;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisputeService")
class DisputeServiceTest {

    @Mock private DisputeRepository disputeRepository;
    @Mock private DisputeEvidenceRepository evidenceRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentService paymentService;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private DisputeResponseMapper disputeMapper;

    @InjectMocks private DisputeService disputeService;

    private User customer;
    private User helper;
    private User admin;
    private Booking booking;
    private Dispute dispute;

    @BeforeEach
    void setUp() {
        customer = User.builder().name("Customer").email("c@test.com").role(Role.CUSTOMER).build();
        customer.setId(UUID.randomUUID());

        helper = User.builder().name("Helper").email("h@test.com").role(Role.HELPER).build();
        helper.setId(UUID.randomUUID());

        admin = User.builder().name("Admin").email("a@test.com").role(Role.ADMIN).build();
        admin.setId(UUID.randomUUID());

        booking = Booking.builder()
                .customer(customer).helper(helper)
                .serviceType(ServiceType.CLEANING).bookingType(BookingType.IMMEDIATE)
                .status(BookingStatus.COMPLETED)
                .addressLine("123 Main St").latitude(28.61).longitude(77.21)
                .durationHours(2).basePrice(299).totalPrice(597)
                .paymentStatus(PaymentStatus.PAID).build();
        booking.setId(UUID.randomUUID());

        dispute = Dispute.builder()
                .booking(booking).raisedBy(customer)
                .raisedByRole(DisputeRaisedBy.CUSTOMER)
                .type(DisputeType.QUALITY_ISSUE)
                .status(DisputeStatus.OPEN)
                .description("Helper did not clean properly")
                .build();
        dispute.setId(UUID.randomUUID());
    }

    // ─── RaiseDispute ────────────────────────────────────────────────

    @Nested
    @DisplayName("RaiseDispute")
    class RaiseDispute {

        @Test
        @DisplayName("should raise dispute, freeze payment, and notify parties")
        void raiseHappyPath() {
            RaiseDisputeRequest request = new RaiseDisputeRequest();
            request.setBookingId(booking.getId());
            request.setType(DisputeType.QUALITY_ISSUE);
            request.setDescription("Helper did not clean properly");

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(disputeRepository.existsByBookingIdAndRaisedByIdAndStatusIn(
                    eq(booking.getId()), eq(customer.getId()), anyList())).thenReturn(false);
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            DisputeResponse expectedResponse = DisputeResponse.builder()
                    .id(UUID.randomUUID()).status(DisputeStatus.OPEN).build();
            when(disputeMapper.toDto(any(Dispute.class))).thenReturn(expectedResponse);

            DisputeResponse result = disputeService.raiseDispute(customer.getId(), request);

            assertNotNull(result);
            assertEquals(DisputeStatus.OPEN, result.getStatus());

            // Payment should be frozen
            assertEquals(PaymentStatus.DISPUTED, booking.getPaymentStatus());
            verify(bookingRepository).save(booking);

            // Both other party and admins should be notified
            verify(notificationService).sendToUser(eq(helper.getId()),
                    eq(NotificationType.DISPUTE_RAISED), anyMap());
            verify(notificationService).sendAdminAlert(anyString(), anyString());
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("should throw DUPLICATE_DISPUTE when open dispute exists")
        void raiseDuplicate() {
            RaiseDisputeRequest request = new RaiseDisputeRequest();
            request.setBookingId(booking.getId());
            request.setType(DisputeType.NO_SHOW);
            request.setDescription("Helper did not show up");

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(disputeRepository.existsByBookingIdAndRaisedByIdAndStatusIn(
                    eq(booking.getId()), eq(customer.getId()), anyList())).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.raiseDispute(customer.getId(), request));
            assertEquals(ErrorCode.DUPLICATE_DISPUTE, ex.getErrorCode());
        }

        @Test
        @DisplayName("should throw DISPUTE_NOT_ALLOWED for PENDING_ASSIGNMENT booking")
        void raiseOnPendingBooking() {
            booking.setStatus(BookingStatus.PENDING_ASSIGNMENT);

            RaiseDisputeRequest request = new RaiseDisputeRequest();
            request.setBookingId(booking.getId());
            request.setType(DisputeType.OTHER);
            request.setDescription("Some issue");

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.raiseDispute(customer.getId(), request));
            assertEquals(ErrorCode.DISPUTE_NOT_ALLOWED, ex.getErrorCode());
        }

        @Test
        @DisplayName("should throw FORBIDDEN when user is not a party to the booking")
        void raiseByNonParty() {
            UUID randomUserId = UUID.randomUUID();
            User randomUser = User.builder().name("Random").email("r@test.com").role(Role.CUSTOMER).build();
            randomUser.setId(randomUserId);

            RaiseDisputeRequest request = new RaiseDisputeRequest();
            request.setBookingId(booking.getId());
            request.setType(DisputeType.QUALITY_ISSUE);
            request.setDescription("Quality issue");

            when(userRepository.findById(randomUserId)).thenReturn(Optional.of(randomUser));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.raiseDispute(randomUserId, request));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }
    }

    // ─── Resolve ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Resolve")
    class Resolve {

        @Test
        @DisplayName("should resolve with FULL_REFUND and call PaymentService.refund(100%)")
        void resolveFullRefund() {
            booking.setPaymentStatus(PaymentStatus.DISPUTED);

            ResolveDisputeRequest request = new ResolveDisputeRequest();
            request.setResolution(DisputeResolution.FULL_REFUND);
            request.setAdminNotes("Customer complaint is valid");

            when(disputeRepository.findById(dispute.getId())).thenReturn(Optional.of(dispute));
            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(evidenceRepository.findByDisputeIdOrderByCreatedAtAsc(dispute.getId()))
                    .thenReturn(List.of());
            when(disputeMapper.toDto(any(Dispute.class), anyList()))
                    .thenReturn(DisputeResponse.builder().id(dispute.getId())
                            .status(DisputeStatus.RESOLVED)
                            .resolution(DisputeResolution.FULL_REFUND).build());

            DisputeResponse result = disputeService.resolve(dispute.getId(), admin.getId(), request);

            assertEquals(DisputeStatus.RESOLVED, result.getStatus());
            assertEquals(DisputeResolution.FULL_REFUND, result.getResolution());
            verify(paymentService).refundBooking(eq(booking.getId()), eq(BigDecimal.ONE));

            // Both parties notified
            verify(notificationService).sendToUser(eq(customer.getId()),
                    eq(NotificationType.DISPUTE_RESOLVED), anyMap());
            verify(notificationService).sendToUser(eq(helper.getId()),
                    eq(NotificationType.DISPUTE_RESOLVED), anyMap());
        }

        @Test
        @DisplayName("should resolve with NO_REFUND and release payment to helper")
        void resolveNoRefund() {
            booking.setPaymentStatus(PaymentStatus.DISPUTED);

            ResolveDisputeRequest request = new ResolveDisputeRequest();
            request.setResolution(DisputeResolution.NO_REFUND);
            request.setAdminNotes("Dispute not valid");

            when(disputeRepository.findById(dispute.getId())).thenReturn(Optional.of(dispute));
            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(evidenceRepository.findByDisputeIdOrderByCreatedAtAsc(dispute.getId()))
                    .thenReturn(List.of());
            when(disputeMapper.toDto(any(Dispute.class), anyList()))
                    .thenReturn(DisputeResponse.builder().id(dispute.getId())
                            .status(DisputeStatus.RESOLVED)
                            .resolution(DisputeResolution.NO_REFUND).build());

            disputeService.resolve(dispute.getId(), admin.getId(), request);

            verify(paymentService).releaseBookingPayment(booking.getId());
        }

        @Test
        @DisplayName("should throw DISPUTE_ALREADY_RESOLVED for resolved dispute")
        void resolveAlreadyResolved() {
            dispute.setStatus(DisputeStatus.RESOLVED);

            ResolveDisputeRequest request = new ResolveDisputeRequest();
            request.setResolution(DisputeResolution.NO_REFUND);

            when(disputeRepository.findById(dispute.getId())).thenReturn(Optional.of(dispute));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.resolve(dispute.getId(), admin.getId(), request));
            assertEquals(ErrorCode.DISPUTE_ALREADY_RESOLVED, ex.getErrorCode());
        }
    }

    // ─── SubmitEvidence ──────────────────────────────────────────────

    @Nested
    @DisplayName("SubmitEvidence")
    class SubmitEvidence {

        @Test
        @DisplayName("should submit evidence by booking party")
        void submitHappyPath() {
            SubmitEvidenceRequest request = new SubmitEvidenceRequest();
            request.setType(EvidenceType.TEXT);
            request.setContent("The kitchen was not cleaned at all");
            request.setDescription("Photo of dirty kitchen");

            when(disputeRepository.findById(dispute.getId())).thenReturn(Optional.of(dispute));
            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(evidenceRepository.save(any(DisputeEvidence.class))).thenAnswer(inv -> {
                DisputeEvidence e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            EvidenceResponse expectedResponse = EvidenceResponse.builder()
                    .id(UUID.randomUUID()).type(EvidenceType.TEXT).build();
            when(disputeMapper.toEvidenceDto(any(DisputeEvidence.class))).thenReturn(expectedResponse);

            EvidenceResponse result = disputeService.submitEvidence(
                    dispute.getId(), customer.getId(), request);

            assertNotNull(result);
            verify(evidenceRepository).save(any(DisputeEvidence.class));
        }

        @Test
        @DisplayName("should throw FORBIDDEN when non-party submits evidence")
        void submitByNonParty() {
            UUID randomUserId = UUID.randomUUID();

            SubmitEvidenceRequest request = new SubmitEvidenceRequest();
            request.setType(EvidenceType.TEXT);
            request.setContent("Some content");

            when(disputeRepository.findById(dispute.getId())).thenReturn(Optional.of(dispute));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.submitEvidence(dispute.getId(), randomUserId, request));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("should throw DISPUTE_ALREADY_RESOLVED when submitting to resolved dispute")
        void submitToResolvedDispute() {
            dispute.setStatus(DisputeStatus.RESOLVED);

            SubmitEvidenceRequest request = new SubmitEvidenceRequest();
            request.setType(EvidenceType.TEXT);
            request.setContent("Too late");

            when(disputeRepository.findById(dispute.getId())).thenReturn(Optional.of(dispute));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.submitEvidence(dispute.getId(), customer.getId(), request));
            assertEquals(ErrorCode.DISPUTE_ALREADY_RESOLVED, ex.getErrorCode());
        }
    }
}

