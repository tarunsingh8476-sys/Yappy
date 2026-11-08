package com.smartcommunity.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSearchCriteria {
    private String query;
    private Long channelId;
    private Long conversationId;
    private Long senderId;
    private Boolean pinned;
    private String status;
    private Instant startDate;
    private Instant endDate;
}
