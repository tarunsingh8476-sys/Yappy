package com.smartcommunity.server.dto;

import com.smartcommunity.server.Server;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponse {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private String visibility;
    private String inviteCode;
    private Long ownerId;
    private String ownerUsername;
    private long memberCount;
    private String guidelines;
    private Instant createdAt;

    public static ServerResponse from(Server server, long memberCount) {
        return ServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .iconUrl(server.getIconUrl())
                .visibility(server.getVisibility())
                .inviteCode(server.getInviteCode())
                .ownerId(server.getOwner() != null ? server.getOwner().getId() : null)
                .ownerUsername(server.getOwner() != null ? server.getOwner().getUsername() : null)
                .memberCount(memberCount)
                .guidelines(server.getGuidelines())
                .createdAt(server.getCreatedAt())
                .build();
    }
}
