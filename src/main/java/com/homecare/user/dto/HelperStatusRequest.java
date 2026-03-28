package com.homecare.user.dto;

import com.homecare.user.enums.HelperStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HelperStatusRequest {

    @NotNull(message = "Status is required")
    private HelperStatus status;
}

