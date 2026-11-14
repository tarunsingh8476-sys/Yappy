
package com.smartcommunity.websocket;

import com.smartcommunity.security.UserPrincipal;
import com.smartcommunity.security.CustomUserDetailsService;
import com.smartcommunity.websocket.dto.WebRtcSignalPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebRtcSignalingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final CustomUserDetailsService userDetailsService;

    @MessageMapping("/webrtc.signal")
    public void processSignal(
            @Payload WebRtcSignalPayload signal,
            Authentication authentication) {

        // 1. Verify authenticated sender
        if (authentication == null ||
                !(authentication.getPrincipal() instanceof UserPrincipal sender)) {
            throw new AccessDeniedException(
                    "Authenticated user required for WebRTC"
            );
        }

        if (signal == null || !StringUtils.hasText(signal.getType())) {
            throw new IllegalArgumentException("Invalid WebRTC signal");
        }

        // 2. Set sender using authenticated identity
        signal.setSenderId(sender.getId());
        signal.setSenderUsername(sender.getUsername());
        signal.setTimestamp(Instant.now());

        log.info(
                "WebRTC signal: type={}, sender={}, target={}",
                signal.getType(),
                sender.getId(),
                signal.getTargetUserId()
        );

        // 3. Direct one-to-one signaling
        if (signal.getTargetUserId() != null) {

            if (signal.getTargetUserId().equals(sender.getId())) {
                throw new IllegalArgumentException(
                        "Cannot call yourself"
                );
            }

            // Convert numeric user ID into actual username
            UserDetails recipient =
                    userDetailsService.loadUserById(
                            signal.getTargetUserId()
                    );

            // Route using Spring's authenticated username
            messagingTemplate.convertAndSendToUser(
                    recipient.getUsername(),
                    "/queue/webrtc",
                    signal
            );

            log.info(
                    "WebRTC {} successfully routed to user {}",
                    signal.getType(),
                    recipient.getUsername()
            );

            return;
        }

        // 4. Community room signaling
        if (StringUtils.hasText(signal.getRoomId())) {
            messagingTemplate.convertAndSend(
                    "/topic/webrtc.room." + signal.getRoomId(),
                    signal
            );
        }
    }
}

