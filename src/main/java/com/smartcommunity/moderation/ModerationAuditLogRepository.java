package com.smartcommunity.moderation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModerationAuditLogRepository extends JpaRepository<ModerationAuditLog, Long> {

    Page<ModerationAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ModerationAuditLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    @Query("SELECT count(mal) FROM ModerationAuditLog mal " +
           "JOIN mal.moderationRecord mr " +
           "JOIN mr.message m " +
           "JOIN m.channel c " +
           "WHERE c.server.id = :serverId")
    long countByServerId(@Param("serverId") Long serverId);
}
