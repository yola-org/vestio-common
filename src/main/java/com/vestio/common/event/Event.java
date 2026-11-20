package com.vestio.common.event;

import com.vestio.common.entity.Notification;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Shared JSON envelope for notifications and future chat payloads. */
public record Event(UUID id, EventType type, Map<String, Object> payload, Instant occurredAt) {
    public Event {
        Objects.requireNonNull(id, "Event ID is required");
        Objects.requireNonNull(type, "Event type is required");
        payload = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payload)));
        Objects.requireNonNull(occurredAt, "Event timestamp is required");
    }

    public static Event notification(Notification notification, ObjectMapper mapper) {
        return new Event(notification.getId(), EventType.NOTIFICATION,
                mapper.convertValue(notification, new TypeReference<Map<String, Object>>() {}), Instant.now());
    }
}
