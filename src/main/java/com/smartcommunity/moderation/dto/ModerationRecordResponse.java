package com.smartcommunity.moderation.dto;

import com.smartcommunity.moderation.ModerationRecord;
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
public class ModerationRecordResponse {
    private Long id;
    private Long messageId;
    private Long channelId;
    private Long serverId;
    private String messageContent;
    private UserProfileResponse sender;
    private String category;
    private Double confidenceScore;
    private String modelVersion;
    private String reviewStatus;
    private UserProfileResponse reviewedBy;
    private String reviewReason;
    private Instant reviewedAt;
    private Instant createdAt;

    public static ModerationRecordResponse from(ModerationRecord record) {
        Long channelId = null;
        Long serverId = null;
        if (record.getMessage() != null && record.getMessage().getChannel() != null) {
            channelId = record.getMessage().getChannel().getId();
            if (record.getMessage().getChannel().getServer() != null) {
                serverId = record.getMessage().getChannel().getServer().getId();
            }
        }

        return ModerationRecordResponse.builder()
                .id(record.getId())
                .messageId(record.getMessage() != null ? record.getMessage().getId() : null)
                .channelId(channelId)
                .serverId(serverId)
                .messageContent(record.getMessage() != null ? record.getMessage().getContent() : null)
                .sender(record.getMessage() != null && record.getMessage().getSender() != null
                        ? UserProfileResponse.from(record.getMessage().getSender()) : null)
                .category(record.getCategory())
                .confidenceScore(record.getConfidenceScore())
                .modelVersion(record.getModelVersion())
                .reviewStatus(record.getReviewStatus())
                .reviewedBy(record.getReviewedBy() != null ? UserProfileResponse.from(record.getReviewedBy()) : null)
                .reviewReason(record.getReviewReason())
                .reviewedAt(record.getReviewedAt())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
