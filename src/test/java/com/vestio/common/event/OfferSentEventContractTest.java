package com.vestio.common.event;

import com.vestio.common.entity.Notification;
import com.vestio.common.entity.enums.NotificationType;
import com.vestio.common.event.payload.OfferSentPayload;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class OfferSentEventContractTest {
    @Test
    void serializesOwnerNotificationWithTheCanonicalOfferPayload() throws Exception {
        var mapper = JsonMapper.builder().build();
        try (var input = getClass().getResourceAsStream("/contracts/offer-sent-event.json")) {
            assertNotNull(input);
            var expected = mapper.readTree(input);
            var event = mapper.treeToValue(expected, Event.class);
            var notification = mapper.convertValue(event.payload(), Notification.class);
            var payload = mapper.convertValue(notification.getPayload(), OfferSentPayload.class);
            assertEquals(NotificationType.OFFER_SENT, notification.getType());
            assertEquals(new BigDecimal("125.5"), payload.amount());
            assertNotEquals(notification.getUserId(), payload.senderId());
            assertNotNull(notification.getListingId());
            assertEquals(event.id(), notification.getId());
            assertEquals(expected.path("payload").path("payload"), mapper.readTree(mapper.writeValueAsString(payload.toMap(mapper))));
            assertEquals(expected.path("payload"), mapper.valueToTree(Event.notification(notification, mapper)).path("payload"));
            assertEquals(expected, mapper.valueToTree(event));
        }
    }
}
