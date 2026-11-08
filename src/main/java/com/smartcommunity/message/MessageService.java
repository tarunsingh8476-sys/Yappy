package com.smartcommunity.message;

import com.smartcommunity.channel.Channel;
import com.smartcommunity.channel.ChannelRepository;
import com.smartcommunity.common.PageResponse;
import com.smartcommunity.common.exception.BadRequestException;
import com.smartcommunity.common.exception.ForbiddenException;
import com.smartcommunity.common.exception.ResourceNotFoundException;
import com.smartcommunity.conversation.Conversation;
import com.smartcommunity.conversation.ConversationRepository;
import com.smartcommunity.membership.Membership;
import com.smartcommunity.membership.MembershipRepository;
import com.smartcommunity.membership.MembershipService;
import com.smartcommunity.message.dto.MessageResponse;
import com.smartcommunity.message.dto.MessageSearchCriteria;
import com.smartcommunity.message.dto.SendMessageRequest;
import com.smartcommunity.moderation.ModerationService;
import com.smartcommunity.security.ServerSecurityService;
import com.smartcommunity.user.User;
import com.smartcommunity.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ConversationRepository conversationRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipService membershipService;
    private final ModerationService moderationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageResponse sendMessage(Long userId, SendMessageRequest request) {
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (sender.isSuspended()) {
            throw new ForbiddenException("Suspended accounts cannot post messages.");
        }

        Channel channel = null;
        Conversation conversation = null;

        if (request.getChannelId() != null) {
            channel = channelRepository.findById(request.getChannelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", request.getChannelId()));

            Membership membership = membershipService.getMembership(channel.getServer().getId(), userId);
            if (membership.isMuted()) {
                if (membership.getMutedUntil() != null && membership.getMutedUntil().isAfter(Instant.now())) {
                    throw new ForbiddenException("You are muted in this community until " + membership.getMutedUntil());
                } else if (membership.getMutedUntil() != null) {
                    membership.setMuted(false);
                    membership.setMutedUntil(null);
                    membershipRepository.save(membership);
                } else {
                    throw new ForbiddenException("You are indefinitely muted in this community.");
                }
            }
        } else if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", request.getConversationId()));

            boolean isParticipant = conversation.getParticipants().stream().anyMatch(u -> u.getId().equals(userId));
            if (!isParticipant) {
                throw new ForbiddenException("You are not a member of this private conversation.");
            }
        } else {
            throw new BadRequestException("Either channelId or conversationId must be supplied.");
        }

        Message replyTo = null;
        if (request.getReplyToId() != null) {
            replyTo = messageRepository.findById(request.getReplyToId()).orElse(null);
        }

        Message message = Message.builder()
                .channel(channel)
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent().trim())
                .status("PUBLISHED")
                .pinned(false)
                .edited(false)
                .replyTo(replyTo)
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentType(request.getAttachmentType())
                .attachmentName(request.getAttachmentName())
                .attachmentSize(request.getAttachmentSize())
                .build();

        Message saved = messageRepository.save(message);

        // Perform AI Content Moderation
        moderationService.evaluateAndModerate(saved);

        MessageResponse response = MessageResponse.from(saved);

        // Broadcast real-time message via STOMP WebSocket
        if (channel != null) {
            messagingTemplate.convertAndSend("/topic/channel." + channel.getId(), response);
        } else if (conversation != null) {
            messagingTemplate.convertAndSend("/topic/conversation." + conversation.getId(), response);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getChannelMessages(Long channelId, Long userId, Pageable pageable) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        membershipService.verifyServerMember(channel.getServer().getId(), userId);

        Page<Message> messages = messageRepository.findByChannelId(channelId, pageable);
        return PageResponse.from(messages.map(MessageResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getConversationMessages(Long conversationId, Long userId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        boolean isParticipant = conversation.getParticipants().stream().anyMatch(u -> u.getId().equals(userId));
        if (!isParticipant) {
            throw new ForbiddenException("You are not a participant in this conversation.");
        }

        Page<Message> messages = messageRepository.findByConversationId(conversationId, pageable);
        return PageResponse.from(messages.map(MessageResponse::from));
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getPinnedMessages(Long channelId, Long userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        membershipService.verifyServerMember(channel.getServer().getId(), userId);

        List<Message> pinned = messageRepository.findPinnedMessagesByChannelId(channelId);
        return pinned.stream().map(MessageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> searchMessages(MessageSearchCriteria criteria, Long userId, Pageable pageable) {
        if (criteria.getChannelId() != null) {
            Channel channel = channelRepository.findById(criteria.getChannelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", criteria.getChannelId()));
            membershipService.verifyServerMember(channel.getServer().getId(), userId);
        }

        Page<Message> results = messageRepository.findAll(MessageSpecification.withCriteria(criteria), pageable);
        return PageResponse.from(results.map(MessageResponse::from));
    }

    @Transactional
    public MessageResponse editMessage(Long messageId, Long userId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        if (!message.getSender().getId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own messages.");
        }

        message.setContent(newContent.trim());
        message.setEdited(true);

        // Re-evaluate updated text against AI moderation
        moderationService.evaluateAndModerate(message);

        Message updated = messageRepository.save(message);
        MessageResponse response = MessageResponse.from(updated);

        // Broadcast real-time edit event
        if (message.getChannel() != null) {
            messagingTemplate.convertAndSend("/topic/channel." + message.getChannel().getId() + ".edit", response);
        } else if (message.getConversation() != null) {
            messagingTemplate.convertAndSend("/topic/conversation." + message.getConversation().getId() + ".edit", response);
        }

        return response;
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        boolean isAuthor = message.getSender().getId().equals(userId);
        boolean isModerator = false;

        if (message.getChannel() != null) {
            Long serverId = message.getChannel().getServer().getId();
            isModerator = membershipRepository.findByServerIdAndUserId(serverId, userId)
                    .map(m -> "OWNER".equalsIgnoreCase(m.getRole()) || "MODERATOR".equalsIgnoreCase(m.getRole()))
                    .orElse(false);
        }

        if (!isAuthor && !isModerator) {
            throw new ForbiddenException("You do not have permission to delete this message.");
        }

        message.setStatus("REMOVED");
        messageRepository.save(message);

        // Broadcast deletion event
        Long channelId = message.getChannel() != null ? message.getChannel().getId() : null;
        if (channelId != null) {
            messagingTemplate.convertAndSend("/topic/channel." + channelId + ".delete", messageId);
        }
    }

    @Transactional
    public MessageResponse togglePin(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        if (message.getChannel() == null) {
            throw new BadRequestException("Only channel messages can be pinned.");
        }

        membershipService.verifyModeratorOrOwner(message.getChannel().getServer().getId(), userId);

        message.setPinned(!message.isPinned());
        Message saved = messageRepository.save(message);
        return MessageResponse.from(saved);
    }

    @Transactional
    public MessageResponse reactToMessage(Long messageId, Long userId, String emoji) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Optional<Reaction> existingReaction = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);

        if (existingReaction.isPresent()) {
            reactionRepository.delete(existingReaction.get());
            message.getReactions().remove(existingReaction.get());
        } else {
            Reaction reaction = Reaction.builder()
                    .message(message)
                    .user(user)
                    .emoji(emoji)
                    .build();
            reactionRepository.save(reaction);
            message.getReactions().add(reaction);
        }

        Message saved = messageRepository.save(message);
        MessageResponse response = MessageResponse.from(saved);

        if (message.getChannel() != null) {
            messagingTemplate.convertAndSend("/topic/channel." + message.getChannel().getId() + ".reaction", response);
        }

        return response;
    }
}
