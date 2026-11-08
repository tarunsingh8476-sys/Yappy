package com.smartcommunity.channel;

import com.smartcommunity.channel.dto.ChannelResponse;
import com.smartcommunity.channel.dto.CreateChannelRequest;
import com.smartcommunity.channel.dto.UpdateChannelRequest;
import com.smartcommunity.common.exception.ResourceNotFoundException;
import com.smartcommunity.membership.Membership;
import com.smartcommunity.membership.MembershipService;
import com.smartcommunity.security.ServerSecurityService;
import com.smartcommunity.server.Server;
import com.smartcommunity.server.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ServerRepository serverRepository;
    private final MembershipService membershipService;
    private final ServerSecurityService serverSecurity;

    @Transactional
    @CacheEvict(value = "channels", allEntries = true)
    public ChannelResponse createChannel(Long serverId, Long userId, CreateChannelRequest request) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));

        membershipService.verifyModeratorOrOwner(serverId, userId);

        Channel channel = Channel.builder()
                .server(server)
                .name(request.getName().trim().toLowerCase().replaceAll("\\s+", "-"))
                .topic(request.getTopic())
                .category(request.getCategory() != null ? request.getCategory() : "Text Channels")
                .type(request.getType() != null ? request.getType().toUpperCase() : "TEXT")
                .isPrivate(request.isPrivate())
                .build();

        Channel saved = channelRepository.save(channel);
        log.info("Channel '{}' created in server {} by user {}", saved.getName(), serverId, userId);
        return ChannelResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "channels", key = "#serverId + '-' + #userId")
    public List<ChannelResponse> getChannelsForServer(Long serverId, Long userId) {
        membershipService.verifyServerMember(serverId, userId);
        Membership membership = membershipService.getMembership(serverId, userId);
        boolean isStaff = "OWNER".equalsIgnoreCase(membership.getRole()) || "MODERATOR".equalsIgnoreCase(membership.getRole());

        List<Channel> channels = channelRepository.findByServerIdOrderByNameAsc(serverId);
        return channels.stream()
                .filter(ch -> !ch.isPrivate() || isStaff)
                .map(ChannelResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChannelResponse getChannelById(Long channelId, Long userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        membershipService.verifyServerMember(channel.getServer().getId(), userId);

        if (channel.isPrivate()) {
            Membership membership = membershipService.getMembership(channel.getServer().getId(), userId);
            boolean isStaff = "OWNER".equalsIgnoreCase(membership.getRole()) || "MODERATOR".equalsIgnoreCase(membership.getRole());
            if (!isStaff) {
                throw new ResourceNotFoundException("Channel", "id", channelId);
            }
        }

        return ChannelResponse.from(channel);
    }

    @Transactional
    @CacheEvict(value = "channels", allEntries = true)
    public ChannelResponse updateChannel(Long channelId, Long userId, UpdateChannelRequest request) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        membershipService.verifyModeratorOrOwner(channel.getServer().getId(), userId);

        if (request.getName() != null) {
            channel.setName(request.getName().trim().toLowerCase().replaceAll("\\s+", "-"));
        }
        if (request.getTopic() != null) {
            channel.setTopic(request.getTopic());
        }
        if (request.getCategory() != null) {
            channel.setCategory(request.getCategory());
        }
        if (request.getIsPrivate() != null) {
            channel.setPrivate(request.getIsPrivate());
        }

        Channel updated = channelRepository.save(channel);
        return ChannelResponse.from(updated);
    }

    @Transactional
    @CacheEvict(value = "channels", allEntries = true)
    public void deleteChannel(Long channelId, Long userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        membershipService.verifyModeratorOrOwner(channel.getServer().getId(), userId);
        channelRepository.delete(channel);
        log.info("Channel {} deleted by user {}", channelId, userId);
    }
}
