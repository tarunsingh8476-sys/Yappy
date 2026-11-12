package com.smartcommunity.config;


import com.smartcommunity.security.CustomUserDetailsService;
import com.smartcommunity.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Development origins only. Restrict this to your deployed frontend origin in production.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = extractToken(accessor);

                    if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
                        throw new MessageDeliveryException("Invalid or missing WebSocket JWT");
                    }

                    Long userId = jwtTokenProvider.getUserIdFromJWT(token);
                    UserDetails userDetails = customUserDetailsService.loadUserById(userId);

                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    accessor.setUser(authentication);
                    log.info("Authenticated WebSocket user id={}", userId);
                } else if (!StompCommand.DISCONNECT.equals(accessor.getCommand())
                        && accessor.getUser() == null) {
                    throw new MessageDeliveryException("Unauthenticated STOMP command");
                }

                return message;
            }

            private String extractToken(StompHeaderAccessor accessor) {
                List<String> authorization = accessor.getNativeHeader("Authorization");
                if (authorization != null && !authorization.isEmpty()) {
                    String value = authorization.get(0);
                    if (StringUtils.hasText(value) && value.startsWith("Bearer ")) {
                        return value.substring(7);
                    }
                }

                List<String> tokenHeaders = accessor.getNativeHeader("token");
                if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
                    return tokenHeaders.get(0);
                }
                return null;
            }
        });
    }
}
