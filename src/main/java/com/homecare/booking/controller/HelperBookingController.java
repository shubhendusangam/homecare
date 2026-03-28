package com.homecare.booking.controller;

import com.homecare.booking.dto.BookingResponse;
import com.homecare.booking.service.BookingService;
import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/helpers/bookings")
@RequiredArgsConstructor
public class HelperBookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<BookingResponse> bookings = bookingService
                .getHelperBookings(principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getPendingBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<BookingResponse> bookings = bookingService.getPendingBookingsForHelper(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<BookingResponse>> acceptBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        BookingResponse booking = bookingService.acceptBooking(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(booking, "Booking accepted"));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        BookingResponse booking = bookingService.rejectBooking(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(booking, "Booking rejected — reassigning"));
    }

    @PatchMapping("/{id}/start-travel")
    public ResponseEntity<ApiResponse<BookingResponse>> startTravel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        BookingResponse booking = bookingService.startTravel(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(booking, "Travel started"));
    }

    @PatchMapping("/{id}/start-job")
    public ResponseEntity<ApiResponse<BookingResponse>> startJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        BookingResponse booking = bookingService.startJob(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(booking, "Job started"));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<BookingResponse>> completeJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        BookingResponse booking = bookingService.completeJob(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(booking, "Job completed"));
    }
}

