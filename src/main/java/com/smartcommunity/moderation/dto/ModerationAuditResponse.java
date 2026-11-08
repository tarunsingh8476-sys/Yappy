package com.smartcommunity.moderation.dto;

import com.smartcommunity.moderation.ModerationAuditLog;
import com.smartcommunity.user.dto.UserProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationAuditResponse {
    private Long id;
    private Long moderationRecordId;
    private UserProfileResponse actor;
    private Long targetUserId;
    private String action;
    private String reason;
    private Instant createdAt;

    public static ModerationAuditResponse from(ModerationAuditLog log) {
        return ModerationAuditResponse.builder()
                .id(log.getId())
                .moderationRecordId(log.getModerationRecord() != null ? log.getModerationRecord().getId() : null)
                .actor(log.getActor() != null ? UserProfileResponse.from(log.getActor()) : null)
                .targetUserId(log.getTargetUserId())
                .action(log.getAction())
                .reason(log.getReason())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
