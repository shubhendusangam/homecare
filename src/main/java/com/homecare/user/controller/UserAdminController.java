package com.homecare.user.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
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
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserAdminDto>>> listUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserAdminDto> page = userAdminService.listUsers(role, active, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page)));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<UserAdminDto>> activateUser(@PathVariable UUID id) {
        UserAdminDto user = userAdminService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.ok(user, "User activated"));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<UserAdminDto>> deactivateUser(@PathVariable UUID id) {
        UserAdminDto user = userAdminService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.ok(user, "User deactivated"));
    }
}

