package com.homecare.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Logs all inbound and outbound STOMP messages for observability.
 * Registered in {@link com.homecare.core.config.WebSocketConfig}.
 */
@Component
@Slf4j
public class WebSocketLoggingInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) return message;

        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();
        String userId = user != null ? user.getName() : "anonymous";

        switch (command) {
            case CONNECT ->
                log.info("WS CONNECT  — session={} user={}", sessionId, userId);
            case SUBSCRIBE -> {
                String dest = accessor.getDestination();
                log.info("WS SUBSCRIBE — session={} user={} dest={}", sessionId, userId, dest);
            }
            case SEND -> {
                String dest = accessor.getDestination();
                int payloadSize = message.getPayload() instanceof byte[] bytes ? bytes.length : -1;
                log.debug("WS SEND     — session={} user={} dest={} payload={}B",
                        sessionId, userId, dest, payloadSize);
            }
            case UNSUBSCRIBE -> {
                String subId = accessor.getSubscriptionId();
                log.info("WS UNSUB    — session={} user={} sub={}", sessionId, userId, subId);
            }
            case DISCONNECT ->
                log.info("WS DISCONNECT — session={} user={}", sessionId, userId);
            default ->
                log.trace("WS {}  — session={} user={}", command, sessionId, userId);
        }

        return message;
    }
}

