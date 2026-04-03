package com.homecare.booking.controller;

import com.homecare.booking.dto.AvailableHelperResponse;
import com.homecare.booking.dto.AvailableSlotResponse;
import com.homecare.booking.dto.BookingResponse;
import com.homecare.booking.dto.CreateBookingRequest;
import com.homecare.booking.service.BookingService;
import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.ServiceType;
import com.homecare.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse booking = bookingService.createBooking(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(booking));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<BookingResponse> bookings = bookingService
                .getCustomerBookings(principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        BookingResponse booking = bookingService.getBooking(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(booking));
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        BookingResponse booking = bookingService.cancelBooking(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(booking, "Booking cancelled successfully"));
    }

    @GetMapping("/available-helpers")
    public ResponseEntity<ApiResponse<List<AvailableHelperResponse>>> findAvailableHelpers(
            @RequestParam ServiceType serviceType,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) Instant scheduledAt) {
        List<AvailableHelperResponse> helpers = bookingService
                .findAvailableHelpers(serviceType, latitude, longitude, radiusKm, scheduledAt);
        return ResponseEntity.ok(ApiResponse.ok(helpers));
    }

    @GetMapping("/available-slots")
    public ResponseEntity<ApiResponse<List<AvailableSlotResponse>>> getAvailableSlots(
            @RequestParam ServiceType serviceType,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Double radiusKm) {
        List<AvailableSlotResponse> slots = bookingService
                .getAvailableSlots(serviceType, latitude, longitude, date, radiusKm);
        return ResponseEntity.ok(ApiResponse.ok(slots));
    }
}

