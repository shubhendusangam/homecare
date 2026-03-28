package com.homecare.admin.controller;

import com.homecare.admin.dto.CustomerDetailResponse;
import com.homecare.admin.dto.HelperDetailResponse;
import com.homecare.admin.service.AdminService;
import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.user.dto.HelperProfileDto;
import com.homecare.user.dto.UserAdminDto;
import com.homecare.user.enums.Role;
import com.homecare.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;
    private final UserAdminService userAdminService;

    // ─── Customer Management ──────────────────────────────────────────

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<PagedResponse<UserAdminDto>>> listCustomers(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserAdminDto> page = userAdminService.listUsers(Role.CUSTOMER, active, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page)));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> getCustomerDetail(@PathVariable UUID id) {
        CustomerDetailResponse detail = adminService.getCustomerDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PatchMapping("/customers/{id}/ban")
    public ResponseEntity<ApiResponse<Void>> banCustomer(@PathVariable UUID id) {
        adminService.banCustomer(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Customer banned successfully"));
    }

    // ─── Helper Management ────────────────────────────────────────────

    @GetMapping("/helpers")
    public ResponseEntity<ApiResponse<PagedResponse<UserAdminDto>>> listHelpers(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserAdminDto> page = userAdminService.listUsers(Role.HELPER, active, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page)));
    }

    @GetMapping("/helpers/pending-verification")
    public ResponseEntity<ApiResponse<PagedResponse<HelperProfileDto>>> pendingVerification(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<HelperProfileDto> page = userAdminService.getPendingVerification(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page)));
    }

    @GetMapping("/helpers/{id}")
    public ResponseEntity<ApiResponse<HelperDetailResponse>> getHelperDetail(@PathVariable UUID id) {
        HelperDetailResponse detail = adminService.getHelperDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PatchMapping("/helpers/{id}/verify")
    public ResponseEntity<ApiResponse<HelperProfileDto>> verifyHelper(@PathVariable UUID id) {
        HelperProfileDto helper = userAdminService.verifyHelper(id);
        return ResponseEntity.ok(ApiResponse.ok(helper, "Helper verified"));
    }

    @PatchMapping("/helpers/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendHelper(@PathVariable UUID id) {
        adminService.suspendHelper(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Helper suspended successfully"));
    }
}

