package com.vestio.events.entity;

import com.vestio.events.entity.enums.NotificationType;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEntityTest {
    @Test
    void persistsGenericNotificationAndOutboxWithoutListingId() {
        try (var factory = new Configuration()
                .addAnnotatedClass(Notification.class).addAnnotatedClass(NotificationOutbox.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:shared-entities;MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory()) {
            UUID userId = UUID.randomUUID();
            UUID[] notificationId = new UUID[1];
            factory.inTransaction(session -> {
                var notification = Notification.builder().userId(userId).type(NotificationType.OFFER_CREATED)
                        .params(Map.of("message", "An offer was received")).build();
                session.persist(notification);
                session.flush();
                assertNotNull(notification.getId());
                assertNotNull(notification.getCreatedAt());
                notificationId[0] = notification.getId();
                session.persist(NotificationOutbox.builder().id(notification.getId()).userId(userId)
                        .payload("{}").createdAt(LocalDateTime.now()).build());
            });
            factory.inTransaction(session -> {
                var notification = session.find(Notification.class, notificationId[0]);
                assertEquals(userId, notification.getUserId());
                assertEquals(NotificationType.OFFER_CREATED, notification.getType());
                assertEquals("An offer was received", notification.getParams().get("message"));
                assertFalse(notification.getParams().containsKey("listingId"));
                assertNull(notification.getReadAt());
                assertNull(session.find(NotificationOutbox.class, notificationId[0]).getListingId());
            });
        }
    }
}
