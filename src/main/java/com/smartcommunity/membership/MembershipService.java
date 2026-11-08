package com.smartcommunity.membership;

import com.smartcommunity.common.AppConstants;
import com.smartcommunity.common.PageResponse;
import com.smartcommunity.common.exception.BadRequestException;
import com.smartcommunity.common.exception.ForbiddenException;
import com.smartcommunity.common.exception.ResourceNotFoundException;
import com.smartcommunity.membership.dto.MemberResponse;
import com.smartcommunity.server.Server;
import com.smartcommunity.server.ServerRepository;
import com.smartcommunity.user.User;
import com.smartcommunity.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final ServerRepository serverRepository;
    private final UserRepository userRepository;

    @Transactional
    public Membership addMember(Server server, User user, String role) {
        return membershipRepository.findByServerIdAndUserId(server.getId(), user.getId())
                .map(existing -> {
                    if (existing.isBanned()) {
                        throw new ForbiddenException("User is banned from this server.");
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Membership membership = Membership.builder()
                            .server(server)
                            .user(user)
                            .role(role)
                            .banned(false)
                            .muted(false)
                            .build();
                    return membershipRepository.save(membership);
                });
    }

    @Transactional
    public MemberResponse joinServer(Long serverId, String inviteCode, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if ("PRIVATE".equalsIgnoreCase(server.getVisibility())) {
            if (!StringUtils.hasText(inviteCode) || !server.getInviteCode().equals(inviteCode.trim())) {
                throw new ForbiddenException("A valid invite code is required to join this private server.");
            }
        }

        Membership membership = addMember(server, user, AppConstants.SERVER_ROLE_MEMBER);
        log.info("User {} joined server '{}' (id: {})", user.getUsername(), server.getName(), serverId);
        return MemberResponse.from(membership);
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> listMembers(Long serverId, Long userId, Pageable pageable) {
        verifyServerMember(serverId, userId);
        Page<Membership> members = membershipRepository.findByServerIdAndBannedFalse(serverId, pageable);
        return PageResponse.from(members.map(MemberResponse::from));
    }

    @Transactional
    public MemberResponse updateMemberRole(Long serverId, Long targetUserId, String newRole, Long actorUserId) {
        verifyModeratorOrOwner(serverId, actorUserId);

        Membership target = membershipRepository.findByServerIdAndUserId(serverId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership", "targetUserId", targetUserId));

        // Prevent privilege escalation: only OWNER can grant OWNER or MODERATOR role
        Membership actor = getMembership(serverId, actorUserId);
        if ("OWNER".equalsIgnoreCase(newRole) && !AppConstants.SERVER_ROLE_OWNER.equalsIgnoreCase(actor.getRole())) {
            throw new ForbiddenException("Only the server owner can transfer ownership.");
        }

        target.setRole(newRole.toUpperCase());
        Membership updated = membershipRepository.save(target);
        log.info("Member {} role updated to {} by user {} on server {}", targetUserId, newRole, actorUserId, serverId);
        return MemberResponse.from(updated);
    }

    @Transactional
    public MemberResponse muteMember(Long serverId, Long targetUserId, int durationMinutes, Long actorUserId) {
        verifyModeratorOrOwner(serverId, actorUserId);

        Membership target = membershipRepository.findByServerIdAndUserId(serverId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership", "targetUserId", targetUserId));

        target.setMuted(durationMinutes > 0);
        target.setMutedUntil(durationMinutes > 0 ? Instant.now().plus(durationMinutes, ChronoUnit.MINUTES) : null);

        Membership updated = membershipRepository.save(target);
        log.info("Member {} mute status updated to {} by actor {} on server {}", targetUserId, target.isMuted(), actorUserId, serverId);
        return MemberResponse.from(updated);
    }

    @Transactional
    public void banMember(Long serverId, Long targetUserId, Long actorUserId) {
        verifyModeratorOrOwner(serverId, actorUserId);

        Membership target = membershipRepository.findByServerIdAndUserId(serverId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership", "targetUserId", targetUserId));

        if (AppConstants.SERVER_ROLE_OWNER.equalsIgnoreCase(target.getRole())) {
            throw new BadRequestException("Cannot ban the server owner.");
        }

        target.setBanned(true);
        membershipRepository.save(target);
        log.info("Member {} banned by actor {} on server {}", targetUserId, actorUserId, serverId);
    }

    @Transactional
    public void leaveServer(Long serverId, Long userId) {
        Membership membership = getMembership(serverId, userId);
        if (AppConstants.SERVER_ROLE_OWNER.equalsIgnoreCase(membership.getRole())) {
            throw new BadRequestException("Server owner cannot leave. Transfer ownership or delete the server instead.");
        }
        membershipRepository.delete(membership);
        log.info("User {} left server {}", userId, serverId);
    }

    @Transactional(readOnly = true)
    public Membership getMembership(Long serverId, Long userId) {
        return membershipRepository.findByServerIdAndUserId(serverId, userId)
                .filter(m -> !m.isBanned())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this server."));
    }

    @Transactional(readOnly = true)
    public void verifyModeratorOrOwner(Long serverId, Long userId) {
        Membership membership = getMembership(serverId, userId);
        String role = membership.getRole();
        if (!AppConstants.SERVER_ROLE_OWNER.equalsIgnoreCase(role) &&
            !AppConstants.SERVER_ROLE_MODERATOR.equalsIgnoreCase(role)) {
            throw new ForbiddenException("Moderator or Owner privileges required.");
        }
    }

    @Transactional(readOnly = true)
    public void verifyServerMember(Long serverId, Long userId) {
        getMembership(serverId, userId);
    }
}
