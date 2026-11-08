package com.smartcommunity.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginRequest {

    @NotBlank(message = "OAuth provider is required (e.g. google, github)")
    private String provider;

    private String email;

    private String name;

    private String avatarUrl;

    private String oauthId;

    private String accessToken;
}
