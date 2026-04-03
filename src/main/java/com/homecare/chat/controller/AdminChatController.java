package com.homecare.chat.controller;

import com.homecare.chat.dto.ChatMessageResponse;
import com.homecare.chat.service.ChatService;
import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
public class AdminChatController {

    private final ChatService chatService;

    /**
     * GET /api/v1/admin/bookings/{id}/chat
     * Admin can read any booking's chat for dispute resolution.
     */
    @GetMapping("/{id}/chat")
    public ResponseEntity<ApiResponse<PagedResponse<ChatMessageResponse>>> getBookingChat(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        PagedResponse<ChatMessageResponse> messages = chatService.getHistoryForAdmin(
                id, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }
}

