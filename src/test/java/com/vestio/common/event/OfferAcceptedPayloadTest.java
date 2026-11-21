package com.vestio.common.event;

import com.vestio.common.entity.enums.NotificationType;
import com.vestio.common.event.payload.OfferAcceptedPayload;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class OfferAcceptedPayloadTest {
    @Test
    void preservesNamesListingAndChatCorrelationInTheWireContract() {
        var mapper = JsonMapper.builder().build();
        var payload = new OfferAcceptedPayload(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("125.50"), "MAD", "Alex Owner", "Sara Buyer", "Shirt");
        var map = payload.toMap(mapper);
        assertEquals(payload.offerId().toString(), map.get("offerId"));
        assertEquals(payload.chatId().toString(), map.get("chatId"));
        assertEquals("Alex Owner", map.get("ownerName"));
        assertEquals("Sara Buyer", map.get("buyerName"));
        assertEquals("Shirt", map.get("listingTitle"));
        assertEquals("MAD", map.get("currency"));
        assertEquals(0, new BigDecimal(map.get("amount").toString()).compareTo(payload.amount()));
        assertEquals(payload, mapper.convertValue(map, OfferAcceptedPayload.class));
        assertEquals("\"offer-accepted\"", mapper.writeValueAsString(NotificationType.OFFER_ACCEPTED));
        assertEquals(NotificationType.OFFER_ACCEPTED, mapper.readValue("\"offer-accepted\"", NotificationType.class));
    }
}
