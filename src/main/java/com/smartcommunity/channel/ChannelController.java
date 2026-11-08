package com.smartcommunity.channel;

import com.smartcommunity.channel.dto.ChannelResponse;
import com.smartcommunity.channel.dto.CreateChannelRequest;
import com.smartcommunity.channel.dto.UpdateChannelRequest;
import com.smartcommunity.common.ApiResponse;
import com.smartcommunity.security.CurrentUser;
import com.smartcommunity.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Channels", description = "Server channel management: creation, topic organization, voice/text channels")
@SecurityRequirement(name = "BearerAuth")
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/servers/{serverId}/channels")
    @PreAuthorize("@serverSecurity.hasServerRole(#serverId, 'MODERATOR')")
    @Operation(summary = "Create channel (Owner/Moderator)", description = "Creates a new text or voice channel within the specified server.")
    public ResponseEntity<ApiResponse<ChannelResponse>> createChannel(
            @PathVariable Long serverId,
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateChannelRequest request) {
        ChannelResponse response = channelService.createChannel(serverId, currentUser.getId(), request);
        return new ResponseEntity<>(ApiResponse.success(response, "Channel created successfully."), HttpStatus.CREATED);
    }

    @GetMapping("/servers/{serverId}/channels")
    @PreAuthorize("@serverSecurity.isServerMember(#serverId)")
    @Operation(summary = "List server channels", description = "Retrieves all public channels and authorized private channels for the authenticated member.")
    public ResponseEntity<ApiResponse<List<ChannelResponse>>> getChannels(
            @PathVariable Long serverId,
            @CurrentUser UserPrincipal currentUser) {
        List<ChannelResponse> response = channelService.getChannelsForServer(serverId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/channels/{channelId}")
    @PreAuthorize("@serverSecurity.canAccessChannel(#channelId)")
    @Operation(summary = "Get channel by ID", description = "Retrieves channel metadata and topic.")
    public ResponseEntity<ApiResponse<ChannelResponse>> getChannel(
            @PathVariable Long channelId,
            @CurrentUser UserPrincipal currentUser) {
        ChannelResponse response = channelService.getChannelById(channelId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/channels/{channelId}")
    @Operation(summary = "Update channel metadata", description = "Updates channel name, category, or topic (Moderators/Owners).")
    public ResponseEntity<ApiResponse<ChannelResponse>> updateChannel(
            @PathVariable Long channelId,
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody UpdateChannelRequest request) {
        ChannelResponse response = channelService.updateChannel(channelId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Channel updated successfully."));
    }

    @DeleteMapping("/channels/{channelId}")
    @Operation(summary = "Delete channel", description = "Permanently deletes a channel and its message history.")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(
            @PathVariable Long channelId,
            @CurrentUser UserPrincipal currentUser) {
        channelService.deleteChannel(channelId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Channel deleted successfully."));
    }
}
