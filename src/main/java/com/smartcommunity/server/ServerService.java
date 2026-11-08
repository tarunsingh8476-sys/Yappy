package com.smartcommunity.server;

import com.smartcommunity.channel.Channel;
import com.smartcommunity.channel.ChannelRepository;
import com.smartcommunity.common.AppConstants;
import com.smartcommunity.common.PageResponse;
import com.smartcommunity.common.exception.ForbiddenException;
import com.smartcommunity.common.exception.ResourceNotFoundException;
import com.smartcommunity.membership.MembershipRepository;
import com.smartcommunity.membership.MembershipService;
import com.smartcommunity.message.MessageRepository;
import com.smartcommunity.moderation.ModerationAuditLogRepository;
import com.smartcommunity.moderation.ModerationRecordRepository;
import com.smartcommunity.server.dto.CreateServerRequest;
import com.smartcommunity.server.dto.ServerAnalyticsResponse;
import com.smartcommunity.server.dto.ServerResponse;
import com.smartcommunity.user.User;
import com.smartcommunity.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipService membershipService;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final ModerationRecordRepository moderationRecordRepository;
    private final ModerationAuditLogRepository auditLogRepository;

    @Transactional
    public ServerResponse createServer(Long userId, CreateServerRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Server server = Server.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .visibility(request.getVisibility().toUpperCase())
                .owner(owner)
                .build();

        Server savedServer = serverRepository.save(server);

        // Automatically assign creator as OWNER membership
        membershipService.addMember(savedServer, owner, AppConstants.SERVER_ROLE_OWNER);

        // Create default general channel
        Channel defaultChannel = Channel.builder()
                .server(savedServer)
                .name("general")
                .topic("General community discussions")
                .type("TEXT")
                .isPrivate(false)
                .build();
        channelRepository.save(defaultChannel);

        log.info("Server '{}' (id: {}) successfully created by user {}", savedServer.getName(), savedServer.getId(), owner.getUsername());
        return ServerResponse.from(savedServer, 1L);
    }

    @Transactional(readOnly = true)
    public PageResponse<ServerResponse> getPublicServers(Pageable pageable) {
        Page<Server> servers = serverRepository.findByVisibilityIgnoreCase("PUBLIC", pageable);
        return PageResponse.from(servers.map(s -> {
            long count = membershipRepository.countByServerIdAndBannedFalse(s.getId());
            return ServerResponse.from(s, count);
        }));
    }

    @Transactional(readOnly = true)
    public List<ServerResponse> getJoinedServers(Long userId) {
        List<Server> joinedServers = serverRepository.findAllJoinedServersByUserId(userId);
        return joinedServers.stream().map(s -> {
            long count = membershipRepository.countByServerIdAndBannedFalse(s.getId());
            return ServerResponse.from(s, count);
        }).toList();
    }

    @Transactional(readOnly = true)
    public ServerResponse getServerDetails(Long serverId, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));

        if ("PRIVATE".equalsIgnoreCase(server.getVisibility())) {
            membershipService.getMembership(serverId, userId);
        }

        long count = membershipRepository.countByServerIdAndBannedFalse(serverId);
        return ServerResponse.from(server, count);
    }

    @Transactional(readOnly = true)
    public ServerAnalyticsResponse getServerAnalytics(Long serverId, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));

        membershipService.verifyModeratorOrOwner(serverId, userId);

        long memberCount = membershipRepository.countByServerIdAndBannedFalse(serverId);
        long channelCount = channelRepository.countByServerId(serverId);

        // Pure Hibernate / Spring Data JPA queries
        long messageCount = messageRepository.countByChannelServerId(serverId);
        long pendingMods = moderationRecordRepository.countByMessageChannelServerIdAndReviewStatus(serverId, "PENDING");
        long modActions = auditLogRepository.countByServerId(serverId);

        return ServerAnalyticsResponse.builder()
                .serverId(serverId)
                .serverName(server.getName())
                .totalMembers(memberCount)
                .totalChannels(channelCount)
                .totalMessages(messageCount)
                .pendingModerations(pendingMods)
                .totalModerationActions(modActions)
                .build();
    }

    @Transactional
    public void deleteServer(Long serverId, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));

        if (!server.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the server owner can delete this server.");
        }

        serverRepository.delete(server);
        log.info("Server {} deleted by owner {}", serverId, userId);
    }
}
