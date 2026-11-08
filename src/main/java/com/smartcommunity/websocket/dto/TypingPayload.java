package com.smartcommunity.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingPayload {
    private Long channelId;
    private Long conversationId;
    private Long userId;
    private String username;
    private boolean typing;
}
