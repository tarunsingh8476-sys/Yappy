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
public class ChatMessagePayload {
    private Long id;
    private Long channelId;
    private Long conversationId;
    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private String senderAvatarUrl;
    private String content;
    private String status;
    private boolean pinned;
    private String attachmentUrl;
    private String attachmentType;
    private String attachmentName;
    private Long replyToId;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
