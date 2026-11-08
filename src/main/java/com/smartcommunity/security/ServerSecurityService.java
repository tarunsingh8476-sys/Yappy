package com.smartcommunity.security;

import com.smartcommunity.channel.Channel;
import com.smartcommunity.channel.ChannelRepository;
import com.smartcommunity.conversation.ConversationRepository;
import com.smartcommunity.membership.Membership;
import com.smartcommunity.membership.MembershipRepository;
import com.smartcommunity.server.Server;
import com.smartcommunity.server.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service("serverSecurity")
@RequiredArgsConstructor
public class ServerSecurityService {

    private final MembershipRepository membershipRepository;
    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final ConversationRepository conversationRepository;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getId();
    }

    public boolean isPlatformAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean isServerMember(Long serverId) {
        if (isPlatformAdmin()) return true;
        Long userId = getCurrentUserId();
        if (userId == null || serverId == null) return false;
        return membershipRepository.existsByServerIdAndUserIdAndBannedFalse(serverId, userId);
    }

    public boolean isServerOwner(Long serverId) {
        if (isPlatformAdmin()) return true;
        Long userId = getCurrentUserId();
        if (userId == null || serverId == null) return false;

        Optional<Server> server = serverRepository.findById(serverId);
        if (server.isEmpty()) return false;
        if (server.get().getOwner() != null && server.get().getOwner().getId().equals(userId)) {
            return true;
        }

        return membershipRepository.findByServerIdAndUserId(serverId, userId)
                .map(m -> !m.isBanned() && "OWNER".equalsIgnoreCase(m.getRole()))
                .orElse(false);
    }

    public boolean hasServerRole(Long serverId, String targetRole) {
        if (isPlatformAdmin()) return true;
        Long userId = getCurrentUserId();
        if (userId == null || serverId == null) return false;

        Optional<Membership> membershipOpt = membershipRepository.findByServerIdAndUserId(serverId, userId);
        if (membershipOpt.isEmpty() || membershipOpt.get().isBanned()) {
            return false;
        }

        String userRole = membershipOpt.get().getRole().toUpperCase();

        if ("OWNER".equalsIgnoreCase(targetRole)) {
            return "OWNER".equalsIgnoreCase(userRole);
        }

        if ("MODERATOR".equalsIgnoreCase(targetRole)) {
            return "OWNER".equalsIgnoreCase(userRole) || "MODERATOR".equalsIgnoreCase(userRole);
        }

        // Default: MEMBER
        return true;
    }

    public boolean canAccessChannel(Long channelId) {
        if (isPlatformAdmin()) return true;
        Long userId = getCurrentUserId();
        if (userId == null || channelId == null) return false;

        Optional<Channel> channelOpt = channelRepository.findById(channelId);
        if (channelOpt.isEmpty()) return false;

        Channel channel = channelOpt.get();
        Long serverId = channel.getServer().getId();

        Optional<Membership> membershipOpt = membershipRepository.findByServerIdAndUserId(serverId, userId);
        if (membershipOpt.isEmpty() || membershipOpt.get().isBanned()) {
            return false;
        }

        if (!channel.isPrivate()) {
            return true;
        }

        // Private channels accessible by OWNER and MODERATOR by default, or server members
        String role = membershipOpt.get().getRole().toUpperCase();
        return "OWNER".equalsIgnoreCase(role) || "MODERATOR".equalsIgnoreCase(role);
    }

    public boolean canAccessConversation(Long conversationId) {
        if (isPlatformAdmin()) return true;
        Long userId = getCurrentUserId();
        if (userId == null || conversationId == null) return false;

        return conversationRepository.findById(conversationId)
                .map(conv -> conv.getParticipants().stream().anyMatch(u -> u.getId().equals(userId)))
                .orElse(false);
    }
}
