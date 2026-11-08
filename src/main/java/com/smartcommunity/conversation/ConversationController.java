package com.smartcommunity.conversation;

import com.smartcommunity.common.ApiResponse;
import com.smartcommunity.common.PageResponse;
import com.smartcommunity.conversation.dto.ConversationResponse;
import com.smartcommunity.conversation.dto.CreateConversationRequest;
import com.smartcommunity.security.CurrentUser;
import com.smartcommunity.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations (DMs & Group Chats)", description = "Direct messages and private group conversations")
@SecurityRequirement(name = "BearerAuth")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @Operation(summary = "Start direct or group conversation", description = "Initiates a 1:1 direct message or multi-user group chat.")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationResponse response = conversationService.getOrCreateConversation(currentUser.getId(), request);
        return new ResponseEntity<>(ApiResponse.success(response, "Conversation started."), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List user conversations with pagination", description = "Retrieves all DMs and group chats the user participates in.")
    public ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> getUserConversations(
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<ConversationResponse> response = conversationService.getUserConversations(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversation details", description = "Retrieves conversation metadata and participant list.")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversationDetails(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        ConversationResponse response = conversationService.getConversationDetails(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
