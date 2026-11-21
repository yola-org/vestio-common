package com.vestio.common.event;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChatEventTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Instant timestamp = Instant.parse("2026-11-21T12:00:00.123456Z");

    @Test
    void roundTripsAChatSubtypeWithAConcreteMessagePayload() {
        ChatEvent.Payload payload = new ChatEvent.Payload(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Hello", false, null, null, timestamp);
        ChatEvent event = new ChatEvent(payload.id(), EventType.CHAT, payload, timestamp);
        Event<ChatEvent.Payload> envelope = event;
        assertEquals(payload.chatId(), envelope.payload().chatId());
        JsonNode json = mapper.valueToTree(event);
        assertEquals("chat", json.path("type").asString());
        assertEquals(payload.id().toString(), json.path("id").asString());
        assertEquals(payload.chatId().toString(), json.path("payload").path("chatId").asString());
        assertEquals(payload.senderId().toString(), json.path("payload").path("senderId").asString());
        assertEquals("Hello", json.path("payload").path("content").asString());
        assertFalse(json.path("payload").path("system").asBoolean());
        assertEquals(timestamp.toString(), json.path("occurredAt").asString());
        assertEquals(timestamp.toString(), json.path("payload").path("createdAt").asString());
        assertEquals(event, mapper.treeToValue(json, ChatEvent.class));
    }

    @Test
    void supportsTypedSystemMessagesAndCopiesTheirParameters() {
        Map<String, Object> params = new HashMap<>(Map.of("ownerName", "Alex", "buyerName", "Sara",
                "listingTitle", "Shirt", "amount", 125.5, "currency", "MAD"));
        ChatEvent.Payload payload = new ChatEvent.Payload(UUID.randomUUID(), UUID.randomUUID(), null, null,
                true, "offer-accepted", params, timestamp);
        params.put("ownerName", "Changed");
        assertEquals("Alex", payload.params().get("ownerName"));
        assertThrows(UnsupportedOperationException.class, () -> payload.params().put("ownerName", "Changed"));
        ChatEvent event = new ChatEvent(payload);
        ChatEvent restored = mapper.readValue(mapper.writeValueAsString(event), ChatEvent.class);
        assertEquals(payload, restored.payload());
        assertEquals(payload.id(), restored.id());
        assertEquals(EventType.CHAT, restored.type());
        assertNull(restored.payload().senderId());
        assertEquals("offer-accepted", restored.payload().systemType());
    }

    @Test
    void rejectsWrongEnvelopeTypeIdsAndInvalidMessageShapes() {
        ChatEvent.Payload payload = new ChatEvent.Payload(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Hello", false, null, null, timestamp);
        assertThrows(IllegalArgumentException.class, () -> new ChatEvent(payload.id(), EventType.NOTIFICATION, payload, timestamp));
        assertThrows(IllegalArgumentException.class, () -> new ChatEvent(UUID.randomUUID(), EventType.CHAT, payload, timestamp));
        assertThrows(NullPointerException.class, () -> new ChatEvent.Payload(UUID.randomUUID(), UUID.randomUUID(), null,
                "Hello", false, null, null, timestamp));
        assertThrows(IllegalArgumentException.class, () -> new ChatEvent.Payload(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                " ", false, null, null, timestamp));
        assertThrows(IllegalArgumentException.class, () -> new ChatEvent.Payload(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Hello", false, "offer-accepted", Map.of(), timestamp));
        assertThrows(IllegalArgumentException.class, () -> new ChatEvent.Payload(UUID.randomUUID(), UUID.randomUUID(), null,
                null, true, "offer-accepted", null, timestamp));
        JsonNode wrongType = mapper.readTree(mapper.writeValueAsString(new ChatEvent(payload)).replace("\"type\":\"chat\"", "\"type\":\"notification\""));
        assertThrows(Exception.class, () -> mapper.treeToValue(wrongType, ChatEvent.class));
    }
}
