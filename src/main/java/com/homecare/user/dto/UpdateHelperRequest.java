package com.homecare.user.dto;

import com.homecare.core.enums.ServiceType;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateHelperRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(min = 10, max = 15, message = "Phone must be between 10 and 15 characters")
    private String phone;

    private String avatarUrl;
    private List<ServiceType> skills;
    private String city;
    private String pincode;
    private String idProofUrl;
}

