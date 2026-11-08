package com.smartcommunity.moderation;

import com.smartcommunity.message.Message;
import com.smartcommunity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "moderation_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(nullable = false, length = 50)
    private String category; // e.g. toxic, vulgar, insult, threat, non-toxic

    @Column(nullable = false)
    private Double confidenceScore;

    @Column(length = 50)
    private String modelVersion;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String reviewStatus = "PENDING"; // PENDING, DISMISSED, WARNED, REMOVED, MUTED, SUSPENDED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Column(length = 500)
    private String reviewReason;

    private Instant reviewedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
