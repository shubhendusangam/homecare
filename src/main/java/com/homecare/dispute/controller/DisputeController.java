package com.homecare.dispute.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.dispute.dto.*;
import com.homecare.dispute.service.DisputeService;
import com.homecare.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    public ResponseEntity<ApiResponse<DisputeResponse>> raiseDispute(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RaiseDisputeRequest request) {
        DisputeResponse dispute = disputeService.raiseDispute(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dispute));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PagedResponse<DisputeResponse>>> getMyDisputes(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<DisputeResponse> disputes = disputeService
                .getMyDisputes(principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(disputes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DisputeResponse>> getDispute(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        DisputeResponse dispute = disputeService.getDispute(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(dispute));
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<ApiResponse<EvidenceResponse>> submitEvidence(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody SubmitEvidenceRequest request) {
        EvidenceResponse evidence = disputeService.submitEvidence(id, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(evidence));
    }
}

