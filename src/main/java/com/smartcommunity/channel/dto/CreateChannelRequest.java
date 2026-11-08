package com.smartcommunity.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChannelRequest {

    @NotBlank(message = "Channel name is required")
    @Size(min = 2, max = 100, message = "Channel name must be between 2 and 100 characters")
    private String name;

    private String topic;

    @Builder.Default
    private String category = "Text Channels";

    @Builder.Default
    private String type = "TEXT"; // TEXT, VOICE

    @Builder.Default
    private boolean isPrivate = false;
}
