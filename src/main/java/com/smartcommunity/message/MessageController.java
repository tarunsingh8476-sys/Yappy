package com.smartcommunity.message;

import com.smartcommunity.common.ApiResponse;
import com.smartcommunity.common.PageResponse;
import com.smartcommunity.message.dto.*;
import com.smartcommunity.ratelimit.RateLimitType;
import com.smartcommunity.ratelimit.WithRateLimit;
import com.smartcommunity.security.CurrentUser;
import com.smartcommunity.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Real-time messaging, pagination, history, emoji reactions, pins, and search")
@SecurityRequirement(name = "BearerAuth")
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/messages")
    @WithRateLimit(type = RateLimitType.MESSAGING)
    @Operation(summary = "Send real-time message", description = "Publishes a message to a channel or DM conversation, runs AI moderation, and broadcasts via STOMP WebSocket.")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = messageService.sendMessage(currentUser.getId(), request);
        return new ResponseEntity<>(ApiResponse.success(response, "Message sent."), HttpStatus.CREATED);
    }

    @GetMapping("/channels/{channelId}/messages")
    @Operation(summary = "Get paginated channel message history", description = "Retrieves paginated message history for a channel.")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getChannelMessages(
            @PathVariable Long channelId,
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 30) Pageable pageable) {
        PageResponse<MessageResponse> response = messageService.getChannelMessages(channelId, currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Get paginated DM / group conversation history", description = "Retrieves paginated message history for a 1:1 or group conversation.")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getConversationMessages(
            @PathVariable Long conversationId,
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 30) Pageable pageable) {
        PageResponse<MessageResponse> response = messageService.getConversationMessages(conversationId, currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/channels/{channelId}/messages/pinned")
    @Operation(summary = "Get pinned messages in channel", description = "Retrieves list of all messages currently pinned in the channel.")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getPinnedMessages(
            @PathVariable Long channelId,
            @CurrentUser UserPrincipal currentUser) {
        List<MessageResponse> response = messageService.getPinnedMessages(channelId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/messages/search")
    @Operation(summary = "Search and filter messages", description = "Searches messages by keyword query with filters for sender, channel, date range, and pagination.")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> searchMessages(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        MessageSearchCriteria criteria = MessageSearchCriteria.builder()
                .query(query)
                .channelId(channelId)
                .conversationId(conversationId)
                .senderId(senderId)
                .pinned(pinned)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        PageResponse<MessageResponse> response = messageService.searchMessages(criteria, currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/messages/{id}")
    @Operation(summary = "Edit message content", description = "Edits message text (author only) and re-evaluates AI moderation.")
    public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody EditMessageRequest request) {
        MessageResponse response = messageService.editMessage(id, currentUser.getId(), request.getContent());
        return ResponseEntity.ok(ApiResponse.success(response, "Message updated."));
    }

    @DeleteMapping("/messages/{id}")
    @Operation(summary = "Delete message", description = "Removes message (allowed for author, server moderator, or server owner).")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        messageService.deleteMessage(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Message deleted."));
    }

    @PostMapping("/messages/{id}/pin")
    @Operation(summary = "Toggle message pin status", description = "Pins or unpins a message in the channel (Moderators/Owners).")
    public ResponseEntity<ApiResponse<MessageResponse>> togglePin(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        MessageResponse response = messageService.togglePin(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Pin status updated."));
    }

    @PostMapping("/messages/{id}/react")
    @Operation(summary = "Add or remove emoji reaction", description = "Toggles emoji reaction on a message and broadcasts to WebSocket subscribers.")
    public ResponseEntity<ApiResponse<MessageResponse>> reactToMessage(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody ReactionRequest request) {
        MessageResponse response = messageService.reactToMessage(id, currentUser.getId(), request.getEmoji());
        return ResponseEntity.ok(ApiResponse.success(response, "Reaction updated."));
    }
}
