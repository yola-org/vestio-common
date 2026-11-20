package com.vestio.common.event;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public abstract class BaseEvent {

    private final UUID id = UUID.randomUUID();
    /** The event type. */
    private String eventType;
    /** The time the event occurred. */
    private Instant occurredAt;

    public BaseEvent() {
        this.occurredAt = Instant.now();
    }

}
