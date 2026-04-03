package com.homecare.dispute.service;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.*;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.core.logging.AuditEvent;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final DisputeEvidenceRepository evidenceRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final DisputeResponseMapper disputeMapper;

    private static final Set<BookingStatus> DISPUTABLE_STATUSES =
            Set.of(BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED, BookingStatus.CANCELLED);

    private static final List<DisputeStatus> ACTIVE_DISPUTE_STATUSES =
            List.of(DisputeStatus.OPEN, DisputeStatus.UNDER_REVIEW, DisputeStatus.WAITING_RESPONSE);

    // ─── Raise Dispute ───────────────────────────────────────────────

    @Transactional
    public DisputeResponse raiseDispute(UUID userId, RaiseDisputeRequest request) {
        User raiser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        // Determine raiser role and validate they are a party to the booking
        DisputeRaisedBy raisedByRole;
        UUID otherPartyId;
        if (booking.getCustomer().getId().equals(userId)) {
            raisedByRole = DisputeRaisedBy.CUSTOMER;
            otherPartyId = booking.getHelper() != null ? booking.getHelper().getId() : null;
        } else if (booking.getHelper() != null && booking.getHelper().getId().equals(userId)) {
            raisedByRole = DisputeRaisedBy.HELPER;
            otherPartyId = booking.getCustomer().getId();
        } else {
            throw new BusinessException("You are not a party to this booking", ErrorCode.FORBIDDEN);
        }

        // Validate booking status allows disputes
        if (!DISPUTABLE_STATUSES.contains(booking.getStatus())) {
            throw new BusinessException(
                    "Disputes can only be raised for bookings that are IN_PROGRESS, COMPLETED, or CANCELLED. Current status: " + booking.getStatus(),
                    ErrorCode.DISPUTE_NOT_ALLOWED);
        }

        // Check for existing active dispute from same raiser
        if (disputeRepository.existsByBookingIdAndRaisedByIdAndStatusIn(
                booking.getId(), userId, ACTIVE_DISPUTE_STATUSES)) {
            throw new BusinessException(
                    "You already have an open dispute for this booking",
                    ErrorCode.DUPLICATE_DISPUTE);
        }

        // Create dispute
        Dispute dispute = Dispute.builder()
                .booking(booking)
                .raisedBy(raiser)
                .raisedByRole(raisedByRole)
                .type(request.getType())
                .status(DisputeStatus.OPEN)
                .description(request.getDescription())
                .build();
        dispute = disputeRepository.save(dispute);

        // Freeze payment if booking is in progress or completed
        PaymentStatus previousPaymentStatus = booking.getPaymentStatus();
        if (previousPaymentStatus == PaymentStatus.HELD || previousPaymentStatus == PaymentStatus.PAID) {
            booking.setPaymentStatus(PaymentStatus.DISPUTED);
            bookingRepository.save(booking);
            log.info("Payment frozen for booking {} due to dispute {}", booking.getId(), dispute.getId());
        }

        // Notify the other party
        if (otherPartyId != null) {
            notificationService.sendToUser(otherPartyId, NotificationType.DISPUTE_RAISED,
                    Map.of("disputeType", request.getType().name(),
                           "bookingId", booking.getId().toString(),
                           "disputeId", dispute.getId().toString(),
                           "message", "Please respond with your side of the story."));
        }

        // Notify all admins
        notificationService.sendAdminAlert(
                "New Dispute Raised",
                "A " + request.getType().name() + " dispute has been raised for booking #" + booking.getId()
                        + " by " + raisedByRole.name().toLowerCase() + " " + raiser.getName() + ". Requires review.");

        log.info("Dispute raised: id={}, bookingId={}, type={}, raisedBy={}",
                dispute.getId(), booking.getId(), request.getType(), userId);
        eventPublisher.publishEvent(AuditEvent.of("DISPUTE_RAISED", userId,
                Map.of("disputeId", dispute.getId(), "bookingId", booking.getId(),
                       "type", request.getType().name())));

        return disputeMapper.toDto(dispute);
    }

    // ─── Submit Evidence ─────────────────────────────────────────────

    @Transactional
    public EvidenceResponse submitEvidence(UUID disputeId, UUID userId, SubmitEvidenceRequest request) {
        Dispute dispute = findDispute(disputeId);

        // Validate submitter is a party to the booking
        Booking booking = dispute.getBooking();
        boolean isCustomer = booking.getCustomer().getId().equals(userId);
        boolean isHelper = booking.getHelper() != null && booking.getHelper().getId().equals(userId);
        if (!isCustomer && !isHelper) {
            throw new BusinessException("You are not a party to this dispute's booking", ErrorCode.FORBIDDEN);
        }

        // Validate dispute is still open for evidence
        if (dispute.getStatus() == DisputeStatus.RESOLVED || dispute.getStatus() == DisputeStatus.CLOSED) {
            throw new BusinessException("Cannot submit evidence to a resolved dispute",
                    ErrorCode.DISPUTE_ALREADY_RESOLVED);
        }

        User submitter = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        DisputeEvidence evidence = DisputeEvidence.builder()
                .dispute(dispute)
                .submittedBy(submitter)
                .type(request.getType())
                .content(request.getContent())
                .description(request.getDescription())
                .build();
        evidence = evidenceRepository.save(evidence);

        log.debug("Evidence submitted for dispute {}: type={}, by={}",
                disputeId, request.getType(), userId);

        return disputeMapper.toEvidenceDto(evidence);
    }

    // ─── Get Dispute (party access) ──────────────────────────────────

    @Transactional(readOnly = true)
    public DisputeResponse getDispute(UUID disputeId, UUID userId) {
        Dispute dispute = findDispute(disputeId);
        validatePartyAccess(dispute, userId);

        List<DisputeEvidence> evidence = evidenceRepository.findByDisputeIdOrderByCreatedAtAsc(disputeId);
        return disputeMapper.toDto(dispute, evidence);
    }

    // ─── My Disputes ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<DisputeResponse> getMyDisputes(UUID userId, Pageable pageable) {
        Page<DisputeResponse> page = disputeRepository
                .findByRaisedByIdOrderByCreatedAtDesc(userId, pageable)
                .map(disputeMapper::toDto);
        return PagedResponse.from(page);
    }

    // ─── Admin: Assign to self ───────────────────────────────────────

    @Transactional
    public DisputeResponse assignToAdmin(UUID disputeId, UUID adminId) {
        Dispute dispute = findDispute(disputeId);

        if (dispute.getStatus() == DisputeStatus.RESOLVED || dispute.getStatus() == DisputeStatus.CLOSED) {
            throw new BusinessException("Cannot assign a resolved dispute", ErrorCode.DISPUTE_ALREADY_RESOLVED);
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        dispute.setAssignedAdmin(admin);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        disputeRepository.save(dispute);

        log.info("Dispute {} assigned to admin {}", disputeId, adminId);
        return disputeMapper.toDto(dispute, evidenceRepository.findByDisputeIdOrderByCreatedAtAsc(disputeId));
    }

    // ─── Admin: Resolve ──────────────────────────────────────────────

    @Transactional
    public DisputeResponse resolve(UUID disputeId, UUID adminId, ResolveDisputeRequest request) {
        Dispute dispute = findDispute(disputeId);

        if (dispute.getStatus() == DisputeStatus.RESOLVED || dispute.getStatus() == DisputeStatus.CLOSED) {
            throw new BusinessException("Dispute is already resolved", ErrorCode.DISPUTE_ALREADY_RESOLVED);
        }

        Booking booking = dispute.getBooking();

        // Apply financial resolution
        applyResolution(booking, request.getResolution(), request.getRefundAmount());

        // Update dispute
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolution(request.getResolution());
        dispute.setAdminNotes(request.getAdminNotes());
        dispute.setRefundAmount(request.getRefundAmount());
        dispute.setResolvedAt(Instant.now());

        if (dispute.getAssignedAdmin() == null) {
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));
            dispute.setAssignedAdmin(admin);
        }

        disputeRepository.save(dispute);

        // Notify both parties
        Map<String, String> notifVars = Map.of(
                "bookingId", booking.getId().toString(),
                "disputeId", dispute.getId().toString(),
                "resolution", request.getResolution().name(),
                "adminNotes", request.getAdminNotes() != null ? request.getAdminNotes() : "");

        notificationService.sendToUser(booking.getCustomer().getId(),
                NotificationType.DISPUTE_RESOLVED, notifVars);

        if (booking.getHelper() != null) {
            notificationService.sendToUser(booking.getHelper().getId(),
                    NotificationType.DISPUTE_RESOLVED, notifVars);
        }

        log.info("Dispute {} resolved by admin {}: resolution={}", disputeId, adminId, request.getResolution());
        eventPublisher.publishEvent(AuditEvent.of("DISPUTE_RESOLVED", adminId,
                Map.of("disputeId", disputeId, "resolution", request.getResolution().name())));

        return disputeMapper.toDto(dispute, evidenceRepository.findByDisputeIdOrderByCreatedAtAsc(disputeId));
    }

    // ─── Admin: List all ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<DisputeResponse> getAllDisputes(DisputeStatus status, DisputeType type,
                                                         Instant from, Instant to, Pageable pageable) {
        Page<DisputeResponse> page = disputeRepository
                .findAllWithFilters(status, type, from, to, pageable)
                .map(disputeMapper::toDto);
        return PagedResponse.from(page);
    }

    // ─── Admin: Get with full evidence ───────────────────────────────

    @Transactional(readOnly = true)
    public DisputeResponse getDisputeForAdmin(UUID disputeId) {
        Dispute dispute = findDispute(disputeId);
        List<DisputeEvidence> evidence = evidenceRepository.findByDisputeIdOrderByCreatedAtAsc(disputeId);
        return disputeMapper.toDto(dispute, evidence);
    }

    // ─── Private Helpers ─────────────────────────────────────────────

    private Dispute findDispute(UUID disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));
    }

    private void validatePartyAccess(Dispute dispute, UUID userId) {
        Booking booking = dispute.getBooking();
        boolean isCustomer = booking.getCustomer().getId().equals(userId);
        boolean isHelper = booking.getHelper() != null && booking.getHelper().getId().equals(userId);
        boolean isRaiser = dispute.getRaisedBy().getId().equals(userId);

        // Allow access to raiser or the other party on the booking
        if (!isCustomer && !isHelper && !isRaiser) {
            throw new BusinessException("You do not have access to this dispute", ErrorCode.FORBIDDEN);
        }
    }

    private void applyResolution(Booking booking, DisputeResolution resolution, BigDecimal refundAmount) {
        switch (resolution) {
            case FULL_REFUND -> {
                try {
                    // Unfreeze first: set back to a refundable status
                    booking.setPaymentStatus(PaymentStatus.PAID);
                    bookingRepository.save(booking);
                    paymentService.refundBooking(booking.getId(), BigDecimal.ONE);
                } catch (Exception e) {
                    log.error("Failed to issue full refund for booking {}: {}", booking.getId(), e.getMessage());
                }
            }
            case PARTIAL_REFUND -> {
                if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("Refund amount is required for partial refund",
                            ErrorCode.VALIDATION_FAILED);
                }
                try {
                    BigDecimal totalPrice = BigDecimal.valueOf(booking.getTotalPrice());
                    BigDecimal percentage = refundAmount.divide(totalPrice, 4, java.math.RoundingMode.HALF_UP);
                    booking.setPaymentStatus(PaymentStatus.PAID);
                    bookingRepository.save(booking);
                    paymentService.refundBooking(booking.getId(), percentage);
                } catch (Exception e) {
                    log.error("Failed to issue partial refund for booking {}: {}", booking.getId(), e.getMessage());
                }
            }
            case NO_REFUND -> {
                // Release hold to helper — helper receives payment
                try {
                    booking.setPaymentStatus(PaymentStatus.HELD);
                    bookingRepository.save(booking);
                    paymentService.releaseBookingPayment(booking.getId());
                } catch (Exception e) {
                    log.error("Failed to release payment for booking {}: {}", booking.getId(), e.getMessage());
                }
            }
            case RE_SERVICE, WARNING_ISSUED -> {
                // Unfreeze payment without changing amount
                if (booking.getPaymentStatus() == PaymentStatus.DISPUTED) {
                    booking.setPaymentStatus(PaymentStatus.PAID);
                    bookingRepository.save(booking);
                }
            }
        }
    }
}

