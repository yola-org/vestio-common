package com.vestio.common.event;

import com.vestio.common.entity.Notification;
import com.vestio.common.entity.enums.NotificationType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class NotificationEventContractTest {
    @Test
    void serializesUtcInstantsAndRejectsAmbiguousLocalTimestamps() {
        var mapper = JsonMapper.builder().build();
        var createdAt = java.time.Instant.parse("2026-11-01T05:30:00.123456Z");
        var readAt = java.time.Instant.parse("2026-11-01T06:30:00.123456Z");
        var notification = Notification.builder().id(java.util.UUID.randomUUID())
                .userId(java.util.UUID.randomUUID()).type(NotificationType.OFFER_SENT)
                .payload(java.util.Map.of()).createdAt(createdAt).readAt(readAt).build();
        var json = mapper.valueToTree(Event.notification(notification, mapper));
        assertEquals(createdAt.toString(), json.path("payload").path("createdAt").asString());
        assertEquals(readAt.toString(), json.path("payload").path("readAt").asString());
        var restored = mapper.treeToValue(json.path("payload"), Notification.class);
        assertEquals(createdAt, restored.getCreatedAt());
        assertEquals(readAt, restored.getReadAt());
        assertThrows(Exception.class, () -> mapper.readValue(
                "{\"createdAt\":\"2026-11-01T01:30:00\"}", Notification.class));
    }

    @Test
    void roundTripsNotificationEnvelopeWithoutListingId() throws Exception {
        var mapper = JsonMapper.builder().build();
        try (var input = getClass().getResourceAsStream("/contracts/notification-event.json")) {
            assertNotNull(input);
            var expected = mapper.readTree(input);
            var event = mapper.treeToValue(expected, Event.class);
            assertEquals(EventType.NOTIFICATION, event.type());
            var notification = mapper.convertValue(event.payload(), Notification.class);
            assertNull(notification.getListingId());
            assertEquals(NotificationType.OFFER_SENT, notification.getType());
            assertEquals(notification.getId(), event.id());
            assertEquals(expected, mapper.valueToTree(event));
        }
    }
}
