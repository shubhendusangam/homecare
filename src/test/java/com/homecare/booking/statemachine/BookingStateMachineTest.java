package com.homecare.booking.statemachine;

import com.homecare.core.enums.BookingStatus;
import com.homecare.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BookingStateMachine — status transition validation")
class BookingStateMachineTest {

    private BookingStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new BookingStateMachine();
    }

    // ─── Valid transitions ────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "PENDING_ASSIGNMENT, ASSIGNED",
            "PENDING_ASSIGNMENT, CANCELLED",
            "PENDING_ASSIGNMENT, EXPIRED",
            "ASSIGNED, HELPER_EN_ROUTE",
            "ASSIGNED, PENDING_ASSIGNMENT",
            "ASSIGNED, CANCELLED",
            "ASSIGNED, COMPLETED",
            "HELPER_EN_ROUTE, IN_PROGRESS",
            "HELPER_EN_ROUTE, CANCELLED",
            "HELPER_EN_ROUTE, COMPLETED",
            "IN_PROGRESS, COMPLETED",
            "IN_PROGRESS, CANCELLED",
    })
    @DisplayName("valid transitions should pass")
    void validTransitions(BookingStatus from, BookingStatus to) {
        assertDoesNotThrow(() -> stateMachine.validateTransition(from, to));
        assertTrue(stateMachine.isValidTransition(from, to));
    }

    // ─── Invalid transitions ──────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "PENDING_ASSIGNMENT, IN_PROGRESS",
            "PENDING_ASSIGNMENT, COMPLETED",
            "PENDING_ASSIGNMENT, HELPER_EN_ROUTE",
            "ASSIGNED, IN_PROGRESS",
            "ASSIGNED, EXPIRED",
            "HELPER_EN_ROUTE, ASSIGNED",
            "HELPER_EN_ROUTE, PENDING_ASSIGNMENT",
            "HELPER_EN_ROUTE, EXPIRED",
            "IN_PROGRESS, ASSIGNED",
            "IN_PROGRESS, PENDING_ASSIGNMENT",
            "IN_PROGRESS, HELPER_EN_ROUTE",
            "IN_PROGRESS, EXPIRED",
    })
    @DisplayName("invalid transitions should throw BusinessException")
    void invalidTransitions(BookingStatus from, BookingStatus to) {
        assertThrows(BusinessException.class, () -> stateMachine.validateTransition(from, to));
        assertFalse(stateMachine.isValidTransition(from, to));
    }

    // ─── Terminal states ──────────────────────────────────────────────

    @Test
    @DisplayName("COMPLETED is a terminal state — no outgoing transitions")
    void completedIsTerminal() {
        for (BookingStatus target : BookingStatus.values()) {
            assertFalse(stateMachine.isValidTransition(BookingStatus.COMPLETED, target),
                    "COMPLETED should not transition to " + target);
        }
    }

    @Test
    @DisplayName("CANCELLED is a terminal state — no outgoing transitions")
    void cancelledIsTerminal() {
        for (BookingStatus target : BookingStatus.values()) {
            assertFalse(stateMachine.isValidTransition(BookingStatus.CANCELLED, target),
                    "CANCELLED should not transition to " + target);
        }
    }

    @Test
    @DisplayName("EXPIRED is a terminal state — no outgoing transitions")
    void expiredIsTerminal() {
        for (BookingStatus target : BookingStatus.values()) {
            assertFalse(stateMachine.isValidTransition(BookingStatus.EXPIRED, target),
                    "EXPIRED should not transition to " + target);
        }
    }

    @Test
    @DisplayName("self-transitions are invalid for all states")
    void selfTransitionsInvalid() {
        for (BookingStatus status : BookingStatus.values()) {
            assertFalse(stateMachine.isValidTransition(status, status),
                    "Self-transition should be invalid for " + status);
        }
    }
}

