package com.smartcommunity.membership.dto;

import com.smartcommunity.membership.Membership;
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
public class MemberResponse {
    private Long id;
    private Long serverId;
    private UserProfileResponse user;
    private String role;
    private boolean banned;
    private boolean muted;
    private Instant mutedUntil;
    private Instant joinedAt;

    public static MemberResponse from(Membership membership) {
        return MemberResponse.builder()
                .id(membership.getId())
                .serverId(membership.getServer() != null ? membership.getServer().getId() : null)
                .user(membership.getUser() != null ? UserProfileResponse.from(membership.getUser()) : null)
                .role(membership.getRole())
                .banned(membership.isBanned())
                .muted(membership.isMuted())
                .mutedUntil(membership.getMutedUntil())
                .joinedAt(membership.getJoinedAt())
                .build();
    }
}
