package com.vestio.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vestio.common.entity.Notification;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Shared JSON envelope with a statically typed payload. */
public class Event<T> {
    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("type")
    private final EventType type;
    @JsonProperty("payload")
    private final T payload;
    @JsonProperty("occurredAt")
    private final Instant occurredAt;

    @JsonCreator
    public Event(@JsonProperty("id") UUID id, @JsonProperty("type") EventType type,
                 @JsonProperty("payload") T payload, @JsonProperty("occurredAt") Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "Event ID is required");
        this.type = Objects.requireNonNull(type, "Event type is required");
        this.payload = Objects.requireNonNull(payload, "Event payload is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Event timestamp is required");
    }

    public final UUID id() { return id; }
    public final EventType type() { return type; }
    public final T payload() { return payload; }
    public final Instant occurredAt() { return occurredAt; }

    public static Event<Map<String, Object>> notification(Notification notification, ObjectMapper mapper) {
        Map<String, Object> payload = mapper.convertValue(notification, new TypeReference<Map<String, Object>>() {});
        return new Event<>(notification.getId(), EventType.NOTIFICATION,
                Collections.unmodifiableMap(new LinkedHashMap<>(payload)), Instant.now());
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Event<?> event = (Event<?>) other;
        return id.equals(event.id) && type == event.type && payload.equals(event.payload) && occurredAt.equals(event.occurredAt);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id, type, payload, occurredAt);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id + ", type=" + type + ", payload=" + payload + ", occurredAt=" + occurredAt + "]";
    }
}
