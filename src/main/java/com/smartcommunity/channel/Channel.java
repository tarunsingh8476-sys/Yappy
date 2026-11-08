package com.smartcommunity.channel;

import com.smartcommunity.server.Server;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String topic;

    @Column(length = 100)
    @Builder.Default
    private String category = "Text Channels";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String type = "TEXT"; // TEXT, VOICE

    @Column(nullable = false)
    @Builder.Default
    private boolean isPrivate = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
