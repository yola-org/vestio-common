package com.vestio.common.entity;

import com.vestio.common.entity.enums.NotificationType;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEntityTest {
    @Test
    void persistsNotificationWithOptionalListingId() {
        try (var factory = new Configuration()
                .addAnnotatedClass(Notification.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:shared-entities;MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory()) {
            UUID userId = UUID.randomUUID();
            Instant readAt = Instant.parse("2026-11-01T05:30:00.123456Z");
            Instant before = Instant.now().minusSeconds(1);
            UUID[] notificationId = new UUID[1];
            factory.inTransaction(session -> {
                var notification = Notification.builder().userId(userId).type(NotificationType.OFFER_SENT)
                        .payload(Map.of("message", "An offer was received")).readAt(readAt).build();
                session.persist(notification);
                session.flush();
                assertNotNull(notification.getId());
                assertNotNull(notification.getCreatedAt());
                notificationId[0] = notification.getId();
            });
            factory.inTransaction(session -> {
                var notification = session.find(Notification.class, notificationId[0]);
                assertEquals(userId, notification.getUserId());
                assertEquals(NotificationType.OFFER_SENT, notification.getType());
                assertEquals("An offer was received", notification.getPayload().get("message"));
                assertFalse(notification.getPayload().containsKey("listingId"));
                assertEquals(readAt, notification.getReadAt());
                assertTrue(notification.getCreatedAt().isAfter(before));
                assertTrue(notification.getCreatedAt().isBefore(Instant.now().plusSeconds(1)));
                assertNull(notification.getListingId());
            });
        }
    }
}
