package com.smartcommunity.conversation.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotEmpty(message = "At least one participant is required")
    private List<Long> participantUserIds;

    private String name;

    @Builder.Default
    private boolean isGroup = false;
}
