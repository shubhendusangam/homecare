package com.homecare.dispute.dto;

import com.homecare.core.enums.EvidenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitEvidenceRequest {

    @NotNull(message = "Evidence type is required")
    private EvidenceType type;

    @NotBlank(message = "Content is required")
    @Size(max = 4000, message = "Content must be under 4000 characters")
    private String content;

    @Size(max = 500, message = "Description must be under 500 characters")
    private String description;
}

