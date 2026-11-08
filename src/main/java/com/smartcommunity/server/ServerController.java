package com.smartcommunity.server;

import com.smartcommunity.common.ApiResponse;
import com.smartcommunity.common.PageResponse;
import com.smartcommunity.security.CurrentUser;
import com.smartcommunity.security.UserPrincipal;
import com.smartcommunity.server.dto.CreateServerRequest;
import com.smartcommunity.server.dto.ServerAnalyticsResponse;
import com.smartcommunity.server.dto.ServerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@Tag(name = "Servers", description = "Server/Community creation, discovery, analytics, and management")
@SecurityRequirement(name = "BearerAuth")
public class ServerController {

    private final ServerService serverService;

    @PostMapping
    @CacheEvict(value = "servers", allEntries = true)
    @Operation(summary = "Create a new server", description = "Creates a community server, assigns current user as OWNER, and auto-provisions #general channel.")
    public ResponseEntity<ApiResponse<ServerResponse>> createServer(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateServerRequest request) {
        ServerResponse response = serverService.createServer(currentUser.getId(), request);
        return new ResponseEntity<>(ApiResponse.success(response, "Server created successfully."), HttpStatus.CREATED);
    }

    @GetMapping("/public")
    @Operation(summary = "Browse public servers with pagination", description = "Returns all discoverable public communities with member counts.")
    public ResponseEntity<ApiResponse<PageResponse<ServerResponse>>> getPublicServers(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<ServerResponse> response = serverService.getPublicServers(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/joined")
    @Operation(summary = "List joined servers", description = "Returns all servers the current authenticated user belongs to.")
    public ResponseEntity<ApiResponse<List<ServerResponse>>> getJoinedServers(
            @CurrentUser UserPrincipal currentUser) {
        List<ServerResponse> response = serverService.getJoinedServers(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Cacheable(value = "servers", key = "#id")
    @Operation(summary = "Get server details by ID", description = "Retrieves server details, channel listing, and member count.")
    public ResponseEntity<ApiResponse<ServerResponse>> getServerDetails(
            @PathVariable("id") Long id,
            @CurrentUser UserPrincipal currentUser) {
        ServerResponse response = serverService.getServerDetails(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/analytics")
    @PreAuthorize("@serverSecurity.hasServerRole(#id, 'MODERATOR')")
    @Operation(summary = "Get community analytics (Owner/Moderator only)", description = "Returns server metrics: active member count, channels, total message volume, and pending moderation queue counts.")
    public ResponseEntity<ApiResponse<ServerAnalyticsResponse>> getServerAnalytics(
            @PathVariable("id") Long id,
            @CurrentUser UserPrincipal currentUser) {
        ServerAnalyticsResponse response = serverService.getServerAnalytics(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@serverSecurity.isServerOwner(#id)")
    @CacheEvict(value = "servers", allEntries = true)
    @Operation(summary = "Delete server (Owner only)", description = "Permanently deletes a server, its channels, and all associated messages.")
    public ResponseEntity<ApiResponse<Void>> deleteServer(
            @PathVariable("id") Long id,
            @CurrentUser UserPrincipal currentUser) {
        serverService.deleteServer(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Server deleted successfully."));
    }
}
