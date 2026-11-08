package com.smartcommunity.websocket;

import com.smartcommunity.security.UserPrincipal;
import com.smartcommunity.websocket.dto.WebRtcSignalPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebRtcSignalingController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles WebRTC signaling messages for voice and video calls.
     * Route: /app/webrtc.signal
     * - Direct 1-to-1: Dispatched to target user queue /user/{targetUserId}/queue/webrtc
     * - Community Voice/Video Room: Dispatched to room topic /topic/webrtc.room.{roomId}
     */
    @MessageMapping("/webrtc.signal")
    public void processSignal(@Payload WebRtcSignalPayload signal, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            signal.setSenderId(principal.getId());
            signal.setSenderUsername(principal.getUsername());
        }
        signal.setTimestamp(Instant.now());

        log.debug("WebRTC signal received: type={}, senderId={}, targetUserId={}, roomId={}",
                signal.getType(), signal.getSenderId(), signal.getTargetUserId(), signal.getRoomId());

        // Scenario 1: Direct 1-on-1 Call signaling
        if (signal.getTargetUserId() != null) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(signal.getTargetUserId()),
                    "/queue/webrtc",
                    signal
            );
            return;
        }

        // Scenario 2: Community Channel Voice/Video Room signaling
        if (StringUtils.hasText(signal.getRoomId())) {
            messagingTemplate.convertAndSend(
                    "/topic/webrtc.room." + signal.getRoomId(),
                    signal
            );
        }
    }
}
