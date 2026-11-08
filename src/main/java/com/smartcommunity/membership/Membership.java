package com.smartcommunity.membership;

import com.smartcommunity.server.Server;
import com.smartcommunity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"server_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String role = "MEMBER"; // OWNER, MODERATOR, MEMBER

    @Column(nullable = false)
    @Builder.Default
    private boolean banned = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean muted = false;

    private Instant mutedUntil;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant joinedAt;
}
