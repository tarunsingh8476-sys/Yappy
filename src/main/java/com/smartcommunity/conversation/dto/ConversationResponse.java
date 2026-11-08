package com.smartcommunity.conversation.dto;

import com.smartcommunity.conversation.Conversation;
import com.smartcommunity.user.dto.UserProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private Long id;
    private String name;
    private boolean isGroup;
    private List<UserProfileResponse> participants;
    private Instant createdAt;
    private Instant updatedAt;

    public static ConversationResponse from(Conversation conversation) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .name(conversation.getName())
                .isGroup(conversation.isGroup())
                .participants(conversation.getParticipants().stream()
                        .map(UserProfileResponse::from)
                        .toList())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
