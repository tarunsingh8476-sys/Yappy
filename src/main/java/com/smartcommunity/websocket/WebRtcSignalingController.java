package com.smartcommunity.websocket;

import com.smartcommunity.security.CustomUserDetailsService;
import com.smartcommunity.security.UserPrincipal;
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
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebRtcSignalingController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "CALL_REQUEST", "CALL_ACCEPTED", "CALL_REJECTED",
            "OFFER", "ANSWER", "ICE_CANDIDATE", "HANG_UP"
    );

    private final SimpMessagingTemplate messagingTemplate;
    private final CustomUserDetailsService userDetailsService;

    @MessageMapping("/webrtc.signal")
    public void processSignal(@Payload WebRtcSignalPayload signal,
                              Authentication authentication) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserPrincipal sender)) {
            throw new AccessDeniedException("Authenticated user required");
        }

        if (signal == null || !StringUtils.hasText(signal.getType())
                || !ALLOWED_TYPES.contains(signal.getType())) {
            throw new IllegalArgumentException("Unsupported WebRTC signal type");
        }

        // Identity is always derived from the authenticated principal, never the client payload.
        signal.setSenderId(sender.getId());
        signal.setSenderUsername(sender.getUsername());
        signal.setTimestamp(Instant.now());

        // This implementation handles direct 1-to-1 calls only.
        if (signal.getTargetUserId() == null) {
            throw new IllegalArgumentException("targetUserId is required for a direct call");
        }
        if (signal.getTargetUserId().equals(sender.getId())) {
            throw new IllegalArgumentException("You cannot call yourself");
        }

        UserDetails recipient = userDetailsService.loadUserById(signal.getTargetUserId());

        // User destinations resolve against Principal.getName(), which is username in this app.
        messagingTemplate.convertAndSendToUser(
                recipient.getUsername(),
                "/queue/webrtc",
                signal
        );

        log.debug("Forwarded WebRTC signal type={} from={} to={}",
                signal.getType(), sender.getId(), signal.getTargetUserId());
    }
}
