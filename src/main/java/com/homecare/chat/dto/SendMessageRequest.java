package com.homecare.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 1000, message = "Message content must not exceed 1000 characters")
    private String content;
}

