package com.homecare.dispute.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.DisputeStatus;
import com.homecare.core.enums.DisputeType;
import com.homecare.dispute.dto.DisputeResponse;
import com.homecare.dispute.dto.ResolveDisputeRequest;
import com.homecare.dispute.service.DisputeService;
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
@RequestMapping("/api/v1/admin/disputes")
@RequiredArgsConstructor
public class AdminDisputeController {

    private final DisputeService disputeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DisputeResponse>>> getAllDisputes(
            @RequestParam(required = false) DisputeStatus status,
            @RequestParam(required = false) DisputeType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<DisputeResponse> disputes = disputeService
                .getAllDisputes(status, type, from, to, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(disputes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DisputeResponse>> getDispute(@PathVariable UUID id) {
        DisputeResponse dispute = disputeService.getDisputeForAdmin(id);
        return ResponseEntity.ok(ApiResponse.ok(dispute));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<DisputeResponse>> assignToSelf(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        DisputeResponse dispute = disputeService.assignToAdmin(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(dispute, "Dispute assigned to you"));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<DisputeResponse>> resolve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ResolveDisputeRequest request) {
        DisputeResponse dispute = disputeService.resolve(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(dispute, "Dispute resolved"));
    }
}

