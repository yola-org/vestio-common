package com.vestio.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** A chat event carrying the saved message rather than an arbitrary payload map. */
public final class ChatEvent extends Event<ChatEvent.Payload> {
    public ChatEvent(Payload payload) {
        this(Objects.requireNonNull(payload, "Chat payload is required").id(), EventType.CHAT, payload, Instant.now());
    }

    @JsonCreator
    public ChatEvent(@JsonProperty("id") UUID id, @JsonProperty("type") EventType type,
                     @JsonProperty("payload") Payload payload, @JsonProperty("occurredAt") Instant occurredAt) {
        super(id, type, payload, occurredAt);
        if (type != EventType.CHAT) throw new IllegalArgumentException("A chat event must have type chat");
        if (!id.equals(payload.id())) throw new IllegalArgumentException("Chat event and message IDs must match");
    }

    /** The nested record is static; each event has its own immutable message payload. */
    public record Payload(UUID id, UUID chatId, UUID senderId, String content, boolean system,
                          String systemType, Map<String, Object> params, Instant createdAt) {
        public Payload {
            Objects.requireNonNull(id, "Message ID is required");
            Objects.requireNonNull(chatId, "Chat ID is required");
            Objects.requireNonNull(createdAt, "Message timestamp is required");
            if (system) {
                if (systemType == null || systemType.isBlank() || params == null) {
                    throw new IllegalArgumentException("A system message requires a system type and params");
                }
            } else {
                Objects.requireNonNull(senderId, "A regular message requires a sender");
                if (content == null || content.isBlank()) throw new IllegalArgumentException("A regular message requires content");
                if (systemType != null || params != null) throw new IllegalArgumentException("Only system messages may carry system metadata");
            }
            if (params != null) params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
        }
    }
}
