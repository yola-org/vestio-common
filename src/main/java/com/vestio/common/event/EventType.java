package com.vestio.common.event;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventType {
    NOTIFICATION("notification"), CHAT("chat");

    private final String value;

    EventType(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }
}
