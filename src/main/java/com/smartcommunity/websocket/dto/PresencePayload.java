package com.smartcommunity.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresencePayload {
    private Long userId;
    private String username;
    private String status; // ONLINE, IDLE, DND, OFFLINE
    @Builder.Default
    private Instant timestamp = Instant.now();
}
