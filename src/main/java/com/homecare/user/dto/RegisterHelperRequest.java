package com.homecare.user.dto;

import com.homecare.core.enums.ServiceType;
import com.homecare.user.validation.ValidPassword;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class RegisterHelperRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    @Size(min = 10, max = 15, message = "Phone must be between 10 and 15 characters")
    private String phone;

    @NotBlank(message = "Password is required")
    @ValidPassword
    private String password;

    @NotEmpty(message = "At least one skill is required")
    private List<ServiceType> skills;
}

