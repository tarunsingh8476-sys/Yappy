package com.smartcommunity.conversation;

import com.smartcommunity.common.PageResponse;
import com.smartcommunity.common.exception.ForbiddenException;
import com.smartcommunity.common.exception.ResourceNotFoundException;
import com.smartcommunity.conversation.dto.ConversationResponse;
import com.smartcommunity.conversation.dto.CreateConversationRequest;
import com.smartcommunity.user.User;
import com.smartcommunity.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Transactional
    public ConversationResponse getOrCreateConversation(Long currentUserId, CreateConversationRequest request) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        // For direct 1:1 conversation, reuse existing if available
        if (!request.isGroup() && request.getParticipantUserIds().size() == 1) {
            Long otherUserId = request.getParticipantUserIds().get(0);
            var existing = conversationRepository.findDirectConversationBetween(currentUserId, otherUserId);
            if (existing.isPresent()) {
                return ConversationResponse.from(existing.get());
            }
        }

        Set<User> participants = new HashSet<>();
        participants.add(currentUser);

        List<User> others = userRepository.findAllById(request.getParticipantUserIds());
        participants.addAll(others);

        Conversation conversation = Conversation.builder()
                .name(request.getName())
                .isGroup(request.isGroup())
                .participants(participants)
                .build();

        Conversation saved = conversationRepository.save(conversation);
        log.info("Conversation created with id: {}, group: {}, participantCount: {}",
                saved.getId(), saved.isGroup(), participants.size());
        return ConversationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> getUserConversations(Long userId, Pageable pageable) {
        Page<Conversation> page = conversationRepository.findUserConversations(userId, pageable);
        return PageResponse.from(page.map(ConversationResponse::from));
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversationDetails(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        boolean isMember = conversation.getParticipants().stream().anyMatch(u -> u.getId().equals(userId));
        if (!isMember) {
            throw new ForbiddenException("You are not a participant in this conversation.");
        }

        return ConversationResponse.from(conversation);
    }
}
