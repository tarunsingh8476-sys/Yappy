package com.smartcommunity.moderation;

import com.smartcommunity.common.PageResponse;
import com.smartcommunity.common.exception.ResourceNotFoundException;
import com.smartcommunity.membership.MembershipService;
import com.smartcommunity.message.Message;
import com.smartcommunity.message.MessageRepository;
import com.smartcommunity.moderation.dto.ModerationActionRequest;
import com.smartcommunity.moderation.dto.ModerationAuditResponse;
import com.smartcommunity.moderation.dto.ModerationRecordResponse;
import com.smartcommunity.user.User;
import com.smartcommunity.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ModerationRecordRepository moderationRecordRepository;
    private final ModerationAuditLogRepository auditLogRepository;
    private final MlModerationClient mlModerationClient;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;

    @Transactional
    public void evaluateAndModerate(Message message) {
        MlModerationClient.ClassificationResult result = mlModerationClient.classifyText(message.getContent());

        if (result.isFlagged()) {
            log.info("Message {} flagged by AI moderation! Category: {}, Confidence: {}",
                    message.getId(), result.getCategory(), result.getConfidenceScore());

            message.setStatus("HELD_FOR_REVIEW");
            messageRepository.save(message);

            ModerationRecord record = ModerationRecord.builder()
                    .message(message)
                    .category(result.getCategory())
                    .confidenceScore(result.getConfidenceScore())
                    .modelVersion(result.getModelVersion())
                    .reviewStatus("PENDING")
                    .build();

            moderationRecordRepository.save(record);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ModerationRecordResponse> getReviewQueue(Long serverId, String status, Pageable pageable) {
        Page<ModerationRecord> records = moderationRecordRepository.findPendingReviewQueue(serverId, status, pageable);
        return PageResponse.from(records.map(ModerationRecordResponse::from));
    }

    @Transactional
    public ModerationRecordResponse executeAction(Long recordId, Long actorUserId, ModerationActionRequest request) {
        ModerationRecord record = moderationRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("ModerationRecord", "id", recordId));

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", actorUserId));

        Message message = record.getMessage();
        Long targetUserId = message.getSender().getId();
        String action = request.getAction().toUpperCase();

        record.setReviewedBy(actor);
        record.setReviewReason(request.getReason());
        record.setReviewedAt(Instant.now());
        record.setReviewStatus(action);

        switch (action) {
            case "DISMISS" -> {
                // False positive: republish message
                message.setStatus("PUBLISHED");
                messageRepository.save(message);
            }
            case "REMOVE_MESSAGE" -> {
                message.setStatus("REMOVED");
                messageRepository.save(message);
            }
            case "WARN" -> {
                // Message remains flagged, author recorded
                log.info("User {} warned by moderator {}", targetUserId, actor.getUsername());
            }
            case "MUTE_USER" -> {
                message.setStatus("REMOVED");
                messageRepository.save(message);
                if (message.getChannel() != null && message.getChannel().getServer() != null) {
                    membershipService.muteMember(message.getChannel().getServer().getId(),
                            targetUserId,
                            request.getMuteDurationMinutes() > 0 ? request.getMuteDurationMinutes() : 60,
                            actorUserId);
                }
            }
            case "SUSPEND_USER" -> {
                message.setStatus("REMOVED");
                messageRepository.save(message);
                userRepository.findById(targetUserId).ifPresent(u -> {
                    u.setSuspended(true);
                    userRepository.save(u);
                });
            }
            default -> log.warn("Unrecognized moderation action: {}", action);
        }

        ModerationRecord updated = moderationRecordRepository.save(record);

        // Record immutable audit log
        ModerationAuditLog auditLog = ModerationAuditLog.builder()
                .moderationRecord(record)
                .actor(actor)
                .targetUserId(targetUserId)
                .action(action)
                .reason(request.getReason())
                .build();
        auditLogRepository.save(auditLog);

        log.info("Moderator action '{}' executed on record {} by user {}", action, recordId, actor.getUsername());
        return ModerationRecordResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<ModerationAuditResponse> getAuditLogs(Pageable pageable) {
        Page<ModerationAuditLog> page = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(page.map(ModerationAuditResponse::from));
    }
}
