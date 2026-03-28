package com.homecare.admin.controller;

import com.homecare.admin.dto.ServiceConfigDto;
import com.homecare.admin.dto.UpdateServiceConfigRequest;
import com.homecare.admin.service.AdminService;
import com.homecare.core.dto.ApiResponse;
import com.homecare.core.enums.ServiceType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/service-config")
@RequiredArgsConstructor
public class AdminServiceConfigController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceConfigDto>>> getAllConfigs() {
        List<ServiceConfigDto> configs = adminService.getAllServiceConfigs();
        return ResponseEntity.ok(ApiResponse.ok(configs));
    }

    @PutMapping("/{serviceType}")
    public ResponseEntity<ApiResponse<ServiceConfigDto>> updateConfig(
            @PathVariable ServiceType serviceType,
            @Valid @RequestBody UpdateServiceConfigRequest request) {
        ServiceConfigDto updated = adminService.updateServiceConfig(serviceType, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Service configuration updated"));
    }
}

