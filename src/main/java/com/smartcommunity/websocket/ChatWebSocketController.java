package com.smartcommunity.websocket;

import com.smartcommunity.security.UserPrincipal;
import com.smartcommunity.websocket.dto.ChatMessagePayload;
import com.smartcommunity.websocket.dto.PresencePayload;
import com.smartcommunity.websocket.dto.TypingPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcasts real-time typing status to a channel or conversation.
     * Destination: /app/chat.typing
     * Broadcast to: /topic/channel.{channelId}.typing or /topic/conversation.{convId}.typing
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingPayload typingPayload, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            typingPayload.setUserId(principal.getId());
            typingPayload.setUsername(principal.getUsername());
        }

        if (typingPayload.getChannelId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/channel." + typingPayload.getChannelId() + ".typing",
                    typingPayload
            );
        } else if (typingPayload.getConversationId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/conversation." + typingPayload.getConversationId() + ".typing",
                    typingPayload
            );
        }
    }

    /**
     * Updates and broadcasts user presence across the platform.
     * Destination: /app/chat.presence
     * Broadcast to: /topic/presence
     */
    
    @MessageMapping("/chat.presence")
    public void handlePresence(@Payload PresencePayload presencePayload, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            presencePayload.setUserId(principal.getId());
            presencePayload.setUsername(principal.getUsername());
        }
        presencePayload.setTimestamp(Instant.now());

        log.debug("User {} presence updated to {}", presencePayload.getUsername(), presencePayload.getStatus());
        messagingTemplate.convertAndSend("/topic/presence", presencePayload);
    }
}
