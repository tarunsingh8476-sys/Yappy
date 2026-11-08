package com.smartcommunity.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerAnalyticsResponse {
    private Long serverId;
    private String serverName;
    private long totalMembers;
    private long totalChannels;
    private long totalMessages;
    private long pendingModerations;
    private long totalModerationActions;
}
