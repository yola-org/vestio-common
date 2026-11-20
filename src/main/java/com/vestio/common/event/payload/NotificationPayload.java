package com.vestio.common.event.payload;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;

/** Common contract for typed notification payloads; conversion uses the application's mapper. */
public interface NotificationPayload {
    default Map<String, Object> toMap(ObjectMapper mapper) {
        return Objects.requireNonNull(mapper, "ObjectMapper is required")
                .convertValue(this, new TypeReference<Map<String, Object>>() {});
    }
}
