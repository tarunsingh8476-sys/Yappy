package com.smartcommunity.moderation;

import com.smartcommunity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "moderation_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderation_record_id")
    private ModerationRecord moderationRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    @Column(nullable = false)
    private Long targetUserId;

    @Column(nullable = false, length = 50)
    private String action; // DISMISS, WARN, REMOVE_MESSAGE, MUTE_USER, SUSPEND_USER

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
