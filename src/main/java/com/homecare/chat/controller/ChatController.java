package com.homecare.chat.controller;

import com.homecare.chat.dto.ChatMessageResponse;
import com.homecare.chat.dto.SendMessageRequest;
import com.homecare.chat.service.ChatService;
import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    // ─── STOMP: Client sends a chat message ─────────────────────────────

    @MessageMapping("/chat/{bookingId}/send")
    public void sendMessage(@DestinationVariable UUID bookingId,
                            @Payload SendMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        if (attrs == null || attrs.get("userId") == null) {
            log.warn("Chat message received without authenticated session");
            return;
        }

        UUID senderId = (UUID) attrs.get("userId");

        try {
            chatService.saveAndBroadcast(bookingId, senderId, request.getContent());
        } catch (Exception e) {
            log.error("Error processing chat message from user {} on booking {}: {}",
                    senderId, bookingId, e.getMessage());
        }
    }

    // ─── REST: Get chat history for a booking ───────────────────────────

    @GetMapping("/api/v1/chat/{bookingId}/messages")
    @ResponseBody
    public ResponseEntity<ApiResponse<PagedResponse<ChatMessageResponse>>> getMessages(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PagedResponse<ChatMessageResponse> messages = chatService.getHistory(
                bookingId, principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }

    // ─── REST: Mark all unread messages as read ─────────────────────────

    @PatchMapping("/api/v1/chat/{bookingId}/read")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAsRead(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        int count = chatService.markAsRead(bookingId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("markedRead", count),
                "Messages marked as read"));
    }

    // ─── REST: Get unread message count ─────────────────────────────────

    @GetMapping("/api/v1/chat/{bookingId}/unread")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        long count = chatService.getUnreadCount(bookingId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count)));
    }
}


