package com.smartcommunity.channel.dto;

import com.smartcommunity.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelResponse {
    private Long id;
    private Long serverId;
    private String name;
    private String topic;
    private String category;
    private String type;
    private boolean isPrivate;
    private Instant createdAt;

    public static ChannelResponse from(Channel channel) {
        return ChannelResponse.builder()
                .id(channel.getId())
                .serverId(channel.getServer() != null ? channel.getServer().getId() : null)
                .name(channel.getName())
                .topic(channel.getTopic())
                .category(channel.getCategory())
                .type(channel.getType())
                .isPrivate(channel.isPrivate())
                .createdAt(channel.getCreatedAt())
                .build();
    }
}
