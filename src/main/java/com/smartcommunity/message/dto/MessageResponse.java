package com.smartcommunity.message.dto;

import com.smartcommunity.message.Message;
import com.smartcommunity.message.Reaction;
import com.smartcommunity.user.dto.UserProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long channelId;
    private Long conversationId;
    private UserProfileResponse sender;
    private String content;
    private String status;
    private boolean pinned;
    private boolean edited;
    private Long replyToId;
    private String attachmentUrl;
    private String attachmentType;
    private String attachmentName;
    private Long attachmentSize;
    @Builder.Default
    private List<ReactionSummary> reactions = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public static MessageResponse from(Message message) {
        List<ReactionSummary> reactionSummaries = new ArrayList<>();
        if (message.getReactions() != null && !message.getReactions().isEmpty()) {
            Map<String, List<Reaction>> grouped = message.getReactions().stream()
                    .collect(Collectors.groupingBy(Reaction::getEmoji));

            for (Map.Entry<String, List<Reaction>> entry : grouped.entrySet()) {
                List<Long> userIds = entry.getValue().stream()
                        .map(r -> r.getUser().getId())
                        .toList();
                reactionSummaries.add(ReactionSummary.builder()
                        .emoji(entry.getKey())
                        .count(userIds.size())
                        .userIds(userIds)
                        .build());
            }
        }

        return MessageResponse.builder()
                .id(message.getId())
                .channelId(message.getChannel() != null ? message.getChannel().getId() : null)
                .conversationId(message.getConversation() != null ? message.getConversation().getId() : null)
                .sender(message.getSender() != null ? UserProfileResponse.from(message.getSender()) : null)
                .content(message.getContent())
                .status(message.getStatus())
                .pinned(message.isPinned())
                .edited(message.isEdited())
                .replyToId(message.getReplyTo() != null ? message.getReplyTo().getId() : null)
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentType(message.getAttachmentType())
                .attachmentName(message.getAttachmentName())
                .attachmentSize(message.getAttachmentSize())
                .reactions(reactionSummaries)
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
