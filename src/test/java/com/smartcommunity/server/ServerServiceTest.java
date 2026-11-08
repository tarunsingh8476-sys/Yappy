package com.smartcommunity.server;

import com.smartcommunity.channel.Channel;
import com.smartcommunity.channel.ChannelRepository;
import com.smartcommunity.common.AppConstants;
import com.smartcommunity.common.exception.ForbiddenException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerServiceTest {

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ModerationRecordRepository moderationRecordRepository;

    @Mock
    private ModerationAuditLogRepository auditLogRepository;

    @InjectMocks
    private ServerService serverService;

    private User owner;
    private Server server;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").email("owner@test.com").build();
        server = Server.builder().id(10L).name("Test Server").visibility("PUBLIC").owner(owner).build();
    }

    @Test
    @DisplayName("Should create server, assign owner, and create default general channel")
    void createServer_Success() {
        CreateServerRequest request = CreateServerRequest.builder()
                .name("Test Server")
                .description("Test Description")
                .visibility("PUBLIC")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(serverRepository.save(any(Server.class))).thenReturn(server);

        ServerResponse response = serverService.createServer(1L, request);

        assertNotNull(response);
        assertEquals("Test Server", response.getName());
        verify(membershipService).addMember(any(Server.class), eq(owner), eq(AppConstants.SERVER_ROLE_OWNER));
        verify(channelRepository).save(any(Channel.class));
    }

    @Test
    @DisplayName("Should query analytics using pure Hibernate repositories")
    void getServerAnalytics_Success() {
        when(serverRepository.findById(10L)).thenReturn(Optional.of(server));
        when(membershipRepository.countByServerIdAndBannedFalse(10L)).thenReturn(5L);
        when(channelRepository.countByServerId(10L)).thenReturn(2L);
        when(messageRepository.countByChannelServerId(10L)).thenReturn(40L);
        when(moderationRecordRepository.countByMessageChannelServerIdAndReviewStatus(10L, "PENDING")).thenReturn(3L);
        when(auditLogRepository.countByServerId(10L)).thenReturn(7L);

        ServerAnalyticsResponse analytics = serverService.getServerAnalytics(10L, 1L);

        assertNotNull(analytics);
        assertEquals(5L, analytics.getTotalMembers());
        assertEquals(2L, analytics.getTotalChannels());
        assertEquals(40L, analytics.getTotalMessages());
        assertEquals(3L, analytics.getPendingModerations());
        assertEquals(7L, analytics.getTotalModerationActions());
    }

    @Test
    @DisplayName("Should prevent non-owner from deleting server")
    void deleteServer_NotOwner_ThrowsForbidden() {
        when(serverRepository.findById(10L)).thenReturn(Optional.of(server));

        assertThrows(ForbiddenException.class, () -> serverService.deleteServer(10L, 999L));
    }
}
