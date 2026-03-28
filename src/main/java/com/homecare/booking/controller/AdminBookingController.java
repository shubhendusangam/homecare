package com.homecare.booking.controller;

import com.homecare.admin.dto.BookingDetailResponse;
import com.homecare.admin.service.AdminService;
import com.homecare.booking.dto.AdminAssignRequest;
import com.homecare.booking.dto.AdminCancelRequest;
import com.homecare.booking.dto.BookingResponse;
import com.homecare.booking.service.BookingService;
import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.ServiceType;
import com.homecare.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingService bookingService;
    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<BookingResponse> bookings = bookingService
                .getAllBookings(status, serviceType, from, to, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingDetail(@PathVariable UUID id) {
        BookingDetailResponse detail = adminService.getBookingDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<BookingResponse>> assignHelper(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AdminAssignRequest request) {
        BookingResponse booking = bookingService.adminAssignHelper(id, request.getHelperId(), principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(booking, "Helper assigned successfully"));
    }

    @PostMapping("/{id}/reassign")
    public ResponseEntity<ApiResponse<BookingResponse>> reassignBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AdminAssignRequest request) {
        BookingResponse booking = adminService.reassignBooking(id, request.getHelperId(), principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(booking, "Booking reassigned successfully"));
    }

    @PostMapping("/{id}/force-complete")
    public ResponseEntity<ApiResponse<BookingResponse>> forceComplete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        BookingResponse booking = adminService.forceCompleteBooking(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(booking, "Booking force-completed"));
    }

    @PostMapping("/{id}/force-cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> forceCancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) AdminCancelRequest request) {
        String reason = request != null ? request.getReason() : null;
        BookingResponse booking = bookingService.adminCancelBooking(id, reason, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(booking, "Booking force-cancelled with full refund"));
    }
}

