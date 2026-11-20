package com.vestio.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationOutbox {
    @Id
    @Column(name = "notification_id")
    private UUID id;

    // Optional correlation for listing workflows; generic notifications do not need it.
    @Column(name = "listing_id")
    private UUID listingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
