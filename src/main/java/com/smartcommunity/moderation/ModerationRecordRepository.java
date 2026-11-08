package com.smartcommunity.moderation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModerationRecordRepository extends JpaRepository<ModerationRecord, Long> {

    Optional<ModerationRecord> findByMessageId(Long messageId);

    long countByMessageChannelServerIdAndReviewStatus(Long serverId, String reviewStatus);

    @Query("SELECT mr FROM ModerationRecord mr " +
           "JOIN mr.message m " +
           "LEFT JOIN m.channel c " +
           "WHERE (:serverId IS NULL OR c.server.id = :serverId) " +
           "AND (:status IS NULL OR mr.reviewStatus = :status) " +
           "ORDER BY mr.createdAt DESC")
    Page<ModerationRecord> findPendingReviewQueue(
            @Param("serverId") Long serverId,
            @Param("status") String status,
            Pageable pageable
    );
}
