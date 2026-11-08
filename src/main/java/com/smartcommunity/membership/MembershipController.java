package com.smartcommunity.membership;

import com.smartcommunity.common.ApiResponse;
import com.smartcommunity.common.PageResponse;
import com.smartcommunity.membership.dto.JoinServerRequest;
import com.smartcommunity.membership.dto.MemberResponse;
import com.smartcommunity.membership.dto.UpdateMemberRoleRequest;
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
@RequestMapping("/api/servers/{serverId}")
@RequiredArgsConstructor
@Tag(name = "Memberships", description = "Community joins, member listing, role assignment, and moderation controls")
@SecurityRequirement(name = "BearerAuth")
public class MembershipController {

    private final MembershipService membershipService;

    @PostMapping("/join")
    @Operation(summary = "Join community server", description = "Joins a public server directly or a private server using an invite code.")
    public ResponseEntity<ApiResponse<MemberResponse>> joinServer(
            @PathVariable Long serverId,
            @CurrentUser UserPrincipal currentUser,
            @RequestBody(required = false) JoinServerRequest request) {
        String inviteCode = request != null ? request.getInviteCode() : null;
        MemberResponse response = membershipService.joinServer(serverId, inviteCode, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Joined server successfully."));
    }

    @GetMapping("/members")
    @PreAuthorize("@serverSecurity.isServerMember(#serverId)")
    @Operation(summary = "List community members with pagination", description = "Retrieves paginated list of non-banned members of the server.")
    public ResponseEntity<ApiResponse<PageResponse<MemberResponse>>> listMembers(
            @PathVariable Long serverId,
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<MemberResponse> response = membershipService.listMembers(serverId, currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/members/{userId}/role")
    @PreAuthorize("@serverSecurity.hasServerRole(#serverId, 'OWNER')")
    @Operation(summary = "Assign role to member (Owner only)", description = "Promotes or demotes a member (OWNER, MODERATOR, MEMBER).")
    public ResponseEntity<ApiResponse<MemberResponse>> updateRole(
            @PathVariable Long serverId,
            @PathVariable Long userId,
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        MemberResponse response = membershipService.updateMemberRole(serverId, userId, request.getRole(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Member role updated."));
    }

    @PostMapping("/members/{userId}/mute")
    @PreAuthorize("@serverSecurity.hasServerRole(#serverId, 'MODERATOR')")
    @Operation(summary = "Mute or unmute member", description = "Mutes a user for specified minutes (or 0 to unmute).")
    public ResponseEntity<ApiResponse<MemberResponse>> muteMember(
            @PathVariable Long serverId,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "60") int durationMinutes,
            @CurrentUser UserPrincipal currentUser) {
        MemberResponse response = membershipService.muteMember(serverId, userId, durationMinutes, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Member mute status updated."));
    }

    @PostMapping("/members/{userId}/ban")
    @PreAuthorize("@serverSecurity.hasServerRole(#serverId, 'MODERATOR')")
    @Operation(summary = "Ban member from server", description = "Bans user and prevents them from reading or sending messages in the server.")
    public ResponseEntity<ApiResponse<Void>> banMember(
            @PathVariable Long serverId,
            @PathVariable Long userId,
            @CurrentUser UserPrincipal currentUser) {
        membershipService.banMember(serverId, userId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "User banned from server."));
    }

    @DeleteMapping("/leave")
    @PreAuthorize("@serverSecurity.isServerMember(#serverId)")
    @Operation(summary = "Leave server", description = "Removes the current user from the server membership.")
    public ResponseEntity<ApiResponse<Void>> leaveServer(
            @PathVariable Long serverId,
            @CurrentUser UserPrincipal currentUser) {
        membershipService.leaveServer(serverId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Left server successfully."));
    }
}
