package com.smartcommunity.moderation;

import com.smartcommunity.common.ApiResponse;
import com.smartcommunity.common.PageResponse;
import com.smartcommunity.moderation.dto.ModerationActionRequest;
import com.smartcommunity.moderation.dto.ModerationAuditResponse;
import com.smartcommunity.moderation.dto.ModerationRecordResponse;
import com.smartcommunity.security.CurrentUser;
import com.smartcommunity.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@Tag(name = "AI Moderation & Safety", description = "AI content analysis queue, human-in-the-loop moderator review, and immutable audit logs")
@SecurityRequirement(name = "BearerAuth")
public class ModerationController {

    private final ModerationService moderationService;

    @GetMapping("/queue")
    @Operation(summary = "Get flagged messages review queue", description = "Retrieves AI-flagged messages with confidence scores, category, and review status.")
    public ResponseEntity<ApiResponse<PageResponse<ModerationRecordResponse>>> getReviewQueue(
            @RequestParam(required = false) Long serverId,
            @RequestParam(required = false, defaultValue = "PENDING") String status,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<ModerationRecordResponse> response = moderationService.getReviewQueue(serverId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/records/{recordId}/action")
    @Operation(summary = "Execute moderator action on flagged message", description = "Executes an action: DISMISS, WARN, REMOVE_MESSAGE, MUTE_USER, or SUSPEND_USER, and updates the audit log.")
    public ResponseEntity<ApiResponse<ModerationRecordResponse>> executeAction(
            @PathVariable Long recordId,
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody ModerationActionRequest request) {
        ModerationRecordResponse response = moderationService.executeAction(recordId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Moderation action recorded."));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "View moderation audit logs with pagination", description = "Returns chronological audit trail of all moderator actions taken.")
    public ResponseEntity<ApiResponse<PageResponse<ModerationAuditResponse>>> getAuditLogs(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<ModerationAuditResponse> response = moderationService.getAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
