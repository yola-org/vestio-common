package com.vestio.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vestio.common.event.payload.ListingNotificationPayload;
import com.vestio.common.event.payload.NotificationPayload;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NotificationPayloadTest {
    @Test
    void usesApplicationMapperConfigurationAndJsonPropertyNames() {
        var mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        NotificationPayload payload = new CustomPayload("hello", new Details("nested"), null);
        var map = payload.toMap(mapper);
        assertEquals("hello", map.get("custom_message"));
        assertEquals(Map.of("display_name", "nested"), map.get("details"));
        assertTrue(map.containsKey("optional_value"));
        assertNull(map.get("optional_value"));
        assertEquals(mapper.readTree(mapper.writeValueAsString(payload)), mapper.valueToTree(map));
    }

    @Test
    void supportsListingPayloadWithoutTitleAndKeepsListingCorrelationSeparate() {
        var mapper = JsonMapper.builder().build();
        assertEquals(Map.of(), new ListingNotificationPayload(null).toMap(mapper));
        assertEquals(Map.of("listingTitle", "Shirt"), new ListingNotificationPayload("Shirt").toMap(mapper));
    }

    private record CustomPayload(@JsonProperty("custom_message") String message, Details details, Object optionalValue)
            implements NotificationPayload {}
    private record Details(String displayName) {}
}
