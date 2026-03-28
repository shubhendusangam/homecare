package com.homecare.booking.statemachine;

import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Centralised booking status transition validator.
 * <p>
 * Defines the complete set of valid {@code from → to} transitions.
 * Every status-changing method should call
 * {@link #validateTransition(BookingStatus, BookingStatus)} before mutating.
 * <p>
 * This is a lightweight State-Machine pattern — avoids class-per-state
 * explosion while keeping transitions explicit, auditable, and in one place.
 */
@Component
public class BookingStateMachine {

    private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new EnumMap<>(BookingStatus.class);

        VALID_TRANSITIONS.put(BookingStatus.PENDING_ASSIGNMENT, Set.of(
                BookingStatus.ASSIGNED,
                BookingStatus.CANCELLED,
                BookingStatus.EXPIRED
        ));

        VALID_TRANSITIONS.put(BookingStatus.ASSIGNED, Set.of(
                BookingStatus.HELPER_EN_ROUTE,
                BookingStatus.PENDING_ASSIGNMENT, // helper rejects → re-assign
                BookingStatus.CANCELLED,
                BookingStatus.COMPLETED            // admin force-complete
        ));

        VALID_TRANSITIONS.put(BookingStatus.HELPER_EN_ROUTE, Set.of(
                BookingStatus.IN_PROGRESS,
                BookingStatus.CANCELLED,           // admin cancel
                BookingStatus.COMPLETED            // admin force-complete
        ));

        VALID_TRANSITIONS.put(BookingStatus.IN_PROGRESS, Set.of(
                BookingStatus.COMPLETED,
                BookingStatus.CANCELLED            // admin cancel
        ));

        // Terminal states — no outgoing transitions
        VALID_TRANSITIONS.put(BookingStatus.COMPLETED, Set.of());
        VALID_TRANSITIONS.put(BookingStatus.CANCELLED, Set.of());
        VALID_TRANSITIONS.put(BookingStatus.EXPIRED, Set.of());
    }

    /**
     * Validates that the transition from {@code currentStatus} to
     * {@code newStatus} is allowed.
     *
     * @throws BusinessException with {@link ErrorCode#BOOKING_CONFLICT} if invalid
     */
    public void validateTransition(BookingStatus currentStatus, BookingStatus newStatus) {
        Set<BookingStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new BusinessException(
                    "Invalid status transition: " + currentStatus + " → " + newStatus,
                    ErrorCode.BOOKING_CONFLICT);
        }
    }

    /**
     * Returns {@code true} if the transition is valid, without throwing.
     */
    public boolean isValidTransition(BookingStatus currentStatus, BookingStatus newStatus) {
        Set<BookingStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(newStatus);
    }
}

