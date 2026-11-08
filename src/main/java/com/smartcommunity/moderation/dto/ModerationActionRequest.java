package com.smartcommunity.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationActionRequest {

    /**
     * Action types:
     * - DISMISS: Mark message as approved/safe (false positive)
     * - WARN: Issue warning to author
     * - REMOVE_MESSAGE: Remove message content from view
     * - MUTE_USER: Temporarily mute user from sending messages
     * - SUSPEND_USER: Suspend account platform-wide
     */
    @NotBlank(message = "Moderation action is required")
    private String action;

    private String reason;

    private int muteDurationMinutes;
}
