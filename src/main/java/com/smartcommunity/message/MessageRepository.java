package com.smartcommunity.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {

    @Query("SELECT m FROM Message m WHERE m.channel.id = :channelId AND m.status IN ('PUBLISHED', 'HELD_FOR_REVIEW') ORDER BY m.createdAt DESC")
    Page<Message> findByChannelId(@Param("channelId") Long channelId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.status = 'PUBLISHED' ORDER BY m.createdAt DESC")
    Page<Message> findByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.channel.id = :channelId AND m.pinned = true ORDER BY m.createdAt DESC")
    List<Message> findPinnedMessagesByChannelId(@Param("channelId") Long channelId);

    long countByChannelServerId(Long serverId);

    @Query("SELECT m FROM Message m WHERE m.channel.id = :channelId " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND (:senderId IS NULL OR m.sender.id = :senderId) " +
           "AND (:startDate IS NULL OR m.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR m.createdAt <= :endDate) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> searchMessages(
            @Param("channelId") Long channelId,
            @Param("query") String query,
            @Param("senderId") Long senderId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );
}
