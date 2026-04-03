package com.homecare.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddFavouriteRequest {

    @Size(max = 200, message = "Nickname must be under 200 characters")
    private String nickname;

    @Size(max = 1000, message = "Notes must be under 1000 characters")
    private String notes;
}

